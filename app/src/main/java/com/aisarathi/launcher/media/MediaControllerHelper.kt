package com.aisarathi.launcher.media

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import com.aisarathi.launcher.utils.AppLauncherHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MediaControllerHelper(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
    private val onPromptFallback: (sourceApp: String, targetApp: String, query: String) -> Unit
) {

    fun isNotificationAccessGranted(): Boolean {
        val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(context)
        return enabledPackages.contains(context.packageName)
    }

    fun openNotificationAccessSettings() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun playMusicWithVerification(appChoice: String, query: String) {
        val isSpotify = appChoice.lowercase().contains("spotify")

        if (isSpotify) {
            AppLauncherHelper.playSpotifyMusic(context, query)
        } else {
            AppLauncherHelper.playYouTubeMusic(context, query)
        }

        // Only perform automated verification if notification listener permission is granted
        if (!isNotificationAccessGranted()) {
            return
        }

        coroutineScope.launch(Dispatchers.Default) {
            // Wait 5 seconds for playback to initiate
            delay(5000)

            val isPlaying = CarNotificationListener.isMusicPlaying.value
            if (!isPlaying) {
                val currentApp = if (isSpotify) "Spotify" else "YouTube Music"
                val alternateApp = if (isSpotify) "YouTube Music" else "Spotify"
                onPromptFallback(currentApp, alternateApp, query)
            }
        }
    }
}
