package jp.kztproject.simplepodcastplayer.screen

import app.cash.turbine.test
import jp.kztproject.simplepodcastplayer.data.Episode
import jp.kztproject.simplepodcastplayer.data.Podcast
import jp.kztproject.simplepodcastplayer.data.PodcastLookupResponse
import jp.kztproject.simplepodcastplayer.data.PodcastLookupResult
import jp.kztproject.simplepodcastplayer.data.repository.PodcastRepository
import jp.kztproject.simplepodcastplayer.fake.FakeAppleSearchApiClient
import jp.kztproject.simplepodcastplayer.fake.FakeDownloadRepository
import jp.kztproject.simplepodcastplayer.fake.FakeEpisodeDao
import jp.kztproject.simplepodcastplayer.fake.FakePodcastDao
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
    private lateinit var downloadRepository: FakeDownloadRepository
    private lateinit var appleApiClient: FakeAppleSearchApiClient
    private lateinit var viewModel: PodcastDetailViewModel
    private var navigatedEpisode: Episode? = null
    private var navigatedPodcast: Podcast? = null

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        podcastDao = FakePodcastDao()
        episodeDao = FakeEpisodeDao()
        repository = PodcastRepository(podcastDao, episodeDao)
        downloadRepository = FakeDownloadRepository()
        appleApiClient = FakeAppleSearchApiClient()
        navigatedEpisode = null
        navigatedPodcast = null

        viewModel = PodcastDetailViewModel(
            podcastRepository = repository,
            downloadRepository = downloadRepository,
            appleApiClient = appleApiClient,
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
    fun initialize_notSubscribed_loadEpisodesFromAppleApi() = runTest {
        val podcast = TestDataFactory.createPodcast(trackId = 1L)
        val lookupResults = listOf(
            PodcastLookupResult(
                wrapperType = "podcastEpisode",
                trackId = 1001L,
                trackName = "Episode 1",
                episodeGuid = "ep1",
                releaseDate = "2024-12-15T10:00:00Z",
                trackTimeMillis = 1800000L,
                description = "Test Description 1",
                episodeUrl = "https://example.com/episode1.mp3",
            ),
            PodcastLookupResult(
                wrapperType = "podcastEpisode",
                trackId = 1002L,
                trackName = "Episode 2",
                episodeGuid = "ep2",
                releaseDate = "2024-12-16T10:00:00Z",
                trackTimeMillis = 2400000L,
                description = "Test Description 2",
                episodeUrl = "https://example.com/episode2.mp3",
            ),
        )
        appleApiClient.setLookupResult(
            PodcastLookupResponse(
                resultCount = lookupResults.size,
                results = lookupResults,
            ),
        )

        viewModel.initialize(podcast)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isSubscribed)
            assertFalse(state.isLoading)
            assertEquals(2, state.episodes.size)
            assertEquals("Episode 2", state.episodes[0].title) // Sorted by trackId DESC
            assertEquals("Episode 1", state.episodes[1].title)
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
    fun initialize_appleApiError_setsErrorMessage() = runTest {
        val podcast = TestDataFactory.createPodcast(trackId = 1L)
        appleApiClient.setShouldThrowError(Exception("Network error"))

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
        val lookupResults = listOf(
            PodcastLookupResult(
                wrapperType = "podcastEpisode",
                trackId = 1001L,
                trackName = "Episode 1",
                episodeGuid = "ep1",
                releaseDate = "2024-12-15T10:00:00Z",
                trackTimeMillis = 1800000L,
                description = "Test Description",
                episodeUrl = "https://example.com/episode1.mp3",
            ),
        )
        appleApiClient.setLookupResult(
            PodcastLookupResponse(
                resultCount = lookupResults.size,
                results = lookupResults,
            ),
        )

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
        val lookupResults = listOf(
            PodcastLookupResult(
                wrapperType = "podcastEpisode",
                trackId = 1001L,
                trackName = "Episode 1",
                episodeGuid = "ep1",
                releaseDate = "2024-12-15T10:00:00Z",
                trackTimeMillis = 1800000L,
                description = "Test Description",
                episodeUrl = "https://example.com/episode1.mp3",
            ),
        )
        appleApiClient.setLookupResult(
            PodcastLookupResponse(
                resultCount = lookupResults.size,
                results = lookupResults,
            ),
        )

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
        appleApiClient.setShouldThrowError(Exception("Error"))

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
        val lookupResults = listOf(
            PodcastLookupResult(
                wrapperType = "podcastEpisode",
                trackId = 1001L,
                trackName = "Episode 1",
                episodeGuid = "ep1",
                releaseDate = "2024-12-15T10:00:00Z",
                trackTimeMillis = 1800000L,
                description = "Test Description",
                episodeUrl = "https://example.com/episode1.mp3",
            ),
        )
        appleApiClient.setLookupResult(
            PodcastLookupResponse(
                resultCount = lookupResults.size,
                results = lookupResults,
            ),
        )

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
        val lookupResults = listOf(
            PodcastLookupResult(
                wrapperType = "podcastEpisode",
                trackId = 1001L,
                trackName = "Episode 1",
                episodeGuid = "ep1",
                releaseDate = "2024-12-15T10:00:00Z",
                trackTimeMillis = 1800000L,
                description = "Test Description",
                episodeUrl = "https://example.com/episode1.mp3",
            ),
        )
        appleApiClient.setLookupResult(
            PodcastLookupResponse(
                resultCount = lookupResults.size,
                results = lookupResults,
            ),
        )
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
        val lookupResults = listOf(
            PodcastLookupResult(
                wrapperType = "podcastEpisode",
                trackId = 1001L,
                trackName = "Episode 1",
                episodeGuid = "ep1",
                releaseDate = "2024-12-15T10:00:00Z",
                trackTimeMillis = 1800000L,
                description = "Test Description",
                episodeUrl = "https://example.com/episode1.mp3",
            ),
        )
        appleApiClient.setLookupResult(
            PodcastLookupResponse(
                resultCount = lookupResults.size,
                results = lookupResults,
            ),
        )
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
