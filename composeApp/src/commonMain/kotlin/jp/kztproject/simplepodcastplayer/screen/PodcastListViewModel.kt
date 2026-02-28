package jp.kztproject.simplepodcastplayer.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import jp.kztproject.simplepodcastplayer.data.Podcast
import jp.kztproject.simplepodcastplayer.data.database.entity.PodcastEntity
import jp.kztproject.simplepodcastplayer.data.repository.IPodcastRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PodcastListViewModel(private val podcastRepository: IPodcastRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(PodcastListUiState())
    val uiState: StateFlow<PodcastListUiState> = _uiState.asStateFlow()

    init {
        loadSubscribedPodcasts()
    }

    private fun loadSubscribedPodcasts() {
        viewModelScope.launch {
            podcastRepository.getSubscribedPodcasts().collect { podcasts ->
                _uiState.value =
                    _uiState.value.copy(
                        subscribedPodcasts = podcasts,
                        isLoading = false,
                    )
            }
        }
    }

    fun getPodcastById(podcastId: Long): Podcast? {
        val podcastEntity = _uiState.value.subscribedPodcasts.find { it.id == podcastId }
        return podcastEntity?.toPodcast()
    }
}

data class PodcastListUiState(val subscribedPodcasts: List<PodcastEntity> = emptyList(), val isLoading: Boolean = true)

/**
 * Convert PodcastEntity to Podcast
 */
private fun PodcastEntity.toPodcast(): Podcast = Podcast(
    trackId = id,
    trackName = name,
    artistName = artistName,
    collectionName = description,
    artworkUrl100 = imageUrl,
    feedUrl = feedUrl,
)
