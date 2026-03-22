package jp.kztproject.simplepodcastplayer.screen

import jp.kztproject.simplepodcastplayer.data.Episode
import jp.kztproject.simplepodcastplayer.data.Podcast

data class InProgressEpisodeUiItem(
    val episode: Episode,
    val podcast: Podcast,
    val episodeTitle: String,
    val podcastName: String,
    val artworkUrl: String?,
    val progressPercent: Int,
    val remainingTimeFormatted: String,
)

data class InProgressEpisodesUiState(
    val isLoading: Boolean = true,
    val episodes: List<InProgressEpisodeUiItem> = emptyList(),
)
