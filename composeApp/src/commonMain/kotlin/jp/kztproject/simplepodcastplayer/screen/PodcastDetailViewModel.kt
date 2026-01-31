package jp.kztproject.simplepodcastplayer.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import jp.kztproject.simplepodcastplayer.data.Episode
import jp.kztproject.simplepodcastplayer.data.EpisodeDisplayModel
import jp.kztproject.simplepodcastplayer.data.IAppleSearchApiClient
import jp.kztproject.simplepodcastplayer.data.Podcast
import jp.kztproject.simplepodcastplayer.data.repository.IDownloadRepository
import jp.kztproject.simplepodcastplayer.data.repository.PodcastRepository
import jp.kztproject.simplepodcastplayer.download.DownloadState
import jp.kztproject.simplepodcastplayer.util.toDisplayModel
import jp.kztproject.simplepodcastplayer.util.toParsedEpisode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PodcastDetailViewModel(
    private val podcastRepository: PodcastRepository,
    private val downloadRepository: IDownloadRepository,
    private val appleApiClient: IAppleSearchApiClient,
    private val onNavigateToPlayer: (Episode, Podcast) -> Unit = { _, _ -> },
) : ViewModel() {
    private val _uiState = MutableStateFlow(PodcastDetailUiState())
    val uiState: StateFlow<PodcastDetailUiState> = _uiState.asStateFlow()

    private var loadedEpisodes: List<Episode> = emptyList()

    fun initialize(podcast: Podcast) {
        _uiState.value = _uiState.value.copy(
            podcast = podcast,
            isLoading = true,
        )
        loadEpisodes(podcast)
    }

    fun toggleSubscription() {
        val currentState = _uiState.value
        val podcast = currentState.podcast ?: return
        val newSubscriptionStatus = !currentState.isSubscribed

        _uiState.value = currentState.copy(
            isSubscribed = newSubscriptionStatus,
            isSubscriptionLoading = true,
        )

        viewModelScope.launch {
            try {
                if (newSubscriptionStatus) {
                    // Subscribe: Save podcast and episodes to database
                    podcastRepository.subscribeToPodcast(podcast, loadedEpisodes)
                } else {
                    // Unsubscribe: Update subscription status
                    podcastRepository.unsubscribeFromPodcast(podcast.trackId)
                }

                _uiState.value = _uiState.value.copy(
                    isSubscriptionLoading = false,
                )
            } catch (e: Exception) {
                Napier.e("Failed to update subscription", e)
                _uiState.value = _uiState.value.copy(
                    isSubscribed = !newSubscriptionStatus,
                    isSubscriptionLoading = false,
                    error = "Failed to update subscription",
                )
            }
        }
    }

    fun playEpisode(episodeId: String) {
        val episode = _uiState.value.episodes.find { it.id == episodeId } ?: return
        val podcast = _uiState.value.podcast ?: return

        // Convert EpisodeDisplayModel to Episode
        val episodeData = Episode(
            id = episode.id,
            podcastId = podcast.trackId.toString(),
            title = episode.title,
            description = episode.description,
            audioUrl = episode.audioUrl,
            duration = parseDurationToSeconds(episode.duration),
            publishedAt = episode.publishedAt,
            listened = episode.listened,
        )

        onNavigateToPlayer(episodeData, podcast)
    }

    private fun parseDurationToSeconds(duration: String): Long {
        // Duration format is "MM:SS"
        val parts = duration.split(":")
        return if (parts.size == 2) {
            val minutes = parts[0].toLongOrNull() ?: 0
            val seconds = parts[1].toLongOrNull() ?: 0
            minutes * 60 + seconds
        } else {
            0
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun loadEpisodes(podcast: Podcast) {
        viewModelScope.launch {
            try {
                // Check if subscribed first to decide data source
                val isSubscribed = podcastRepository.isSubscribed(podcast.trackId)
                _uiState.value = _uiState.value.copy(isSubscribed = isSubscribed)

                val episodes = if (isSubscribed) {
                    // Subscribed: Load from database (offline support)
                    loadEpisodesFromDatabase(podcast)
                } else {
                    // Not subscribed: Fetch from Apple API
                    loadEpisodesFromAppleApi(podcast)
                }

                _uiState.value = _uiState.value.copy(
                    episodes = episodes,
                    isLoading = false,
                )
            } catch (e: Exception) {
                Napier.e("Failed to load episodes", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load episodes",
                )
            }
        }
    }

    private suspend fun loadEpisodesFromDatabase(podcast: Podcast): List<EpisodeDisplayModel> {
        val episodes = podcastRepository.getEpisodesByPodcastId(podcast.trackId.toString())
        loadedEpisodes = episodes
        return episodes
            .map { episode ->
                val isDownloaded = downloadRepository.isDownloaded(episode.id)
                episode.toDisplayModel(isDownloaded)
            }
            .sortedByDescending { it.trackId }
    }

    private suspend fun loadEpisodesFromAppleApi(podcast: Podcast): List<EpisodeDisplayModel> {
        try {
            val lookupResponse = appleApiClient.lookupEpisodes(podcast.trackId)
            Napier.d("Apple Lookup API returned ${lookupResponse.resultCount} results")

            val parsedEpisodes = lookupResponse.results
                .mapNotNull { it.toParsedEpisode() }

            if (parsedEpisodes.isEmpty()) {
                _uiState.value = _uiState.value.copy(
                    error = "No episodes found for this podcast",
                )
                return emptyList()
            }

            // Store loaded episodes for subscription
            loadedEpisodes =
                parsedEpisodes.map { parsedEpisode ->
                    Episode(
                        id = parsedEpisode.id,
                        podcastId = podcast.trackId.toString(),
                        title = parsedEpisode.title,
                        description = parsedEpisode.description,
                        audioUrl = parsedEpisode.audioUrl,
                        duration = parsedEpisode.duration,
                        publishedAt = parsedEpisode.publishedAt,
                        listened = false,
                        trackId = parsedEpisode.trackId,
                    )
                }

            return parsedEpisodes
                .map { parsedEpisode ->
                    val isDownloaded = downloadRepository.isDownloaded(parsedEpisode.id)
                    parsedEpisode.toDisplayModel(isDownloaded)
                }
                .sortedByDescending { it.trackId }
        } catch (e: Exception) {
            Napier.e("Failed to load episodes from Apple API", e)
            _uiState.value = _uiState.value.copy(
                error = "Failed to load episodes: ${e.message}",
            )
            return emptyList()
        }
    }

    fun downloadEpisode(episodeId: String) {
        val episode = _uiState.value.episodes.find { it.id == episodeId } ?: return

        // Update download state to downloading
        _uiState.update { currentState ->
            val updatedDownloadStates = currentState.downloadStates.toMutableMap()
            updatedDownloadStates[episodeId] = DownloadState.Downloading(0f)
            currentState.copy(downloadStates = updatedDownloadStates)
        }

        viewModelScope.launch {
            try {
                downloadRepository.downloadEpisode(episodeId, episode.audioUrl).collect { state ->
                    Napier.d("Download state update: $state")

                    _uiState.update { currentState ->
                        val updatedDownloadStates = currentState.downloadStates.toMutableMap()
                        updatedDownloadStates[episodeId] = state

                        val updatedEpisodes = when (state) {
                            is DownloadState.Completed -> {
                                // Update episode's isDownloaded status
                                currentState.episodes.map { episode ->
                                    if (episode.id == episodeId) {
                                        episode.copy(isDownloaded = true)
                                    } else {
                                        episode
                                    }
                                }
                            }
                            else -> currentState.episodes
                        }

                        val errorMessage = if (state is DownloadState.Failed) {
                            Napier.e("Download failed: ${state.error}")
                            "Download failed"
                        } else {
                            currentState.error
                        }

                        currentState.copy(
                            downloadStates = updatedDownloadStates,
                            episodes = updatedEpisodes,
                            error = errorMessage,
                        )
                    }
                }
            } catch (e: Exception) {
                Napier.e("Download failed", e)
                _uiState.update { currentState ->
                    val updatedDownloadStates = currentState.downloadStates.toMutableMap()
                    updatedDownloadStates[episodeId] = DownloadState.Failed(e.message ?: "Unknown error")
                    currentState.copy(
                        downloadStates = updatedDownloadStates,
                        error = "Download failed",
                    )
                }
            }
        }
    }

    fun deleteDownload(episodeId: String) {
        viewModelScope.launch {
            try {
                val deleted = downloadRepository.deleteDownload(episodeId)
                _uiState.update { currentState ->
                    if (deleted) {
                        val updatedDownloadStates = currentState.downloadStates.toMutableMap()
                        updatedDownloadStates[episodeId] = DownloadState.Idle

                        val updatedEpisodes = currentState.episodes.map { episode ->
                            if (episode.id == episodeId) {
                                episode.copy(isDownloaded = false)
                            } else {
                                episode
                            }
                        }

                        currentState.copy(
                            downloadStates = updatedDownloadStates,
                            episodes = updatedEpisodes,
                        )
                    } else {
                        currentState.copy(error = "Failed to delete download")
                    }
                }
            } catch (e: Exception) {
                Napier.e("Failed to delete download", e)
                _uiState.update { it.copy(error = "Failed to delete download") }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        appleApiClient.close()
    }
}

data class PodcastDetailUiState(
    val podcast: Podcast? = null,
    val episodes: List<EpisodeDisplayModel> = emptyList(),
    val isSubscribed: Boolean = false,
    val isLoading: Boolean = false,
    val isSubscriptionLoading: Boolean = false,
    val error: String? = null,
    val downloadStates: Map<String, DownloadState> = emptyMap(),
)
