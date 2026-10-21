package com.hotwheelsgarage.ui.drops

import android.Manifest
import android.os.Build
import android.text.format.DateUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hotwheelsgarage.data.DropEvent
import com.hotwheelsgarage.ui.common.Badge
import com.hotwheelsgarage.ui.common.PlateLabel
import com.hotwheelsgarage.ui.theme.CardBlue
import com.hotwheelsgarage.ui.theme.Chrome
import com.hotwheelsgarage.ui.theme.Ink
import com.hotwheelsgarage.ui.theme.InkMuted
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropsScreen(onSettings: () -> Unit, vm: DropsViewModel = viewModel()) {
    val events by vm.events.collectAsStateWithLifecycle()
    val checking by vm.checking.collectAsStateWithLifecycle()
    val status by vm.status.collectAsStateWithLifecycle()
    val enabled by vm.enabled.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current

    // Android 13+ asks before an app may post notifications; ask when alerts are switched on.
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        vm.setEnabled(true)
    }
    val turnOn = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            vm.setEnabled(true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Drops & sales", fontWeight = FontWeight.Bold) },
                actions = {
                    if (checking) {
                        CircularProgressIndicator(Modifier.padding(12.dp).size(24.dp), strokeWidth = 2.dp)
                    } else {
                        IconButton(onClick = vm::checkNow) { Icon(Icons.Filled.Refresh, "Check now") }
                    }
                    IconButton(onClick = onSettings) { Icon(Icons.Filled.Settings, "Alert settings") }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Drop alerts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    "Get notified when a store drop or sale mentions a car on your wishlist, " +
                                        "a current Treasure Hunt, or your area.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(checked = enabled, onCheckedChange = { if (it) turnOn() else vm.setEnabled(false) })
                        }
                        Text(scheduleSummary(vm), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        status?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        TextButton(onClick = onSettings, contentPadding = PaddingValues(0.dp)) {
                            Text("Change how often it checks")
                        }
                    }
                }
            }
            item { DropTimesCard(events.orEmpty()) }
            item {
                Text("Recent posts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            when {
                events == null -> item { CircularProgressIndicator() }
                events.orEmpty().isEmpty() -> item {
                    Text(
                        if (enabled) "Nothing yet. Tap refresh to check now." else "Turn on drop alerts to start watching.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                else -> items(events.orEmpty(), key = { it.id }) { event ->
                    EventRow(event) { runCatching { uriHandler.openUri(event.url) } }
                }
            }
        }
    }
}

private fun scheduleSummary(vm: DropsViewModel): String {
    val s = vm.settings
    val every = s.checkIntervalMinutes.let { if (it % 60 == 0) "${it / 60} h" else "$it min" }
    val quiet = if (s.quietStartHour < 0) "no quiet hours" else "quiet ${hourLabel(s.quietStartHour)}–${hourLabel(s.quietEndHour)}"
    val last = s.lastCheckAt.takeIf { it > 0 }
        ?.let { "last checked " + DateUtils.getRelativeTimeSpanString(it, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS) }
        ?: "not checked yet"
    return "Checks every $every · $quiet · $last"
}

private fun hourLabel(hour: Int): String = when (hour) {
    0 -> "12 am"
    12 -> "12 pm"
    in 1..11 -> "$hour am"
    else -> "${hour - 12} pm"
}

/** Learns when drops and sales actually get posted, by hour of day. */
@Composable
private fun DropTimesCard(events: List<DropEvent>) {
    val zone = remember { ZoneId.systemDefault() }
    val byHour = remember(events) {
        val drops = IntArray(24)
        val sales = IntArray(24)
        events.forEach { e ->
            val hour = Instant.ofEpochMilli(e.postedAt).atZone(zone).hour
            if (e.kind == DropEvent.KIND_DROP) drops[hour]++ else sales[hour]++
        }
        drops to sales
    }
    val (drops, sales) = byHour
    val totals = IntArray(24) { drops[it] + sales[it] }
    val max = totals.maxOrNull()?.coerceAtLeast(1) ?: 1
    val peak = totals.indices.maxByOrNull { totals[it] }?.takeIf { totals[it] > 0 }

    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("When drops & sales happen", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                peak?.let { "Busiest: ${hourLabel(it)}–${hourLabel((it + 1) % 24)} · from ${events.size} posts" }
                    ?: "Learns from the posts it sees. Check back after a few days.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .semantics { contentDescription = peak?.let { "Busiest hour ${hourLabel(it)}" } ?: "No data yet" },
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                for (h in 0 until 24) {
                    // Empty space on top, then sales, then drops, so bars grow up from the baseline.
                    Column(Modifier.weight(1f).fillMaxHeight()) {
                        val saleFraction = sales[h] / max.toFloat()
                        val dropFraction = drops[h] / max.toFloat()
                        val empty = 1f - saleFraction - dropFraction
                        if (empty > 0) Spacer(Modifier.weight(empty))
                        if (saleFraction > 0) Box(Modifier.fillMaxWidth().weight(saleFraction).background(InkMuted))
                        if (dropFraction > 0) Box(Modifier.fillMaxWidth().weight(dropFraction).background(Ink))
                    }
                }
            }
            Row {
                listOf(0, 6, 12, 18).forEach { h ->
                    Text(hourLabel(h), Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Legend("Store drops", Ink)
                Legend("Sales", InkMuted)
            }
        }
    }
}

@Composable
private fun Legend(label: String, color: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(color))
        Text("  $label", style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun EventRow(event: DropEvent, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        overlineContent = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                PlateLabel(if (event.kind == DropEvent.KIND_DROP) "DROP" else "SALE")
                event.matched?.let { Badge(it, Chrome) }
                if (event.local) Badge("Near you", CardBlue)
            }
        },
        headlineContent = { Text(event.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Text(
                listOfNotNull(
                    event.platform,
                    event.source,
                    DateUtils.getRelativeTimeSpanString(event.postedAt, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString(),
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
            )
        },
    )
}
