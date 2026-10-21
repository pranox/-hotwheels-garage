package com.hotwheelsgarage.ui.scan

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hotwheelsgarage.data.Car
import com.hotwheelsgarage.data.Release
import com.hotwheelsgarage.scan.Candidate
import com.hotwheelsgarage.ui.common.Badge
import com.hotwheelsgarage.ui.common.CarImage
import com.hotwheelsgarage.ui.common.HuntBadge
import com.hotwheelsgarage.ui.common.PlateLabel
import com.hotwheelsgarage.ui.common.RarityBanner
import com.hotwheelsgarage.ui.common.StoreLinks
import com.hotwheelsgarage.data.CollectorScore
import com.hotwheelsgarage.data.RarityCheck
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.hotwheelsgarage.ui.theme.OwnedContainer
import com.hotwheelsgarage.ui.common.StarRating
import com.hotwheelsgarage.ui.theme.SuperGold
import com.hotwheelsgarage.ui.theme.TreasureGreen

@Composable
fun ScanScreen(onSaved: () -> Unit, vm: ScanViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    BackHandler(enabled = state.stage == Stage.Results || state.stage == Stage.Confirm) { vm.back() }
    when (state.stage) {
        Stage.Camera -> CameraStage(vm)
        Stage.Working -> WorkingStage(state)
        Stage.Results -> ResultsStage(state, vm)
        Stage.Confirm -> ConfirmStage(state, vm, onSaved)
    }
}

// ---------- Camera ----------

@Composable
private fun CameraStage(vm: ScanViewModel) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    LaunchedEffect(Unit) { if (!granted) permission.launch(Manifest.permission.CAMERA) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(vm::onPicked)
    }
    val imageCapture = remember {
        ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY).build()
    }
    var capturing by remember { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxWidth().background(Color.Black)) {
            if (granted) {
                CameraPreview(imageCapture, Modifier.fillMaxSize())
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(0.85f)
                        .aspectRatio(0.75f)
                        .border(BorderStroke(2.dp, Color.White.copy(alpha = 0.7f)), RoundedCornerShape(16.dp)),
                )
            } else {
                Column(
                    Modifier.align(Alignment.Center).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Camera permission is needed to scan cars.", color = Color.White, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { permission.launch(Manifest.permission.CAMERA) }) { Text("Allow camera") }
                }
            }
            Text(
                "Snap the back of the card, or the base of a loose car",
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
            Row(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilledIconButton(
                    onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.Black.copy(alpha = 0.5f)),
                ) { Icon(Icons.Filled.PhotoLibrary, "Pick from gallery", tint = Color.White) }
                FilledIconButton(
                    enabled = granted && !capturing,
                    onClick = {
                        capturing = true
                        val file = vm.newPhotoFile()
                        imageCapture.takePicture(
                            ImageCapture.OutputFileOptions.Builder(file).build(),
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                    capturing = false
                                    vm.analyze(file)
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    capturing = false
                                    vm.showError("Camera error: ${exception.message}")
                                }
                            },
                        )
                    },
                    modifier = Modifier.size(76.dp),
                    shape = CircleShape,
                ) { Icon(Icons.Filled.CameraAlt, "Take photo", Modifier.size(36.dp)) }
                Spacer(Modifier.size(40.dp))
            }
        }
        SearchRow(query, { query = it }, onSearch = { vm.search(query) }, Modifier.padding(12.dp))
    }
}

@Composable
private fun CameraPreview(imageCapture: ImageCapture, modifier: Modifier) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }

    DisposableEffect(owner) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            val provider = future.get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
            runCatching {
                provider.unbindAll()
                provider.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
            }.onFailure { Log.e("ScanScreen", "Camera bind failed", it) }
        }, ContextCompat.getMainExecutor(context))
        onDispose { runCatching { future.get().unbindAll() } }
    }
    AndroidView(factory = { previewView }, modifier = modifier)
}

@Composable
private fun SearchRow(query: String, onChange: (String) -> Unit, onSearch: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = query,
        onValueChange = onChange,
        placeholder = { Text("…or type a model name") },
        singleLine = true,
        trailingIcon = { IconButton(onClick = onSearch) { Icon(Icons.Filled.Search, "Search") } },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        modifier = modifier.fillMaxWidth(),
    )
}

// ---------- Working ----------

@Composable
private fun WorkingStage(state: ScanUiState) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        state.photo?.let {
            CarImage(it, Modifier.fillMaxWidth(0.6f).aspectRatio(0.75f).clip(RoundedCornerShape(16.dp)))
            Spacer(Modifier.height(24.dp))
        }
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text(state.message, textAlign = TextAlign.Center)
    }
}

// ---------- Results ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResultsStage(state: ScanUiState, vm: ScanViewModel) {
    var query by remember(state.query) { mutableStateOf(state.query) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Which one is it?") },
                navigationIcon = { IconButton(onClick = vm::back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            )
        },
    ) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    state.photo?.let {
                        CarImage(it, Modifier.size(84.dp).clip(RoundedCornerShape(12.dp)))
                        Spacer(Modifier.size(12.dp))
                    }
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        state.hints?.toyCodes?.take(2)?.forEach { AssistChip(onClick = {}, label = { Text("Toy # $it") }) }
                        state.hints?.collectorNumber?.let { AssistChip(onClick = {}, label = { Text("Col # $it") }) }
                        state.aiGuess?.model?.let { AssistChip(onClick = {}, label = { Text("AI: $it") }) }
                    }
                }
            }
            state.candidates.firstNotNullOfOrNull { c ->
                c.release?.let { RarityCheck.of(it.isTreasureHunt, it.isSuperTreasureHunt, it.series) }
            }?.let { rare ->
                item { RarityBanner(rare, Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
            }
            items(state.notes) { note ->
                Text(
                    note,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            item { SearchRow(query, { query = it }, onSearch = { vm.search(query) }, Modifier.padding(16.dp)) }
            if (state.candidates.isEmpty()) {
                item {
                    Text(
                        "No match found. Type the name printed on the card or cast on the base.",
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center,
                    )
                }
            }
            items(state.candidates, key = { it.title }) { candidate ->
                CandidateRow(candidate) { vm.select(candidate) }
            }
        }
    }
}

@Composable
private fun CandidateRow(candidate: Candidate, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = { CarImage(candidate.imageUrl, Modifier.size(72.dp).clip(RoundedCornerShape(8.dp))) },
        headlineContent = { Text(candidate.title, fontWeight = FontWeight.SemiBold) },
        supportingContent = {
            Text(candidate.reason, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
        },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                candidate.release?.let { HuntBadge(it.isTreasureHunt, it.isSuperTreasureHunt) }
                if (candidate.ownedCount > 0) Badge("Owned ×${candidate.ownedCount}", MaterialTheme.colorScheme.secondary)
            }
        },
    )
}

// ---------- Confirm ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfirmStage(state: ScanUiState, vm: ScanViewModel, onSaved: () -> Unit) {
    val candidate = state.selected ?: return
    val details = state.details
    val rarity = RarityCheck.of(state.isTreasureHunt, state.isSuperTreasureHunt, state.chosenRelease?.series)
    // A rare find gets a buzz the moment it's recognised.
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(candidate.title, rarity?.level) {
        if (rarity != null) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(details?.name ?: candidate.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { IconButton(onClick = vm::back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { vm.save(wishlist = true, onSaved) }, modifier = Modifier.weight(1f)) {
                        Text("Add to wishlist")
                    }
                    Button(onClick = { vm.save(wishlist = false, onSaved) }, modifier = Modifier.weight(1f)) {
                        Text(if (candidate.ownedCount > 0) "Add another" else "Add to garage")
                    }
                }
            }
        },
    ) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.photo?.let { CarImage(it, Modifier.weight(1f).aspectRatio(4f / 3f).clip(RoundedCornerShape(12.dp))) }
                    CarImage(
                        state.chosenRelease?.imageUrl ?: details?.imageUrl ?: candidate.imageUrl,
                        Modifier.weight(1f).aspectRatio(4f / 3f).clip(RoundedCornerShape(12.dp)),
                    )
                }
                Spacer(Modifier.height(12.dp))
                rarity?.let {
                    RarityBanner(it)
                    Spacer(Modifier.height(12.dp))
                }
                if (candidate.ownedCount > 0) {
                    Card(colors = CardDefaults.cardColors(containerColor = OwnedContainer)) {
                        Text(
                            "You already own ${candidate.ownedCount} of this casting.",
                            Modifier.padding(12.dp).fillMaxWidth(),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }
                if (details != null) {
                    val facts = listOfNotNull(
                        details.designer?.let { "Designer: $it" },
                        details.years?.let { "Produced: $it" },
                        details.firstSeries?.let { "Debut: $it" },
                    )
                    facts.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
                    details.description?.let {
                        Spacer(Modifier.height(6.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 5, overflow = TextOverflow.Ellipsis)
                    }
                } else if (state.detailsError != null) {
                    Text("Couldn't load details: ${state.detailsError}", color = MaterialTheme.colorScheme.error)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text("  Loading details from the wiki…")
                    }
                }

                Spacer(Modifier.height(12.dp))
                StoreLinks(details?.name ?: candidate.title)
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(Car.CONDITION_CARDED, Car.CONDITION_LOOSE).forEach { c ->
                        FilterChip(selected = state.condition == c, onClick = { vm.setCondition(c) }, label = { Text(c) })
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    val score = CollectorScore.of(
                        state.isTreasureHunt, state.isSuperTreasureHunt,
                        state.chosenRelease?.series, state.chosenRelease?.year, details?.versions?.size,
                    )
                    Column(Modifier.weight(1f)) {
                        Text("Collector score", fontWeight = FontWeight.SemiBold)
                        Text(score.reasons.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    StarRating(score.stars, size = 22.dp)
                }
                HuntToggle("Treasure Hunt", "Circle-flame logo on card", TreasureGreen, state.isTreasureHunt, vm::setTreasureHunt)
                HuntToggle("Super Treasure Hunt", "Spectraflame paint + Real Riders", SuperGold, state.isSuperTreasureHunt, vm::setSuperTreasureHunt)

                Spacer(Modifier.height(16.dp))
                Text("Which version do you have?", style = MaterialTheme.typography.titleMedium)
                // The matched version goes first so it's visible without scrolling.
                state.chosenRelease?.let { chosen -> VersionRow(chosen, true) {} }
                VersionRow(null, state.chosenRelease == null) { vm.chooseRelease(null) }
            }
            items(details?.versions.orEmpty().filter { it != state.chosenRelease }) { release ->
                VersionRow(release, release == state.chosenRelease) { vm.chooseRelease(release) }
            }
        }
    }
}

@Composable
private fun HuntToggle(title: String, subtitle: String, color: Color, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = color, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun VersionRow(release: Release?, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        if (release == null) {
            Text("Not sure / skip")
            return@Row
        }
        CarImage(release.imageUrl, Modifier.size(64.dp, 48.dp).clip(RoundedCornerShape(6.dp)))
        Spacer(Modifier.size(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                listOfNotNull(release.year, release.color).joinToString(" · ").ifEmpty { release.modelName },
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                release.series.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            release.toyNumber?.let { PlateLabel(it, Modifier.padding(top = 2.dp)) }
        }
        HuntBadge(release.isTreasureHunt, release.isSuperTreasureHunt)
    }
}
