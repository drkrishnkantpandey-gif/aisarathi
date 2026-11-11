package com.aisarathi.launcher.media

import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CarNotificationListener : NotificationListenerService() {

    companion object {
        private val _isMusicPlaying = MutableStateFlow(false)
        val isMusicPlaying: StateFlow<Boolean> = _isMusicPlaying.asStateFlow()

        private val _lastActivePackage = MutableStateFlow<String?>(null)
        val lastActivePackage: StateFlow<String?> = _lastActivePackage.asStateFlow()

        fun notifyPlaybackStarted(packageName: String) {
            _isMusicPlaying.value = true
            _lastActivePackage.value = packageName
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        checkMediaNotification(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        val pkg = sbn?.packageName
        if (pkg == "com.spotify.music" || pkg == "com.google.android.apps.youtube.music") {
            _isMusicPlaying.value = false
        }
    }

    private fun checkMediaNotification(sbn: StatusBarNotification?) {
        sbn ?: return
        val pkg = sbn.packageName
        if (pkg == "com.spotify.music" || pkg == "com.google.android.apps.youtube.music") {
            val extras = sbn.notification?.extras
            val template = extras?.getString("android.template")
            val isMedia = template?.contains("MediaStyle") == true ||
                    extras?.containsKey("android.mediaSession") == true

            if (isMedia) {
                _isMusicPlaying.value = true
                _lastActivePackage.value = pkg
            }
        }
    }
}
