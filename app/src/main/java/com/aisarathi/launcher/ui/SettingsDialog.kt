package com.aisarathi.launcher.ui

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.aisarathi.launcher.utils.PreferenceManager

@Composable
fun SettingsDialog(
    prefs: PreferenceManager,
    isNotificationGranted: Boolean,
    onOpenNotificationSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = remember {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    }

    var apiKey by remember { mutableStateOf(prefs.geminiApiKey) }
    var paidApiKey by remember { mutableStateOf(prefs.paidApiKey) }
    var isPaidEnabled by remember { mutableStateOf(prefs.isPaidKeyEnabled) }
    var defaultMusic by remember { mutableStateOf(prefs.defaultMusicApp) }
    var homeAddress by remember { mutableStateOf(prefs.homeAddress) }
    var officeAddress by remember { mutableStateOf(prefs.officeAddress) }
    var voiceStyle by remember { mutableStateOf(prefs.voiceStyle) }
    var showAdvanced by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = CarBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, CarCardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sarathi Settings",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = CarTextPrimary
                    )

                    IconButton(
                        onClick = {
                            prefs.geminiApiKey = apiKey
                            prefs.paidApiKey = paidApiKey
                            prefs.isPaidKeyEnabled = isPaidEnabled
                            prefs.defaultMusicApp = defaultMusic
                            prefs.homeAddress = homeAddress
                            prefs.officeAddress = officeAddress
                            prefs.voiceStyle = voiceStyle
                            onDismiss()
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .background(CarCardBg, RoundedCornerShape(10.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Save and Close",
                            tint = CarTextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. Gemini Live API Key (Optional)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CarCardBg),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Gemini Live API Key (Optional for Real-time Streaming)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CarPrimary
                            )
                            Text(
                                text = "Leave blank to use Gemini App (Pro subscription) or offline commands",
                                fontSize = 10.sp,
                                color = CarTextSecondary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = apiKey,
                                    onValueChange = { apiKey = it },
                                    label = { Text("Paste Gemini Key (Optional)", fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = CarTextPrimary,
                                        unfocusedTextColor = CarTextPrimary
                                    )
                                )

                                Button(
                                    onClick = {
                                        val clip = clipboardManager?.primaryClip
                                        if (clip != null && clip.itemCount > 0) {
                                            apiKey = clip.getItemAt(0).text.toString().trim()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CarPrimary),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                                ) {
                                    Icon(Icons.Default.ContentPaste, contentDescription = "Paste", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Paste", fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    // 2. Default Music App
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CarCardBg),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Default Music Player",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CarPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                FilterChip(
                                    selected = defaultMusic == "spotify",
                                    onClick = { defaultMusic = "spotify" },
                                    label = { Text("Spotify (Default)", fontSize = 11.sp) }
                                )
                                FilterChip(
                                    selected = defaultMusic == "ytmusic",
                                    onClick = { defaultMusic = "ytmusic" },
                                    label = { Text("YouTube Music", fontSize = 11.sp) }
                                )
                            }
                        }
                    }

                    // 3. Saved Places
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CarCardBg),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Saved Places ('Take me Home' / 'Office')",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CarPrimary
                            )
                            OutlinedTextField(
                                value = homeAddress,
                                onValueChange = { homeAddress = it },
                                label = { Text("Home Address or Landmark", fontSize = 11.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = CarTextPrimary,
                                    unfocusedTextColor = CarTextPrimary
                                )
                            )
                            OutlinedTextField(
                                value = officeAddress,
                                onValueChange = { officeAddress = it },
                                label = { Text("Office Address or Landmark", fontSize = 11.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = CarTextPrimary,
                                    unfocusedTextColor = CarTextPrimary
                                )
                            )
                        }
                    }

                    // 4. Voice Style & Today's Usage
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CarCardBg),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Speaking Style",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CarPrimary
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("Friendly", "Calm", "Energetic").forEach { style ->
                                    FilterChip(
                                        selected = voiceStyle == style,
                                        onClick = { voiceStyle = style },
                                        label = { Text(style, fontSize = 11.sp) }
                                    )
                                }
                            }

                            Divider(color = CarCardBorder, modifier = Modifier.padding(vertical = 4.dp))

                            Text(
                                text = "Today's Live Audio Usage: ${prefs.getDailyAudioMinutes()} minutes",
                                fontSize = 11.sp,
                                color = CarAccentGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // 5. Media Playback Permission
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CarCardBg),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Music Detection Permission",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = CarTextPrimary
                                )
                                Text(
                                    text = if (isNotificationGranted) "Enabled (can verify if songs play)" else "Needed to verify if songs start playing",
                                    fontSize = 10.sp,
                                    color = if (isNotificationGranted) CarAccentGreen else CarAccentAmber
                                )
                            }
                            if (!isNotificationGranted) {
                                Button(
                                    onClick = onOpenNotificationSettings,
                                    colors = ButtonDefaults.buttonColors(containerColor = CarPrimary),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("Grant", fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    // 6. Advanced Section (Paid Key Fallback)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CarCardBg.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showAdvanced = !showAdvanced },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Security, contentDescription = "Advanced", tint = CarAccentAmber, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Advanced (Optional Paid Key Fallback)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = CarTextPrimary
                                    )
                                }
                                Text(
                                    text = if (showAdvanced) "Hide" else "Show",
                                    color = CarPrimary,
                                    fontSize = 11.sp
                                )
                            }

                            if (showAdvanced) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Enable Paid API Key Backup",
                                        fontSize = 11.sp,
                                        color = CarTextPrimary
                                    )
                                    Switch(
                                        checked = isPaidEnabled,
                                        onCheckedChange = { isPaidEnabled = it }
                                    )
                                }

                                if (isPaidEnabled) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    OutlinedTextField(
                                        value = paidApiKey,
                                        onValueChange = { paidApiKey = it },
                                        label = { Text("Optional Paid Gemini API Key", fontSize = 11.sp) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = CarTextPrimary,
                                            unfocusedTextColor = CarTextPrimary
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
