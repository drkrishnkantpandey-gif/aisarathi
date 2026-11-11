package com.aisarathi.launcher

import android.Manifest
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.aisarathi.launcher.live.GeminiLiveManager
import com.aisarathi.launcher.live.MicState
import com.aisarathi.launcher.media.MediaControllerHelper
import com.aisarathi.launcher.model.AppItem
import com.aisarathi.launcher.model.ConversationMode
import com.aisarathi.launcher.speech.SystemTtsManager
import com.aisarathi.launcher.speech.WakeWordListener
import com.aisarathi.launcher.ui.AISarathiTheme
import com.aisarathi.launcher.ui.AllAppsDialog
import com.aisarathi.launcher.ui.HomeScreen
import com.aisarathi.launcher.ui.SettingsDialog
import com.aisarathi.launcher.utils.AppLauncherHelper
import com.aisarathi.launcher.utils.BatteryNetworkMonitor
import com.aisarathi.launcher.utils.PreferenceManager
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    private lateinit var prefs: PreferenceManager
    private lateinit var batteryNetworkMonitor: BatteryNetworkMonitor
    private lateinit var tts: SystemTtsManager
    private lateinit var mediaController: MediaControllerHelper
    private lateinit var geminiLiveManager: GeminiLiveManager
    private lateinit var wakeWordListener: WakeWordListener

    private val localMicState = MutableStateFlow(MicState.IDLE)
    private val localTranscript = MutableStateFlow("")

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Permissions granted
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            // Force landscape orientation automatically as soon as app opens
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Keep screen awake for car dashboard
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Immersive car display (hide navigation bars)
        try {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
        } catch (e: Exception) {
            e.printStackTrace()
        }

        prefs = PreferenceManager(this)
        batteryNetworkMonitor = BatteryNetworkMonitor(this)
        tts = SystemTtsManager(this)

        mediaController = MediaControllerHelper(this, lifecycleScope) { sourceApp, targetApp, query ->
            val question = "$query was not found on $sourceApp. Should I search it in $targetApp?"
            tts.speak(question)
        }

        wakeWordListener = WakeWordListener(
            context = this,
            coroutineScope = lifecycleScope,
            onCommandRecognized = { command ->
                handleVoiceCommand(command)
            },
            onListeningStateChanged = { isListening ->
                localMicState.value = if (isListening) MicState.LISTENING else MicState.IDLE
            }
        )

        geminiLiveManager = GeminiLiveManager(
            context = this,
            coroutineScope = lifecycleScope,
            prefs = prefs,
            tts = tts,
            mediaController = mediaController,
            onNoApiKeyFallback = {
                // Seamlessly listen via device speech recognizer without demanding API key
                localMicState.value = MicState.LISTENING
                wakeWordListener.startSingleShot()
            },
            onModeChanged = { newMode ->
                prefs.conversationMode = newMode
            }
        )

        batteryNetworkMonitor.start()
        requestCarPermissions()

        setContent {
            AISarathiTheme {
                val liveMicState by geminiLiveManager.micState.collectAsState()
                val localState by localMicState.collectAsState()
                val currentMicState = if (liveMicState != MicState.IDLE) liveMicState else localState

                val liveTranscriptText by geminiLiveManager.liveTranscript.collectAsState()
                val localTranscriptText by localTranscript.collectAsState()
                val transcript = if (liveTranscriptText.isNotBlank()) liveTranscriptText else localTranscriptText

                val errorMessage by geminiLiveManager.errorMessage.collectAsState()
                val batteryStatus by batteryNetworkMonitor.batteryStatus.collectAsState()
                val networkStatus by batteryNetworkMonitor.networkStatus.collectAsState()

                var currentMode by remember { mutableStateOf(prefs.conversationMode) }
                var showAllApps by remember { mutableStateOf(false) }
                var showSettings by remember { mutableStateOf(false) }
                var allAppsList by remember { mutableStateOf<List<AppItem>>(emptyList()) }

                HomeScreen(
                    micState = currentMicState,
                    conversationMode = currentMode,
                    batteryStatus = batteryStatus,
                    networkStatus = networkStatus,
                    liveTranscript = transcript,
                    errorMessage = errorMessage,
                    onMicClick = {
                        if (currentMicState != MicState.IDLE) {
                            geminiLiveManager.stopSession()
                            wakeWordListener.stop()
                            localMicState.value = MicState.IDLE
                        } else {
                            if (prefs.activeApiKey.isNotBlank()) {
                                geminiLiveManager.startSession()
                            } else {
                                localTranscript.value = "Listening..."
                                localMicState.value = MicState.LISTENING
                                wakeWordListener.startSingleShot()
                            }
                        }
                    },
                    onModeClick = {
                        val nextMode = when (currentMode) {
                            ConversationMode.COMMAND -> ConversationMode.TALK
                            ConversationMode.TALK -> ConversationMode.CASUAL_CHAT
                            ConversationMode.CASUAL_CHAT -> ConversationMode.COMMAND
                        }
                        currentMode = nextMode
                        prefs.conversationMode = nextMode
                    },
                    onMapsClick = {
                        AppLauncherHelper.openApp(this@MainActivity, "com.google.android.apps.maps")
                    },
                    onSpotifyClick = {
                        AppLauncherHelper.openApp(this@MainActivity, "com.spotify.music")
                    },
                    onYtMusicClick = {
                        AppLauncherHelper.openApp(this@MainActivity, "com.google.android.apps.youtube.music")
                    },
                    onYouTubeClick = {
                        AppLauncherHelper.openYouTube(this@MainActivity)
                    },
                    onPhoneClick = {
                        val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        startActivity(dialIntent)
                    },
                    onWhatsAppClick = {
                        AppLauncherHelper.openApp(this@MainActivity, "com.whatsapp")
                    },
                    onAllAppsClick = {
                        allAppsList = AppLauncherHelper.getAllInstalledApps(this@MainActivity)
                        showAllApps = true
                    },
                    onSettingsClick = {
                        showSettings = true
                    },
                    onWifiSettingsClick = {
                        AppLauncherHelper.openWifiSettings(this@MainActivity)
                    }
                )

                if (showAllApps) {
                    AllAppsDialog(
                        apps = allAppsList,
                        onAppClick = { app ->
                            AppLauncherHelper.openApp(this@MainActivity, app.packageName)
                            showAllApps = false
                        },
                        onDismiss = { showAllApps = false }
                    )
                }

                if (showSettings) {
                    SettingsDialog(
                        prefs = prefs,
                        isNotificationGranted = mediaController.isNotificationAccessGranted(),
                        onOpenNotificationSettings = {
                            mediaController.openNotificationAccessSettings()
                        },
                        onDismiss = {
                            showSettings = false
                            currentMode = prefs.conversationMode
                        }
                    )
                }
            }
        }
    }

    private fun handleVoiceCommand(command: String) {
        localTranscript.value = command
        localMicState.value = MicState.IDLE
        val lower = command.lowercase().trim()

        when {
            // Mode changes
            lower.contains("talk mode") || lower.contains("baat karte hain") || lower.contains("let's talk") -> {
                prefs.conversationMode = ConversationMode.TALK
                tts.speak("Switched to Talk mode. Opening Gemini.") {
                    AppLauncherHelper.openGeminiApp(this@MainActivity)
                }
            }
            lower.contains("casual") || lower.contains("chat") || lower.contains("gup-shup") || lower.contains("gupshup") || lower.contains("gappe") -> {
                prefs.conversationMode = ConversationMode.CASUAL_CHAT
                tts.speak("Switched to Casual Chat mode. Opening Gemini.") {
                    AppLauncherHelper.openGeminiApp(this@MainActivity)
                }
            }
            lower.contains("command mode") -> {
                prefs.conversationMode = ConversationMode.COMMAND
                tts.speak("Command mode active.")
            }

            // General chat or questions -> Open Gemini official app (uses Google AI Pro subscription)
            lower.contains("gemini") || lower.startsWith("what") || lower.startsWith("how") ||
                    lower.startsWith("why") || lower.startsWith("tell me") || lower.startsWith("kya") ||
                    lower.startsWith("kaise") || lower.startsWith("batao") -> {
                tts.speak("Opening Gemini") {
                    AppLauncherHelper.openGeminiApp(this@MainActivity)
                }
            }

            // Navigation
            lower.contains("navigate") || lower.contains("take me to") || lower.contains("direction") ||
                    lower.contains("rasta") || lower.contains("chalo") || lower.contains("home") || lower.contains("office") -> {
                val dest = when {
                    lower.contains("home") -> prefs.homeAddress.ifBlank { "Home" }
                    lower.contains("office") || lower.contains("work") -> prefs.officeAddress.ifBlank { "Office" }
                    else -> lower.replace("navigate to", "")
                        .replace("take me to", "")
                        .replace("directions to", "")
                        .replace("chalo", "")
                        .replace("rasta", "").trim()
                }
                tts.speak("Navigating to $dest") {
                    AppLauncherHelper.openNavigation(this@MainActivity, dest)
                }
            }

            // Nearby Search
            lower.contains("nearby") || lower.contains("petrol") || lower.contains("hotel") ||
                    lower.contains("parking") || lower.contains("restaurant") || lower.contains("food") -> {
                tts.speak("Searching nearby") {
                    AppLauncherHelper.searchNearby(this@MainActivity, lower)
                }
            }

            // Music
            lower.contains("play") || lower.contains("gana") || lower.contains("song") || lower.contains("music") -> {
                val query = lower.replace("play", "")
                    .replace("on spotify", "")
                    .replace("on youtube music", "")
                    .replace("on yt music", "")
                    .replace("gana", "")
                    .replace("song", "").trim()

                val service = if (lower.contains("youtube") || lower.contains("yt")) "ytmusic" else prefs.defaultMusicApp
                tts.speak("Playing $query") {
                    mediaController.playMusicWithVerification(service, query)
                }
            }

            // Phone call
            lower.contains("call") || lower.contains("dial") || lower.contains("phone lagao") -> {
                val contact = lower.replace("call", "").replace("dial", "").replace("phone lagao", "").trim()
                tts.speak("Calling $contact") {
                    AppLauncherHelper.makeCall(this@MainActivity, contact)
                }
            }

            // Open Apps
            lower.contains("open") -> {
                when {
                    lower.contains("spotify") -> AppLauncherHelper.openApp(this@MainActivity, "com.spotify.music")
                    lower.contains("maps") -> AppLauncherHelper.openApp(this@MainActivity, "com.google.android.apps.maps")
                    lower.contains("youtube") -> AppLauncherHelper.openYouTube(this@MainActivity)
                    lower.contains("whatsapp") -> AppLauncherHelper.openApp(this@MainActivity, "com.whatsapp")
                    lower.contains("gemini") -> AppLauncherHelper.openGeminiApp(this@MainActivity)
                    else -> tts.speak("Opening app")
                }
            }

            else -> {
                // If query is not recognized, offer Gemini App
                tts.speak("Opening Gemini") {
                    AppLauncherHelper.openGeminiApp(this@MainActivity)
                }
            }
        }
    }

    private fun requestCarPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
        wakeWordListener.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        batteryNetworkMonitor.stop()
        wakeWordListener.stop()
        geminiLiveManager.release()
        tts.release()
    }
}
