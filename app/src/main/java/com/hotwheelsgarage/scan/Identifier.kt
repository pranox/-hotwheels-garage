package com.hotwheelsgarage.scan

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import com.hotwheelsgarage.data.AiGuess
import com.hotwheelsgarage.data.GeminiApi
import com.hotwheelsgarage.data.Release
import com.hotwheelsgarage.data.Settings
import com.hotwheelsgarage.data.UpcLookup
import com.hotwheelsgarage.data.WikiApi
import java.io.ByteArrayOutputStream
import java.io.File

data class Candidate(
    val title: String,
    val imageUrl: String?,
    /** Why this was suggested, e.g. "Toy # JJM00". */
    val reason: String,
    /** Exact release, when found via toy number. */
    val release: Release? = null,
    val ownedCount: Int = 0,
)

data class IdentifyResult(
    val hints: ScanHints,
    val aiGuess: AiGuess?,
    val candidates: List<Candidate>,
    val notes: List<String>,
)

/**
 * Turns a photo into a ranked list of wiki pages:
 * toy number on the card > Gemini guess (if a key is set) > barcode > biggest text on the card/base.
 */
class Identifier(private val wiki: WikiApi, private val settings: Settings) {

    suspend fun identify(context: Context, photo: File, progress: (String) -> Unit): IdentifyResult {
        progress("Reading text & barcode…")
        val hints = CardReader.read(context, Uri.fromFile(photo))
        val notes = mutableListOf<String>()
        val found = LinkedHashMap<String, Candidate>()
        fun add(c: Candidate) {
            if (isCastingPage(c.title)) found.putIfAbsent(c.title.lowercase(), c)
        }

        for (code in hints.toyCodes.take(3)) {
            progress("Looking up toy number $code…")
            runCatching { wiki.findReleaseByToyCode(code) }.getOrNull()?.let { r ->
                r.modelTitle?.let { add(Candidate(it, r.imageUrl, "Toy # $code", r)) }
            }
        }

        var guess: AiGuess? = null
        if (settings.geminiKey.isNotBlank()) {
            progress("Asking Gemini to look at the photo…")
            guess = runCatching { GeminiApi.identify(settings.geminiKey, settings.geminiModel, uploadJpeg(photo)) }
                .onFailure { notes += "Gemini failed: ${it.message}" }
                .getOrNull()
            guess?.model?.let { model ->
                runCatching { wiki.search(model, 3) }.getOrDefault(emptyList())
                    .forEach { add(Candidate(it.title, it.imageUrl, "AI guess: $model")) }
            }
        }

        hints.barcodes.firstOrNull()?.let { upc ->
            progress("Looking up barcode…")
            runCatching { UpcLookup.title(upc) }.getOrNull()?.let { title ->
                runCatching { wiki.search(title, 3) }.getOrDefault(emptyList())
                    .forEach { add(Candidate(it.title, it.imageUrl, "Barcode: $title")) }
            }
        }

        for (line in hints.nameLines.take(3)) {
            if (found.size >= 10) break
            progress("Searching the wiki for \"$line\"…")
            runCatching { wiki.search(line, 4) }.getOrDefault(emptyList())
                .forEach { add(Candidate(it.title, it.imageUrl, "Text: \"$line\"")) }
        }

        if (hints.toyCodes.isEmpty() && hints.nameLines.isEmpty() && guess == null) {
            notes += "No readable text found. Try the back of the card or the base of the car, in good light."
        }
        return IdentifyResult(hints, guess, found.values.take(12), notes)
    }

    private fun isCastingPage(title: String) =
        !title.startsWith("List of", ignoreCase = true) &&
            !title.contains("Treasure Hunts", ignoreCase = true) &&
            !title.contains("Mini Collection", ignoreCase = true)

    /** Downscaled JPEG so the upload stays small. */
    private fun uploadJpeg(file: File): ByteArray {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= 1280) sample *= 2
        val bitmap = BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: return file.readBytes()
        return ByteArrayOutputStream().use { out ->
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, out)
            bitmap.recycle()
            out.toByteArray()
        }
    }
}
