package com.hotwheelsgarage.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cars")
data class Car(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Title of the casting page on the Hot Wheels Fandom wiki. */
    val wikiTitle: String? = null,
    val series: String? = null,
    val year: String? = null,
    val color: String? = null,
    val toyNumber: String? = null,
    val collectorNumber: String? = null,
    /** Official photo from the wiki. */
    val imageUrl: String? = null,
    /** The user's own photo, stored in app files. */
    val photoPath: String? = null,
    val designer: String? = null,
    val description: String? = null,
    val isTreasureHunt: Boolean = false,
    val isSuperTreasureHunt: Boolean = false,
    val isWishlist: Boolean = false,
    val condition: String = CONDITION_CARDED,
    val quantity: Int = 1,
    val pricePaid: Double? = null,
    val notes: String = "",
    /** Collector score 1–5, worked out by [CollectorScore] from the wiki data. 0 = not scored yet. */
    val rating: Int = 0,
    /** Why it got that score, e.g. "Treasure Hunt · Older release (2008)". */
    val ratingNote: String? = null,
    /** How many versions of this casting the wiki lists. */
    val versionCount: Int? = null,
    val addedAt: Long = System.currentTimeMillis(),
) {
    companion object {
        const val CONDITION_CARDED = "Carded"
        const val CONDITION_LOOSE = "Loose"
    }
}
