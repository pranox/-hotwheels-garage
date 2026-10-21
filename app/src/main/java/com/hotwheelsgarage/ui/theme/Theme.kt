package com.hotwheelsgarage.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Pure-black (OLED) palette: true black so car photos pop and the screen draws almost no power,
// neutral near-black cards, and a Hot Wheels flame (orange → red) for primary actions.
// Every text color is at least 6:1 against the surfaces it sits on.

/** Background: true black. */
val Garage = Color(0xFF000000)
/** Cards and bars, lifted just off black. */
val Bay = Color(0xFF121212)
val BayHigh = Color(0xFF1E1E1E)
/** Content text, 18.7:1 on [Bay]. */
val Ink = Color(0xFFFFFFFF)
/** Secondary text, 9.7:1 on [Garage], 8.6:1 on [Bay]. */
val InkMuted = Color(0xFFB0B0B0)

/** Flame orange: primary actions only (scan, add to garage). 6.7:1 on [Garage]. */
val TrackOrange = Color(0xFFFF5A1F)
/** The scan button's flame, orange into red. Dark text on it stays at 5.5:1 or better. */
val FlameGradient = Brush.linearGradient(listOf(Color(0xFFFF8A00), Color(0xFFFF3D1F)))
/** Ownership and counts ("Owned ×2"). 8.2:1 on [Bay]. */
val CardBlue = Color(0xFF82AAFF)
/** Background for "you already own this" notes, in the ownership blue family. */
val OwnedContainer = Color(0xFF0F1E3D)
/** Treasure Hunt flame. */
val TreasureGreen = Color(0xFF48C774)
/** Super Treasure Hunt gold flame. */
val SuperGold = Color(0xFFE6B84A)
/** Your own star ratings: chrome, so they never read as a Super Treasure Hunt. */
val Chrome = Color(0xFFE6E6E6)
/** License-plate label behind toy and collector numbers. */
val Plate = Color(0xFF1F1F1F)

private val GarageColors = darkColorScheme(
    primary = TrackOrange,
    onPrimary = Color(0xFF1A0600),
    primaryContainer = Color(0xFF3D1A0A),
    onPrimaryContainer = Color(0xFFFFDBCC),
    // Selected chips and the selected tab get a dark flame tint instead of gray.
    secondaryContainer = Color(0xFF3D1A0A),
    onSecondaryContainer = Color(0xFFFFDBCC),
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
    surfaceContainerLowest = Garage,
    surfaceContainerLow = Color(0xFF0A0A0A),
    surfaceContainer = Bay,
    surfaceContainerHigh = BayHigh,
    surfaceContainerHighest = Color(0xFF262626),
    outline = Color(0xFF6E6E6E),
    outlineVariant = Color(0xFF2A2A2A),
    error = Color(0xFFFF7B7B),
    onError = Color(0xFF2A0606),
    errorContainer = Color(0xFF5C1A1A),
    onErrorContainer = Color(0xFFFFDAD6),
)

/**
 * The app is always dark: it's a showcase for car photos, and a black garage lets the cars stand out
 * (HIG dark-mode.md: "consider using only a dark appearance" for media-focused apps).
 */
@Composable
fun GarageTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = GarageColors, content = content)
}
