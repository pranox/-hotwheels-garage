package com.hotwheelsgarage.ui.garage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hotwheelsgarage.data.Car
import com.hotwheelsgarage.ui.common.CarImage
import com.hotwheelsgarage.ui.common.StarRating
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarDetailScreen(onBack: () -> Unit, vm: CarDetailViewModel = viewModel()) {
    val car by vm.car.collectAsStateWithLifecycle()
    val loaded = car
    var edit by remember(loaded?.id) { mutableStateOf(loaded) }
    var priceText by remember(loaded?.id) { mutableStateOf(loaded?.pricePaid?.toString().orEmpty()) }
    var confirmDelete by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(loaded?.name ?: "") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, "Delete") }
                    IconButton(onClick = {
                        edit?.let { vm.save(it.copy(pricePaid = priceText.replace(',', '.').toDoubleOrNull())) }
                        onBack()
                    }) { Icon(Icons.Filled.Check, "Save") }
                },
            )
        },
    ) { padding ->
        val current = edit
        if (current == null) {
            Box(Modifier.padding(padding).fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        fun update(transform: Car.() -> Car) { edit = current.transform() }

        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val images = listOfNotNull(current.photoPath?.let(::File), current.imageUrl)
                (images.ifEmpty { listOf(null) }).forEach { model ->
                    CarImage(
                        model,
                        Modifier.weight(1f).aspectRatio(4f / 3f).clip(RoundedCornerShape(12.dp)),
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("My rating", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                StarRating(current.rating, onRate = { r -> update { copy(rating = r) } })
            }
            if (current.isWishlist) {
                OutlinedButton(onClick = { update { copy(isWishlist = false) } }, modifier = Modifier.fillMaxWidth()) {
                    Text("I got it! Move to garage")
                }
            }

            Field("Name", current.name) { update { copy(name = it) } }
            Field("Series", current.series.orEmpty()) { update { copy(series = it.ifBlank { null }) } }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Field("Year", current.year.orEmpty(), Modifier.weight(1f)) { update { copy(year = it.ifBlank { null }) } }
                Field("Color", current.color.orEmpty(), Modifier.weight(1f)) { update { copy(color = it.ifBlank { null }) } }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Field("Toy #", current.toyNumber.orEmpty(), Modifier.weight(1f)) { update { copy(toyNumber = it.ifBlank { null }) } }
                Field("Col. #", current.collectorNumber.orEmpty(), Modifier.weight(1f)) { update { copy(collectorNumber = it.ifBlank { null }) } }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                listOf(Car.CONDITION_CARDED, Car.CONDITION_LOOSE).forEach { c ->
                    FilterChip(selected = current.condition == c, onClick = { update { copy(condition = c) } }, label = { Text(c) })
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { update { copy(quantity = (quantity - 1).coerceAtLeast(1)) } }) { Icon(Icons.Filled.Remove, "Less") }
                Text("×${current.quantity}", style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = { update { copy(quantity = quantity + 1) } }) { Icon(Icons.Filled.Add, "More") }
            }

            ToggleRow("Treasure Hunt", current.isTreasureHunt) { update { copy(isTreasureHunt = it) } }
            ToggleRow("Super Treasure Hunt", current.isSuperTreasureHunt) { update { copy(isSuperTreasureHunt = it) } }
            ToggleRow("On wishlist (don't own yet)", current.isWishlist) { update { copy(isWishlist = it) } }

            OutlinedTextField(
                value = priceText,
                onValueChange = { priceText = it },
                label = { Text("Price paid") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = current.notes,
                onValueChange = { update { copy(notes = it) } },
                label = { Text("Notes (where you found it, card condition…)") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )

            current.designer?.let { Text("Designer: $it", style = MaterialTheme.typography.bodyMedium) }
            current.description?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            current.wikiTitle?.let { title ->
                TextButton(onClick = { uriHandler.openUri(vm.wiki.pageUrl(title)) }) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, null)
                    Text("  Open on Hot Wheels wiki")
                }
            }
        }
    }

    if (confirmDelete && loaded != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Remove ${loaded.name}?") },
            text = { Text("This deletes it from your garage, including your photo.") },
            confirmButton = { TextButton(onClick = { vm.delete(loaded, onBack) }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun Field(label: String, value: String, modifier: Modifier = Modifier.fillMaxWidth(), onChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label) }, singleLine = true, modifier = modifier)
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
