package com.aisarathi.launcher

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
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

class MainActivity : ComponentActivity() {

    private lateinit var prefs: PreferenceManager
    private lateinit var batteryNetworkMonitor: BatteryNetworkMonitor
    private lateinit var tts: SystemTtsManager
    private lateinit var mediaController: MediaControllerHelper
    private lateinit var geminiLiveManager: GeminiLiveManager
    private lateinit var wakeWordListener: WakeWordListener

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Start wake word listener once audio permission is granted
        if (permissions[Manifest.permission.RECORD_AUDIO] == true) {
            wakeWordListener.start()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep screen awake for car dashboard
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        prefs = PreferenceManager(this)
        batteryNetworkMonitor = BatteryNetworkMonitor(this)
        tts = SystemTtsManager(this)

        mediaController = MediaControllerHelper(this, lifecycleScope) { sourceApp, targetApp, query ->
            // Song failed or not started verification prompt
            val question = "$query was not found on $sourceApp. Should I search it in $targetApp?"
            tts.speak(question)
        }

        geminiLiveManager = GeminiLiveManager(
            context = this,
            coroutineScope = lifecycleScope,
            prefs = prefs,
            tts = tts,
            mediaController = mediaController,
            onModeChanged = { newMode ->
                // Mode changed via voice command
            }
        )

        wakeWordListener = WakeWordListener(this, lifecycleScope) {
            // Wake word "Sarathi" detected
            tts.speak("Haanji, boliye") {
                geminiLiveManager.startSession()
            }
        }

        batteryNetworkMonitor.start()
        requestCarPermissions()

        setContent {
            AISarathiTheme {
                val micState by geminiLiveManager.micState.collectAsState()
                val liveTranscript by geminiLiveManager.liveTranscript.collectAsState()
                val errorMessage by geminiLiveManager.errorMessage.collectAsState()
                val batteryStatus by batteryNetworkMonitor.batteryStatus.collectAsState()
                val networkStatus by batteryNetworkMonitor.networkStatus.collectAsState()

                var currentMode by remember { mutableStateOf(prefs.conversationMode) }
                var showAllApps by remember { mutableStateOf(false) }
                var showSettings by remember { mutableStateOf(false) }
                var allAppsList by remember { mutableStateOf<List<AppItem>>(emptyList()) }

                HomeScreen(
                    micState = micState,
                    conversationMode = currentMode,
                    batteryStatus = batteryStatus,
                    networkStatus = networkStatus,
                    liveTranscript = liveTranscript,
                    errorMessage = errorMessage,
                    onMicClick = {
                        if (micState == MicState.IDLE) {
                            geminiLiveManager.startSession()
                        } else {
                            geminiLiveManager.stopSession()
                            wakeWordListener.start()
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
        if (geminiLiveManager.micState.value == MicState.IDLE) {
            wakeWordListener.start()
        }
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
