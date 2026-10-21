package com.hotwheelsgarage.data

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject

data class AiGuess(
    val model: String?,
    val color: String?,
    val year: String?,
    val isTreasureHunt: Boolean,
    val isSuperTreasureHunt: Boolean,
    val confidence: Double,
)

/** Optional photo identification with Google's Gemini API (free tier, needs a key from aistudio.google.com). */
object GeminiApi {
    private const val PROMPT = """You are a Hot Wheels die-cast expert. Identify the Hot Wheels car in this photo.
The photo may show a carded car, a loose car, or the metal base underneath (which has the casting name and year cast in).
Reply with JSON only, no markdown:
{"model": "casting name exactly as used on the Hot Wheels Fandom wiki, or null if this is not a Hot Wheels car",
 "color": "main body color or null",
 "year": "release year if printed on the card, else null",
 "treasureHunt": true if the circle-flame Treasure Hunt logo is visible,
 "superTreasureHunt": true if it has Spectraflame paint, Real Riders rubber tires or the gold circle-flame logo,
 "confidence": number from 0 to 1}"""

    suspend fun identify(apiKey: String, model: String, jpeg: ByteArray): AiGuess? {
        val body = JSONObject()
            .put(
                "contents", JSONArray().put(
                    JSONObject().put(
                        "parts", JSONArray()
                            .put(JSONObject().put("text", PROMPT))
                            .put(
                                JSONObject().put(
                                    "inline_data", JSONObject()
                                        .put("mime_type", "image/jpeg")
                                        .put("data", Base64.encodeToString(jpeg, Base64.NO_WRAP))
                                )
                            )
                    )
                )
            )
            .put("generationConfig", JSONObject().put("response_mime_type", "application/json"))

        val response = JSONObject(
            Http.postJson(
                "https://generativelanguage.googleapis.com/v1beta/models/${Http.encode(model)}:generateContent",
                body.toString(),
                mapOf("x-goog-api-key" to apiKey),
            )
        )
        val parts = response.optJSONArray("candidates")?.optJSONObject(0)
            ?.optJSONObject("content")?.optJSONArray("parts") ?: return null
        val text = (0 until parts.length()).joinToString("") { parts.getJSONObject(it).optString("text") }
            .trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val json = JSONObject(text)
        fun str(key: String) = if (json.isNull(key)) null else json.optString(key).takeIf { it.isNotBlank() }
        return AiGuess(
            model = str("model"),
            color = str("color"),
            year = str("year"),
            isTreasureHunt = json.optBoolean("treasureHunt"),
            isSuperTreasureHunt = json.optBoolean("superTreasureHunt"),
            confidence = json.optDouble("confidence", 0.0),
        )
    }
}
