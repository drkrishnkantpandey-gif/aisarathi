package com.aisarathi.launcher.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val CarBg = Color(0xFF0D0F14)
val CarCardBg = Color(0xFF181C24)
val CarCardBorder = Color(0xFF28303F)
val CarPrimary = Color(0xFF00E5FF)
val CarAccentGreen = Color(0xFF00E676)
val CarAccentAmber = Color(0xFFFFD600)
val CarAccentRed = Color(0xFFFF1744)
val CarTextPrimary = Color(0xFFFFFFFF)
val CarTextSecondary = Color(0xFFB0BEC5)

private val DarkColorScheme = darkColorScheme(
    primary = CarPrimary,
    background = CarBg,
    surface = CarCardBg,
    onPrimary = Color.Black,
    onBackground = CarTextPrimary,
    onSurface = CarTextPrimary
)

@Composable
fun AISarathiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
