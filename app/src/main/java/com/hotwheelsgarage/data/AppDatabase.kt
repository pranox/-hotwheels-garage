package com.hotwheelsgarage.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Car::class, DropEvent::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun carDao(): CarDao
    abstract fun dropDao(): DropDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "garage.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE cars ADD COLUMN rating INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `drop_events` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`postId` TEXT NOT NULL, `source` TEXT NOT NULL, `kind` TEXT NOT NULL, `platform` TEXT, " +
                        "`title` TEXT NOT NULL, `url` TEXT NOT NULL, `matched` TEXT, `local` INTEGER NOT NULL, " +
                        "`postedAt` INTEGER NOT NULL, `notified` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_drop_events_postId` ON `drop_events` (`postId`)")
                // Ratings become automatic collector scores; old manual stars are recalculated on next launch.
                db.execSQL("ALTER TABLE cars ADD COLUMN ratingNote TEXT")
                db.execSQL("ALTER TABLE cars ADD COLUMN versionCount INTEGER")
            }
        }
    }
}
