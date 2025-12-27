package jp.kztproject.simplepodcastplayer.data.repository

import app.cash.turbine.test
import jp.kztproject.simplepodcastplayer.fake.FakeEpisodeDao
import jp.kztproject.simplepodcastplayer.fake.FakePodcastDao
import jp.kztproject.simplepodcastplayer.fake.TestDataFactory
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PodcastRepositoryTest {
    private val podcastDao = FakePodcastDao()
    private val episodeDao = FakeEpisodeDao()
    private val repository = PodcastRepository(podcastDao, episodeDao)

    @Test
    fun subscribeToPodcast_savePodcastAndEpisodes() = runTest {
        val podcast = TestDataFactory.createPodcast(trackId = 1L)
        val episodes = listOf(
            TestDataFactory.createEpisode(id = "ep1", podcastId = "1"),
            TestDataFactory.createEpisode(id = "ep2", podcastId = "1"),
        )

        repository.subscribeToPodcast(podcast, episodes)

        // Verify podcast is saved
        val savedPodcast = podcastDao.getById(1L)
        assertEquals(podcast.trackName, savedPodcast?.name)
        assertTrue(savedPodcast?.subscribed ?: false)

        // Verify episodes are saved
        episodeDao.getByPodcastId("1").test {
            val savedEpisodes = awaitItem()
            assertEquals(2, savedEpisodes.size)
            assertEquals("ep1", savedEpisodes[0].id)
            assertEquals("ep2", savedEpisodes[1].id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun unsubscribeFromPodcast_updateSubscriptionStatus() = runTest {
        // Setup: Subscribe to a podcast first
        val podcast = TestDataFactory.createPodcast(trackId = 1L)
        repository.subscribeToPodcast(podcast, emptyList())

        // Unsubscribe
        repository.unsubscribeFromPodcast(1L)

        // Verify subscription status is false
        val updatedPodcast = podcastDao.getById(1L)
        assertFalse(updatedPodcast?.subscribed ?: true)
    }

    @Test
    fun isSubscribed_returnsTrue_whenPodcastIsSubscribed() = runTest {
        val podcast = TestDataFactory.createPodcast(trackId = 1L)
        repository.subscribeToPodcast(podcast, emptyList())

        val isSubscribed = repository.isSubscribed(1L)

        assertTrue(isSubscribed)
    }

    @Test
    fun isSubscribed_returnsFalse_whenPodcastIsNotSubscribed() = runTest {
        val isSubscribed = repository.isSubscribed(999L)

        assertFalse(isSubscribed)
    }

    @Test
    fun isSubscribed_returnsFalse_afterUnsubscribe() = runTest {
        val podcast = TestDataFactory.createPodcast(trackId = 1L)
        repository.subscribeToPodcast(podcast, emptyList())
        repository.unsubscribeFromPodcast(1L)

        val isSubscribed = repository.isSubscribed(1L)

        assertFalse(isSubscribed)
    }

    @Test
    fun getEpisodesByPodcastId_returnsEpisodes() = runTest {
        val podcast = TestDataFactory.createPodcast(trackId = 1L)
        val episodes = listOf(
            TestDataFactory.createEpisode(id = "ep1", podcastId = "1", title = "Episode 1"),
            TestDataFactory.createEpisode(id = "ep2", podcastId = "1", title = "Episode 2"),
        )
        repository.subscribeToPodcast(podcast, episodes)

        val retrievedEpisodes = repository.getEpisodesByPodcastId("1")

        assertEquals(2, retrievedEpisodes.size)
        assertEquals("Episode 1", retrievedEpisodes[0].title)
        assertEquals("Episode 2", retrievedEpisodes[1].title)
    }

    @Test
    fun getEpisodesByPodcastId_returnsEmptyList_whenNoPodcastExists() = runTest {
        val episodes = repository.getEpisodesByPodcastId("999")

        assertTrue(episodes.isEmpty())
    }

    @Test
    fun getSubscribedPodcasts_returnsOnlySubscribedPodcasts() = runTest {
        val podcast1 = TestDataFactory.createPodcast(trackId = 1L)
        val podcast2 = TestDataFactory.createPodcast(trackId = 2L)
        repository.subscribeToPodcast(podcast1, emptyList())
        repository.subscribeToPodcast(podcast2, emptyList())
        repository.unsubscribeFromPodcast(2L)

        repository.getSubscribedPodcasts().test {
            val podcasts = awaitItem()
            assertEquals(1, podcasts.size)
            assertEquals(1L, podcasts[0].id)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
