package jp.kztproject.simplepodcastplayer.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import jp.kztproject.simplepodcastplayer.data.Podcast
import jp.kztproject.simplepodcastplayer.data.database.entity.PodcastEntity
import jp.kztproject.simplepodcastplayer.data.repository.IEpisodeAudioRepository
import jp.kztproject.simplepodcastplayer.data.repository.IPodcastRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PodcastListViewModel(
    private val podcastRepository: IPodcastRepository,
    private val episodeAudioRepository: IEpisodeAudioRepository,
) : ViewModel() {
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

    /**
     * Counts the audio files that a cleanup would delete and opens the confirmation dialog.
     * A count of 0 still opens the dialog, which then only reports that there is nothing to delete.
     */
    fun requestCleanupListenedAudioFiles() {
        viewModelScope.launch {
            val count = runCatching { episodeAudioRepository.countListenedAudioFiles() }
                .onFailure { Napier.e("Failed to count listened audio files", it) }
                .getOrDefault(0)
            _uiState.value = _uiState.value.copy(cleanupConfirmAudioFileCount = count)
        }
    }

    fun confirmCleanupListenedAudioFiles() {
        _uiState.value = _uiState.value.copy(cleanupConfirmAudioFileCount = null, isCleaningUp = true)

        viewModelScope.launch {
            val deletedCount = runCatching { episodeAudioRepository.deleteListenedAudioFiles() }
                .onFailure { Napier.e("Failed to delete listened audio files", it) }
                .getOrNull()

            _uiState.value = _uiState.value.copy(
                isCleaningUp = false,
                cleanupMessage = if (deletedCount == null) {
                    "Failed to delete downloads"
                } else {
                    "Deleted $deletedCount ${if (deletedCount == 1) "download" else "downloads"}"
                },
            )
        }
    }

    fun dismissCleanupConfirm() {
        _uiState.value = _uiState.value.copy(cleanupConfirmAudioFileCount = null)
    }

    fun clearCleanupMessage() {
        _uiState.value = _uiState.value.copy(cleanupMessage = null)
    }
}

data class PodcastListUiState(
    val subscribedPodcasts: List<PodcastEntity> = emptyList(),
    val isLoading: Boolean = true,
    /** Number of listened audio files a cleanup would delete. null hides the confirmation dialog. */
    val cleanupConfirmAudioFileCount: Int? = null,
    val isCleaningUp: Boolean = false,
    val cleanupMessage: String? = null,
)

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
