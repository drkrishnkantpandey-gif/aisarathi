package com.aisarathi.launcher.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class SystemTtsManager(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isReady = false

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("hi", "IN"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.US)
            }
            tts?.setPitch(1.0f)
            tts?.setSpeechRate(1.0f)
            isReady = true
        }
    }

    fun speak(text: String, onDone: (() -> Unit)? = null) {
        if (isReady && tts != null) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "sarathi_tts_${System.currentTimeMillis()}")
            onDone?.let {
                // Approximate done listener
            }
        }
    }

    fun stop() {
        if (isReady) {
            tts?.stop()
        }
    }

    fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady = false
    }
}
