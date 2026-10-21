package com.hotwheelsgarage.data

import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class CarRepository(private val dao: CarDao) {
    val cars: Flow<List<Car>> = dao.observeAll()

    fun observe(id: Long): Flow<Car?> = dao.observe(id)

    /** wikiTitle -> number of copies already in the garage. */
    suspend fun ownedCounts(titles: List<String>): Map<String, Int> {
        if (titles.isEmpty()) return emptyMap()
        return dao.ownedWithTitles(titles)
            .groupBy { it.wikiTitle.orEmpty() }
            .mapValues { (_, cars) -> cars.sumOf { it.quantity } }
    }

    /** Adds the car, or bumps the quantity if the exact same release is already owned. */
    suspend fun addOrIncrement(car: Car): Long {
        if (!car.isWishlist && car.wikiTitle != null) {
            val same = dao.ownedWithTitles(listOf(car.wikiTitle)).firstOrNull {
                it.toyNumber == car.toyNumber && it.color == car.color
            }
            if (same != null) {
                dao.update(same.copy(quantity = same.quantity + 1, photoPath = same.photoPath ?: car.photoPath))
                return same.id
            }
        }
        return dao.insert(car)
    }

    suspend fun update(car: Car) = dao.update(car)

    suspend fun delete(car: Car) {
        dao.delete(car)
        car.photoPath?.let { File(it).delete() }
    }

    suspend fun exportJson(): String {
        val array = JSONArray()
        dao.getAll().forEach { array.put(it.toJson()) }
        return JSONObject().put("version", 1).put("cars", array).toString(2)
    }

    /** Returns the number of cars imported. Photos are not part of the backup. */
    suspend fun importJson(json: String): Int {
        val array = JSONObject(json).getJSONArray("cars")
        val cars = (0 until array.length()).map { array.getJSONObject(it).toCar() }
        dao.insertAll(cars)
        return cars.size
    }

    private fun Car.toJson() = JSONObject()
        .put("name", name)
        .putOpt("wikiTitle", wikiTitle)
        .putOpt("series", series)
        .putOpt("year", year)
        .putOpt("color", color)
        .putOpt("toyNumber", toyNumber)
        .putOpt("collectorNumber", collectorNumber)
        .putOpt("imageUrl", imageUrl)
        .putOpt("designer", designer)
        .putOpt("description", description)
        .put("isTreasureHunt", isTreasureHunt)
        .put("isSuperTreasureHunt", isSuperTreasureHunt)
        .put("isWishlist", isWishlist)
        .put("condition", condition)
        .put("quantity", quantity)
        .putOpt("pricePaid", pricePaid)
        .put("notes", notes)
        .put("rating", rating)
        .put("addedAt", addedAt)

    private fun JSONObject.str(key: String): String? = if (isNull(key)) null else optString(key)

    private fun JSONObject.toCar() = Car(
        name = getString("name"),
        wikiTitle = str("wikiTitle"),
        series = str("series"),
        year = str("year"),
        color = str("color"),
        toyNumber = str("toyNumber"),
        collectorNumber = str("collectorNumber"),
        imageUrl = str("imageUrl"),
        designer = str("designer"),
        description = str("description"),
        isTreasureHunt = optBoolean("isTreasureHunt"),
        isSuperTreasureHunt = optBoolean("isSuperTreasureHunt"),
        isWishlist = optBoolean("isWishlist"),
        condition = optString("condition", Car.CONDITION_CARDED),
        quantity = optInt("quantity", 1),
        pricePaid = if (isNull("pricePaid")) null else optDouble("pricePaid"),
        notes = optString("notes"),
        rating = optInt("rating", 0).coerceIn(0, 5),
        addedAt = optLong("addedAt", System.currentTimeMillis()),
    )
}
