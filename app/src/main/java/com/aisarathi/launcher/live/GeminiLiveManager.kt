package com.aisarathi.launcher.live

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import com.aisarathi.launcher.media.MediaControllerHelper
import com.aisarathi.launcher.model.ConversationMode
import com.aisarathi.launcher.speech.SystemTtsManager
import com.aisarathi.launcher.utils.AppLauncherHelper
import com.aisarathi.launcher.utils.PreferenceManager
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

enum class MicState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING
}

class GeminiLiveManager(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
    private val prefs: PreferenceManager,
    private val tts: SystemTtsManager,
    private val mediaController: MediaControllerHelper,
    private val onModeChanged: (ConversationMode) -> Unit
) {
    companion object {
        private const val TAG = "GeminiLiveManager"
        private const val HOST = "generativelanguage.googleapis.com"
        private const val PATH = "/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent"
        private const val MODEL_NAME = "models/gemini-2.0-flash-exp"
    }

    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .connectTimeout(15, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var audioRecordManager: AudioRecordManager? = null
    private val audioTrackManager = AudioTrackManager()

    private val _micState = MutableStateFlow(MicState.IDLE)
    val micState: StateFlow<MicState> = _micState.asStateFlow()

    private val _liveTranscript = MutableStateFlow("")
    val liveTranscript: StateFlow<String> = _liveTranscript.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var isSessionActive = false
    private var quotaErrorCount = 0
    private var usageTrackingJob: Job? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    init {
        audioRecordManager = AudioRecordManager { pcmBytes ->
            sendAudioChunk(pcmBytes)
        }
    }

    fun startSession() {
        val apiKey = prefs.activeApiKey
        if (apiKey.isBlank()) {
            _errorMessage.value = "Please enter Gemini API Key in Settings"
            tts.speak("Please enter your Gemini API key in Settings")
            return
        }

        if (isSessionActive) {
            return
        }

        isSessionActive = true
        _micState.value = MicState.LISTENING
        audioTrackManager.init()

        connectWebSocket(apiKey)
        startUsageTracking()
    }

    fun stopSession() {
        isSessionActive = false
        usageTrackingJob?.cancel()
        usageTrackingJob = null

        audioRecordManager?.stopRecording()
        audioTrackManager.stopAndFlush()

        try {
            webSocket?.close(1000, "User stopped session")
        } catch (e: Exception) {
            // Ignored
        }
        webSocket = null
        _micState.value = MicState.IDLE
    }

    private fun connectWebSocket(apiKey: String) {
        val url = "wss://$HOST$PATH?key=$apiKey"
        val request = Request.Builder().url(url).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket Connected successfully")
                quotaErrorCount = 0
                _errorMessage.value = null

                // Send setup message
                val setupJson = buildSetupJson()
                ws.send(setupJson)

                // Start recording from mic
                mainHandler.post {
                    audioRecordManager?.startRecording(coroutineScope)
                    _micState.value = MicState.LISTENING
                }
            }

            override fun onMessage(ws: WebSocket, text: String) {
                handleServerMessage(ws, text)
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket Failure: ${t.message}", t)
                handleConnectionFailure(t.message ?: "Connection error", response?.code)
            }

            override fun onClosing(ws: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket Closing: code=$code, reason=$reason")
                ws.close(1000, null)
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket Closed")
                if (isSessionActive) {
                    // Transparent silent reconnect
                    coroutineScope.launch(Dispatchers.IO) {
                        delay(1000)
                        if (isSessionActive) {
                            connectWebSocket(prefs.activeApiKey)
                        }
                    }
                }
            }
        })
    }

    private fun handleServerMessage(ws: WebSocket, json: String) {
        try {
            val response = gson.fromJson(json, ServerResponse::class.java)

            // 1. Check if model interrupted
            if (response.serverContent?.interrupted == true) {
                audioTrackManager.stopAndFlush()
                _micState.value = MicState.LISTENING
            }

            // 2. Play incoming audio chunks
            response.serverContent?.modelTurn?.parts?.forEach { part ->
                // Update transcript if available
                part.text?.let { text ->
                    _liveTranscript.value = text
                }

                part.inlineData?.let { inline ->
                    if (inline.data != null && inline.mimeType?.contains("audio") == true) {
                        val pcm = Base64.decode(inline.data, Base64.DEFAULT)
                        _micState.value = MicState.SPEAKING
                        audioTrackManager.playChunk(pcm)
                    }
                }
            }

            if (response.serverContent?.turnComplete == true) {
                _micState.value = MicState.LISTENING
            }

            // 3. Handle Tool Calls
            response.toolCall?.functionCalls?.forEach { toolCall ->
                executeToolCall(ws, toolCall)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error parsing server message", e)
        }
    }

    private fun executeToolCall(ws: WebSocket, call: FunctionCall) {
        _micState.value = MicState.THINKING
        val name = call.name
        val args = call.args
        var resultOutput = "done"

        when (name) {
            "openApp" -> {
                val appName = args["appName"]?.asString ?: ""
                resultOutput = executeOpenApp(appName)
            }
            "navigate" -> {
                val dest = args["destination"]?.asString ?: ""
                val actualDest = when (dest.lowercase().trim()) {
                    "home" -> prefs.homeAddress.ifBlank { "Home" }
                    "office", "work" -> prefs.officeAddress.ifBlank { "Office" }
                    else -> dest
                }
                mainHandler.post {
                    AppLauncherHelper.openNavigation(context, actualDest)
                }
                resultOutput = "Started navigation to $actualDest"
            }
            "searchNearby" -> {
                val query = args["query"]?.asString ?: ""
                mainHandler.post {
                    AppLauncherHelper.searchNearby(context, query)
                }
                resultOutput = "Searching nearby for $query"
            }
            "playMusic" -> {
                val query = args["query"]?.asString ?: ""
                val service = args["service"]?.asString ?: prefs.defaultMusicApp
                mainHandler.post {
                    mediaController.playMusicWithVerification(service, query)
                }
                resultOutput = "Playing $query on $service"
            }
            "makeCall" -> {
                val contact = args["contactName"]?.asString ?: ""
                mainHandler.post {
                    tts.speak("Calling $contact") {
                        AppLauncherHelper.makeCall(context, contact)
                    }
                }
                resultOutput = "Calling $contact"
            }
            "switchMode" -> {
                val modeStr = args["mode"]?.asString ?: "Command"
                val mode = ConversationMode.fromString(modeStr)
                prefs.conversationMode = mode
                mainHandler.post {
                    onModeChanged(mode)
                }
                resultOutput = "Switched to ${mode.displayName} mode"
            }
        }

        // Send Tool Response back to Gemini
        val toolResponse = ToolResponseMessage(
            toolResponse = ToolResponsePayload(
                functionResponses = listOf(
                    FunctionResponse(
                        response = mapOf("result" to resultOutput),
                        id = call.id
                    )
                )
            )
        )
        ws.send(gson.toJson(toolResponse))
    }

    private fun executeOpenApp(appName: String): String {
        val lower = appName.lowercase().trim()
        val pkg = when {
            lower.contains("spotify") -> "com.spotify.music"
            lower.contains("yt music") || lower.contains("youtube music") -> "com.google.android.apps.youtube.music"
            lower.contains("youtube") -> "com.google.android.youtube"
            lower.contains("map") -> "com.google.android.apps.maps"
            lower.contains("whatsapp") -> "com.whatsapp"
            else -> null
        }

        return if (pkg != null) {
            mainHandler.post { AppLauncherHelper.openApp(context, pkg) }
            "Opened $appName"
        } else {
            "Could not find app $appName"
        }
    }

    private fun sendAudioChunk(pcm: ByteArray) {
        if (!isSessionActive || webSocket == null) return
        val base64Audio = Base64.encodeToString(pcm, Base64.NO_WRAP)
        val msg = RealtimeInputMessage(
            realtimeInput = RealtimeInputPayload(
                mediaChunks = listOf(
                    MediaChunk(
                        mimeType = "audio/pcm;rate=16000",
                        data = base64Audio
                    )
                )
            )
        )
        webSocket?.send(gson.toJson(msg))
    }

    private fun handleConnectionFailure(message: String, statusCode: Int?) {
        mainHandler.post {
            audioRecordManager?.stopRecording()
            _micState.value = MicState.IDLE
        }

        val isQuotaError = statusCode == 429 || message.contains("quota", ignoreCase = true) ||
                message.contains("exhausted", ignoreCase = true)

        if (isQuotaError) {
            quotaErrorCount++
            if (quotaErrorCount == 1) {
                tts.speak("Gemini free quota limit reached. Retrying shortly.")
                coroutineScope.launch {
                    delay(5000)
                    if (isSessionActive) connectWebSocket(prefs.activeApiKey)
                }
            } else if (quotaErrorCount >= 2) {
                // Persistent quota error: offer Gemini app handoff
                tts.speak("Free quota exhausted. Would you like to switch to the Gemini app?")
                // If paid key available and enabled, switch to it
                if (prefs.isPaidKeyEnabled && prefs.paidApiKey.isNotBlank()) {
                    tts.speak("Switching to paid key backup.")
                    connectWebSocket(prefs.paidApiKey)
                }
            }
        } else {
            _errorMessage.value = message
        }
    }

    private fun startUsageTracking() {
        usageTrackingJob?.cancel()
        usageTrackingJob = coroutineScope.launch(Dispatchers.IO) {
            while (isSessionActive) {
                delay(10_000) // update every 10 seconds
                if (isSessionActive) {
                    prefs.addAudioUsageSeconds(10)
                }
            }
        }
    }

    private fun buildSetupJson(): String {
        val currentMode = prefs.conversationMode
        val modeInstruction = when (currentMode) {
            ConversationMode.COMMAND -> """
                You are in COMMAND MODE.
                - Keep all answers strictly to 1 or 2 short sentences.
                - When asked to perform an action (navigate, play music, call someone, open an app), immediately call the appropriate tool.
                - Always prioritize safety and speed.
            """.trimIndent()

            ConversationMode.TALK -> """
                You are in TALK MODE (A car driving companion for deep conversations).
                - Speak like an intelligent, articulate, warm travelling companion.
                - Have full, in-depth discussions on any topic (philosophy, news, history, science, life, tech).
                - Give detailed explanations, provide personal perspectives, and ask engaging follow-up questions.
                - DO NOT artificially shorten your responses. Talk naturally and keep the conversation alive.
                - When the user asks for a car action (navigate, call, music), execute the tool immediately and then resume conversation smoothly.
            """.trimIndent()

            ConversationMode.CASUAL_CHAT -> """
                You are in CASUAL CHAT MODE ("Gup-Shup" Mode).
                - Speak in friendly, humorous, relaxed Hinglish (Hindi + English mixed naturally).
                - Act like a fun road trip best friend: tell funny driving stories, light jokes, quirky trivia, and playful banter.
                - Keep the mood light and entertaining.
            """.trimIndent()
        }

        val systemPrompt = """
            You are "Sarathi" (AI Sarathi), a native Android hands-free car infotainment assistant running on a dashboard-mounted phone in India.
            
            Personality & Language:
            - Your wake word is "Sarathi".
            - Respond in whatever language the driver speaks: fluent Hindi, natural English, or Hinglish.
            - Driving safety is paramount. Be polite, attentive, and helpful.
            
            Current Conversation Mode:
            $modeInstruction
            
            Tools available:
            - openApp(appName): Open installed apps like Spotify, Maps, YouTube Music, YouTube, WhatsApp.
            - navigate(destination): Open Google Maps turn-by-turn navigation. Destination can be a place name, or "Home" / "Office".
            - searchNearby(query): Search nearby petrol pumps, hotels, parking, food on Google Maps.
            - playMusic(query, service): Play songs, singers, or playlists on Spotify or YouTube Music.
            - makeCall(contactName): Place a direct phone call to a contact.
            - switchMode(mode): Switch between "Command", "Talk", and "Casual Chat" modes.
        """.trimIndent()

        val setupReq = GeminiLiveSetupRequest(
            setup = SetupPayload(
                model = MODEL_NAME,
                generationConfig = GenerationConfig(
                    responseModalities = listOf("AUDIO"),
                    speechConfig = SpeechConfig(
                        voiceConfig = VoiceConfig(
                            prebuiltVoiceConfig = PrebuiltVoiceConfig(
                                voiceName = prefs.voiceName
                            )
                        )
                    )
                ),
                systemInstruction = SystemInstruction(
                    parts = listOf(TextPart(systemPrompt))
                ),
                tools = listOf(
                    ToolDefinition(
                        functionDeclarations = listOf(
                            FunctionDeclaration(
                                name = "openApp",
                                description = "Opens an installed Android application",
                                parameters = FunctionParameters(
                                    properties = mapOf(
                                        "appName" to PropertySchema("STRING", "App name: Maps, Spotify, YouTube Music, YouTube, WhatsApp, Phone")
                                    ),
                                    required = listOf("appName")
                                )
                            ),
                            FunctionDeclaration(
                                name = "navigate",
                                description = "Opens Google Maps navigation to a destination",
                                parameters = FunctionParameters(
                                    properties = mapOf(
                                        "destination" to PropertySchema("STRING", "Destination name or Home/Office")
                                    ),
                                    required = listOf("destination")
                                )
                            ),
                            FunctionDeclaration(
                                name = "searchNearby",
                                description = "Find places nearby on Google Maps (hotels, petrol pumps, parking)",
                                parameters = FunctionParameters(
                                    properties = mapOf(
                                        "query" to PropertySchema("STRING", "Query to search nearby")
                                    ),
                                    required = listOf("query")
                                )
                            ),
                            FunctionDeclaration(
                                name = "playMusic",
                                description = "Plays music on Spotify or YouTube Music",
                                parameters = FunctionParameters(
                                    properties = mapOf(
                                        "query" to PropertySchema("STRING", "Song name, artist, or genre"),
                                        "service" to PropertySchema("STRING", "spotify or ytmusic")
                                    ),
                                    required = listOf("query")
                                )
                            ),
                            FunctionDeclaration(
                                name = "makeCall",
                                description = "Initiates a phone call to a contact",
                                parameters = FunctionParameters(
                                    properties = mapOf(
                                        "contactName" to PropertySchema("STRING", "Contact name or phone number")
                                    ),
                                    required = listOf("contactName")
                                )
                            ),
                            FunctionDeclaration(
                                name = "switchMode",
                                description = "Changes conversational mode",
                                parameters = FunctionParameters(
                                    properties = mapOf(
                                        "mode" to PropertySchema("STRING", "Command, Talk, or Casual Chat")
                                    ),
                                    required = listOf("mode")
                                )
                            )
                        )
                    )
                )
            )
        )

        return gson.toJson(setupReq)
    }

    fun release() {
        stopSession()
        audioTrackManager.release()
    }
}
