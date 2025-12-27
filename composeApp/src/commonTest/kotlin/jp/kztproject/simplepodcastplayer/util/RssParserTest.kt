package jp.kztproject.simplepodcastplayer.util

import jp.kztproject.simplepodcastplayer.data.RssChannel
import jp.kztproject.simplepodcastplayer.data.RssEnclosure
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RssParserTest {
    private val parser = RssParser()

    @Test
    fun parseRssFeed_normalRss_returnsValidChannel() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0">
                <channel>
                    <title>Test Podcast</title>
                    <description>Test Description</description>
                    <item>
                        <title>Episode 1</title>
                        <description>Episode 1 Description</description>
                        <pubDate>Wed, 15 Dec 2024 10:00:00 GMT</pubDate>
                        <guid>episode-1</guid>
                        <itunes:duration>1:30:00</itunes:duration>
                        <enclosure url="https://example.com/episode1.mp3" type="audio/mpeg" length="12345"/>
                    </item>
                </channel>
            </rss>
        """.trimIndent()

        val result = parser.parseRssFeed(xml)

        assertEquals("Test Podcast", result.title)
        assertEquals("Test Description", result.description)
        assertEquals(1, result.items.size)

        val episode = result.items[0]
        assertEquals("Episode 1", episode.title)
        assertEquals("Episode 1 Description", episode.description)
        assertEquals("Wed, 15 Dec 2024 10:00:00 GMT", episode.pubDate)
        assertEquals("episode-1", episode.guid)
        assertEquals("1:30:00", episode.duration)
        assertNotNull(episode.enclosure)
        assertEquals("https://example.com/episode1.mp3", episode.enclosure?.url)
    }

    @Test
    fun parseRssFeed_emptyContent_returnsEmptyChannel() {
        val result = parser.parseRssFeed("")

        assertEquals("", result.title)
        assertEquals("", result.description)
        assertTrue(result.items.isEmpty())
    }

    @Test
    fun parseRssFeed_invalidXml_returnsEmptyChannel() {
        val invalidXml = "<invalid><xml"

        val result = parser.parseRssFeed(invalidXml)

        assertEquals("", result.title)
        assertEquals("", result.description)
        assertTrue(result.items.isEmpty())
    }

    @Test
    fun parseRssFeed_cdataContent_cleansCdata() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0">
                <channel>
                    <title><![CDATA[CDATA Title]]></title>
                    <description><![CDATA[CDATA Description]]></description>
                    <item>
                        <title><![CDATA[Episode Title]]></title>
                        <description><![CDATA[Episode Description]]></description>
                        <guid>ep1</guid>
                    </item>
                </channel>
            </rss>
        """.trimIndent()

        val result = parser.parseRssFeed(xml)

        assertEquals("CDATA Title", result.title)
        assertEquals("CDATA Description", result.description)
        assertEquals("Episode Title", result.items[0].title)
        assertEquals("Episode Description", result.items[0].description)
    }

    @Test
    fun parseRssFeed_htmlEntities_decodesEntities() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0">
                <channel>
                    <title>Test &amp; Podcast</title>
                    <description>Test &lt;Description&gt; &quot;Quote&quot;</description>
                    <item>
                        <title>Episode &apos;1&apos;</title>
                        <description>Test &amp; Description</description>
                        <guid>ep1</guid>
                    </item>
                </channel>
            </rss>
        """.trimIndent()

        val result = parser.parseRssFeed(xml)

        assertEquals("Test & Podcast", result.title)
        assertEquals("Test <Description> \"Quote\"", result.description)
        assertEquals("Episode '1'", result.items[0].title)
        assertEquals("Test & Description", result.items[0].description)
    }

    @Test
    fun toEpisodes_normalConversion_returnsEpisodeList() {
        val channel = RssChannel(
            title = "Test Podcast",
            description = "Test Description",
            items = listOf(
                jp.kztproject.simplepodcastplayer.data.RssItem(
                    title = "Episode 1",
                    description = "Desc 1",
                    pubDate = "Wed, 15 Dec 2024 10:00:00 GMT",
                    guid = "ep1",
                    duration = "1:30:00",
                    enclosure = RssEnclosure(
                        url = "https://example.com/ep1.mp3",
                        type = "audio/mpeg",
                        length = "12345",
                    ),
                ),
            ),
        )

        val episodes = channel.toEpisodes()

        assertEquals(1, episodes.size)
        val episode = episodes[0]
        assertEquals("ep1", episode.id)
        assertEquals("Episode 1", episode.title)
        assertEquals("Desc 1", episode.description)
        assertEquals("Dec 15, 2024", episode.publishedAt)
        assertEquals(5400L, episode.duration) // 1:30:00 = 5400 seconds
        assertEquals("https://example.com/ep1.mp3", episode.audioUrl)
    }

    @Test
    fun toEpisodes_emptyList_returnsEmptyList() {
        val channel = RssChannel(
            title = "Test Podcast",
            description = "Test Description",
            items = emptyList(),
        )

        val episodes = channel.toEpisodes()

        assertTrue(episodes.isEmpty())
    }

    @Test
    fun toEpisodes_noGuid_usesFallbackId() {
        val channel = RssChannel(
            title = "Test Podcast",
            description = "Test Description",
            items = listOf(
                jp.kztproject.simplepodcastplayer.data.RssItem(
                    title = "Episode 1",
                    description = "Desc 1",
                    pubDate = "Wed, 15 Dec 2024 10:00:00 GMT",
                    guid = "",
                    duration = "30:00",
                    enclosure = null,
                ),
            ),
        )

        val episodes = channel.toEpisodes()

        assertEquals(1, episodes.size)
        assertEquals("Test Podcast_0", episodes[0].id)
    }
}
