package jp.kztproject.simplepodcastplayer.screen

import app.cash.turbine.test
import jp.kztproject.simplepodcastplayer.fake.FakeAppleSearchApiClient
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
class PodcastSearchViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var apiClient: FakeAppleSearchApiClient
    private lateinit var viewModel: PodcastSearchViewModel

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        apiClient = FakeAppleSearchApiClient()
        viewModel = PodcastSearchViewModel(apiClient)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_isCorrect() = runTest {
        assertEquals("", viewModel.searchQuery.value)
        assertTrue(viewModel.podcasts.value.isEmpty())
        assertFalse(viewModel.isLoading.value)
        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun updateSearchQuery_updatesState() = runTest {
        viewModel.updateSearchQuery("test query")

        viewModel.searchQuery.test {
            assertEquals("test query", awaitItem())
        }
    }

    @Test
    fun searchPodcasts_withBlankQuery_doesNotSearch() = runTest {
        viewModel.updateSearchQuery("   ")

        viewModel.searchPodcasts()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.podcasts.test {
            assertTrue(awaitItem().isEmpty())
        }
    }

    @Test
    fun searchPodcasts_success_updatesPodcasts() = runTest {
        val podcast1 = TestDataFactory.createPodcast(trackId = 1L, trackName = "Podcast 1")
        val podcast2 = TestDataFactory.createPodcast(trackId = 2L, trackName = "Podcast 2")
        apiClient.setSearchResult(listOf(podcast1, podcast2))

        viewModel.updateSearchQuery("test")
        viewModel.searchPodcasts()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.podcasts.test {
            val podcasts = awaitItem()
            assertEquals(2, podcasts.size)
            assertEquals("Podcast 1", podcasts[0].trackName)
            assertEquals("Podcast 2", podcasts[1].trackName)
        }
    }

    @Test
    fun searchPodcasts_success_clearsErrorMessage() = runTest {
        apiClient.setSearchResult(emptyList())

        viewModel.updateSearchQuery("test")
        viewModel.searchPodcasts()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.errorMessage.test {
            assertNull(awaitItem())
        }
    }

    @Test
    fun searchPodcasts_error_setsErrorMessage() = runTest {
        apiClient.setShouldThrowError(Exception("Network error"))

        viewModel.updateSearchQuery("test")
        viewModel.searchPodcasts()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.errorMessage.test {
            val error = awaitItem()
            assertNotNull(error)
            assertTrue(error.contains("エラーが発生しました"))
        }
    }

    @Test
    fun searchPodcasts_error_clearsPodcasts() = runTest {
        // Setup: First successful search
        val podcast = TestDataFactory.createPodcast(trackId = 1L)
        apiClient.setSearchResult(listOf(podcast))
        viewModel.updateSearchQuery("test")
        viewModel.searchPodcasts()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then error
        apiClient.setShouldThrowError(Exception("Network error"))
        viewModel.searchPodcasts()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.podcasts.test {
            assertTrue(awaitItem().isEmpty())
        }
    }

    @Test
    fun searchPodcasts_finishesWithLoadingFalse() = runTest {
        apiClient.setSearchResult(emptyList())
        viewModel.updateSearchQuery("test")

        viewModel.searchPodcasts()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.isLoading.test {
            assertFalse(awaitItem())
        }
    }

    @Test
    fun searchPodcasts_emptyResults_returnsEmptyList() = runTest {
        apiClient.setSearchResult(emptyList())

        viewModel.updateSearchQuery("nonexistent")
        viewModel.searchPodcasts()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.podcasts.test {
            assertTrue(awaitItem().isEmpty())
        }
    }
}
