package jp.kztproject.simplepodcastplayer.screen

import jp.kztproject.simplepodcastplayer.data.Episode
import jp.kztproject.simplepodcastplayer.data.Podcast

data class PlayerUiState(
    val episode: Episode? = null,
    val podcast: Podcast? = null,
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val isLoading: Boolean = false,
    val error: String? = null,
    val bufferedPosition: Long = 0L,
)
