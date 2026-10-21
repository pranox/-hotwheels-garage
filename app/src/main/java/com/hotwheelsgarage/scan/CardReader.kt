package com.hotwheelsgarage.scan

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

data class ScanHints(
    /** Mattel toy numbers like "JJM00" or "J3247". */
    val toyCodes: List<String>,
    /** e.g. "009/250". */
    val collectorNumber: String?,
    /** Lines most likely to be the model name, biggest text first. */
    val nameLines: List<String>,
    /** UPC/EAN numbers. */
    val barcodes: List<String>,
)

/** Reads the card back (or the car's base) with on-device ML Kit – free and offline. */
object CardReader {
    // OCR often reads 0 as O and 1 as I/L, so accept those in the digit positions and fix them up.
    private val toyCodeRegex = Regex("\\b([A-Z]{3}[0-9OIL]{2}|[A-Z][0-9OIL]{4})\\b")
    private val collectorRegex = Regex("\\b(\\d{1,3})\\s?/\\s?(\\d{2,3})\\b")

    private val stopWords = listOf(
        "HOT WHEELS", "HOTWHEELS", "MATTEL", "WARNING", "CHOKING", "HAZARD", "SMALL PARTS", "AGES", "AGE ",
        "YEARS", "NOT FOR", "CHILDREN", "UNDER", "MADE IN", "MALAYSIA", "CHINA", "THAILAND", "INDONESIA",
        "WWW", ".COM", "COLLECT", "ASSORTMENT", "CONTENTS", "IMPORTED", "DISTRIBUTED", "LTD", "INC",
        "PATENT", "PAT.", "1:64", "TM", "©", "®", "™", "RETAIN", "PACKAGE", "INFORMATION", "ATTENTION",
        "ADVERTENCIA", "ATENCIÓN", "PEQUEÑAS", "PIÈCES", "BASIC CAR", "FOR AGES",
    )

    suspend fun read(context: Context, uri: Uri): ScanHints {
        val image = InputImage.fromFilePath(context, uri)
        val text = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS).process(image).await()
        val barcodes = runCatching {
            BarcodeScanning.getClient().process(image).await()
        }.getOrDefault(emptyList())

        val raw = text.text.uppercase()
        Log.d("CardReader", "OCR: ${raw.replace("\n", " | ")}")
        // Whole blocks first (a name printed over two lines, e.g. "'87 BUICK REGAL" + "GNX"), then single lines.
        val lines = text.textBlocks.map { block ->
            block.lines.joinToString(" ") { it.text.trim() } to (block.lines.maxOfOrNull { it.boundingBox?.height() ?: 0 } ?: 0)
        } + text.textBlocks.flatMap { it.lines }.map { it.text.trim() to (it.boundingBox?.height() ?: 0) }

        val nameLines = lines
            .map { (t, h) -> t.replace(Regex("[^\\p{L}\\p{N}' &.-]"), " ").replace(Regex("\\s+"), " ").trim() to h }
            .filter { (t, _) ->
                val upper = t.uppercase()
                t.length in 3..40 &&
                    t.count { it.isLetter() } >= 3 &&
                    !toyCodeRegex.matches(upper) &&
                    stopWords.none { upper.contains(it) }
            }
            .sortedByDescending { it.second }
            .map { it.first }
            .distinctBy { it.lowercase() }
            .take(4)

        return ScanHints(
            toyCodes = toyCodeRegex.findAll(raw)
                .map { it.value }
                .filter { code -> code.any { it.isDigit() } } // skips words like "GRILL"
                .map(::fixToyCode)
                .distinct()
                .toList(),
            // Prefer "009/250" (mainline number) over "1/10" (position within the series).
            collectorNumber = collectorRegex.findAll(raw).maxByOrNull { it.groupValues[2].length }
                ?.let { "${it.groupValues[1]}/${it.groupValues[2]}" },
            nameLines = nameLines,
            barcodes = barcodes
                .filter { it.format in setOf(Barcode.FORMAT_UPC_A, Barcode.FORMAT_UPC_E, Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8) }
                .mapNotNull { it.rawValue }
                .distinct(),
        )
    }

    private fun fixToyCode(code: String): String {
        val letters = if (code[1].isLetter()) 3 else 1
        return code.take(letters) + code.drop(letters).map {
            when (it) {
                'O' -> '0'
                'I', 'L' -> '1'
                else -> it
            }
        }.joinToString("")
    }
}
