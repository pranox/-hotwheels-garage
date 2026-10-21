package com.hotwheelsgarage.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.hotwheelsgarage.ui.theme.Chrome
import com.hotwheelsgarage.ui.theme.Garage
import com.hotwheelsgarage.ui.theme.Ink
import com.hotwheelsgarage.ui.theme.Plate
import com.hotwheelsgarage.ui.theme.SuperGold
import com.hotwheelsgarage.ui.theme.TreasureGreen

/** Image from a URL or File with a car placeholder while loading / on error. */
@Composable
fun CarImage(model: Any?, modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Crop) {
    Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
        if (model == null) {
            Placeholder()
        } else {
            SubcomposeAsyncImage(
                model = model,
                contentDescription = null,
                contentScale = contentScale,
                loading = { Placeholder() },
                error = { Placeholder() },
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}

@Composable
private fun Placeholder() {
    Icon(Icons.Filled.DirectionsCar, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
}

/** Small status pill. Dark text on the bright fill keeps it at 7.9:1 or better. */
@Composable
fun Badge(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text,
        color = Garage,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
fun HuntBadge(treasureHunt: Boolean, superTreasureHunt: Boolean, modifier: Modifier = Modifier) {
    when {
        superTreasureHunt -> Badge("STH", SuperGold, modifier)
        treasureHunt -> Badge("TH", TreasureGreen, modifier)
    }
}

/**
 * Toy and collector numbers on a little license plate – the app's one signature detail.
 * They're codes collectors read and compare, so they get a monospaced face.
 */
@Composable
fun PlateLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        color = Ink,
        fontSize = 12.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.5.sp,
        maxLines = 1,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Plate)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

/** 1–5 stars. Pass [onRate] to make it tappable; tapping the current rating clears it. */
@Composable
fun StarRating(rating: Int, modifier: Modifier = Modifier, size: Dp = 28.dp, onRate: ((Int) -> Unit)? = null) {
    val readOnly = onRate == null
    Row(
        modifier.then(
            if (readOnly) Modifier.clearAndSetSemantics { contentDescription = "Rated $rating of 5 stars" } else Modifier
        )
    ) {
        for (star in 1..5) {
            val icon = if (star <= rating) Icons.Filled.Star else Icons.Filled.StarBorder
            val tint = if (star <= rating) Chrome else MaterialTheme.colorScheme.outline
            if (readOnly) {
                Icon(icon, null, tint = tint, modifier = Modifier.size(size))
            } else {
                // 48dp touch target even when the star itself is smaller.
                IconButton(onClick = { onRate?.invoke(if (star == rating) 0 else star) }, modifier = Modifier.size(maxOf(size + 8.dp, 48.dp))) {
                    Icon(icon, if (star == rating) "Clear rating" else "Rate $star of 5", tint = tint, modifier = Modifier.size(size))
                }
            }
        }
    }
}
