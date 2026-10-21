package com.hotwheelsgarage.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
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
