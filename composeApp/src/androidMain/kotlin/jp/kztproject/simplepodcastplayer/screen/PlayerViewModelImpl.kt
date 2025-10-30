package jp.kztproject.simplepodcastplayer.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import jp.kztproject.simplepodcastplayer.data.Episode
import jp.kztproject.simplepodcastplayer.data.Podcast
import jp.kztproject.simplepodcastplayer.player.AudioPlayer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PlayerViewModelImpl(
    private val exoPlayer: ExoPlayer,
) : ViewModel(),
    PlayerViewModel {
    private val audioPlayer = AudioPlayer(exoPlayer)

    private val _uiState = MutableStateFlow(PlayerUiState())
    override val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var positionUpdateJob: Job? = null
    private var savePositionJob: Job? = null

    init {
        setupPlayerListener()
    }

    private fun setupPlayerListener() {
        exoPlayer.addListener(
            object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    _uiState.value =
                        _uiState.value.copy(
                            isLoading = playbackState == Player.STATE_BUFFERING,
                        )

                    if (playbackState == Player.STATE_READY) {
                        _uiState.value =
                            _uiState.value.copy(
                                duration = audioPlayer.getDuration(),
                            )
                    }

                    if (playbackState == Player.STATE_ENDED) {
                        handlePlaybackCompleted()
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _uiState.value = _uiState.value.copy(isPlaying = isPlaying)

                    if (isPlaying) {
                        startPositionUpdates()
                        startPeriodicSave()
                    } else {
                        stopPositionUpdates()
                        stopPeriodicSave()
                        saveCurrentPosition()
                    }
                }
            },
        )
    }

    override fun play() {
        audioPlayer.play()
    }

    override fun pause() {
        audioPlayer.pause()
    }

    override fun seekTo(position: Long) {
        audioPlayer.seekTo(position)
        _uiState.value = _uiState.value.copy(currentPosition = position)
    }

    override fun skipForward(seconds: Int) {
        val newPosition = (audioPlayer.getCurrentPosition() + seconds * 1000).coerceAtMost(_uiState.value.duration)
        seekTo(newPosition)
    }

    override fun skipBackward(seconds: Int) {
        val newPosition = (audioPlayer.getCurrentPosition() - seconds * 1000).coerceAtLeast(0)
        seekTo(newPosition)
    }

    override fun setPlaybackSpeed(speed: Float) {
        audioPlayer.setPlaybackSpeed(speed)
        _uiState.value = _uiState.value.copy(playbackSpeed = speed)
    }

    override fun loadEpisode(
        episode: Episode,
        podcast: Podcast,
    ) {
        _uiState.value =
            _uiState.value.copy(
                episode = episode,
                podcast = podcast,
                isLoading = true,
            )

        audioPlayer.loadUrl(episode.audioUrl)

        // TODO: Load saved playback position from database when Room is re-enabled
    }

    override fun release() {
        saveCurrentPosition()
        stopPositionUpdates()
        stopPeriodicSave()
        audioPlayer.release()
    }

    private fun startPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob =
            viewModelScope.launch {
                while (isActive) {
                    val currentPosition = audioPlayer.getCurrentPosition()
                    _uiState.value = _uiState.value.copy(currentPosition = currentPosition)
                    delay(500)
                }
            }
    }

    private fun stopPositionUpdates() {
        positionUpdateJob?.cancel()
    }

    private fun startPeriodicSave() {
        savePositionJob?.cancel()
        savePositionJob =
            viewModelScope.launch {
                while (isActive) {
                    delay(5000) // Save every 5 seconds
                    saveCurrentPosition()
                }
            }
    }

    private fun stopPeriodicSave() {
        savePositionJob?.cancel()
    }

    private fun saveCurrentPosition() {
        // TODO: Save playback position to database when Room is re-enabled
    }

    private fun handlePlaybackCompleted() {
        // TODO: Record playback history to database when Room is re-enabled
    }

    override fun onCleared() {
        super.onCleared()
        release()
    }
}
