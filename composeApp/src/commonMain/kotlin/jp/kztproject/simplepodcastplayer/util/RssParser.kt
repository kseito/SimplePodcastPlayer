package jp.kztproject.simplepodcastplayer.util

import jp.kztproject.simplepodcastplayer.data.ParsedEpisode
import jp.kztproject.simplepodcastplayer.data.RssChannel
import jp.kztproject.simplepodcastplayer.data.RssEnclosure
import jp.kztproject.simplepodcastplayer.data.RssItem

class RssParser {
    fun parseRssFeed(xmlContent: String): RssChannel {
        try {
            // Simple XML parsing for RSS feed
            val items = mutableListOf<RssItem>()

            // Extract channel title and description
            val channelTitle = extractTextBetween(xmlContent, "<title>", "</title>")
                .split("<title>")[1].split("</title>")[0].trim()
            val channelDescription = extractTextBetween(xmlContent, "<description>", "</description>")
                .split("<description>")[1].split("</description>")[0].trim()

            // Extract all items
            val itemMatches = xmlContent.split("<item>").drop(1)

            for (itemXml in itemMatches) {
                val endIndex = itemXml.indexOf("</item>")
                if (endIndex == -1) continue

                val itemContent = itemXml.substring(0, endIndex)

                val title = extractSingleValue(itemContent, "title")
                val description = extractSingleValue(itemContent, "description")
                val pubDate = extractSingleValue(itemContent, "pubDate")
                val guid = extractSingleValue(itemContent, "guid")
                val duration = extractSingleValue(itemContent, "itunes:duration")

                // Extract enclosure
                val enclosure = extractEnclosure(itemContent)

                items.add(
                    RssItem(
                        title = cleanText(title),
                        description = cleanText(description),
                        pubDate = pubDate,
                        enclosure = enclosure,
                        duration = duration,
                        guid = guid.ifEmpty { title },
                    ),
                )
            }

            return RssChannel(
                title = cleanText(channelTitle),
                description = cleanText(channelDescription),
                items = items,
            )
        } catch (_: Exception) {
            return RssChannel()
        }
    }

    @Suppress("ReturnCount")
    private fun extractTextBetween(text: String, startTag: String, endTag: String): String {
        val startIndex = text.indexOf(startTag)
        if (startIndex == -1) return ""

        val endIndex = text.indexOf(endTag, startIndex)
        if (endIndex == -1) return ""

        return text.substring(startIndex, endIndex + endTag.length)
    }

    @Suppress("ReturnCount")
    private fun extractSingleValue(content: String, tagName: String): String {
        val startTag = "<$tagName>"
        val endTag = "</$tagName>"

        val startIndex = content.indexOf(startTag)
        if (startIndex == -1) return ""

        val valueStart = startIndex + startTag.length
        val endIndex = content.indexOf(endTag, valueStart)
        if (endIndex == -1) return ""

        return content.substring(valueStart, endIndex).trim()
    }

    @Suppress("ReturnCount")
    private fun extractEnclosure(content: String): RssEnclosure? {
        val enclosurePattern = "<enclosure"
        val startIndex = content.indexOf(enclosurePattern)
        if (startIndex == -1) return null

        val endIndex = content.indexOf(">", startIndex)
        if (endIndex == -1) return null

        val enclosureTag = content.substring(startIndex, endIndex + 1)

        val url = extractAttribute(enclosureTag, "url")
        val type = extractAttribute(enclosureTag, "type")
        val length = extractAttribute(enclosureTag, "length")

        return if (url.isNotEmpty()) {
            RssEnclosure(url = url, type = type, length = length)
        } else {
            null
        }
    }

    @Suppress("ReturnCount")
    private fun extractAttribute(tag: String, attributeName: String): String {
        val pattern = "$attributeName=\""
        val startIndex = tag.indexOf(pattern)
        if (startIndex == -1) return ""

        val valueStart = startIndex + pattern.length
        val endIndex = tag.indexOf("\"", valueStart)
        if (endIndex == -1) return ""

        return tag.substring(valueStart, endIndex)
    }

    private fun cleanText(text: String): String = text
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&apos;", "'")
        .replace("<![CDATA[", "")
        .replace("]]>", "")
        .trim()
}

fun RssChannel.toEpisodes(): List<ParsedEpisode> = items.mapIndexed { index, item ->
    ParsedEpisode(
        id = item.guid.ifEmpty { "${title}_$index" },
        title = item.title,
        description = item.description,
        publishedAt = formatRssDate(item.pubDate),
        audioUrl = item.enclosure?.url ?: "",
        duration = parseItunesDuration(item.duration),
    )
}

private fun formatRssDate(rssDate: String): String = try {
    // RSS dates are typically in format: "Wed, 15 Dec 2024 10:00:00 GMT"
    if (rssDate.contains(",")) {
        val parts = rssDate.split(",")[1].trim().split(" ")
        if (parts.size >= DATE_PARTS_MIN_SIZE) {
            val day = parts[0]
            val month = parts[1]
            val year = parts[2]
            "$month $day, $year"
        } else {
            rssDate
        }
    } else {
        rssDate
    }
} catch (_: Exception) {
    rssDate
}

private fun parseItunesDuration(duration: String): Long = try {
    if (duration.contains(":")) {
        val parts = duration.split(":")
        when (parts.size) {
            2 -> {
                // MM:SS format
                val minutes = parts[0].toLongOrNull() ?: 0
                val seconds = parts[1].toLongOrNull() ?: 0
                minutes * SECONDS_PER_MINUTE + seconds
            }

            TIME_PARTS_HMS -> {
                // HH:MM:SS format
                val hours = parts[0].toLongOrNull() ?: 0
                val minutes = parts[1].toLongOrNull() ?: 0
                val seconds = parts[2].toLongOrNull() ?: 0
                hours * SECONDS_PER_HOUR + minutes * SECONDS_PER_MINUTE + seconds
            }

            else -> 0
        }
    } else {
        // Assume seconds
        duration.toLongOrNull() ?: 0
    }
} catch (_: Exception) {
    0
}

private const val SECONDS_PER_MINUTE = 60L
private const val SECONDS_PER_HOUR = 3600L
private const val DATE_PARTS_MIN_SIZE = 3
private const val TIME_PARTS_HMS = 3
