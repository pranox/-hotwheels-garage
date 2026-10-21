package com.hotwheelsgarage.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** A community post about a store drop or a sale, kept to show alerts and learn when drops happen. */
@Entity(tableName = "drop_events", indices = [Index(value = ["postId"], unique = true)])
data class DropEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Source-unique id, e.g. "reddit:t3_abc" or "telegram:channel/123". */
    val postId: String,
    /** "r/HotWheelsIndia" or "t.me/channel". */
    val source: String,
    val kind: String,
    /** Store mentioned, e.g. "Blinkit". */
    val platform: String?,
    val title: String,
    val url: String,
    /** Wishlist or Treasure Hunt name the post mentioned. */
    val matched: String?,
    /** Mentions your city or area. */
    val local: Boolean,
    val postedAt: Long,
    val notified: Boolean,
) {
    companion object {
        const val KIND_DROP = "drop"
        const val KIND_SALE = "sale"
    }
}

@Dao
interface DropDao {
    @Query("SELECT * FROM drop_events ORDER BY postedAt DESC LIMIT 100")
    fun observeRecent(): Flow<List<DropEvent>>

    /** Returns -1 when the post was already recorded. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(event: DropEvent): Long
}
