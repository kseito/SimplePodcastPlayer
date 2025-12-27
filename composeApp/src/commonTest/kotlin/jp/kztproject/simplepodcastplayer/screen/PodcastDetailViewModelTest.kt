package jp.kztproject.simplepodcastplayer.screen

import app.cash.turbine.test
import jp.kztproject.simplepodcastplayer.data.Episode
import jp.kztproject.simplepodcastplayer.data.Podcast
import jp.kztproject.simplepodcastplayer.data.repository.PodcastRepository
import jp.kztproject.simplepodcastplayer.fake.FakeDownloadRepository
import jp.kztproject.simplepodcastplayer.fake.FakeEpisodeDao
import jp.kztproject.simplepodcastplayer.fake.FakePodcastDao
import jp.kztproject.simplepodcastplayer.fake.FakeRssService
import jp.kztproject.simplepodcastplayer.fake.TestDataFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PodcastDetailViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var podcastDao: FakePodcastDao
    private lateinit var episodeDao: FakeEpisodeDao
    private lateinit var repository: PodcastRepository
    private lateinit var rssService: FakeRssService
    private lateinit var downloadRepository: FakeDownloadRepository
    private lateinit var viewModel: PodcastDetailViewModel
    private var navigatedEpisode: Episode? = null
    private var navigatedPodcast: Podcast? = null

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        podcastDao = FakePodcastDao()
        episodeDao = FakeEpisodeDao()
        repository = PodcastRepository(podcastDao, episodeDao)
        rssService = FakeRssService()
        downloadRepository = FakeDownloadRepository()
        navigatedEpisode = null
        navigatedPodcast = null

        viewModel = PodcastDetailViewModel(
            rssService = rssService,
            podcastRepository = repository,
            downloadRepository = downloadRepository,
            onNavigateToPlayer = { episode, podcast ->
                navigatedEpisode = episode
                navigatedPodcast = podcast
            },
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialize_notSubscribed_loadEpisodesFromRss() = runTest {
        val podcast = TestDataFactory.createPodcast(trackId = 1L)
        val parsedEpisodes = listOf(
            TestDataFactory.createParsedEpisode(id = "ep1", title = "Episode 1"),
            TestDataFactory.createParsedEpisode(id = "ep2", title = "Episode 2"),
        )
        rssService.setEpisodes(parsedEpisodes)

        viewModel.initialize(podcast)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isSubscribed)
            assertFalse(state.isLoading)
            assertEquals(2, state.episodes.size)
            assertEquals("Episode 1", state.episodes[0].title)
            assertEquals("Episode 2", state.episodes[1].title)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun initialize_subscribed_loadEpisodesFromDatabase() = runTest {
        val podcast = TestDataFactory.createPodcast(trackId = 1L)
        val episodes = listOf(
            TestDataFactory.createEpisode(id = "ep1", podcastId = "1", title = "DB Episode 1"),
            TestDataFactory.createEpisode(id = "ep2", podcastId = "1", title = "DB Episode 2"),
        )
        repository.subscribeToPodcast(podcast, episodes)

        viewModel.initialize(podcast)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.isSubscribed)
            assertFalse(state.isLoading)
            assertEquals(2, state.episodes.size)
            assertEquals("DB Episode 1", state.episodes[0].title)
            assertEquals("DB Episode 2", state.episodes[1].title)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun initialize_rssError_setsErrorMessage() = runTest {
        val podcast = TestDataFactory.createPodcast(trackId = 1L)
        rssService.setError(Exception("Network error"))

        viewModel.initialize(podcast)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertNotNull(state.error)
            assertTrue(state.episodes.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun toggleSubscription_subscribe_savesDataToRepository() = runTest {
        val podcast = TestDataFactory.createPodcast(trackId = 1L)
        val parsedEpisodes = listOf(
            TestDataFactory.createParsedEpisode(id = "ep1", title = "Episode 1"),
        )
        rssService.setEpisodes(parsedEpisodes)

        viewModel.initialize(podcast)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.toggleSubscription()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.isSubscribed)
            assertFalse(state.isSubscriptionLoading)
            cancelAndIgnoreRemainingEvents()
        }

        // Verify saved in repository
        assertTrue(repository.isSubscribed(1L))
    }

    @Test
    fun toggleSubscription_unsubscribe_updatesRepository() = runTest {
        val podcast = TestDataFactory.createPodcast(trackId = 1L)
        val episodes = listOf(TestDataFactory.createEpisode(id = "ep1", podcastId = "1"))
        repository.subscribeToPodcast(podcast, episodes)

        viewModel.initialize(podcast)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.toggleSubscription()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isSubscribed)
            assertFalse(state.isSubscriptionLoading)
            cancelAndIgnoreRemainingEvents()
        }

        // Verify unsubscribed in repository
        assertFalse(repository.isSubscribed(1L))
    }

    @Test
    fun playEpisode_navigatesToPlayer() = runTest {
        val podcast = TestDataFactory.createPodcast(trackId = 1L)
        val parsedEpisodes = listOf(
            TestDataFactory.createParsedEpisode(id = "ep1", title = "Episode 1"),
        )
        rssService.setEpisodes(parsedEpisodes)

        viewModel.initialize(podcast)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.playEpisode("ep1")
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(navigatedEpisode)
        assertNotNull(navigatedPodcast)
        assertEquals("ep1", navigatedEpisode?.id)
        assertEquals(1L, navigatedPodcast?.trackId)
    }

    @Test
    fun clearError_clearsErrorMessage() = runTest {
        val podcast = TestDataFactory.createPodcast(trackId = 1L)
        rssService.setError(Exception("Error"))

        viewModel.initialize(podcast)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.clearError()

        viewModel.uiState.test {
            val state = awaitItem()
            assertNull(state.error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun downloadEpisode_success_updatesDownloadState() = runTest {
        val podcast = TestDataFactory.createPodcast(trackId = 1L)
        val parsedEpisodes = listOf(
            TestDataFactory.createParsedEpisode(id = "ep1", title = "Episode 1"),
        )
        rssService.setEpisodes(parsedEpisodes)

        viewModel.initialize(podcast)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.downloadEpisode("ep1")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.episodes[0].isDownloaded)
            assertTrue(downloadRepository.isDownloaded("ep1"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun downloadEpisode_failure_setsErrorState() = runTest {
        val podcast = TestDataFactory.createPodcast(trackId = 1L)
        val parsedEpisodes = listOf(
            TestDataFactory.createParsedEpisode(id = "ep1", title = "Episode 1"),
        )
        rssService.setEpisodes(parsedEpisodes)
        downloadRepository.setShouldFailDownload(true, "Download failed")

        viewModel.initialize(podcast)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.downloadEpisode("ep1")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertNotNull(state.error)
            assertFalse(state.episodes[0].isDownloaded)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun deleteDownload_success_updatesEpisodeState() = runTest {
        val podcast = TestDataFactory.createPodcast(trackId = 1L)
        val parsedEpisodes = listOf(
            TestDataFactory.createParsedEpisode(id = "ep1", title = "Episode 1"),
        )
        rssService.setEpisodes(parsedEpisodes)
        downloadRepository.setDownloadedEpisode("ep1", "/fake/path/ep1.mp3")

        viewModel.initialize(podcast)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.deleteDownload("ep1")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.episodes[0].isDownloaded)
            assertFalse(downloadRepository.isDownloaded("ep1"))
            cancelAndIgnoreRemainingEvents()
        }
    }
}
