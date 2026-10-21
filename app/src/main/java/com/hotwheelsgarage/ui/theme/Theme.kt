package com.hotwheelsgarage.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Always-dark palette, drawn from the toys themselves: blister-card blue at night for surfaces,
// orange track for primary actions only, green/gold card flames for Treasure Hunt status.
// Every text color is at least 7:1 against the surfaces it sits on.

/** Background: deep blister-card navy. */
val Garage = Color(0xFF0E1320)
/** Cards and bars. */
val Bay = Color(0xFF171D2E)
val BayHigh = Color(0xFF20283C)
/** Softened white for content, 16.1:1 on [Garage]. */
val Ink = Color(0xFFECEFF5)
/** Secondary text, 8.6:1 on [Garage], 7.8:1 on [Bay]. */
val InkMuted = Color(0xFFA9B1C6)

/** Orange track: primary actions only (scan, add to garage). 7.1:1 on [Garage]. */
val TrackOrange = Color(0xFFFF7A1A)
/** Ownership and counts ("Owned ×2"). 7.9:1 on [Garage]. */
val CardBlue = Color(0xFF7FA8FF)
/** Treasure Hunt flame. */
val TreasureGreen = Color(0xFF48C774)
/** Super Treasure Hunt gold flame. */
val SuperGold = Color(0xFFE6B84A)
/** Your own star ratings: chrome, so they never read as a Super Treasure Hunt. */
val Chrome = Color(0xFFDCE1EC)
/** License-plate label behind toy and collector numbers. */
val Plate = Color(0xFF2A3349)

private val GarageColors = darkColorScheme(
    primary = TrackOrange,
    onPrimary = Color(0xFF1A0D00),
    primaryContainer = Color(0xFF4A2408),
    onPrimaryContainer = Color(0xFFFFDCC7),
    secondary = CardBlue,
    onSecondary = Garage,
    secondaryContainer = Color(0xFF243150),
    onSecondaryContainer = Color(0xFFDCE6FF),
    tertiary = SuperGold,
    onTertiary = Garage,
    background = Garage,
    onBackground = Ink,
    surface = Garage,
    onSurface = Ink,
    surfaceVariant = BayHigh,
    onSurfaceVariant = InkMuted,
    surfaceContainerLowest = Color(0xFF0A0E18),
    surfaceContainerLow = Color(0xFF131929),
    surfaceContainer = Bay,
    surfaceContainerHigh = BayHigh,
    surfaceContainerHighest = Color(0xFF283149),
    outline = Color(0xFF6B7591),
    outlineVariant = Color(0xFF2E3750),
    error = Color(0xFFFF7B7B),
    onError = Color(0xFF2A0606),
    errorContainer = Color(0xFF5C1A1A),
    onErrorContainer = Color(0xFFFFDAD6),
)

/**
 * The app is always dark: it's a showcase for car photos, and a dark garage lets the cars stand out
 * (HIG dark-mode.md: "consider using only a dark appearance" for media-focused apps).
 */
@Composable
fun GarageTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = GarageColors, content = content)
}
