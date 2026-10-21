package com.hotwheelsgarage.data

import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

data class WikiPage(val title: String, val imageUrl: String?)

/** One specific release (color/year/series) of a casting. */
data class Release(
    val modelTitle: String?,
    val modelName: String,
    val toyNumber: String?,
    val collectorNumber: String?,
    val year: String?,
    val series: String?,
    val color: String?,
    val photoFile: String?,
    val imageUrl: String? = null,
    val isTreasureHunt: Boolean = false,
    val isSuperTreasureHunt: Boolean = false,
)

data class CastingDetails(
    val title: String,
    val name: String,
    val designer: String?,
    val years: String?,
    val firstSeries: String?,
    val description: String?,
    val imageUrl: String?,
    /** Newest first. */
    val versions: List<Release>,
)

data class TreasureHunts(val year: Int, val regular: List<Release>, val supers: List<Release>)

/** Free MediaWiki API of hotwheels.fandom.com – no key required. */
class WikiApi {
    private val api = "https://hotwheels.fandom.com/api.php?format=json&formatversion=2&"
    private val wikitextCache = ConcurrentHashMap<String, String>()

    fun pageUrl(title: String) = "https://hotwheels.fandom.com/wiki/" + Http.encode(title.replace(' ', '_'))

    suspend fun search(query: String, limit: Int = 8): List<WikiPage> {
        if (query.isBlank()) return emptyList()
        val json = JSONObject(
            Http.get(
                api + "action=query&generator=search&gsrnamespace=0&gsrlimit=$limit" +
                    "&prop=pageimages&piprop=thumbnail&pithumbsize=400&gsrsearch=" + Http.encode(query)
            )
        )
        val pages = json.optJSONObject("query")?.optJSONArray("pages") ?: return emptyList()
        return (0 until pages.length())
            .map { pages.getJSONObject(it) }
            .sortedBy { it.optInt("index") }
            .map { WikiPage(it.getString("title"), it.optJSONObject("thumbnail")?.optString("source")) }
    }

    private suspend fun searchTitles(query: String, limit: Int): List<String> {
        val json = JSONObject(
            Http.get(api + "action=query&list=search&srnamespace=0&srlimit=$limit&srsearch=" + Http.encode(query))
        )
        val results = json.optJSONObject("query")?.optJSONArray("search") ?: return emptyList()
        return (0 until results.length()).map { results.getJSONObject(it).getString("title") }
    }

    /** Raw wikitext of a page, or null if the page does not exist. */
    suspend fun wikitext(title: String): String? {
        wikitextCache[title]?.let { return it }
        val json = JSONObject(
            Http.get(api + "action=parse&prop=wikitext&redirects=1&page=" + Http.encode(title))
        )
        val text = json.optJSONObject("parse")?.optString("wikitext") ?: return null
        wikitextCache[title] = text
        return text
    }

    /** Maps wiki file names to direct thumbnail URLs (400px wide). */
    suspend fun resolveImages(files: Collection<String>): Map<String, String> {
        val out = HashMap<String, String>()
        files.distinct().chunked(50).forEach { chunk ->
            val titles = chunk.joinToString("|") { "File:$it" }
            val json = JSONObject(
                Http.get(api + "action=query&prop=imageinfo&iiprop=url&iiurlwidth=400&titles=" + Http.encode(titles))
            )
            val query = json.optJSONObject("query") ?: return@forEach
            val fromTitle = HashMap<String, String>()
            query.optJSONArray("normalized")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    fromTitle[o.getString("to")] = o.getString("from")
                }
            }
            val pages = query.optJSONArray("pages") ?: return@forEach
            for (i in 0 until pages.length()) {
                val page = pages.getJSONObject(i)
                val info = page.optJSONArray("imageinfo")?.optJSONObject(0) ?: continue
                val url = info.optString("thumburl").ifBlank { info.optString("url") }
                if (url.isBlank()) continue
                val title = page.getString("title")
                out[(fromTitle[title] ?: title).removePrefix("File:")] = url
            }
        }
        return out
    }

    private suspend fun withImages(releases: List<Release>): List<Release> {
        val urls = runCatching { resolveImages(releases.mapNotNull { it.photoFile }) }.getOrDefault(emptyMap())
        return releases.map { r -> r.copy(imageUrl = r.photoFile?.let { urls[it] }) }
    }

    /**
     * Finds the release with this toy number (e.g. "JJM00") by searching the wiki and reading the
     * tables of the pages that mention it. Also tells whether it is a (Super) Treasure Hunt.
     */
    suspend fun findReleaseByToyCode(code: String): Release? {
        val matches = mutableListOf<Release>()
        for (title in searchTitles(code, 4)) {
            val text = wikitext(title) ?: continue
            if (!text.contains(code, ignoreCase = true)) continue
            releasesFrom(text, title).firstOrNull { r ->
                r.toyNumber?.split(' ', '/', ',', '-')?.any { it.equals(code, ignoreCase = true) } == true
            }?.let(matches::add)
        }
        // Different pages list different columns: start from the most complete row and fill the gaps.
        val best = matches.maxByOrNull { r ->
            listOf(r.modelTitle, r.series, r.color, r.photoFile, r.collectorNumber, r.year).count { it != null }
        } ?: return null
        val sth = matches.any { it.isSuperTreasureHunt }
        val merged = best.copy(
            series = best.series ?: matches.firstNotNullOfOrNull { it.series },
            color = best.color ?: matches.firstNotNullOfOrNull { it.color },
            photoFile = best.photoFile ?: matches.firstNotNullOfOrNull { it.photoFile },
            collectorNumber = best.collectorNumber ?: matches.firstNotNullOfOrNull { it.collectorNumber },
            year = best.year ?: matches.firstNotNullOfOrNull { it.year },
            isTreasureHunt = !sth && matches.any { it.isTreasureHunt },
            isSuperTreasureHunt = sth,
        )
        return withImages(listOf(merged)).first()
    }

    suspend fun casting(title: String): CastingDetails {
        val text = wikitext(title) ?: throw IllegalStateException("Page not found on the wiki")
        val box = WikiText.template(text, "casting")
        val imageFile = box["image"]?.let { WikiText.clean(it) }?.takeIf { it.isNotBlank() }
        val versions = releasesFrom(text, title).reversed().sortedByDescending { it.year?.toIntOrNull() ?: 0 }
        val urls = runCatching {
            resolveImages(listOfNotNull(imageFile) + versions.take(150).mapNotNull { it.photoFile })
        }.getOrDefault(emptyMap())
        return CastingDetails(
            title = title,
            name = box["name"]?.let { WikiText.clean(it) }?.takeIf { it.isNotBlank() } ?: title,
            designer = box["designer"]?.let { WikiText.clean(it) }?.takeIf { it.isNotBlank() },
            years = box["years"]?.let { WikiText.clean(it) }?.takeIf { it.isNotBlank() },
            firstSeries = box["series"]?.let { WikiText.clean(it) }?.takeIf { it.isNotBlank() },
            description = WikiText.section(text, "Description"),
            imageUrl = imageFile?.let { urls[it] } ?: versions.firstNotNullOfOrNull { r -> r.photoFile?.let { urls[it] } },
            versions = versions.map { r -> r.copy(imageUrl = r.photoFile?.let { urls[it] }) },
        )
    }

    /** Treasure Hunts for [year], falling back to the previous year if the page doesn't exist yet. */
    suspend fun treasureHunts(year: Int, fallback: Boolean = true): TreasureHunts? {
        val years = if (fallback) listOf(year, year - 1) else listOf(year)
        for (y in years) {
            val title = "$y Treasure Hunts Series"
            val text = wikitext(title) ?: continue
            val all = withImages(releasesFrom(text, title))
            if (all.isEmpty()) continue
            return TreasureHunts(y, all.filterNot { it.isSuperTreasureHunt }, all.filter { it.isSuperTreasureHunt })
        }
        return null
    }

    /** Most recent mainline releases that already have a photo on the wiki. */
    suspend fun latestReleases(year: Int, count: Int = 20): List<Release> {
        for (y in listOf(year, year - 1)) {
            val title = "List of $y Hot Wheels"
            val text = wikitext(title) ?: continue
            val recent = releasesFrom(text, title).filter { it.photoFile != null }.takeLast(count).reversed()
            if (recent.isNotEmpty()) return withImages(recent)
        }
        return emptyList()
    }

    /** Converts wiki release tables into [Release]s. Casting pages have no model column, so the page title is used. */
    fun releasesFrom(text: String, pageTitle: String): List<Release> {
        val pageYear = Regex("\\b(19[6-9]\\d|20\\d\\d)\\b").find(pageTitle)?.value
        return WikiText.tables(text).flatMap { table ->
            val c = Columns(table.headers)
            if (c.toy == null && c.col == null) return@flatMap emptyList()
            val superSection = table.heading.contains("Super Treasure", ignoreCase = true)
            val thSection = table.heading.contains("Treasure Hunt", ignoreCase = true) ||
                pageTitle.contains("Treasure Hunt", ignoreCase = true)
            table.rows.mapNotNull { row ->
                fun cell(i: Int?) = i?.let { row.getOrNull(it) }
                fun cleanCell(i: Int?) = cell(i)?.let { WikiText.clean(it) }?.takeIf { it.isNotBlank() }

                val modelRaw = cell(c.model)
                val name = if (c.model != null) cleanCell(c.model) ?: return@mapNotNull null else pageTitle
                val series = listOfNotNull(cleanCell(c.series), cleanCell(c.seriesNumber)).joinToString(" ").ifBlank { null }
                val isSuper = superSection || series?.contains("Super Treasure", ignoreCase = true) == true
                val isTh = !isSuper && (thSection || series?.contains("Treasure Hunt", ignoreCase = true) == true)
                Release(
                    modelTitle = if (c.model != null) modelRaw?.let { WikiText.firstLink(it) } else pageTitle,
                    modelName = name,
                    toyNumber = cleanCell(c.toy),
                    collectorNumber = cleanCell(c.col),
                    year = cleanCell(c.year)?.let { Regex("\\d{4}").find(it)?.value ?: it } ?: pageYear,
                    series = series,
                    color = cleanCell(c.color),
                    photoFile = cell(c.photo)?.let { WikiText.fileName(it) },
                    isTreasureHunt = isTh,
                    isSuperTreasureHunt = isSuper,
                )
            }
        }
    }

    private class Columns(headers: List<String>) {
        private val keys = headers.map { it.lowercase().replace(Regex("[\\s.]"), "") }
        private fun find(predicate: (String) -> Boolean) = keys.indexOfFirst(predicate).takeIf { it >= 0 }

        val toy = find { it.startsWith("toy") }
        val col = find { it.startsWith("col") && !it.startsWith("colo") }
        val model = find { it.startsWith("model") || it.startsWith("casting") }
        val series = find { it == "series" || it == "seriesname" }
        val seriesNumber = find { it == "series#" }
        val color = find { it == "color" || it == "bodycolor" || it == "colour" }
        val year = find { it == "year" }
        val photo = find { it.startsWith("photo") || it == "image" }
    }
}
