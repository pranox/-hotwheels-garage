package com.hotwheelsgarage.ui.hype

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hotwheelsgarage.HotWheelsApp
import com.hotwheelsgarage.data.Car
import com.hotwheelsgarage.data.RedditApi
import com.hotwheelsgarage.data.RedditPost
import com.hotwheelsgarage.data.Release
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

data class Section<T>(val title: String, val loading: Boolean = true, val items: List<T> = emptyList(), val error: String? = null)

class HypeViewModel(app: Application) : AndroidViewModel(app) {
    private val garage = app as HotWheelsApp
    private val year = Calendar.getInstance().get(Calendar.YEAR)

    val supers = MutableStateFlow(Section<Release>("Super Treasure Hunts"))
    val hunts = MutableStateFlow(Section<Release>("Treasure Hunts"))
    val upcoming = MutableStateFlow(Section<Release>("Coming next year"))
    val latest = MutableStateFlow(Section<Release>("Newest mainline releases"))
    val reddit = MutableStateFlow(Section<RedditPost>("Trending on r/HotWheels"))

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            supers.value = supers.value.copy(loading = true, error = null)
            hunts.value = hunts.value.copy(loading = true, error = null)
            runCatching { garage.wiki.treasureHunts(year) }
                .onSuccess { th ->
                    supers.value = Section("${th?.year ?: year} Super Treasure Hunts", false, th?.supers.orEmpty())
                    hunts.value = Section("${th?.year ?: year} Treasure Hunts", false, th?.regular.orEmpty())
                }
                .onFailure { e ->
                    supers.value = supers.value.copy(loading = false, error = e.message)
                    hunts.value = hunts.value.copy(loading = false, error = e.message)
                }
        }
        viewModelScope.launch {
            upcoming.value = upcoming.value.copy(loading = true, error = null)
            runCatching { garage.wiki.treasureHunts(year + 1, fallback = false) }
                .onSuccess { th ->
                    upcoming.value = Section("Coming in ${year + 1}: Treasure Hunts", false, th?.let { it.supers + it.regular }.orEmpty())
                }
                .onFailure { e -> upcoming.value = upcoming.value.copy(loading = false, error = e.message) }
        }
        viewModelScope.launch {
            latest.value = latest.value.copy(loading = true, error = null)
            runCatching { garage.wiki.latestReleases(year) }
                .onSuccess { latest.value = latest.value.copy(loading = false, items = it) }
                .onFailure { e -> latest.value = latest.value.copy(loading = false, error = e.message) }
        }
        viewModelScope.launch {
            reddit.value = reddit.value.copy(loading = true, error = null)
            runCatching { RedditApi.topOfWeek() }
                .onSuccess { reddit.value = reddit.value.copy(loading = false, items = it) }
                .onFailure { e -> reddit.value = reddit.value.copy(loading = false, error = "Reddit unavailable right now (${e.message})") }
        }
    }

    fun pageUrl(title: String) = garage.wiki.pageUrl(title)

    fun addToWishlist(release: Release, onDone: () -> Unit) = viewModelScope.launch {
        garage.repository.addOrIncrement(
            Car(
                name = release.modelName,
                wikiTitle = release.modelTitle,
                series = release.series,
                year = release.year,
                color = release.color,
                toyNumber = release.toyNumber,
                collectorNumber = release.collectorNumber,
                imageUrl = release.imageUrl,
                isTreasureHunt = release.isTreasureHunt,
                isSuperTreasureHunt = release.isSuperTreasureHunt,
                isWishlist = true,
            )
        )
        onDone()
    }
}

