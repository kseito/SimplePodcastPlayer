package jp.kztproject.simplepodcastplayer.screen

import jp.kztproject.simplepodcastplayer.data.Episode
import jp.kztproject.simplepodcastplayer.data.Podcast
import jp.kztproject.simplepodcastplayer.data.database.entity.EpisodeEntity
import jp.kztproject.simplepodcastplayer.data.repository.IDownloadRepository
import jp.kztproject.simplepodcastplayer.data.repository.PlaybackRepository
import jp.kztproject.simplepodcastplayer.player.AudioPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

abstract class BasePlayerViewModel : PlayerViewModel {
    @Suppress("ktlint:standard:backing-property-naming")
    internal val _uiState = MutableStateFlow(PlayerUiState())
    override val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    internal abstract val audioPlayer: AudioPlayer
    internal abstract val playbackRepository: PlaybackRepository
    internal abstract val downloadRepository: IDownloadRepository
    internal abstract val coroutineScope: CoroutineScope

    internal var positionUpdateJob: Job? = null
    internal var savePositionJob: Job? = null
    internal var durationCheckJob: Job? = null

    // Platform-specific implementations must override these
    abstract override fun play()

    abstract override fun pause()

    override fun seekTo(position: Long) {
        audioPlayer.seekTo(position)
        _uiState.value = _uiState.value.copy(currentPosition = position)
    }

    override fun skipForward(seconds: Int) {
        val newPosition =
            (audioPlayer.getCurrentPosition() + seconds * 1000).coerceAtMost(_uiState.value.duration)
        seekTo(newPosition)
    }

    override fun skipBackward(seconds: Int) {
        val newPosition =
            (audioPlayer.getCurrentPosition() - seconds * 1000).coerceAtLeast(0)
        seekTo(newPosition)
    }

    override fun setPlaybackSpeed(speed: Float) {
        audioPlayer.setPlaybackSpeed(speed)
        _uiState.value = _uiState.value.copy(playbackSpeed = speed)
    }

    override fun loadEpisode(episode: Episode, podcast: Podcast) {
        _uiState.value =
            _uiState.value.copy(
                episode = episode,
                podcast = podcast,
                isLoading = true,
            )

        // Save episode to database and load playback position
        coroutineScope.launch {
            // Get existing episode from database to preserve playback position
            val existingEpisode = playbackRepository.getEpisode(episode.id)

            if (existingEpisode == null) {
                // New episode - save to database
                val episodeEntity =
                    EpisodeEntity(
                        id = episode.id,
                        podcastId = episode.podcastId,
                        title = episode.title,
                        description = episode.description,
                        audioUrl = episode.audioUrl,
                        duration = episode.duration,
                        publishedAt = episode.publishedAt,
                        listened = episode.listened,
                    )
                playbackRepository.saveEpisode(episodeEntity)
            }

            // Load saved playback position (will be 0 for new episodes)
            val savedPosition = playbackRepository.getPlaybackPosition(episode.id)

            // Check if episode is downloaded, use local file if available
            val audioSource =
                downloadRepository.getLocalFilePath(episode.id) ?: episode.audioUrl

            audioPlayer.loadUrl(audioSource)

            // Start checking for duration availability
            startDurationCheck()

            // Seek to saved position if exists
            if (savedPosition > 0) {
                audioPlayer.seekTo(savedPosition)
                _uiState.value = _uiState.value.copy(currentPosition = savedPosition)
            }
        }
    }

    override fun release() {
        saveCurrentPosition()
        stopPositionUpdates()
        stopPeriodicSave()
        stopDurationCheck()
        audioPlayer.release()
    }

    internal fun startPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob =
            coroutineScope.launch {
                while (isActive) {
                    val currentPosition = audioPlayer.getCurrentPosition()
                    val duration = audioPlayer.getDuration()
                    _uiState.value =
                        _uiState.value.copy(
                            currentPosition = currentPosition,
                            duration = if (duration > 0) duration else _uiState.value.duration,
                        )
                    delay(500)
                }
            }
    }

    internal fun stopPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
    }

    internal fun startPeriodicSave() {
        savePositionJob?.cancel()
        savePositionJob =
            coroutineScope.launch {
                while (isActive) {
                    delay(5000) // Save every 5 seconds
                    saveCurrentPosition()
                }
            }
    }

    internal fun stopPeriodicSave() {
        savePositionJob?.cancel()
    }

    internal fun saveCurrentPosition() {
        val episode = _uiState.value.episode ?: return
        val currentPosition = audioPlayer.getCurrentPosition()

        coroutineScope.launch {
            playbackRepository.savePlaybackPosition(episode.id, currentPosition)

            // Check if episode is 95% complete and mark as listened
            val duration = _uiState.value.duration
            if (duration > 0 && currentPosition >= duration * 0.95) {
                playbackRepository.markAsListened(episode.id)
            }
        }
    }

    internal fun handlePlaybackCompleted() {
        val episode = _uiState.value.episode ?: return
        val currentPosition = audioPlayer.getCurrentPosition()

        coroutineScope.launch {
            // Mark as listened
            playbackRepository.markAsListened(episode.id)

            // Record play history
            playbackRepository.recordPlayHistory(
                episodeId = episode.id,
                position = currentPosition,
                completed = true,
            )
        }
    }

    internal fun startDurationCheck() {
        durationCheckJob?.cancel()
        durationCheckJob =
            coroutineScope.launch {
                // Check duration periodically until it's available
                var attempts = 0
                while (isActive && attempts < 30) { // Try for up to 30 seconds
                    val duration = audioPlayer.getDuration()
                    if (duration > 0) {
                        _uiState.value =
                            _uiState.value.copy(
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

    internal fun stopDurationCheck() {
        durationCheckJob?.cancel()
    }
}
