package com.hotwheelsgarage.ui.stats

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hotwheelsgarage.HotWheelsApp
import com.hotwheelsgarage.data.Car
import com.hotwheelsgarage.ui.theme.SuperGold
import com.hotwheelsgarage.ui.theme.TreasureGreen
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class StatsViewModel(app: Application) : AndroidViewModel(app) {
    val cars = (app as HotWheelsApp).repository.cars.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(vm: StatsViewModel = viewModel()) {
    val all by vm.cars.collectAsStateWithLifecycle()
    val owned = remember(all) { all.filterNot { it.isWishlist } }
    val total = owned.sumOf { it.quantity }

    Scaffold(topBar = { TopAppBar(title = { Text("Stats", fontWeight = FontWeight.Bold) }) }) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Tile("Cars", total.toString(), Modifier.weight(1f))
                Tile("Castings", owned.mapNotNull { it.wikiTitle ?: it.name }.distinct().size.toString(), Modifier.weight(1f))
                Tile("Wishlist", all.count { it.isWishlist }.toString(), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Tile("Treasure Hunts", owned.filter { it.isTreasureHunt }.sumOf { it.quantity }.toString(), Modifier.weight(1f), TreasureGreen)
                Tile("Super THs", owned.filter { it.isSuperTreasureHunt }.sumOf { it.quantity }.toString(), Modifier.weight(1f), SuperGold)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Tile("Carded", owned.filter { it.condition == Car.CONDITION_CARDED }.sumOf { it.quantity }.toString(), Modifier.weight(1f))
                Tile("Loose", owned.filter { it.condition == Car.CONDITION_LOOSE }.sumOf { it.quantity }.toString(), Modifier.weight(1f))
                Tile("Spent", "%.2f".format(owned.sumOf { (it.pricePaid ?: 0.0) * it.quantity }), Modifier.weight(1f))
            }
            Breakdown("Top series", owned.groupingBy { seriesName(it.series) }.fold(0) { acc, c -> acc + c.quantity })
            Breakdown("By year", owned.groupingBy { it.year ?: "Unknown" }.fold(0) { acc, c -> acc + c.quantity })
            Breakdown("By color", owned.groupingBy { it.color?.substringBefore(" /")?.trim() ?: "Unknown" }.fold(0) { acc, c -> acc + c.quantity })
        }
    }
}

/** "HW Dream Garage 1/5" -> "HW Dream Garage". */
private fun seriesName(series: String?): String =
    series?.replace(Regex("\\s*\\d+/\\d+\\s*$"), "")?.trim()?.ifEmpty { null } ?: "Unknown"

@Composable
private fun Tile(label: String, value: String, modifier: Modifier, accent: Color = MaterialTheme.colorScheme.primary) {
    Card(modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = accent)
            Text(label, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun Breakdown(title: String, counts: Map<String, Int>) {
    if (counts.isEmpty()) return
    val top = counts.entries.sortedByDescending { it.value }.take(8)
    val max = top.first().value.coerceAtLeast(1)
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            top.forEach { (name, count) ->
                Row {
                    Text(name, Modifier.weight(0.45f), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                    Box(Modifier.weight(0.45f).height(16.dp)) {
                        Box(
                            Modifier
                                .fillMaxWidth(count / max.toFloat())
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primary),
                        )
                    }
                    Text(count.toString(), Modifier.weight(0.1f).padding(start = 6.dp), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
