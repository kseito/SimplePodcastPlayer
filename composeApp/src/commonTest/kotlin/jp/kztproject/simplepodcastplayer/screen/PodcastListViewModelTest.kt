package jp.kztproject.simplepodcastplayer.screen

import app.cash.turbine.test
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import jp.kztproject.simplepodcastplayer.data.repository.EpisodeAudioRepository
import jp.kztproject.simplepodcastplayer.data.repository.PodcastRepository
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
class PodcastListViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var podcastDao: FakePodcastDao
    private lateinit var episodeDao: FakeEpisodeDao
    private lateinit var repository: PodcastRepository
    private lateinit var audioDownloader: FakeAudioDownloader
    private lateinit var episodeAudioRepository: EpisodeAudioRepository
    private lateinit var viewModel: PodcastListViewModel

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        podcastDao = FakePodcastDao()
        episodeDao = FakeEpisodeDao()
        repository = PodcastRepository(podcastDao, episodeDao)
        audioDownloader = FakeAudioDownloader()
        episodeAudioRepository = EpisodeAudioRepository(audioDownloader, episodeDao)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_isLoading() = runTest {
        viewModel = PodcastListViewModel(repository, episodeAudioRepository)

        viewModel.uiState.test {
            val state = awaitItem()
            state.isLoading shouldBe true
            state.subscribedPodcasts.size shouldBe 0
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

        viewModel = PodcastListViewModel(repository, episodeAudioRepository)

        viewModel.uiState.test {
            // Skip initial loading state and get the updated state
            testDispatcher.scheduler.advanceUntilIdle()
            val state = expectMostRecentItem()
            state.isLoading shouldBe false
            state.subscribedPodcasts.size shouldBe 2
            val podcastNames = state.subscribedPodcasts.map { it.name }.toSet()
            podcastNames shouldContain "Podcast 1"
            podcastNames shouldContain "Podcast 2"
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun loadSubscribedPodcasts_emptyList_updatesUiState() = runTest {
        viewModel = PodcastListViewModel(repository, episodeAudioRepository)

        viewModel.uiState.test {
            testDispatcher.scheduler.advanceUntilIdle()
            val state = expectMostRecentItem()
            state.isLoading shouldBe false
            state.subscribedPodcasts.size shouldBe 0
        }
    }

    @Test
    fun getPodcastById_existingPodcast_returnsPodcast() = runTest {
        val podcast = TestDataFactory.createPodcast(trackId = 1L, trackName = "Test Podcast")
        repository.subscribeToPodcast(podcast, emptyList())

        viewModel = PodcastListViewModel(repository, episodeAudioRepository)

        viewModel.uiState.test {
            testDispatcher.scheduler.advanceUntilIdle()
            expectMostRecentItem()
            val result = viewModel.getPodcastById(1L)
            result.shouldNotBeNull()
            result.trackName shouldBe "Test Podcast"
        }
    }

    @Test
    fun getPodcastById_nonExistingPodcast_returnsNull() = runTest {
        viewModel = PodcastListViewModel(repository, episodeAudioRepository)

        viewModel.uiState.test {
            testDispatcher.scheduler.advanceUntilIdle()
            expectMostRecentItem()
            val result = viewModel.getPodcastById(999L)
            result.shouldBeNull()
        }
    }

    @Test
    fun requestCleanupListenedAudioFiles_listenedDownloadsExist_showsDialogWithCount() = runTest {
        // Two listened downloads, plus one downloaded but unlistened, plus one listened but not downloaded
        setupEpisode(id = "ep1", listened = true, isDownloaded = true)
        setupEpisode(id = "ep2", listened = true, isDownloaded = true)
        setupEpisode(id = "ep3", listened = false, isDownloaded = true)
        setupEpisode(id = "ep4", listened = true, isDownloaded = false)

        viewModel = PodcastListViewModel(repository, episodeAudioRepository)
        viewModel.requestCleanupListenedAudioFiles()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            state.cleanupConfirmAudioFileCount shouldBe 2
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun requestCleanupListenedAudioFiles_noListenedDownloads_showsDialogWithZero() = runTest {
        setupEpisode(id = "ep1", listened = false, isDownloaded = true)

        viewModel = PodcastListViewModel(repository, episodeAudioRepository)
        viewModel.requestCleanupListenedAudioFiles()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            state.cleanupConfirmAudioFileCount shouldBe 0
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun requestCleanupListenedAudioFiles_countFails_reportsErrorWithoutDialog() = runTest {
        setupEpisode(id = "ep1", listened = true, isDownloaded = true)
        episodeDao.setDownloadedEpisodeQueryError(IllegalStateException("DB error"))

        viewModel = PodcastListViewModel(repository, episodeAudioRepository)
        viewModel.requestCleanupListenedAudioFiles()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            // A failed count must not be shown as "there is nothing to delete"
            state.cleanupConfirmAudioFileCount.shouldBeNull()
            state.cleanupMessage shouldBe "Failed to check downloads"
            cancelAndIgnoreRemainingEvents()
        }

        episodeAudioRepository.isDownloaded("ep1") shouldBe true
    }

    @Test
    fun confirmCleanupListenedAudioFiles_deletesOnlyListenedDownloads() = runTest {
        setupEpisode(id = "ep1", listened = true, isDownloaded = true)
        setupEpisode(id = "ep2", listened = false, isDownloaded = true)

        viewModel = PodcastListViewModel(repository, episodeAudioRepository)
        viewModel.confirmCleanupListenedAudioFiles()
        testDispatcher.scheduler.advanceUntilIdle()

        // The listened episode's file is gone, the unlistened one is kept
        episodeAudioRepository.isDownloaded("ep1") shouldBe false
        episodeAudioRepository.isDownloaded("ep2") shouldBe true
        episodeDao.getById("ep1")!!.isDownloaded shouldBe false
        episodeDao.getById("ep2")!!.isDownloaded shouldBe true

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            state.cleanupConfirmAudioFileCount.shouldBeNull()
            state.isCleaningUp shouldBe false
            state.cleanupMessage shouldBe "Deleted 1 download"
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun dismissCleanupConfirm_keepsDownloads() = runTest {
        setupEpisode(id = "ep1", listened = true, isDownloaded = true)

        viewModel = PodcastListViewModel(repository, episodeAudioRepository)
        viewModel.requestCleanupListenedAudioFiles()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.dismissCleanupConfirm()

        episodeAudioRepository.isDownloaded("ep1") shouldBe true

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            state.cleanupConfirmAudioFileCount.shouldBeNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    private suspend fun setupEpisode(id: String, listened: Boolean, isDownloaded: Boolean) {
        episodeDao.insert(
            TestDataFactory.createEpisodeEntity(id = id, listened = listened, isDownloaded = isDownloaded),
        )
        if (isDownloaded) {
            audioDownloader.setDownloadedEpisode(id, "/fake/path/$id.mp3")
        }
    }

    @Test
    fun uiStateUpdates_whenRepositoryChanges() = runTest {
        viewModel = PodcastListViewModel(repository, episodeAudioRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            // Initial state
            val initialState = awaitItem()
            initialState.subscribedPodcasts.size shouldBe 0

            // Add a podcast
            val podcast = TestDataFactory.createPodcast(trackId = 1L)
            repository.subscribeToPodcast(podcast, emptyList())
            testDispatcher.scheduler.advanceUntilIdle()

            // Updated state
            val updatedState = awaitItem()
            updatedState.subscribedPodcasts.size shouldBe 1

            cancelAndIgnoreRemainingEvents()
        }
    }
}
