package com.hotwheelsgarage.alerts

import com.hotwheelsgarage.HotWheelsApp
import com.hotwheelsgarage.data.CommunityPost
import com.hotwheelsgarage.data.DropEvent
import com.hotwheelsgarage.data.DropSources
import java.time.LocalDate

data class CheckResult(val newPosts: Int, val alerts: Int, val errors: List<String>)

/** One pass over all sources: record drops/sales, notify about the ones worth it. Used by the worker and "Check now". */
class DropChecker(private val app: HotWheelsApp) {
    private val settings get() = app.settings

    suspend fun check(): CheckResult {
        val matcher = DropMatcher(
            watchNames = app.repository.wishlistNames() + huntNames(),
            locationKeywords = settings.locationKeywords.split(','),
        )
        val errors = mutableListOf<String>()
        var newPosts = 0
        var alerts = 0

        val sources: List<Pair<String, suspend () -> List<CommunityPost>>> =
            settings.subreddits.split(',').map { it.trim() }.filter { it.isNotEmpty() }
                .map { name -> "r/${name.removePrefix("r/")}" to suspend { DropSources.reddit(name) } } +
                settings.telegramChannels.split(',').map { it.trim().removePrefix("@") }.filter { it.isNotEmpty() }
                    .map { name -> "t.me/$name" to suspend { DropSources.telegram(name) } }

        for ((source, fetch) in sources) {
            val fetched = runCatching { fetch() }
            val posts = fetched.getOrNull()
            if (posts == null) {
                errors += "$source: ${fetched.exceptionOrNull()?.message}"
                continue
            }
            // First time we see a source, record its history for the timing chart but don't alert on old posts.
            val lastSeen = settings.lastSeen(source)
            for (post in posts.sortedBy { it.postedAt }) {
                val match = matcher.match(post) ?: continue
                val fresh = lastSeen != null && post.postedAt > lastSeen
                val notify = fresh && match.alertWorthy
                val event = DropEvent(
                    postId = post.id, source = source, kind = match.kind, platform = match.platform,
                    title = post.title, url = post.url, matched = match.matched, local = match.local,
                    postedAt = post.postedAt, notified = notify,
                )
                if (app.database.dropDao().insert(event) == -1L) continue
                if (fresh) newPosts++
                if (notify) {
                    alerts++
                    Notifications.show(
                        app, post.id.hashCode(), Notifications.CHANNEL_DROPS,
                        title = alertTitle(match),
                        text = "${post.title}\n$source",
                        url = post.url,
                    )
                }
            }
            posts.maxOfOrNull { it.postedAt }?.let { newest ->
                settings.setLastSeen(source, maxOf(newest, lastSeen ?: 0))
            }
        }
        settings.lastCheckAt = System.currentTimeMillis()
        return CheckResult(newPosts, alerts, errors)
    }

    private fun alertTitle(m: Match): String {
        val what = m.matched?.let { "$it " } ?: ""
        val where = if (m.local) " near you" else ""
        return when (m.kind) {
            DropEvent.KIND_DROP -> "🔥 ${what}drop on ${m.platform}$where"
            else -> "🏷️ ${what}up for sale$where"
        }
    }

    /** This year's Treasure Hunt names, cached for a day so checks stay one small request per source. */
    private suspend fun huntNames(): List<String> {
        val today = LocalDate.now().toString()
        val cached = settings.huntNamesCache
        if (cached.startsWith("$today|")) return cached.substringAfter('|').split('\n').filter { it.isNotBlank() }
        val names = runCatching { app.wiki.treasureHuntNames(LocalDate.now().year) }.getOrNull()
            ?: return cached.substringAfter('|', "").split('\n').filter { it.isNotBlank() }
        settings.huntNamesCache = today + "|" + names.joinToString("\n")
        return names
    }
}
