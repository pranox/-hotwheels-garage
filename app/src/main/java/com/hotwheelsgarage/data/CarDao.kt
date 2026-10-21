package com.hotwheelsgarage.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CarDao {
    @Query("SELECT * FROM cars ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<Car>>

    @Query("SELECT * FROM cars WHERE id = :id")
    fun observe(id: Long): Flow<Car?>

    @Query("SELECT * FROM cars")
    suspend fun getAll(): List<Car>

    @Query("SELECT * FROM cars WHERE isWishlist = 0 AND wikiTitle IN (:titles)")
    suspend fun ownedWithTitles(titles: List<String>): List<Car>

    @Insert
    suspend fun insert(car: Car): Long

    @Insert
    suspend fun insertAll(cars: List<Car>)

    @Update
    suspend fun update(car: Car)

    @Delete
    suspend fun delete(car: Car)
}
