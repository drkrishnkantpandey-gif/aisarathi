package com.aisarathi.launcher.utils

import android.content.Context
import android.content.SharedPreferences
import com.aisarathi.launcher.model.ConversationMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PreferenceManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("ai_sarathi_prefs", Context.MODE_PRIVATE)

    var geminiApiKey: String
        get() = prefs.getString("gemini_api_key", "") ?: ""
        set(value) = prefs.edit().putString("gemini_api_key", value.trim()).apply()

    var paidApiKey: String
        get() = prefs.getString("paid_api_key", "") ?: ""
        set(value) = prefs.edit().putString("paid_api_key", value.trim()).apply()

    var isPaidKeyEnabled: Boolean
        get() = prefs.getBoolean("is_paid_key_enabled", false)
        set(value) = prefs.edit().putBoolean("is_paid_key_enabled", value).apply()

    var defaultMusicApp: String
        get() = prefs.getString("default_music_app", "spotify") ?: "spotify"
        set(value) = prefs.edit().putString("default_music_app", value).apply()

    var homeAddress: String
        get() = prefs.getString("home_address", "") ?: ""
        set(value) = prefs.edit().putString("home_address", value.trim()).apply()

    var officeAddress: String
        get() = prefs.getString("office_address", "") ?: ""
        set(value) = prefs.edit().putString("office_address", value.trim()).apply()

    var voiceName: String
        get() = prefs.getString("voice_name", "Aoede") ?: "Aoede"
        set(value) = prefs.edit().putString("voice_name", value).apply()

    var voiceStyle: String
        get() = prefs.getString("voice_style", "Friendly") ?: "Friendly"
        set(value) = prefs.edit().putString("voice_style", value).apply()

    var voiceSpeed: Float
        get() = prefs.getFloat("voice_speed", 1.0f)
        set(value) = prefs.edit().putFloat("voice_speed", value).apply()

    var conversationMode: ConversationMode
        get() {
            val name = prefs.getString("conversation_mode", ConversationMode.COMMAND.name)
            return try {
                ConversationMode.valueOf(name ?: ConversationMode.COMMAND.name)
            } catch (e: Exception) {
                ConversationMode.COMMAND
            }
        }
        set(value) = prefs.edit().putString("conversation_mode", value.name).apply()

    // Daily Live Audio Usage in Seconds
    fun addAudioUsageSeconds(seconds: Long) {
        checkAndResetDailyUsage()
        val current = getDailyAudioSeconds()
        prefs.edit().putLong("daily_audio_seconds", current + seconds).apply()
    }

    fun getDailyAudioMinutes(): Int {
        checkAndResetDailyUsage()
        return (prefs.getLong("daily_audio_seconds", 0L) / 60).toInt()
    }

    private fun getDailyAudioSeconds(): Long {
        return prefs.getLong("daily_audio_seconds", 0L)
    }

    private fun checkAndResetDailyUsage() {
        val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val savedDate = prefs.getString("usage_date", "")
        if (savedDate != today) {
            prefs.edit()
                .putString("usage_date", today)
                .putLong("daily_audio_seconds", 0L)
                .apply()
        }
    }

    val activeApiKey: String
        get() {
            return if (isPaidKeyEnabled && paidApiKey.isNotBlank()) {
                paidApiKey
            } else {
                geminiApiKey
            }
        }
}
