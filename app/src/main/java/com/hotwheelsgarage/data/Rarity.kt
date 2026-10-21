package com.hotwheelsgarage.data

enum class RarityLevel { SuperTreasureHunt, TreasureHunt, Limited }

data class Rarity(val level: RarityLevel, val title: String, val detail: String)

/** Decides whether a scanned car is a rare find worth shouting about. */
object RarityCheck {
    /** Series names on the wiki that mark limited or exclusive releases. */
    private val limitedMarkers = listOf(
        Regex("Red Line Club|\\bRLC\\b", RegexOption.IGNORE_CASE) to "Red Line Club exclusive, sold in small numbers to club members.",
        Regex("Convention", RegexOption.IGNORE_CASE) to "Collectors Convention car, made in very small numbers.",
        Regex("Elite 64", RegexOption.IGNORE_CASE) to "Elite 64 premium release with a limited production run.",
        Regex("\\bChase\\b", RegexOption.IGNORE_CASE) to "Chase car, a rare variation hidden in premium cases.",
        Regex("Zamac", RegexOption.IGNORE_CASE) to "ZAMAC (unpainted) store exclusive, produced in limited numbers.",
        Regex("Mail-?in", RegexOption.IGNORE_CASE) to "Mail-in exclusive that never hit store shelves.",
        Regex("Exclusive", RegexOption.IGNORE_CASE) to "Store or event exclusive, harder to find than mainlines.",
    )

    fun of(isTreasureHunt: Boolean, isSuperTreasureHunt: Boolean, series: String?): Rarity? = when {
        isSuperTreasureHunt -> Rarity(
            RarityLevel.SuperTreasureHunt,
            "Super Treasure Hunt!",
            "The rarest mainline find: Spectraflame paint and Real Riders rubber tires. Keep it carded.",
        )
        isTreasureHunt -> Rarity(
            RarityLevel.TreasureHunt,
            "Treasure Hunt!",
            "A limited-run car hidden in mainline cases. Look for the circle-flame logo on the card.",
        )
        series != null -> limitedMarkers.firstOrNull { it.first.containsMatchIn(series) }
            ?.let { Rarity(RarityLevel.Limited, "Limited release", it.second) }
        else -> null
    }
}
