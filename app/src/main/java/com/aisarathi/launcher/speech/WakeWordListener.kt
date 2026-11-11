package com.aisarathi.launcher.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

class WakeWordListener(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
    private val onWakeWordDetected: () -> Unit
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var shouldKeepListening = false
    private var restartJob: Job? = null

    private val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-IN")
        putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
        putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}

        override fun onError(error: Int) {
            isListening = false
            if (shouldKeepListening) {
                scheduleRestart(500)
            }
        }

        override fun onResults(results: Bundle?) {
            isListening = false
            handleMatches(results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION))
            if (shouldKeepListening) {
                scheduleRestart(200)
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            handleMatches(partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION))
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun handleMatches(matches: List<String>?) {
        matches ?: return
        for (text in matches) {
            val lower = text.lowercase().trim()
            if (lower.contains("sarathi") || lower.contains("saarthi") || lower.contains("sarthi") ||
                lower.contains("सारथी") || lower.contains("sarathy")
            ) {
                stop()
                onWakeWordDetected()
                break
            }
        }
    }

    fun start() {
        val hasAudio = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (!hasAudio) {
            Log.w("WakeWord", "RECORD_AUDIO permission not granted yet, skipping start")
            return
        }

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e("WakeWord", "SpeechRecognizer not available on device")
            return
        }
        shouldKeepListening = true
        initAndListen()
    }

    private fun initAndListen() {
        if (isListening) return

        try {
            if (speechRecognizer == null) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(listener)
                }
            }
            speechRecognizer?.startListening(recognizerIntent)
            isListening = true
        } catch (e: Exception) {
            e.printStackTrace()
            isListening = false
            if (shouldKeepListening) {
                scheduleRestart(1000)
            }
        }
    }

    private fun scheduleRestart(delayMs: Long) {
        restartJob?.cancel()
        restartJob = coroutineScope.launch(Dispatchers.Main) {
            delay(delayMs)
            if (shouldKeepListening && !isListening) {
                initAndListen()
            }
        }
    }

    fun stop() {
        shouldKeepListening = false
        restartJob?.cancel()
        restartJob = null
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            // Ignored
        }
        speechRecognizer = null
        isListening = false
    }
}
