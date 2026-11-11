package com.aisarathi.launcher.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aisarathi.launcher.live.MicState
import com.aisarathi.launcher.model.ConversationMode
import com.aisarathi.launcher.utils.BatteryStatus
import com.aisarathi.launcher.utils.NetworkStatus

@Composable
fun HomeScreen(
    micState: MicState,
    conversationMode: ConversationMode,
    batteryStatus: BatteryStatus,
    networkStatus: NetworkStatus,
    liveTranscript: String,
    errorMessage: String?,
    onMicClick: () -> Unit,
    onModeClick: () -> Unit,
    onMapsClick: () -> Unit,
    onSpotifyClick: () -> Unit,
    onYtMusicClick: () -> Unit,
    onYouTubeClick: () -> Unit,
    onPhoneClick: () -> Unit,
    onWhatsAppClick: () -> Unit,
    onAllAppsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onWifiSettingsClick: () -> Unit
) {
    Scaffold(
        topBar = {
            CarTopBar(
                micState = micState,
                conversationMode = conversationMode,
                batteryStatus = batteryStatus,
                networkStatus = networkStatus,
                onModeClick = onModeClick
            )
        },
        containerColor = CarBg
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Connection Card
                ConnectionCard(
                    networkStatus = networkStatus,
                    onWifiSettingsClick = onWifiSettingsClick
                )

                // Live Transcript / Status Banner
                AnimatedVisibility(
                    visible = liveTranscript.isNotBlank() || errorMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = if (errorMessage != null) CarAccentRed.copy(alpha = 0.2f) else CarCardBg,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (errorMessage != null) CarAccentRed else CarPrimary.copy(alpha = 0.4f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (errorMessage != null) Icons.Default.Warning else Icons.Default.GraphicEq,
                                contentDescription = "Status",
                                tint = if (errorMessage != null) CarAccentRed else CarPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = errorMessage ?: liveTranscript,
                                fontSize = 13.sp,
                                color = if (errorMessage != null) CarAccentRed else CarTextPrimary,
                                maxLines = 2
                            )
                        }
                    }
                }

                // 8 Large Action Buttons (2 Rows of 4 in Landscape)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Row 1
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        CarActionButton(
                            icon = Icons.Default.Navigation,
                            label = "Maps",
                            accentColor = Color(0xFF00E5FF),
                            onClick = onMapsClick,
                            modifier = Modifier.weight(1f)
                        )
                        CarActionButton(
                            icon = Icons.Default.MusicNote,
                            label = "Spotify",
                            accentColor = Color(0xFF1DB954),
                            onClick = onSpotifyClick,
                            modifier = Modifier.weight(1f)
                        )
                        CarActionButton(
                            icon = Icons.Default.PlayCircle,
                            label = "YT Music",
                            accentColor = Color(0xFFFF0033),
                            onClick = onYtMusicClick,
                            modifier = Modifier.weight(1f)
                        )
                        CarActionButton(
                            icon = Icons.Default.SmartDisplay,
                            label = "YouTube",
                            accentColor = Color(0xFFFF4444),
                            onClick = onYouTubeClick,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Row 2
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        CarActionButton(
                            icon = Icons.Default.Call,
                            label = "Phone",
                            accentColor = Color(0xFF00E676),
                            onClick = onPhoneClick,
                            modifier = Modifier.weight(1f)
                        )
                        CarActionButton(
                            icon = Icons.Default.Chat,
                            label = "WhatsApp",
                            accentColor = Color(0xFF25D366),
                            onClick = onWhatsAppClick,
                            modifier = Modifier.weight(1f)
                        )
                        CarActionButton(
                            icon = Icons.Default.Apps,
                            label = "All Apps",
                            accentColor = Color(0xFF7C4DFF),
                            onClick = onAllAppsClick,
                            modifier = Modifier.weight(1f)
                        )
                        CarActionButton(
                            icon = Icons.Default.Settings,
                            label = "Settings",
                            accentColor = Color(0xFFFFD600),
                            onClick = onSettingsClick,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Big Central Car Hands-Free Mic Button
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = (-4).dp)
            ) {
                val isBusy = micState == MicState.LISTENING || micState == MicState.SPEAKING || micState == MicState.THINKING
                val micBg = if (isBusy) {
                    Brush.radialGradient(listOf(CarPrimary, Color(0xFF007A99)))
                } else {
                    Brush.radialGradient(listOf(Color(0xFF2A3342), Color(0xFF181C24)))
                }

                Surface(
                    onClick = onMicClick,
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .border(
                            2.dp,
                            if (isBusy) CarPrimary else CarCardBorder,
                            CircleShape
                        ),
                    shape = CircleShape,
                    shadowElevation = 8.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(micBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (micState == MicState.SPEAKING) Icons.Default.VolumeUp else Icons.Default.Mic,
                            contentDescription = "Sarathi Mic",
                            tint = if (isBusy) Color.Black else CarPrimary,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }
            }
        }
    }
}
