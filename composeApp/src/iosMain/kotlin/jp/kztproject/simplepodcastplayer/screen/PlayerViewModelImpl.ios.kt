package jp.kztproject.simplepodcastplayer.screen

import jp.kztproject.simplepodcastplayer.data.Episode
import jp.kztproject.simplepodcastplayer.data.Podcast
import jp.kztproject.simplepodcastplayer.player.AudioPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PlayerViewModelImpl : PlayerViewModel {
    private val _uiState = MutableStateFlow(PlayerUiState())
    override val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private val audioPlayer = AudioPlayer()
    private val scope = CoroutineScope(Dispatchers.Main)
    private var positionUpdateJob: Job? = null
    private var durationCheckJob: Job? = null

    override fun play() {
        audioPlayer.play()
        _uiState.value = _uiState.value.copy(isPlaying = true)
        startPositionUpdates()
    }

    override fun pause() {
        audioPlayer.pause()
        _uiState.value = _uiState.value.copy(isPlaying = false)
        stopPositionUpdates()
    }

    override fun seekTo(position: Long) {
        audioPlayer.seekTo(position)
        _uiState.value = _uiState.value.copy(currentPosition = position)
    }

    override fun skipForward(seconds: Int) {
        val newPosition = (_uiState.value.currentPosition + (seconds * 1000)).coerceAtMost(_uiState.value.duration)
        seekTo(newPosition)
    }

    override fun skipBackward(seconds: Int) {
        val newPosition = (_uiState.value.currentPosition - (seconds * 1000)).coerceAtLeast(0)
        seekTo(newPosition)
    }

    override fun setPlaybackSpeed(speed: Float) {
        audioPlayer.setPlaybackSpeed(speed)
        _uiState.value = _uiState.value.copy(playbackSpeed = speed)
    }

    override fun loadEpisode(episode: Episode, podcast: Podcast) {
        _uiState.value = _uiState.value.copy(
            episode = episode,
            podcast = podcast,
            isLoading = true,
        )

        audioPlayer.loadUrl(episode.audioUrl)

        // Start checking for duration availability
        startDurationCheck()
    }

    override fun release() {
        stopPositionUpdates()
        stopDurationCheck()
        audioPlayer.release()
    }

    private fun startPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = scope.launch {
            while (isActive && audioPlayer.isPlaying()) {
                val position = audioPlayer.getCurrentPosition()
                val duration = audioPlayer.getDuration()
                _uiState.value = _uiState.value.copy(
                    currentPosition = position,
                    duration = if (duration > 0) duration else _uiState.value.duration,
                )
                delay(1000)
            }
        }
    }

    private fun stopPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
    }

    private fun startDurationCheck() {
        durationCheckJob?.cancel()
        durationCheckJob = scope.launch {
            // Check duration periodically until it's available
            var attempts = 0
            while (isActive && attempts < 30) { // Try for up to 30 seconds
                val duration = audioPlayer.getDuration()
                if (duration > 0) {
                    _uiState.value = _uiState.value.copy(
                        duration = duration,
                        isLoading = false,
                    )
                    break
                }
                attempts++
                delay(1000) // Check every second
            }

            // If duration is still not available after 30 attempts, stop loading anyway
            if (_uiState.value.isLoading) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    private fun stopDurationCheck() {
        durationCheckJob?.cancel()
        durationCheckJob = null
    }
}
