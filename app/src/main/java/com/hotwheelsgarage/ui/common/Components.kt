package com.hotwheelsgarage.ui.common

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
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

@Composable
fun Badge(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text,
        color = Color.White,
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

val StarGold = Color(0xFFFFB300)

/** 1–5 stars. Pass [onRate] to make it tappable; tapping the current rating clears it. */
@Composable
fun StarRating(rating: Int, modifier: Modifier = Modifier, size: Dp = 32.dp, onRate: ((Int) -> Unit)? = null) {
    Row(modifier) {
        for (star in 1..5) {
            val icon = if (star <= rating) Icons.Filled.Star else Icons.Filled.StarBorder
            val tint = if (star <= rating) StarGold else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            if (onRate == null) {
                Icon(icon, null, tint = tint, modifier = Modifier.size(size))
            } else {
                IconButton(onClick = { onRate(if (star == rating) 0 else star) }, modifier = Modifier.size(size + 8.dp)) {
                    Icon(icon, "$star stars", tint = tint, modifier = Modifier.size(size))
                }
            }
        }
    }
}
