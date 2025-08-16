package jp.kztproject.simplepodcastplayer.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PodcastTest {

    private fun createBasePodcast() = Podcast(
        trackId = 1L,
        trackName = "Test Podcast",
        artistName = "Test Artist",
    )

    // bestArtworkUrl() method tests - all conditions covered
    @Test
    fun bestArtworkUrl_whenAllUrlsNull_returnsNull() {
        val podcast = createBasePodcast().copy(
            artworkUrl100 = null,
            artworkUrl60 = null,
            artworkUrl30 = null,
        )
        assertNull(podcast.bestArtworkUrl())
    }

    @Test
    fun bestArtworkUrl_whenOnlyUrl100Available_returnsUrl100() {
        val url100 = "https://example.com/100.jpg"
        val podcast = createBasePodcast().copy(
            artworkUrl100 = url100,
            artworkUrl60 = null,
            artworkUrl30 = null,
        )
        assertEquals(url100, podcast.bestArtworkUrl())
    }

    @Test
    fun bestArtworkUrl_whenOnlyUrl60Available_returnsUrl60() {
        val url60 = "https://example.com/60.jpg"
        val podcast = createBasePodcast().copy(
            artworkUrl100 = null,
            artworkUrl60 = url60,
            artworkUrl30 = null,
        )
        assertEquals(url60, podcast.bestArtworkUrl())
    }

    @Test
    fun bestArtworkUrl_whenOnlyUrl30Available_returnsUrl30() {
        val url30 = "https://example.com/30.jpg"
        val podcast = createBasePodcast().copy(
            artworkUrl100 = null,
            artworkUrl60 = null,
            artworkUrl30 = url30,
        )
        assertEquals(url30, podcast.bestArtworkUrl())
    }

    @Test
    fun bestArtworkUrl_whenUrl100AndUrl60Available_returnsUrl100() {
        val url100 = "https://example.com/100.jpg"
        val url60 = "https://example.com/60.jpg"
        val podcast = createBasePodcast().copy(
            artworkUrl100 = url100,
            artworkUrl60 = url60,
            artworkUrl30 = null,
        )
        assertEquals(url100, podcast.bestArtworkUrl())
    }

    @Test
    fun bestArtworkUrl_whenUrl100AndUrl30Available_returnsUrl100() {
        val url100 = "https://example.com/100.jpg"
        val url30 = "https://example.com/30.jpg"
        val podcast = createBasePodcast().copy(
            artworkUrl100 = url100,
            artworkUrl60 = null,
            artworkUrl30 = url30,
        )
        assertEquals(url100, podcast.bestArtworkUrl())
    }

    @Test
    fun bestArtworkUrl_whenUrl60AndUrl30Available_returnsUrl60() {
        val url60 = "https://example.com/60.jpg"
        val url30 = "https://example.com/30.jpg"
        val podcast = createBasePodcast().copy(
            artworkUrl100 = null,
            artworkUrl60 = url60,
            artworkUrl30 = url30,
        )
        assertEquals(url60, podcast.bestArtworkUrl())
    }

    @Test
    fun bestArtworkUrl_whenAllUrlsAvailable_returnsUrl100() {
        val url100 = "https://example.com/100.jpg"
        val url60 = "https://example.com/60.jpg"
        val url30 = "https://example.com/30.jpg"
        val podcast = createBasePodcast().copy(
            artworkUrl100 = url100,
            artworkUrl60 = url60,
            artworkUrl30 = url30,
        )
        assertEquals(url100, podcast.bestArtworkUrl())
    }

    // hasArtwork property tests - all conditions covered
    @Test
    fun hasArtwork_whenAllUrlsNull_returnsFalse() {
        val podcast = createBasePodcast().copy(
            artworkUrl100 = null,
            artworkUrl60 = null,
            artworkUrl30 = null,
        )
        assertFalse(podcast.hasArtwork)
    }

    @Test
    fun hasArtwork_whenOnlyUrl100Available_returnsTrue() {
        val podcast = createBasePodcast().copy(
            artworkUrl100 = "https://example.com/100.jpg",
            artworkUrl60 = null,
            artworkUrl30 = null,
        )
        assertTrue(podcast.hasArtwork)
    }

    @Test
    fun hasArtwork_whenOnlyUrl60Available_returnsTrue() {
        val podcast = createBasePodcast().copy(
            artworkUrl100 = null,
            artworkUrl60 = "https://example.com/60.jpg",
            artworkUrl30 = null,
        )
        assertTrue(podcast.hasArtwork)
    }

    @Test
    fun hasArtwork_whenOnlyUrl30Available_returnsTrue() {
        val podcast = createBasePodcast().copy(
            artworkUrl100 = null,
            artworkUrl60 = null,
            artworkUrl30 = "https://example.com/30.jpg",
        )
        assertTrue(podcast.hasArtwork)
    }

    @Test
    fun hasArtwork_whenMultipleUrlsAvailable_returnsTrue() {
        val podcast = createBasePodcast().copy(
            artworkUrl100 = "https://example.com/100.jpg",
            artworkUrl60 = "https://example.com/60.jpg",
            artworkUrl30 = null,
        )
        assertTrue(podcast.hasArtwork)
    }

    @Test
    fun hasArtwork_whenAllUrlsAvailable_returnsTrue() {
        val podcast = createBasePodcast().copy(
            artworkUrl100 = "https://example.com/100.jpg",
            artworkUrl60 = "https://example.com/60.jpg",
            artworkUrl30 = "https://example.com/30.jpg",
        )
        assertTrue(podcast.hasArtwork)
    }
}
