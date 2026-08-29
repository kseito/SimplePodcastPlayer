package jp.kztproject.simplepodcastplayer.screen

import app.cash.turbine.test
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import jp.kztproject.simplepodcastplayer.data.Episode
import jp.kztproject.simplepodcastplayer.data.Podcast
import jp.kztproject.simplepodcastplayer.data.PodcastLookupResponse
import jp.kztproject.simplepodcastplayer.data.PodcastLookupResult
import jp.kztproject.simplepodcastplayer.data.repository.EpisodeAudioRepository
import jp.kztproject.simplepodcastplayer.data.repository.PodcastRepository
import jp.kztproject.simplepodcastplayer.fake.FakeAppleSearchApiClient
import jp.kztproject.simplepodcastplayer.fake.FakeAudioDownloader
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

@OptIn(ExperimentalCoroutinesApi::class)
class PodcastDetailViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var podcastDao: FakePodcastDao
    private lateinit var episodeDao: FakeEpisodeDao
    private lateinit var repository: PodcastRepository
    private lateinit var audioDownloader: FakeAudioDownloader
    private lateinit var episodeAudioRepository: EpisodeAudioRepository
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
        audioDownloader = FakeAudioDownloader()
        episodeAudioRepository = EpisodeAudioRepository(audioDownloader, episodeDao)
        appleApiClient = FakeAppleSearchApiClient()
        navigatedEpisode = null
        navigatedPodcast = null

        viewModel = PodcastDetailViewModel(
            podcastRepository = repository,
            episodeAudioRepository = episodeAudioRepository,
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
            state.isSubscribed shouldBe false
            state.isLoading shouldBe false
            state.episodes.size shouldBe 2
            state.episodes[0].title shouldBe "Episode 2" // Sorted by trackId DESC
            state.episodes[1].title shouldBe "Episode 1"
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
            state.isSubscribed shouldBe true
            state.isLoading shouldBe false
            state.episodes.size shouldBe 2
            state.episodes[0].title shouldBe "DB Episode 1"
            state.episodes[1].title shouldBe "DB Episode 2"
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
            state.isLoading shouldBe false
            state.error.shouldNotBeNull()
            state.episodes.shouldBeEmpty()
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
            state.isSubscribed shouldBe true
            state.isSubscriptionLoading shouldBe false
            cancelAndIgnoreRemainingEvents()
        }

        // Verify saved in repository
        repository.isSubscribed(1L) shouldBe true
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
            state.isSubscribed shouldBe false
            state.isSubscriptionLoading shouldBe false
            cancelAndIgnoreRemainingEvents()
        }

        // Verify unsubscribed in repository
        repository.isSubscribed(1L) shouldBe false
    }

    @Test
    fun toggleSubscription_unsubscribeWithDownloads_showsConfirmDialogWithoutDeleting() = runTest {
        val podcast = subscribeWithDownloadedEpisode()

        viewModel.initialize(podcast)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.toggleSubscription()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            state.unsubscribeConfirmAudioFileCount shouldBe 1
            // Nothing happens until the user confirms
            state.isSubscribed shouldBe true
            cancelAndIgnoreRemainingEvents()
        }

        repository.isSubscribed(1L) shouldBe true
        episodeAudioRepository.isDownloaded("ep1") shouldBe true
    }

    @Test
    fun confirmUnsubscribe_deletesDownloadsAndUnsubscribes() = runTest {
        val podcast = subscribeWithDownloadedEpisode()

        viewModel.initialize(podcast)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.toggleSubscription()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.confirmUnsubscribe()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            state.unsubscribeConfirmAudioFileCount.shouldBeNull()
            state.isSubscribed shouldBe false
            state.isSubscriptionLoading shouldBe false
            state.episodes.none { it.isDownloaded } shouldBe true
            cancelAndIgnoreRemainingEvents()
        }

        repository.isSubscribed(1L) shouldBe false
        episodeAudioRepository.isDownloaded("ep1") shouldBe false
        episodeDao.getById("ep1")!!.isDownloaded shouldBe false
    }

    @Test
    fun dismissUnsubscribeConfirm_keepsSubscriptionAndDownloads() = runTest {
        val podcast = subscribeWithDownloadedEpisode()

        viewModel.initialize(podcast)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.toggleSubscription()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.dismissUnsubscribeConfirm()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            state.unsubscribeConfirmAudioFileCount.shouldBeNull()
            state.isSubscribed shouldBe true
            cancelAndIgnoreRemainingEvents()
        }

        repository.isSubscribed(1L) shouldBe true
        episodeAudioRepository.isDownloaded("ep1") shouldBe true
    }

    private suspend fun subscribeWithDownloadedEpisode(): Podcast {
        val podcast = TestDataFactory.createPodcast(trackId = 1L)
        val episodes = listOf(TestDataFactory.createEpisode(id = "ep1", podcastId = "1"))
        repository.subscribeToPodcast(podcast, episodes)
        episodeDao.updateDownloadStatus(
            episodeId = "ep1",
            isDownloaded = true,
            localFilePath = "/fake/path/ep1.mp3",
            downloadedAt = 1703001600000L,
        )
        audioDownloader.setDownloadedEpisode("ep1", "/fake/path/ep1.mp3")
        return podcast
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

        navigatedEpisode.shouldNotBeNull()
        navigatedPodcast.shouldNotBeNull()
        navigatedEpisode?.id shouldBe "ep1"
        navigatedPodcast?.trackId shouldBe 1L
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
            state.error.shouldBeNull()
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
            state.episodes[0].isDownloaded shouldBe true
            episodeAudioRepository.isDownloaded("ep1") shouldBe true
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
        audioDownloader.setShouldFailDownload(true, "Download failed")

        viewModel.initialize(podcast)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.downloadEpisode("ep1")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            state.error.shouldNotBeNull()
            state.episodes[0].isDownloaded shouldBe false
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun refreshEpisodes_fetchesFromAppleApiAndSavesToDatabase() = runTest {
        val podcast = TestDataFactory.createPodcast(trackId = 1L)
        val initialEpisodes = listOf(
            TestDataFactory.createEpisode(id = "ep1", podcastId = "1", title = "Old Episode"),
        )
        repository.subscribeToPodcast(podcast, initialEpisodes)

        viewModel.initialize(podcast)
        testDispatcher.scheduler.advanceUntilIdle()

        // Set up Apple API with new episodes
        val lookupResults = listOf(
            PodcastLookupResult(
                wrapperType = "podcastEpisode",
                trackId = 1001L,
                trackName = "Old Episode",
                episodeGuid = "ep1",
                releaseDate = "2024-12-15T10:00:00Z",
                trackTimeMillis = 1800000L,
                description = "Old Description",
                episodeUrl = "https://example.com/episode1.mp3",
            ),
            PodcastLookupResult(
                wrapperType = "podcastEpisode",
                trackId = 1002L,
                trackName = "New Episode",
                episodeGuid = "ep2",
                releaseDate = "2024-12-16T10:00:00Z",
                trackTimeMillis = 2400000L,
                description = "New Description",
                episodeUrl = "https://example.com/episode2.mp3",
            ),
        )
        appleApiClient.setLookupResult(
            PodcastLookupResponse(resultCount = lookupResults.size, results = lookupResults),
        )

        viewModel.refreshEpisodes()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            state.isRefreshing shouldBe false
            state.episodes.size shouldBe 2
            cancelAndIgnoreRemainingEvents()
        }

        // Verify episodes are saved to database
        val savedEpisodes = repository.getEpisodesByPodcastId("1")
        savedEpisodes.size shouldBe 2
    }

    @Test
    fun refreshEpisodes_preservesListenedState() = runTest {
        val podcast = TestDataFactory.createPodcast(trackId = 1L)
        val initialEpisodes = listOf(
            TestDataFactory.createEpisode(id = "ep1", podcastId = "1", title = "Episode 1", listened = true),
        )
        repository.subscribeToPodcast(podcast, initialEpisodes)

        viewModel.initialize(podcast)
        testDispatcher.scheduler.advanceUntilIdle()

        // Apple API returns same episode (API doesn't know about listened state)
        val lookupResults = listOf(
            PodcastLookupResult(
                wrapperType = "podcastEpisode",
                trackId = 1001L,
                trackName = "Episode 1",
                episodeGuid = "ep1",
                releaseDate = "2024-12-15T10:00:00Z",
                trackTimeMillis = 1800000L,
                description = "Description 1",
                episodeUrl = "https://example.com/episode1.mp3",
            ),
            PodcastLookupResult(
                wrapperType = "podcastEpisode",
                trackId = 1002L,
                trackName = "New Episode",
                episodeGuid = "ep2",
                releaseDate = "2024-12-16T10:00:00Z",
                trackTimeMillis = 2400000L,
                description = "Description 2",
                episodeUrl = "https://example.com/episode2.mp3",
            ),
        )
        appleApiClient.setLookupResult(
            PodcastLookupResponse(resultCount = lookupResults.size, results = lookupResults),
        )

        viewModel.refreshEpisodes()
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify listened state is preserved in database
        val savedEpisodes = repository.getEpisodesByPodcastId("1")
        savedEpisodes.first { it.id == "ep1" }.listened shouldBe true
        savedEpisodes.first { it.id == "ep2" }.listened shouldBe false
    }

    @Test
    fun refreshEpisodes_apiError_setsErrorMessage() = runTest {
        val podcast = TestDataFactory.createPodcast(trackId = 1L)
        val episodes = listOf(
            TestDataFactory.createEpisode(id = "ep1", podcastId = "1", title = "Episode 1"),
        )
        repository.subscribeToPodcast(podcast, episodes)

        viewModel.initialize(podcast)
        testDispatcher.scheduler.advanceUntilIdle()

        appleApiClient.setShouldThrowError(Exception("Network error"))

        viewModel.refreshEpisodes()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            state.isRefreshing shouldBe false
            state.error.shouldNotBeNull()
            // Original episodes should still be available
            state.episodes.size shouldBe 1
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun refreshEpisodes_notSubscribed_doesNothing() = runTest {
        val podcast = TestDataFactory.createPodcast(trackId = 1L)
        val lookupResults = listOf(
            PodcastLookupResult(
                wrapperType = "podcastEpisode",
                trackId = 1001L,
                trackName = "Episode 1",
                episodeGuid = "ep1",
                releaseDate = "2024-12-15T10:00:00Z",
                trackTimeMillis = 1800000L,
                description = "Description",
                episodeUrl = "https://example.com/episode1.mp3",
            ),
        )
        appleApiClient.setLookupResult(
            PodcastLookupResponse(resultCount = lookupResults.size, results = lookupResults),
        )

        viewModel.initialize(podcast)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.refreshEpisodes()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            state.isRefreshing shouldBe false
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun deleteAudioFile_success_updatesEpisodeState() = runTest {
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
        audioDownloader.setDownloadedEpisode("ep1", "/fake/path/ep1.mp3")

        viewModel.initialize(podcast)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.deleteAudioFile("ep1")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            state.episodes[0].isDownloaded shouldBe false
            episodeAudioRepository.isDownloaded("ep1") shouldBe false
            cancelAndIgnoreRemainingEvents()
        }
    }
}
