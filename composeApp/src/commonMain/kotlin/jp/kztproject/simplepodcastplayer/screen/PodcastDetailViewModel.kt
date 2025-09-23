package jp.kztproject.simplepodcastplayer.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import jp.kztproject.simplepodcastplayer.data.EpisodeDisplayModel
import jp.kztproject.simplepodcastplayer.data.Podcast
import jp.kztproject.simplepodcastplayer.data.RssService
import jp.kztproject.simplepodcastplayer.util.toDisplayModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PodcastDetailViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(PodcastDetailUiState())
    val uiState: StateFlow<PodcastDetailUiState> = _uiState.asStateFlow()

    private val rssService = RssService()

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
        // Future: Implement episode playback logic when PlayerScreen is added
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun loadEpisodes(podcast: Podcast) {
        viewModelScope.launch {
            try {
                val episodes = if (podcast.feedUrl.isNullOrBlank()) {
                    _uiState.value = _uiState.value.copy(
                        error = "No RSS feed URL available for this podcast"
                    )
                    emptyList()
                } else {
                    // Fetch real episodes from RSS feed
                    val result = rssService.fetchEpisodes(podcast.feedUrl)
                    if (result.isSuccess) {
                        result.getOrNull()?.map { it.toDisplayModel() } ?: emptyList()
                    } else {
                        val error = result.exceptionOrNull()
                        _uiState.value = _uiState.value.copy(
                            error = "Failed to load RSS feed: ${error?.message}"
                        )
                        emptyList()
                    }
                }

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

    override fun onCleared() {
        super.onCleared()
        rssService.close()
    }

    companion object {
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
