package jp.kztproject.simplepodcastplayer.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import org.jetbrains.compose.ui.tooling.preview.Preview
import jp.kztproject.simplepodcastplayer.data.Episode
import jp.kztproject.simplepodcastplayer.data.Podcast
import jp.kztproject.simplepodcastplayer.ui.HtmlText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(viewModel: PlayerViewModel, onNavigateBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Now Playing") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close")
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier =
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            EpisodeArtwork(artworkUrl = uiState.podcast?.bestArtworkUrl())

            Spacer(modifier = Modifier.height(32.dp))

            EpisodeInformation(
                title = uiState.episode?.title ?: "",
                podcastName = uiState.podcast?.trackName ?: "",
                description = uiState.episode?.description ?: "",
            )

            Spacer(modifier = Modifier.height(32.dp))

            PlaybackControls(
                state = PlaybackState(
                    isPlaying = uiState.isPlaying,
                    isLoading = uiState.isLoading,
                    currentPosition = uiState.currentPosition,
                    duration = uiState.duration,
                ),
                actions = PlaybackActions(
                    onPlayPause = {
                        if (uiState.isPlaying) {
                            viewModel.pause()
                        } else {
                            viewModel.play()
                        }
                    },
                    onSeek = { viewModel.seekTo(it.toLong()) },
                    onSkipBackward = { viewModel.skipBackward(15) },
                    onSkipForward = { viewModel.skipForward(15) },
                ),
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun EpisodeArtwork(artworkUrl: String?) {
    AsyncImage(
        model = artworkUrl,
        contentDescription = "Episode artwork",
        modifier =
        Modifier
            .size(300.dp)
            .clip(RoundedCornerShape(8.dp)),
    )
}

@Composable
private fun EpisodeInformation(title: String, podcastName: String, description: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = podcastName,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        HtmlText(
            html = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
        )
    }
}

private data class PlaybackState(
    val isPlaying: Boolean,
    val isLoading: Boolean,
    val currentPosition: Long,
    val duration: Long,
)

private data class PlaybackActions(
    val onPlayPause: () -> Unit,
    val onSeek: (Float) -> Unit,
    val onSkipBackward: () -> Unit,
    val onSkipForward: () -> Unit,
)

@Composable
private fun PlaybackControls(state: PlaybackState, actions: PlaybackActions) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ProgressBar(
            currentPosition = state.currentPosition,
            duration = state.duration,
            onSeek = actions.onSeek,
        )

        TimeLabels(
            currentPosition = state.currentPosition,
            duration = state.duration,
        )

        Spacer(modifier = Modifier.height(24.dp))

        ControlButtons(
            isPlaying = state.isPlaying,
            isLoading = state.isLoading,
            onPlayPause = actions.onPlayPause,
            onSkipBackward = actions.onSkipBackward,
            onSkipForward = actions.onSkipForward,
        )
    }
}

@Composable
private fun ProgressBar(currentPosition: Long, duration: Long, onSeek: (Float) -> Unit) {
    Slider(
        value = if (duration > 0) currentPosition.toFloat() else 0f,
        onValueChange = onSeek,
        valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun TimeLabels(currentPosition: Long, duration: Long) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = formatTime(currentPosition),
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            text = formatTime(duration),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun ControlButtons(
    isPlaying: Boolean,
    isLoading: Boolean,
    onPlayPause: () -> Unit,
    onSkipBackward: () -> Unit,
    onSkipForward: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onSkipBackward) {
            Text("-15s", style = MaterialTheme.typography.titleLarge)
        }

        PlayPauseButton(
            isPlaying = isPlaying,
            isLoading = isLoading,
            onClick = onPlayPause,
        )

        TextButton(onClick = onSkipForward) {
            Text("+15s", style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun PlayPauseButton(isPlaying: Boolean, isLoading: Boolean, onClick: () -> Unit) {
    FilledIconButton(
        onClick = onClick,
        modifier = Modifier.size(80.dp),
        colors =
        IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.primary,
        ),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(40.dp),
                color = MaterialTheme.colorScheme.onPrimary,
            )
        } else {
            Icon(
                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                modifier = Modifier.size(48.dp),
            )
        }
    }
}

private fun formatTime(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

// Mock ViewModel for Preview
private class PreviewPlayerViewModel : PlayerViewModel {
    private val _uiState = MutableStateFlow(
        PlayerUiState(
            podcast = Podcast(
                trackId = 1,
                trackName = "Sample Podcast",
                artistName = "Podcast Creator",
                artworkUrl60 = null,
                artworkUrl100 = null,
            ),
            episode = Episode(
                id = "episode-1",
                podcastId = "podcast-1",
                title = "Episode 1: Introduction to Podcasting",
                description =
                "In this episode, we explore the fundamentals of podcasting " +
                    "and how to get started with your own show.",
                audioUrl = "https://example.com/episode1.mp3",
                duration = 1800000, // 30 minutes
                publishedAt = "2024-01-01T00:00:00Z",
                listened = false,
            ),
            isPlaying = true,
            isLoading = false,
            currentPosition = 600000, // 10 minutes
            duration = 1800000, // 30 minutes
        ),
    )
    override val uiState: StateFlow<PlayerUiState> = _uiState
    override fun play() {}
    override fun pause() {}
    override fun seekTo(position: Long) {}
    override fun skipForward(seconds: Int) {}
    override fun skipBackward(seconds: Int) {}
    override fun setPlaybackSpeed(speed: Float) {}
    override fun loadEpisode(episode: Episode, podcast: Podcast) {}
    override fun release() {}
}

@Preview
@Composable
fun PlayerScreenPreview() {
    MaterialTheme {
        PlayerScreen(
            viewModel = PreviewPlayerViewModel(),
            onNavigateBack = {},
        )
    }
}
