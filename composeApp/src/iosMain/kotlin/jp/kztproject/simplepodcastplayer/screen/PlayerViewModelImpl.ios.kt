package jp.kztproject.simplepodcastplayer.screen

import jp.kztproject.simplepodcastplayer.data.Episode
import jp.kztproject.simplepodcastplayer.data.Podcast
import jp.kztproject.simplepodcastplayer.data.repository.DownloadRepository
import jp.kztproject.simplepodcastplayer.data.repository.PlaybackRepository
import jp.kztproject.simplepodcastplayer.player.AudioPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class PlayerViewModelImpl : BasePlayerViewModel() {
    override val audioPlayer = AudioPlayer()
    override val playbackRepository = PlaybackRepository()
    override val downloadRepository = DownloadRepository()
    override val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun play() {
        audioPlayer.play()
        _uiState.value = _uiState.value.copy(isPlaying = true)
        startPositionUpdates()
        startPeriodicSave()
    }

    override fun pause() {
        audioPlayer.pause()
        _uiState.value = _uiState.value.copy(isPlaying = false)
        stopPositionUpdates()
        stopPeriodicSave()
        saveCurrentPosition()
    }

    override fun loadEpisode(episode: Episode, podcast: Podcast) {
        // Call parent implementation to handle loading
        super.loadEpisode(episode, podcast)

        // Update Now Playing info for lock screen and control center
        audioPlayer.updateNowPlayingInfo(
            episodeTitle = episode.title,
            podcastName = podcast.trackName,
        )
    }
}
