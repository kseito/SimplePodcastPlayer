package jp.kztproject.simplepodcastplayer.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import jp.kztproject.simplepodcastplayer.data.Episode
import jp.kztproject.simplepodcastplayer.data.Podcast
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun InProgressEpisodesScreen(onNavigateBack: () -> Unit, onNavigateToPlayer: (Episode, Podcast) -> Unit) {
    val viewModel: InProgressEpisodesViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    InProgressEpisodesContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onNavigateToPlayer = onNavigateToPlayer,
    )
}

@Composable
private fun InProgressEpisodesContent(
    uiState: InProgressEpisodesUiState,
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: (Episode, Podcast) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "In Progress",
                style = MaterialTheme.typography.headlineMedium,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.episodes.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No episodes in progress",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(items = uiState.episodes, key = { it.episode.id }) { item ->
                        InProgressEpisodeItem(
                            item = item,
                            onClick = { onNavigateToPlayer(item.episode, item.podcast) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InProgressEpisodeItem(item: InProgressEpisodeUiItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (item.artworkUrl != null) {
                AsyncImage(
                    model = item.artworkUrl,
                    contentDescription = "Artwork for ${item.podcastName}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Text(
                    text = "🎧",
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.episodeTitle,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.podcastName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { item.progressPercent / 100f },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${item.remainingTimeFormatted} remaining",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview
@Composable
fun InProgressEpisodesScreenPreview() {
    val sampleEpisode = Episode(
        id = "ep1",
        podcastId = "1",
        title = "Episode 1: Introduction to Kotlin Multiplatform",
        description = "In this episode we cover the basics.",
        audioUrl = "https://example.com/ep1.mp3",
        duration = 3600L,
        publishedAt = "2024-01-15",
    )
    val samplePodcast = Podcast(
        trackId = 1L,
        trackName = "Sample Tech Podcast",
        artistName = "Tech Creator",
    )
    MaterialTheme {
        InProgressEpisodesContent(
            uiState = InProgressEpisodesUiState(
                isLoading = false,
                episodes = listOf(
                    InProgressEpisodeUiItem(
                        episode = sampleEpisode,
                        podcast = samplePodcast,
                        episodeTitle = "Episode 1: Introduction to Kotlin Multiplatform",
                        podcastName = "Sample Tech Podcast",
                        artworkUrl = null,
                        progressPercent = 45,
                        remainingTimeFormatted = "33:00",
                    ),
                    InProgressEpisodeUiItem(
                        episode = sampleEpisode.copy(id = "ep2", title = "Episode 2: Compose Multiplatform Deep Dive"),
                        podcast = samplePodcast,
                        episodeTitle = "Episode 2: Compose Multiplatform Deep Dive",
                        podcastName = "Sample Tech Podcast",
                        artworkUrl = null,
                        progressPercent = 72,
                        remainingTimeFormatted = "14:22",
                    ),
                ),
            ),
            onNavigateBack = {},
            onNavigateToPlayer = { _, _ -> },
        )
    }
}

@Preview
@Composable
fun InProgressEpisodesScreenEmptyPreview() {
    MaterialTheme {
        InProgressEpisodesContent(
            uiState = InProgressEpisodesUiState(isLoading = false, episodes = emptyList()),
            onNavigateBack = {},
            onNavigateToPlayer = { _, _ -> },
        )
    }
}
