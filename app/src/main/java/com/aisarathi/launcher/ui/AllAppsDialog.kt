package com.aisarathi.launcher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.drawable.toBitmap
import com.aisarathi.launcher.model.AppItem

@Composable
fun AllAppsDialog(
    apps: List<AppItem>,
    onAppClick: (AppItem) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            shape = RoundedCornerShape(20.dp),
            color = CarBg,
            border = androidx.compose.foundation.BorderStroke(2.dp, CarCardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "All Installed Applications",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = CarTextPrimary
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(44.dp)
                            .background(CarCardBg, RoundedCornerShape(12.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = CarTextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Apps Grid
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 120.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(apps) { app ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(CarCardBg)
                                .border(1.dp, CarCardBorder, RoundedCornerShape(14.dp))
                                .clickable { onAppClick(app) }
                                .padding(14.dp)
                        ) {
                            app.icon?.let { drawable ->
                                val bitmap = drawable.toBitmap(96, 96)
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = app.label,
                                    modifier = Modifier.size(56.dp)
                                )
                            } ?: Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(CarPrimary.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = app.label,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CarTextPrimary,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
