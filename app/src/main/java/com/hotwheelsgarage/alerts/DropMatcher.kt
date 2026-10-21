package com.hotwheelsgarage.alerts

import com.hotwheelsgarage.data.CommunityPost
import com.hotwheelsgarage.data.DropEvent

data class Match(val kind: String, val platform: String?, val matched: String?, val local: Boolean) {
    /** Worth a notification: a wanted car, or any store drop near you. */
    val alertWorthy: Boolean get() = matched != null || (kind == DropEvent.KIND_DROP && local)
}

/** Decides whether a community post is a store drop or a sale, and whether it mentions something you want. */
class DropMatcher(watchNames: List<String>, locationKeywords: List<String>) {
    private val platforms = linkedMapOf(
        "Blinkit" to Regex("\\bblinkit\\b"),
        "Zepto" to Regex("\\bzepto\\b"),
        "Flipkart Minutes" to Regex("\\bflipkart minutes\\b|\\bfk minutes\\b"),
        "Instamart" to Regex("\\binstamart\\b"),
        "Flipkart" to Regex("\\bflipkart\\b"),
        "Amazon" to Regex("\\bamazon\\b"),
        "BigBasket" to Regex("\\bbig ?basket\\b|\\bbb now\\b"),
        "FirstCry" to Regex("\\bfirst ?cry\\b"),
    )
    private val sale = Regex("\\bmrp\\b|\\bfor sale\\b|\\bselling\\b|\\bsale\\b|\\bbelow mrp\\b|\\bwts\\b")
    private val huntWords = Regex("\\bsuper treasure hunt\\b|\\bsth\\b|\\btreasure hunt\\b|\\bth\\b")

    private val names = watchNames.map(::normalize).filter { it.length >= 4 }.distinct()
        .sortedByDescending { it.length }
    private val places = locationKeywords.map(::normalize).filter { it.isNotEmpty() }

    fun match(post: CommunityPost): Match? {
        val text = normalize(post.title + " " + post.text)
        val padded = " $text "
        val platform = platforms.entries.firstOrNull { it.value.containsMatchIn(text) }?.key
        val kind = when {
            platform != null -> DropEvent.KIND_DROP
            sale.containsMatchIn(text) -> DropEvent.KIND_SALE
            else -> return null
        }
        val matched = names.firstOrNull { padded.contains(" $it ") }
            ?: huntWords.find(text)?.value?.uppercase()?.let { if (it == "TH" || it == "STH") it else it.replaceFirstChar(Char::titlecase) }
        val local = places.any { padded.contains(" $it ") }
        return Match(kind, platform, matched, local)
    }

    companion object {
        /** "'87 Buick Regal GNX" -> "87 buick regal gnx". */
        fun normalize(s: String): String =
            s.lowercase().replace(Regex("[’'`]"), "").replace(Regex("[^\\p{L}\\p{N}]+"), " ").trim()
    }
}
