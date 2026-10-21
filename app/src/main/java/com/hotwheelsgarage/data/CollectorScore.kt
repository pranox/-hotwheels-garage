package com.hotwheelsgarage.data

data class Score(val stars: Int, val reasons: List<String>)

/**
 * Rates a car 1–5 from what the wiki says about it, so you don't have to:
 * hunts and limited runs score highest, then premium lines, age and how special the casting is.
 */
object CollectorScore {
    private val premium = Regex(
        "Car Culture|Pop Culture|Boulevard|Premium|Team Transport|Retro Entertainment|Replica Entertainment|Speed Machines|Elite",
        RegexOption.IGNORE_CASE,
    )
    private val firstEdition = Regex("First Edition|New Models", RegexOption.IGNORE_CASE)

    fun of(
        isTreasureHunt: Boolean,
        isSuperTreasureHunt: Boolean,
        series: String?,
        year: String?,
        versionCount: Int?,
    ): Score {
        if (isSuperTreasureHunt) return Score(5, listOf("Super Treasure Hunt, the rarest mainline find"))

        var points = 2.0
        val reasons = mutableListOf<String>()
        if (isTreasureHunt) { points += 1.5; reasons += "Treasure Hunt" }
        val limited = RarityCheck.of(false, false, series)
        if (limited != null) { points += 2.0; reasons += limited.title }
        else if (series != null && premium.containsMatchIn(series)) { points += 1.0; reasons += "Premium line" }
        if (series != null && firstEdition.containsMatchIn(series)) { points += 0.5; reasons += "First edition of the casting" }

        val y = year?.toIntOrNull()
        when {
            y != null && y <= 1995 -> { points += 1.5; reasons += "Vintage ($y)" }
            y != null && y <= 2010 -> { points += 0.5; reasons += "Older release ($y)" }
        }
        when {
            versionCount != null && versionCount >= 100 -> { points += 0.5; reasons += "Iconic casting, $versionCount versions" }
            versionCount != null && versionCount in 1..9 -> { points += 0.5; reasons += "Only $versionCount versions ever made" }
        }
        if (reasons.isEmpty()) reasons += "Regular mainline release"
        return Score(Math.round(points).toInt().coerceIn(1, 5), reasons)
    }

    fun of(car: Car): Score = of(car.isTreasureHunt, car.isSuperTreasureHunt, car.series, car.year, car.versionCount)
}
