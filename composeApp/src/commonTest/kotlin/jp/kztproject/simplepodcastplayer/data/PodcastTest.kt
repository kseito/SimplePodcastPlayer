package jp.kztproject.simplepodcastplayer.data

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlin.test.Test

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

        podcast.bestArtworkUrl().shouldBeNull()
    }

    @Test
    fun bestArtworkUrl_whenOnlyUrl100Available_returnsUrl100() {
        val url100 = "https://example.com/100.jpg"
        val podcast = createBasePodcast().copy(
            artworkUrl100 = url100,
            artworkUrl60 = null,
            artworkUrl30 = null,
        )

        podcast.bestArtworkUrl() shouldBe url100
    }

    @Test
    fun bestArtworkUrl_whenOnlyUrl60Available_returnsUrl60() {
        val url60 = "https://example.com/60.jpg"
        val podcast = createBasePodcast().copy(
            artworkUrl100 = null,
            artworkUrl60 = url60,
            artworkUrl30 = null,
        )

        podcast.bestArtworkUrl() shouldBe url60
    }

    @Test
    fun bestArtworkUrl_whenOnlyUrl30Available_returnsUrl30() {
        val url30 = "https://example.com/30.jpg"
        val podcast = createBasePodcast().copy(
            artworkUrl100 = null,
            artworkUrl60 = null,
            artworkUrl30 = url30,
        )

        podcast.bestArtworkUrl() shouldBe url30
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

        podcast.bestArtworkUrl() shouldBe url100
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

        podcast.bestArtworkUrl() shouldBe url100
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

        podcast.bestArtworkUrl() shouldBe url60
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

        podcast.bestArtworkUrl() shouldBe url100
    }

    // hasArtwork property tests - all conditions covered
    @Test
    fun hasArtwork_whenAllUrlsNull_returnsFalse() {
        val podcast = createBasePodcast().copy(
            artworkUrl100 = null,
            artworkUrl60 = null,
            artworkUrl30 = null,
        )

        podcast.hasArtwork shouldBe false
    }

    @Test
    fun hasArtwork_whenOnlyUrl100Available_returnsTrue() {
        val podcast = createBasePodcast().copy(
            artworkUrl100 = "https://example.com/100.jpg",
            artworkUrl60 = null,
            artworkUrl30 = null,
        )

        podcast.hasArtwork shouldBe true
    }

    @Test
    fun hasArtwork_whenOnlyUrl60Available_returnsTrue() {
        val podcast = createBasePodcast().copy(
            artworkUrl100 = null,
            artworkUrl60 = "https://example.com/60.jpg",
            artworkUrl30 = null,
        )

        podcast.hasArtwork shouldBe true
    }

    @Test
    fun hasArtwork_whenOnlyUrl30Available_returnsTrue() {
        val podcast = createBasePodcast().copy(
            artworkUrl100 = null,
            artworkUrl60 = null,
            artworkUrl30 = "https://example.com/30.jpg",
        )

        podcast.hasArtwork shouldBe true
    }

    @Test
    fun hasArtwork_whenMultipleUrlsAvailable_returnsTrue() {
        val podcast = createBasePodcast().copy(
            artworkUrl100 = "https://example.com/100.jpg",
            artworkUrl60 = "https://example.com/60.jpg",
            artworkUrl30 = null,
        )

        podcast.hasArtwork shouldBe true
    }

    @Test
    fun hasArtwork_whenAllUrlsAvailable_returnsTrue() {
        val podcast = createBasePodcast().copy(
            artworkUrl100 = "https://example.com/100.jpg",
            artworkUrl60 = "https://example.com/60.jpg",
            artworkUrl30 = "https://example.com/30.jpg",
        )

        podcast.hasArtwork shouldBe true
    }
}
