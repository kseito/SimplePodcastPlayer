package jp.kztproject.simplepodcastplayer.screen

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import jp.kztproject.simplepodcastplayer.data.repository.DownloadRepository
import jp.kztproject.simplepodcastplayer.data.repository.PlaybackRepository
import jp.kztproject.simplepodcastplayer.player.AudioPlayer
import kotlinx.coroutines.CoroutineScope

class PlayerViewModelImpl(
    exoPlayer: ExoPlayer,
    context: Context,
    playbackRepository: PlaybackRepository,
    downloadRepository: DownloadRepository,
) : ViewModel(),
    PlayerViewModel {
    private val delegate =
        object : BasePlayerViewModel() {
            override val audioPlayer = AudioPlayer(exoPlayer)
            override val playbackRepository = playbackRepository
            override val downloadRepository = downloadRepository
            override val coroutineScope: CoroutineScope = viewModelScope

            override fun play() {
                audioPlayer.play()
            }

            override fun pause() {
                audioPlayer.pause()
            }
        }

    override val uiState = delegate.uiState

    init {
        setupPlayerListener(exoPlayer)
    }

    private fun setupPlayerListener(exoPlayer: ExoPlayer) {
        exoPlayer.addListener(
            object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    delegate._uiState.value =
                        delegate._uiState.value.copy(
                            isLoading = playbackState == Player.STATE_BUFFERING,
                        )

                    if (playbackState == Player.STATE_READY) {
                        delegate._uiState.value =
                            delegate._uiState.value.copy(
                                duration = delegate.audioPlayer.getDuration(),
                            )
                    }

                    if (playbackState == Player.STATE_ENDED) {
                        delegate.handlePlaybackCompleted()
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    delegate._uiState.value = delegate._uiState.value.copy(isPlaying = isPlaying)

                    if (isPlaying) {
                        delegate.startPositionUpdates()
                        delegate.startPeriodicSave()
                    } else {
                        delegate.stopPositionUpdates()
                        delegate.stopPeriodicSave()
                        delegate.saveCurrentPosition()
                    }
                }
            },
        )
    }

    override fun play() = delegate.play()

    override fun pause() = delegate.pause()

    override fun seekTo(position: Long) = delegate.seekTo(position)

    override fun skipForward(seconds: Int) = delegate.skipForward(seconds)

    override fun skipBackward(seconds: Int) = delegate.skipBackward(seconds)

    override fun setPlaybackSpeed(speed: Float) = delegate.setPlaybackSpeed(speed)

    override fun loadEpisode(
        episode: jp.kztproject.simplepodcastplayer.data.Episode,
        podcast: jp.kztproject.simplepodcastplayer.data.Podcast,
    ) = delegate.loadEpisode(episode, podcast)

    override fun release() = delegate.release()

    override fun onCleared() {
        super.onCleared()
        release()
    }
}
