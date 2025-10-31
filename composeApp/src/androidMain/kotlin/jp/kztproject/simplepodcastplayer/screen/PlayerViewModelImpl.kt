package jp.kztproject.simplepodcastplayer.screen

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import jp.kztproject.simplepodcastplayer.data.Episode
import jp.kztproject.simplepodcastplayer.data.Podcast
import jp.kztproject.simplepodcastplayer.data.database.entity.EpisodeEntity
import jp.kztproject.simplepodcastplayer.data.repository.DownloadRepository
import jp.kztproject.simplepodcastplayer.data.repository.PlaybackRepository
import jp.kztproject.simplepodcastplayer.player.AudioPlayer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PlayerViewModelImpl(private val exoPlayer: ExoPlayer, private val context: Context) :
    ViewModel(),
    PlayerViewModel {
    private val audioPlayer = AudioPlayer(exoPlayer)
    private val playbackRepository = PlaybackRepository()
    private val downloadRepository = DownloadRepository(context)

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

    override fun loadEpisode(episode: Episode, podcast: Podcast) {
        _uiState.value =
            _uiState.value.copy(
                episode = episode,
                podcast = podcast,
                isLoading = true,
            )

        // Save episode to database and load playback position
        viewModelScope.launch {
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
        val episode = _uiState.value.episode ?: return
        val currentPosition = audioPlayer.getCurrentPosition()

        viewModelScope.launch {
            playbackRepository.savePlaybackPosition(episode.id, currentPosition)

            // Check if episode is 95% complete and mark as listened
            val duration = _uiState.value.duration
            if (duration > 0 && currentPosition >= duration * 0.95) {
                playbackRepository.markAsListened(episode.id)
            }
        }
    }

    private fun handlePlaybackCompleted() {
        val episode = _uiState.value.episode ?: return
        val currentPosition = audioPlayer.getCurrentPosition()

        viewModelScope.launch {
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

    override fun onCleared() {
        super.onCleared()
        release()
    }
}
