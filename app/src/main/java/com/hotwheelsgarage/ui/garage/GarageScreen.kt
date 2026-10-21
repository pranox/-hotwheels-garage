package com.hotwheelsgarage.ui.garage

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hotwheelsgarage.data.Car
import com.hotwheelsgarage.ui.common.Badge
import com.hotwheelsgarage.ui.common.CarImage
import com.hotwheelsgarage.ui.common.HuntBadge
import java.io.File

enum class GarageFilter(val label: String) {
    Owned("Garage"), Wishlist("Wishlist"), Hunts("Treasure Hunts"), Carded("Carded"), Loose("Loose")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GarageScreen(
    onCar: (Long) -> Unit,
    onScan: () -> Unit,
    onSettings: () -> Unit,
    vm: GarageViewModel = viewModel(),
) {
    val cars by vm.cars.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(GarageFilter.Owned) }

    val shown = remember(cars, query, filter) {
        cars.orEmpty().filter { car ->
            val matchesFilter = when (filter) {
                GarageFilter.Owned -> !car.isWishlist
                GarageFilter.Wishlist -> car.isWishlist
                GarageFilter.Hunts -> !car.isWishlist && (car.isTreasureHunt || car.isSuperTreasureHunt)
                GarageFilter.Carded -> !car.isWishlist && car.condition == Car.CONDITION_CARDED
                GarageFilter.Loose -> !car.isWishlist && car.condition == Car.CONDITION_LOOSE
            }
            val q = query.trim()
            matchesFilter && (q.isEmpty() || listOfNotNull(car.name, car.series, car.year, car.color, car.toyNumber)
                .any { it.contains(q, ignoreCase = true) })
        }
    }
    val ownedCount = cars.orEmpty().filterNot { it.isWishlist }.sumOf { it.quantity }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("My Garage", fontWeight = FontWeight.Bold)
                        Text(if (ownedCount == 1) "1 car" else "$ownedCount cars", style = MaterialTheme.typography.bodySmall)
                    }
                },
                actions = { IconButton(onClick = onSettings) { Icon(Icons.Filled.Settings, "Settings") } },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onScan) { Icon(Icons.Filled.CameraAlt, "Scan a car") }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search name, series, year, color…") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                GarageFilter.entries.forEach { f ->
                    FilterChip(selected = filter == f, onClick = { filter = f }, label = { Text(f.label) })
                }
            }
            when {
                cars == null -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                shown.isEmpty() -> EmptyGarage(
                    message = if (cars.orEmpty().isEmpty()) "Your garage is empty." else "Nothing matches.",
                    showScan = cars.orEmpty().isEmpty(),
                    onScan = onScan,
                )
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(160.dp),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 88.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(shown, key = { it.id }) { car -> CarCard(car) { onCar(car.id) } }
                }
            }
        }
    }
}

@Composable
private fun EmptyGarage(message: String, showScan: Boolean, onScan: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(message, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        if (showScan) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Take a photo of a card back or the base of a car and it will be identified for you.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onScan) { Text("Scan your first car") }
        }
    }
}

@Composable
private fun CarCard(car: Car, onClick: () -> Unit) {
    Card(onClick = onClick) {
        Box {
            CarImage(
                // Official photo looks best in the grid; your own photo is shown on the detail screen.
                model = car.imageUrl ?: car.photoPath?.let(::File),
                modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f),
            )
            Row(Modifier.padding(6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                HuntBadge(car.isTreasureHunt, car.isSuperTreasureHunt)
                if (car.quantity > 1) Badge("×${car.quantity}", MaterialTheme.colorScheme.secondary)
            }
        }
        Column(Modifier.padding(10.dp)) {
            Text(car.name, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val sub = listOfNotNull(car.year, car.series).joinToString(" · ")
            if (sub.isNotEmpty()) {
                Text(
                    sub,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
