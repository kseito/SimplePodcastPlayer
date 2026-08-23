package jp.kztproject.simplepodcastplayer.screen

import app.cash.turbine.test
import jp.kztproject.simplepodcastplayer.data.repository.DownloadCleanupRepository
import jp.kztproject.simplepodcastplayer.data.repository.PodcastRepository
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
class PodcastListViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var podcastDao: FakePodcastDao
    private lateinit var episodeDao: FakeEpisodeDao
    private lateinit var repository: PodcastRepository
    private lateinit var downloadRepository: FakeDownloadRepository
    private lateinit var cleanupRepository: DownloadCleanupRepository
    private lateinit var viewModel: PodcastListViewModel

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        podcastDao = FakePodcastDao()
        episodeDao = FakeEpisodeDao()
        repository = PodcastRepository(podcastDao, episodeDao)
        downloadRepository = FakeDownloadRepository(episodeDao)
        cleanupRepository = DownloadCleanupRepository(episodeDao, downloadRepository)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_isLoading() = runTest {
        viewModel = PodcastListViewModel(repository, cleanupRepository)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(true, state.isLoading)
            assertEquals(0, state.subscribedPodcasts.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun loadSubscribedPodcasts_updatesUiState() = runTest {
        // Setup: Add podcasts to repository
        val podcast1 = TestDataFactory.createPodcast(trackId = 1L, trackName = "Podcast 1")
        val podcast2 = TestDataFactory.createPodcast(trackId = 2L, trackName = "Podcast 2")
        repository.subscribeToPodcast(podcast1, emptyList())
        repository.subscribeToPodcast(podcast2, emptyList())

        viewModel = PodcastListViewModel(repository, cleanupRepository)

        viewModel.uiState.test {
            // Skip initial loading state and get the updated state
            testDispatcher.scheduler.advanceUntilIdle()
            val state = expectMostRecentItem()
            assertFalse(state.isLoading)
            assertEquals(2, state.subscribedPodcasts.size)
            val podcastNames = state.subscribedPodcasts.map { it.name }.toSet()
            assertTrue(podcastNames.contains("Podcast 1"))
            assertTrue(podcastNames.contains("Podcast 2"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun loadSubscribedPodcasts_emptyList_updatesUiState() = runTest {
        viewModel = PodcastListViewModel(repository, cleanupRepository)

        viewModel.uiState.test {
            testDispatcher.scheduler.advanceUntilIdle()
            val state = expectMostRecentItem()
            assertFalse(state.isLoading)
            assertEquals(0, state.subscribedPodcasts.size)
        }
    }

    @Test
    fun getPodcastById_existingPodcast_returnsPodcast() = runTest {
        val podcast = TestDataFactory.createPodcast(trackId = 1L, trackName = "Test Podcast")
        repository.subscribeToPodcast(podcast, emptyList())

        viewModel = PodcastListViewModel(repository, cleanupRepository)

        viewModel.uiState.test {
            testDispatcher.scheduler.advanceUntilIdle()
            expectMostRecentItem()
            val result = viewModel.getPodcastById(1L)
            assertNotNull(result)
            assertEquals("Test Podcast", result.trackName)
        }
    }

    @Test
    fun getPodcastById_nonExistingPodcast_returnsNull() = runTest {
        viewModel = PodcastListViewModel(repository, cleanupRepository)

        viewModel.uiState.test {
            testDispatcher.scheduler.advanceUntilIdle()
            expectMostRecentItem()
            val result = viewModel.getPodcastById(999L)
            assertNull(result)
        }
    }

    @Test
    fun requestCleanupListenedDownloads_listenedDownloadsExist_showsDialogWithCount() = runTest {
        // Two listened downloads, plus one downloaded but unlistened, plus one listened but not downloaded
        setupEpisode(id = "ep1", listened = true, isDownloaded = true)
        setupEpisode(id = "ep2", listened = true, isDownloaded = true)
        setupEpisode(id = "ep3", listened = false, isDownloaded = true)
        setupEpisode(id = "ep4", listened = true, isDownloaded = false)

        viewModel = PodcastListViewModel(repository, cleanupRepository)
        viewModel.requestCleanupListenedDownloads()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertEquals(2, state.cleanupConfirmDownloadCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun requestCleanupListenedDownloads_noListenedDownloads_showsDialogWithZero() = runTest {
        setupEpisode(id = "ep1", listened = false, isDownloaded = true)

        viewModel = PodcastListViewModel(repository, cleanupRepository)
        viewModel.requestCleanupListenedDownloads()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertEquals(0, state.cleanupConfirmDownloadCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun confirmCleanupListenedDownloads_deletesOnlyListenedDownloads() = runTest {
        setupEpisode(id = "ep1", listened = true, isDownloaded = true)
        setupEpisode(id = "ep2", listened = false, isDownloaded = true)

        viewModel = PodcastListViewModel(repository, cleanupRepository)
        viewModel.confirmCleanupListenedDownloads()
        testDispatcher.scheduler.advanceUntilIdle()

        // The listened episode's file is gone, the unlistened one is kept
        assertFalse(downloadRepository.isDownloaded("ep1"))
        assertTrue(downloadRepository.isDownloaded("ep2"))
        assertFalse(episodeDao.getById("ep1")!!.isDownloaded)
        assertTrue(episodeDao.getById("ep2")!!.isDownloaded)

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertNull(state.cleanupConfirmDownloadCount)
            assertFalse(state.isCleaningUp)
            assertEquals("Deleted 1 download", state.cleanupMessage)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun dismissCleanupConfirm_keepsDownloads() = runTest {
        setupEpisode(id = "ep1", listened = true, isDownloaded = true)

        viewModel = PodcastListViewModel(repository, cleanupRepository)
        viewModel.requestCleanupListenedDownloads()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.dismissCleanupConfirm()

        assertTrue(downloadRepository.isDownloaded("ep1"))

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertNull(state.cleanupConfirmDownloadCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private suspend fun setupEpisode(id: String, listened: Boolean, isDownloaded: Boolean) {
        episodeDao.insert(
            TestDataFactory.createEpisodeEntity(id = id, listened = listened, isDownloaded = isDownloaded),
        )
        if (isDownloaded) {
            downloadRepository.setDownloadedEpisode(id, "/fake/path/$id.mp3")
        }
    }

    @Test
    fun uiStateUpdates_whenRepositoryChanges() = runTest {
        viewModel = PodcastListViewModel(repository, cleanupRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            // Initial state
            val initialState = awaitItem()
            assertEquals(0, initialState.subscribedPodcasts.size)

            // Add a podcast
            val podcast = TestDataFactory.createPodcast(trackId = 1L)
            repository.subscribeToPodcast(podcast, emptyList())
            testDispatcher.scheduler.advanceUntilIdle()

            // Updated state
            val updatedState = awaitItem()
            assertEquals(1, updatedState.subscribedPodcasts.size)

            cancelAndIgnoreRemainingEvents()
        }
    }
}
