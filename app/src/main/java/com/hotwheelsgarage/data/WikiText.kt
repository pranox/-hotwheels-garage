package com.hotwheelsgarage.data

/** Small, forgiving parser for the MediaWiki markup used on the Hot Wheels wiki. */
object WikiText {
    data class Table(val heading: String, val headers: List<String>, val rows: List<List<String>>)

    private val headingRegex = Regex("^(=+)\\s*(.*?)\\s*\\1$")
    private val rowspanRegex = Regex("rowspan\\s*=\\s*\"?(\\d+)", RegexOption.IGNORE_CASE)
    private val entities = mapOf(
        "nbsp" to " ", "amp" to "&", "quot" to "\"", "apos" to "'", "lt" to "<", "gt" to ">",
        "ndash" to "–", "mdash" to "—", "times" to "×", "eacute" to "é", "deg" to "°",
    )

    /** Parses every top-level `{| ... |}` table, expanding rowspans so each row has all its cells. */
    fun tables(text: String): List<Table> {
        val result = mutableListOf<Table>()
        var heading = ""
        var tableHeading = ""
        var depth = 0
        var headers = listOf<String>()
        var rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        var rowIsHeader = false
        val carry = HashMap<Int, Pair<String, Int>>()

        fun fillCarried() {
            while (true) {
                val (value, remaining) = carry[row.size] ?: return
                if (remaining <= 0) return
                carry[row.size] = value to remaining - 1
                row.add(value)
            }
        }

        fun addCell(raw: String) {
            val (attrs, value) = splitAttrs(raw)
            if (rowIsHeader) {
                row.add(value)
                return
            }
            fillCarried()
            val span = rowspanRegex.find(attrs)?.groupValues?.get(1)?.toIntOrNull() ?: 1
            if (span > 1) carry[row.size] = value to span - 1
            row.add(value)
        }

        fun flushRow() {
            if (row.isNotEmpty()) {
                if (rowIsHeader) {
                    if (headers.isEmpty()) headers = row.map { clean(it) }
                } else {
                    fillCarried()
                    rows.add(row.toList())
                }
            }
            row = mutableListOf()
            rowIsHeader = false
        }

        for (rawLine in text.lines()) {
            val line = rawLine.trim()
            if (depth == 0) {
                val match = headingRegex.matchEntire(line)
                if (match != null) {
                    heading = clean(match.groupValues[2])
                    continue
                }
            }
            when {
                line.startsWith("{|") -> {
                    depth++
                    if (depth == 1) {
                        tableHeading = heading
                        headers = emptyList()
                        rows = mutableListOf()
                        row = mutableListOf()
                        rowIsHeader = false
                        carry.clear()
                    }
                }
                depth == 0 -> Unit
                line.startsWith("|}") -> {
                    if (depth == 1) {
                        flushRow()
                        result.add(Table(tableHeading, headers, rows))
                    }
                    depth--
                }
                depth > 1 -> Unit
                line.startsWith("|-") -> flushRow()
                line.startsWith("|+") -> Unit
                line.startsWith("!") -> {
                    if (row.isNotEmpty() && !rowIsHeader) flushRow()
                    rowIsHeader = true
                    splitTopLevel(line.substring(1), "!!").forEach(::addCell)
                }
                line.startsWith("|") -> splitTopLevel(line.substring(1), "||").forEach(::addCell)
                row.isNotEmpty() -> row[row.lastIndex] = row.last() + "\n" + line
            }
        }
        return result
    }

    /** Reads `{{name|key = value|...}}` into a map with lowercase keys. */
    fun template(text: String, name: String): Map<String, String> {
        val start = Regex("\\{\\{\\s*$name\\s*\\|", RegexOption.IGNORE_CASE).find(text) ?: return emptyMap()
        var depth = 0
        var i = start.range.first
        var end = -1
        while (i < text.length - 1) {
            when {
                text.startsWith("{{", i) -> { depth++; i += 2 }
                text.startsWith("}}", i) -> {
                    depth--
                    i += 2
                    if (depth == 0) { end = i - 2; break }
                }
                else -> i++
            }
        }
        if (end < 0) return emptyMap()
        return splitTopLevel(text.substring(start.range.last + 1, end), "|").mapNotNull { part ->
            val eq = part.indexOf('=')
            if (eq < 0) null else part.substring(0, eq).trim().lowercase() to part.substring(eq + 1).trim()
        }.toMap()
    }

    /** Plain text of the first paragraphs of a `==title==` section. */
    fun section(text: String, title: String): String? {
        val start = Regex("^==\\s*$title\\s*==\\s*$", setOf(RegexOption.MULTILINE, RegexOption.IGNORE_CASE))
            .find(text) ?: return null
        val rest = text.substring(start.range.last + 1)
        val end = Regex("^==", RegexOption.MULTILINE).find(rest)?.range?.first ?: rest.length
        return rest.substring(0, end)
            .split(Regex("\\n\\s*\\n"))
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("{|") && !it.startsWith("[[File", true) }
            .take(2)
            .joinToString("\n\n") { clean(it) }
            .takeIf { it.isNotBlank() }
    }

    fun clean(s: String): String {
        var t = s
        t = Regex("<ref[^>]*/>|<ref[^>]*>.*?</ref>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)).replace(t, "")
        t = Regex("\\[\\[(?:File|Image|Category):[^\\]]*\\]\\]", RegexOption.IGNORE_CASE).replace(t, "")
        t = Regex("\\[\\[[^\\]|]*\\|([^\\]]*)\\]\\]").replace(t) { it.groupValues[1] }
        t = Regex("\\[\\[([^\\]]*)\\]\\]").replace(t) { it.groupValues[1] }
        t = Regex("\\[https?://\\S+\\s+([^\\]]*)\\]").replace(t) { it.groupValues[1] }
        repeat(2) { t = Regex("\\{\\{[^{}]*\\}\\}").replace(t, "") }
        t = Regex("<br\\s*/?>", RegexOption.IGNORE_CASE).replace(t, " ")
        t = Regex("<[^>]+>").replace(t, "")
        t = t.replace("'''", "").replace("''", "")
        t = Regex("&(#\\d+|[a-z]+);").replace(t) { m ->
            val name = m.groupValues[1]
            if (name.startsWith("#")) name.drop(1).toIntOrNull()?.toChar()?.toString() ?: m.value
            else entities[name] ?: m.value
        }
        return t.replace(Regex("\\s+"), " ").trim()
    }

    /** Target of the first normal `[[link]]`, i.e. the page it points to. */
    fun firstLink(s: String): String? =
        Regex("\\[\\[(?!(?:File|Image|Category):)([^\\]|#]+)", RegexOption.IGNORE_CASE)
            .find(s)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }

    /** File name of the first `[[File:...]]`, skipping the wiki's "not available" placeholder. */
    fun fileName(s: String): String? =
        Regex("\\[\\[(?:File|Image):([^\\]|]+)", RegexOption.IGNORE_CASE)
            .find(s)?.groupValues?.get(1)?.trim()
            ?.takeUnless { it.startsWith("Image Not Available", ignoreCase = true) || it.isEmpty() }

    /** Splits on [separator] only outside `[[ ]]` and `{{ }}`. */
    fun splitTopLevel(s: String, separator: String): List<String> {
        val out = mutableListOf<String>()
        var depth = 0
        var start = 0
        var i = 0
        while (i < s.length) {
            when {
                s.startsWith("[[", i) || s.startsWith("{{", i) -> { depth++; i += 2 }
                depth > 0 && (s.startsWith("]]", i) || s.startsWith("}}", i)) -> { depth--; i += 2 }
                depth == 0 && s.startsWith(separator, i) -> {
                    out.add(s.substring(start, i))
                    i += separator.length
                    start = i
                }
                else -> i++
            }
        }
        out.add(s.substring(start))
        return out
    }

    /** `style="..." | value` -> ("style=...", "value"). */
    private fun splitAttrs(cell: String): Pair<String, String> {
        val parts = splitTopLevel(cell, "|")
        if (parts.size >= 2 && parts[0].contains('=') && !parts[0].contains("[[")) {
            return parts[0] to parts.drop(1).joinToString("|").trim()
        }
        return "" to cell.trim()
    }
}
