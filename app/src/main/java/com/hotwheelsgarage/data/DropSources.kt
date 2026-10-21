package com.hotwheelsgarage.data

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.time.OffsetDateTime

data class CommunityPost(
    val id: String,
    val source: String,
    val title: String,
    val text: String,
    val url: String,
    val postedAt: Long,
)

/** Free, public, read-only feeds: subreddit RSS and Telegram's public channel preview pages. */
object DropSources {
    suspend fun reddit(subreddit: String): List<CommunityPost> {
        val name = subreddit.trim().removePrefix("r/").removePrefix("/r/")
        val xml = Http.get("https://www.reddit.com/r/${Http.encode(name)}/new/.rss?limit=50")
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(StringReader(xml))

        val posts = mutableListOf<CommunityPost>()
        var inEntry = false
        var id = ""
        var title = ""
        var content = ""
        var link = ""
        var published = ""
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> if (parser.name == "entry") {
                    inEntry = true; id = ""; title = ""; content = ""; link = ""; published = ""
                } else if (inEntry) when (parser.name) {
                    "id" -> id = parser.nextText()
                    "title" -> title = parser.nextText()
                    "content" -> content = parser.nextText()
                    "link" -> link = parser.getAttributeValue(null, "href").orEmpty()
                    "published" -> published = parser.nextText()
                }
                XmlPullParser.END_TAG -> if (parser.name == "entry") {
                    inEntry = false
                    val time = runCatching { OffsetDateTime.parse(published).toInstant().toEpochMilli() }.getOrNull()
                    if (id.isNotBlank() && time != null) {
                        posts += CommunityPost("reddit:$id", "r/$name", title, plainText(content), link, time)
                    }
                }
            }
        }
        return posts
    }

    private val telegramText = Regex("class=\"tgme_widget_message_text[^\"]*\"[^>]*>(.*?)</div>", RegexOption.DOT_MATCHES_ALL)
    private val telegramTime = Regex("<time[^>]*datetime=\"([^\"]+)\"")

    suspend fun telegram(channel: String): List<CommunityPost> {
        val name = channel.trim().removePrefix("@").removePrefix("https://t.me/").removePrefix("t.me/")
        val page = Http.get("https://t.me/s/${Http.encode(name)}")
        // One chunk per message, so a photo-only post can't borrow the next message's text.
        return page.split("data-post=\"").drop(1).mapNotNull { chunk ->
            val post = chunk.substringBefore('"')
            val text = telegramText.find(chunk)?.groupValues?.get(1)?.let(::plainText)
            val time = telegramTime.find(chunk)?.groupValues?.get(1)
                ?.let { runCatching { OffsetDateTime.parse(it).toInstant().toEpochMilli() }.getOrNull() }
            if (text.isNullOrBlank() || time == null) return@mapNotNull null
            CommunityPost("telegram:$post", "t.me/$name", text.lineSequence().first().take(120), text, "https://t.me/$post", time)
        }
    }

    private fun plainText(html: String): String =
        html.replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<[^>]+>"), " ")
            .replace("&amp;", "&").replace("&quot;", "\"").replace("&#39;", "'").replace("&lt;", "<").replace("&gt;", ">")
            .replace(Regex("[ \\t]+"), " ")
            .trim()
}
