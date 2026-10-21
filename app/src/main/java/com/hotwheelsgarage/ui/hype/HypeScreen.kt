package com.hotwheelsgarage.ui.hype

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hotwheelsgarage.data.Release
import com.hotwheelsgarage.ui.common.CarImage
import com.hotwheelsgarage.ui.common.HuntBadge
import com.hotwheelsgarage.ui.common.PlateLabel
import com.hotwheelsgarage.ui.common.StoreLinks
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HypeScreen(vm: HypeViewModel = viewModel()) {
    val supers by vm.supers.collectAsStateWithLifecycle()
    val hunts by vm.hunts.collectAsStateWithLifecycle()
    val upcoming by vm.upcoming.collectAsStateWithLifecycle()
    val latest by vm.latest.collectAsStateWithLifecycle()
    val reddit by vm.reddit.collectAsStateWithLifecycle()
    var open by remember { mutableStateOf<Release?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rare & Hyped", fontWeight = FontWeight.Bold) },
                actions = { IconButton(onClick = vm::refresh) { Icon(Icons.Filled.Refresh, "Refresh") } },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
            listOf(supers, hunts, latest, upcoming).forEach { section ->
                if (!section.loading && section.items.isEmpty() && section.error == null) return@forEach
                item(key = section.title) {
                    SectionHeader(section.title, section.loading, section.error)
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(section.items) { release -> ReleaseCard(release) { open = release } }
                    }
                }
            }
            item(key = "reddit") { SectionHeader(reddit.title, reddit.loading, reddit.error) }
            items(reddit.items, key = { it.url }) { post ->
                ListItem(
                    modifier = Modifier.clickable { uriHandler.openUri(post.url) },
                    leadingContent = post.thumbnail?.let { { CarImage(it, Modifier.size(64.dp).clip(RoundedCornerShape(8.dp))) } },
                    headlineContent = { Text(post.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                    supportingContent = { Text(post.author, style = MaterialTheme.typography.bodySmall) },
                )
            }
        }
    }

    open?.let { release ->
        AlertDialog(
            onDismissRequest = { open = null },
            title = { Text(release.modelName) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    CarImage(
                        release.imageUrl,
                        Modifier.fillMaxWidth().aspectRatio(16f / 10f).clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Fit,
                    )
                    HuntBadge(release.isTreasureHunt, release.isSuperTreasureHunt)
                    listOfNotNull(
                        release.series?.let { "Series: $it" },
                        release.color?.let { "Color: $it" },
                        release.collectorNumber?.let { "Col. #: $it" },
                        release.toyNumber?.let { "Toy #: $it" },
                    ).forEach { Text(it) }
                    StoreLinks(release.modelName, Modifier.padding(top = 8.dp))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.addToWishlist(release) { scope.launch { snackbar.showSnackbar("Added to your wishlist") } }
                    open = null
                }) { Text("Add to wishlist") }
            },
            dismissButton = {
                release.modelTitle?.let { title ->
                    TextButton(onClick = { uriHandler.openUri(vm.pageUrl(title)) }) { Text("Wiki page") }
                }
            },
        )
    }
}

@Composable
private fun SectionHeader(title: String, loading: Boolean, error: String?) {
    Row(Modifier.fillMaxWidth().padding(16.dp, 20.dp, 16.dp, 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        if (loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
    }
    error?.let {
        Text(
            it,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}

@Composable
private fun ReleaseCard(release: Release, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.width(170.dp)) {
        Box {
            CarImage(release.imageUrl, Modifier.fillMaxWidth().height(110.dp), contentScale = ContentScale.Crop)
            HuntBadge(release.isTreasureHunt, release.isSuperTreasureHunt, Modifier.padding(6.dp))
        }
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(release.modelName, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            release.collectorNumber?.let { PlateLabel(it) }
            Text(
                release.series.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
