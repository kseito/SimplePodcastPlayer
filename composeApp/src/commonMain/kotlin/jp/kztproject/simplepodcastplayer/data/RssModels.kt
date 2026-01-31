package jp.kztproject.simplepodcastplayer.data

import kotlinx.serialization.Serializable

@Serializable
data class RssChannel(val title: String = "", val description: String = "", val items: List<RssItem> = emptyList())

@Serializable
data class RssItem(
    val title: String = "",
    val description: String = "",
    val pubDate: String = "",
    val enclosure: RssEnclosure? = null,
    val duration: String = "",
    val guid: String = "",
)

@Serializable
data class RssEnclosure(val url: String = "", val type: String = "", val length: String = "")

data class ParsedEpisode(
    val id: String,
    val title: String,
    val description: String,
    val publishedAt: String,
    val audioUrl: String,
    val duration: Long, // Duration in seconds
    val trackId: Long? = null,
)
