package jp.kztproject.simplepodcastplayer.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import jp.kztproject.simplepodcastplayer.data.Episode
import jp.kztproject.simplepodcastplayer.data.Podcast
import jp.kztproject.simplepodcastplayer.data.database.entity.EpisodeEntity
import jp.kztproject.simplepodcastplayer.data.database.entity.PodcastEntity
import jp.kztproject.simplepodcastplayer.data.repository.IPlaybackRepository
import jp.kztproject.simplepodcastplayer.data.repository.IPodcastRepository
import jp.kztproject.simplepodcastplayer.util.formatDuration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Suppress("MagicNumber")
class InProgressEpisodesViewModel(
    private val playbackRepository: IPlaybackRepository,
    private val podcastRepository: IPodcastRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(InProgressEpisodesUiState())
    val uiState: StateFlow<InProgressEpisodesUiState> = _uiState.asStateFlow()

    init {
        loadInProgressEpisodes()
    }

    private fun loadInProgressEpisodes() {
        viewModelScope.launch {
            playbackRepository.getInProgressEpisodes().collect { entities ->
                val items = entities.mapNotNull { entity ->
                    val podcastId = entity.podcastId.toLongOrNull() ?: return@mapNotNull null
                    val podcastEntity = podcastRepository.getPodcast(podcastId) ?: return@mapNotNull null
                    entity.toUiItem(podcastEntity)
                }
                _uiState.value = InProgressEpisodesUiState(isLoading = false, episodes = items)
            }
        }
    }

    private fun EpisodeEntity.toUiItem(podcastEntity: PodcastEntity): InProgressEpisodeUiItem {
        val progressPercent = if (duration > 0L) {
            (lastPlaybackPosition / 1000.0 / duration * 100).toInt().coerceIn(0, 100)
        } else {
            0
        }
        val remainingSeconds = (duration * 1000 - lastPlaybackPosition).coerceAtLeast(0L) / 1000
        return InProgressEpisodeUiItem(
            episode = toEpisode(),
            podcast = podcastEntity.toPodcast(),
            episodeTitle = title,
            podcastName = podcastEntity.name,
            artworkUrl = podcastEntity.imageUrl,
            progressPercent = progressPercent,
            remainingTimeFormatted = formatDuration(remainingSeconds),
        )
    }

    private fun EpisodeEntity.toEpisode(): Episode = Episode(
        id = id,
        podcastId = podcastId,
        title = title,
        description = description,
        audioUrl = audioUrl,
        duration = duration,
        publishedAt = publishedAt,
        listened = listened,
        trackId = trackId,
    )

    private fun PodcastEntity.toPodcast(): Podcast = Podcast(
        trackId = id,
        trackName = name,
        artistName = artistName,
        collectionName = description,
        artworkUrl100 = imageUrl,
        feedUrl = feedUrl,
    )
}
