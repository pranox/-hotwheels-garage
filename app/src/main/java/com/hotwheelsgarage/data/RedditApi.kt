package com.hotwheelsgarage.data

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader

data class RedditPost(val title: String, val url: String, val author: String, val thumbnail: String?)

/** Top posts of the week from r/HotWheels via the public RSS feed (no key). */
object RedditApi {
    suspend fun topOfWeek(): List<RedditPost> {
        val xml = Http.get("https://www.reddit.com/r/HotWheels/top/.rss?t=week&limit=30")
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(StringReader(xml))

        val posts = mutableListOf<RedditPost>()
        var inEntry = false
        var inAuthor = false
        var title = ""
        var link = ""
        var author = ""
        var thumb: String? = null
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "entry" -> { inEntry = true; title = ""; link = ""; author = ""; thumb = null }
                    "author" -> inAuthor = true
                    "title" -> if (inEntry) title = parser.nextText()
                    "name" -> if (inEntry && inAuthor) author = parser.nextText()
                    "link" -> if (inEntry) link = parser.getAttributeValue(null, "href").orEmpty()
                    "media:thumbnail" -> if (inEntry) thumb = parser.getAttributeValue(null, "url")
                }
                XmlPullParser.END_TAG -> when (parser.name) {
                    "author" -> inAuthor = false
                    "entry" -> {
                        inEntry = false
                        if (title.isNotBlank() && link.isNotBlank()) posts += RedditPost(title, link, author, thumb)
                    }
                }
            }
        }
        return posts
    }
}
