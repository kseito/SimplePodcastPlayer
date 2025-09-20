package jp.kztproject.simplepodcastplayer.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import jp.kztproject.simplepodcastplayer.data.EpisodeDisplayModel
import jp.kztproject.simplepodcastplayer.data.Podcast
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PodcastDetailViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(PodcastDetailUiState())
    val uiState: StateFlow<PodcastDetailUiState> = _uiState.asStateFlow()

    fun initialize(podcast: Podcast) {
        _uiState.value = _uiState.value.copy(
            podcast = podcast,
            isLoading = true
        )
        loadEpisodes(podcast)
        checkSubscriptionStatus(podcast)
    }

    fun toggleSubscription() {
        val currentState = _uiState.value
        val newSubscriptionStatus = !currentState.isSubscribed

        _uiState.value = currentState.copy(
            isSubscribed = newSubscriptionStatus,
            isSubscriptionLoading = true
        )

        viewModelScope.launch {
            try {
                // Simulate subscription operation
                // Future: Implement actual subscription logic with repository
                kotlinx.coroutines.delay(SUBSCRIPTION_DELAY_MS)

                _uiState.value = _uiState.value.copy(
                    isSubscriptionLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSubscribed = !newSubscriptionStatus,
                    isSubscriptionLoading = false,
                    error = "Failed to update subscription: ${e.message}"
                )
            }
        }
    }

    @Suppress("UnusedParameter")
    fun playEpisode(episodeId: String) {
        // TODO: Implement episode playback logic when PlayerScreen is added
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun loadEpisodes(podcast: Podcast) {
        viewModelScope.launch {
            try {
                // Generate sample episodes for demonstration
                // Future: Replace with actual data source
                // TODO: Replace with real episodes fetched from a data source
                val episodes = createSampleEpisodes(podcast.trackId.toString())

                _uiState.value = _uiState.value.copy(
                    episodes = episodes,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load episodes: ${e.message}"
                )
            }
        }
    }

    @Suppress("UnusedParameter")
    private fun checkSubscriptionStatus(podcast: Podcast) {
        viewModelScope.launch {
            try {
                // Default to not subscribed for demonstration
                // Future: Check actual subscription status from repository
                val isSubscribed = false

                _uiState.value = _uiState.value.copy(
                    isSubscribed = isSubscribed
                )
            } catch (_: Exception) {
                // Ignore subscription check errors for now
            }
        }
    }

    private fun createSampleEpisodes(podcastId: String): List<EpisodeDisplayModel> =
        (1..SAMPLE_EPISODE_COUNT).map { index ->
            EpisodeDisplayModel(
                id = "episode_${podcastId}_$index",
                title = "Episode $index: Sample Episode Title That Might Be Long",
                description = "This is a sample episode description for episode $index",
                publishedAt = "Dec ${20 - index}, 2024",
                duration = "${(30 + index * 5)}:${(10 + index * 2).toString().padStart(2, '0')}",
                audioUrl = "https://example.com/episode$index.mp3",
                listened = index <= 2,
            )
        }

    companion object {
        private const val SAMPLE_EPISODE_COUNT = 5
        private const val SUBSCRIPTION_DELAY_MS = 500L
    }
}

data class PodcastDetailUiState(
    val podcast: Podcast? = null,
    val episodes: List<EpisodeDisplayModel> = emptyList(),
    val isSubscribed: Boolean = false,
    val isLoading: Boolean = false,
    val isSubscriptionLoading: Boolean = false,
    val error: String? = null,
)
