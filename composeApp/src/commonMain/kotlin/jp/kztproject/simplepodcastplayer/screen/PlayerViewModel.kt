package jp.kztproject.simplepodcastplayer.screen

import jp.kztproject.simplepodcastplayer.data.Episode
import jp.kztproject.simplepodcastplayer.data.Podcast
import kotlinx.coroutines.flow.StateFlow

interface PlayerViewModel {
    val uiState: StateFlow<PlayerUiState>

    fun play()

    fun pause()

    fun seekTo(position: Long)

    fun skipForward(seconds: Int = 15)

    fun skipBackward(seconds: Int = 15)

    fun setPlaybackSpeed(speed: Float)

    fun loadEpisode(episode: Episode, podcast: Podcast)

    fun release()
}
