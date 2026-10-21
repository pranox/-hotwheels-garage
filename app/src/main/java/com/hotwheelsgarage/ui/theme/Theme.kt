package com.hotwheelsgarage.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val FlameOrange = Color(0xFFFF6B00)
val TrackBlue = Color(0xFF1E88E5)
val TreasureGreen = Color(0xFF2E7D32)
val SuperGold = Color(0xFFC6930A)

private val Dark = darkColorScheme(
    primary = FlameOrange,
    onPrimary = Color.White,
    secondary = TrackBlue,
    onSecondary = Color.White,
    background = Color(0xFF121212),
    surface = Color(0xFF121212),
    surfaceVariant = Color(0xFF2A2A2A),
    surfaceContainer = Color(0xFF1E1E1E),
)

private val Light = lightColorScheme(
    primary = Color(0xFFE65100),
    onPrimary = Color.White,
    secondary = Color(0xFF1565C0),
    onSecondary = Color.White,
)

@Composable
fun GarageTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, content = content)
}
