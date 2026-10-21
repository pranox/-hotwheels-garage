package com.hotwheelsgarage.ui.settings

import android.app.Application
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hotwheelsgarage.HotWheelsApp
import com.hotwheelsgarage.alerts.AlertScheduler
import com.hotwheelsgarage.data.Settings
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Switch
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.KeyboardType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val garage = app as HotWheelsApp
    val settings = garage.settings

    fun applyAlerts() = AlertScheduler.apply(garage)

    fun export(uri: Uri, onDone: (String) -> Unit) = viewModelScope.launch {
        val result = runCatching {
            val json = garage.repository.exportJson()
            withContext(Dispatchers.IO) {
                garage.contentResolver.openOutputStream(uri)!!.use { it.write(json.toByteArray()) }
            }
        }
        onDone(if (result.isSuccess) "Backup saved" else "Backup failed: ${result.exceptionOrNull()?.message}")
    }

    fun import(uri: Uri, onDone: (String) -> Unit) = viewModelScope.launch {
        val result = runCatching {
            val json = withContext(Dispatchers.IO) {
                garage.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() }
            }
            garage.repository.importJson(json)
        }
        onDone(result.fold({ "Imported $it cars" }, { "Import failed: ${it.message}" }))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, vm: SettingsViewModel = viewModel()) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    var key by remember { mutableStateOf(vm.settings.geminiKey) }
    var model by remember { mutableStateOf(vm.settings.geminiModel) }
    var interval by remember { mutableStateOf(vm.settings.checkIntervalMinutes) }
    var customInterval by remember { mutableStateOf("") }
    var quiet by remember { mutableStateOf(vm.settings.quietStartHour to vm.settings.quietEndHour) }
    var salesHour by remember { mutableStateOf(vm.settings.salesHourReminder) }
    var subreddits by remember { mutableStateOf(vm.settings.subreddits) }
    var channels by remember { mutableStateOf(vm.settings.telegramChannels) }
    var places by remember { mutableStateOf(vm.settings.locationKeywords) }
    val saveAll = {
        vm.settings.geminiKey = key
        vm.settings.geminiModel = model
        vm.settings.checkIntervalMinutes = customInterval.toIntOrNull() ?: interval
        vm.settings.quietStartHour = quiet.first
        vm.settings.quietEndHour = quiet.second
        vm.settings.salesHourReminder = salesHour
        vm.settings.subreddits = subreddits
        vm.settings.telegramChannels = channels
        vm.settings.locationKeywords = places
        vm.applyAlerts()
    }
    val toast: (String) -> Unit = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }

    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { vm.export(it, toast) }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { vm.import(it, toast) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = {
                        saveAll()
                        onBack()
                    }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("AI photo recognition (optional)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Cars are identified on your phone by reading the card and barcode. Adding a free Gemini API key " +
                    "also lets the app recognise loose cars from a photo of the car itself.",
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(onClick = { uriHandler.openUri("https://aistudio.google.com/apikey") }) {
                Text("Get a free key at aistudio.google.com")
            }
            OutlinedTextField(
                value = key,
                onValueChange = { key = it },
                label = { Text("Gemini API key") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = model,
                onValueChange = { model = it },
                label = { Text("Gemini model") },
                supportingText = { Text("Change this if Google retires the model and you get a 404 error.") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            HorizontalDivider()
            Text("Drop alerts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Check every", style = MaterialTheme.typography.titleSmall)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(15, 30, 45, 60, 120, 180, 360).forEach { m ->
                    FilterChip(
                        selected = customInterval.isEmpty() && interval == m,
                        onClick = { interval = m; customInterval = "" },
                        label = { Text(if (m < 60) "$m min" else "${m / 60} h") },
                    )
                }
            }
            OutlinedTextField(
                value = customInterval,
                onValueChange = { v -> customInterval = v.filter { it.isDigit() }.take(4) },
                label = { Text("Custom (minutes)") },
                supportingText = {
                    val n = customInterval.toIntOrNull()
                    Text(
                        if (n != null && n < Settings.MIN_INTERVAL_MINUTES) "Android allows every 15 minutes at the most, so this will use 15."
                        else "Shorter = faster alerts, a little more battery. Android may delay checks while the phone is idle."
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            Text("Quiet hours (no checks)", style = MaterialTheme.typography.titleSmall)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    (-1 to -1) to "Off",
                    (23 to 7) to "11 pm–7 am",
                    (0 to 6) to "12 am–6 am",
                    (22 to 8) to "10 pm–8 am",
                ).forEach { (range, label) ->
                    FilterChip(selected = quiet == range, onClick = { quiet = range }, label = { Text(label) })
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("6 pm Sales Hour reminder")
                    Text("A nudge before r/HotWheelsIndia's daily MRP sales", style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = salesHour, onCheckedChange = { salesHour = it })
            }
            OutlinedTextField(
                value = subreddits,
                onValueChange = { subreddits = it },
                label = { Text("Subreddits to watch") },
                supportingText = { Text("Comma separated, e.g. HotWheelsIndia") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = channels,
                onValueChange = { channels = it },
                label = { Text("Telegram channels to watch") },
                supportingText = { Text("Public channel usernames, comma separated (without @)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = places,
                onValueChange = { places = it },
                label = { Text("Your area") },
                supportingText = { Text("Posts mentioning these get a \"Near you\" tag and always alert") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(onClick = {
                saveAll()
                toast("Saved")
            }) { Text("Save") }

            HorizontalDivider()
            Text("Backup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Save your garage to a file (e.g. Google Drive) and restore it on a new phone. Your own photos are not included.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { exporter.launch("hotwheels-garage.json") }) { Text("Export") }
                OutlinedButton(onClick = { importer.launch(arrayOf("application/json", "*/*")) }) { Text("Import") }
            }

            HorizontalDivider()
            Text(
                "Car data and photos come from the Hot Wheels Fandom wiki (CC BY-SA). Trending posts from r/HotWheels. " +
                    "Not affiliated with Mattel.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
