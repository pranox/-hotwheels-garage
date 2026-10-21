package com.hotwheelsgarage.data

import org.json.JSONObject

/** UPCitemdb free trial endpoint (about 100 lookups per day, no key). */
object UpcLookup {
    private val noise = Regex(
        "hot ?wheels|mattel|die-?cast|1:64|scale|vehicle|toy car|basic car|\\bcar\\b|\\bpack\\b|\\(.*?\\)|[-–|,]",
        RegexOption.IGNORE_CASE,
    )

    /** Returns a cleaned product title usable as a wiki search, or null. */
    suspend fun title(upc: String): String? {
        val json = JSONObject(Http.get("https://api.upcitemdb.com/prod/trial/lookup?upc=" + Http.encode(upc)))
        val raw = json.optJSONArray("items")?.optJSONObject(0)?.optString("title") ?: return null
        return noise.replace(raw, " ").replace(Regex("\\s+"), " ").trim().takeIf { it.length >= 3 }
    }
}
