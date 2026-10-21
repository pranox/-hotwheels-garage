package com.hotwheelsgarage.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Standard Material dark theme: #121212 base with lighter neutral surfaces for depth (no pure black,
// no pure white), one desaturated brand accent, and neutral selection states.
// Body text 15:1, secondary text 6.2:1 or better, borders 3.6:1 or better.

/** Base background. */
val Garage = Color(0xFF121212)
/** Cards and bars. */
val Bay = Color(0xFF1E1E1E)
val BayHigh = Color(0xFF262626)
/** Primary text: off-white, 15:1 on [Garage]. */
val Ink = Color(0xFFE6E6E6)
/** Secondary text, 7.7:1 on [Garage], 6.2:1 on [BayHigh]. */
val InkMuted = Color(0xFFA6A6A6)

/** Hot Wheels orange, toned down for dark surfaces: primary actions only. 8:1 on [Garage]. */
val TrackOrange = Color(0xFFFF8A4C)
/** Ownership and counts ("Owned ×2"). 7.9:1 on [Bay]. */
val CardBlue = Color(0xFF8AB4F8)
/** Background for "you already own this" notes. */
val OwnedContainer = Color(0xFF1F2A3D)
/** Treasure Hunt. */
val TreasureGreen = Color(0xFF6DD58C)
/** Super Treasure Hunt. */
val SuperGold = Color(0xFFF2C14E)
/** Collector-score stars. Neutral, so they never read as a Super Treasure Hunt. */
val Chrome = Color(0xFFE6E6E6)
/** License-plate label behind toy and collector numbers. */
val Plate = Color(0xFF2A2A2A)

private val GarageColors = darkColorScheme(
    primary = TrackOrange,
    onPrimary = Color(0xFF2B1000),
    primaryContainer = Color(0xFF5A2A12),
    onPrimaryContainer = Color(0xFFFFDBCB),
    // Selected chips and the selected tab: neutral, like Google's own apps.
    secondaryContainer = Color(0xFF383838),
    onSecondaryContainer = Ink,
    secondary = CardBlue,
    onSecondary = Garage,
    tertiary = SuperGold,
    onTertiary = Garage,
    background = Garage,
    onBackground = Ink,
    surface = Garage,
    onSurface = Ink,
    surfaceVariant = BayHigh,
    onSurfaceVariant = InkMuted,
    surfaceContainerLowest = Color(0xFF0D0D0D),
    surfaceContainerLow = Color(0xFF1A1A1A),
    surfaceContainer = Bay,
    surfaceContainerHigh = BayHigh,
    surfaceContainerHighest = Color(0xFF2E2E2E),
    // Plain neutral surfaces: no orange tint from tonal elevation.
    surfaceTint = Color.Transparent,
    outline = Color(0xFF767676),
    outlineVariant = Color(0xFF3A3A3A),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
)

/**
 * The app is always dark: it's a showcase for car photos, and a dark garage lets the cars stand out
 * (HIG dark-mode.md: "consider using only a dark appearance" for media-focused apps).
 */
@Composable
fun GarageTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = GarageColors, content = content)
}
