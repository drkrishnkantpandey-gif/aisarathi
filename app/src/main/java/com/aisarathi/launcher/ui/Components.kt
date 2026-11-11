package com.aisarathi.launcher.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aisarathi.launcher.live.MicState
import com.aisarathi.launcher.model.ConversationMode
import com.aisarathi.launcher.utils.BatteryStatus
import com.aisarathi.launcher.utils.NetworkStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CarTopBar(
    micState: MicState,
    conversationMode: ConversationMode,
    batteryStatus: BatteryStatus,
    networkStatus: NetworkStatus,
    onModeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentTime by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            val format = SimpleDateFormat("HH:mm", Locale.getDefault())
            currentTime = format.format(Date())
            kotlinx.coroutines.delay(1000)
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .background(CarCardBg.copy(alpha = 0.95f))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left: Digital Clock & Network
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = currentTime,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = CarTextPrimary
            )

            // Network Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Icon(
                    imageVector = if (networkStatus.typeName.contains("Wi-Fi")) Icons.Default.Wifi else Icons.Default.SignalCellularAlt,
                    contentDescription = "Network",
                    tint = if (networkStatus.isConnected) CarAccentGreen else CarAccentRed,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${networkStatus.typeName} (${networkStatus.detail})",
                    fontSize = 10.sp,
                    color = CarTextSecondary,
                    maxLines = 1
                )
            }
        }

        // Center: Mode Pill
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(CarPrimary.copy(alpha = 0.15f))
                .border(1.dp, CarPrimary.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                .clickable { onModeClick() }
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.RecordVoiceOver,
                contentDescription = "Mode",
                tint = CarPrimary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "${conversationMode.displayName} Mode",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = CarPrimary
            )
        }

        // Right: Mic State & Battery Status
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // Mic Indicator
            MicStatusIndicator(micState = micState)

            // Battery % & Temp
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Icon(
                    imageVector = if (batteryStatus.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryStd,
                    contentDescription = "Battery",
                    tint = if (batteryStatus.temperatureCelsius > 42f) CarAccentRed else CarAccentGreen,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${batteryStatus.percentage}% | ${batteryStatus.temperatureCelsius.toInt()}°C",
                    fontSize = 10.sp,
                    color = CarTextPrimary
                )
            }
        }
    }
}

@Composable
fun MicStatusIndicator(micState: MicState) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val (color, label) = when (micState) {
        MicState.LISTENING -> Pair(CarAccentGreen, "Listening")
        MicState.THINKING -> Pair(CarAccentAmber, "Thinking")
        MicState.SPEAKING -> Pair(CarPrimary, "Speaking")
        MicState.IDLE -> Pair(Color.Gray, "Mic Ready")
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .scale(if (micState != MicState.IDLE) pulseScale else 1f)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}

@Composable
fun CarActionButton(
    icon: ImageVector,
    label: String,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(82.dp),
        shape = RoundedCornerShape(14.dp),
        color = CarCardBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, CarCardBorder),
        shadowElevation = 3.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = CarTextPrimary,
                maxLines = 1
            )
        }
    }
}

@Composable
fun ConnectionCard(
    networkStatus: NetworkStatus,
    onWifiSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = CarCardBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, CarCardBorder),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (networkStatus.typeName.contains("Wi-Fi")) Icons.Default.Wifi else Icons.Default.Router,
                    contentDescription = "Connection",
                    tint = CarPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Connection: ${networkStatus.typeName}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CarTextPrimary
                    )
                    Text(
                        text = if (networkStatus.isConnected) "Status: Connected (${networkStatus.detail})" else "Status: No internet",
                        fontSize = 9.sp,
                        color = CarTextSecondary
                    )
                }
            }

            Button(
                onClick = onWifiSettingsClick,
                colors = ButtonDefaults.buttonColors(containerColor = CarPrimary.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Wi-Fi / Hotspot",
                    fontSize = 10.sp,
                    color = CarPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
