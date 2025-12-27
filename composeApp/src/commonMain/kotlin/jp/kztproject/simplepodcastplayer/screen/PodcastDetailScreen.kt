package jp.kztproject.simplepodcastplayer.screen

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import jp.kztproject.simplepodcastplayer.data.Episode
import jp.kztproject.simplepodcastplayer.data.EpisodeDisplayModel
import jp.kztproject.simplepodcastplayer.data.Podcast
import jp.kztproject.simplepodcastplayer.download.DownloadState
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun PodcastDetailScreen(
    podcast: Podcast,
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: (Episode, Podcast) -> Unit,
    viewModel: PodcastDetailViewModel = koinViewModel { parametersOf(onNavigateToPlayer) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(podcast) {
        viewModel.initialize(podcast)
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Box {
        PodcastDetailScreenContent(
            state = PodcastDetailState(
                podcast = uiState.podcast ?: podcast,
                episodes = uiState.episodes,
                isSubscribed = uiState.isSubscribed,
                isLoading = uiState.isLoading,
                isSubscriptionLoading = uiState.isSubscriptionLoading,
                downloadStates = uiState.downloadStates,
            ),
            actions = PodcastDetailActions(
                onNavigateBack = onNavigateBack,
                onSubscribe = { viewModel.toggleSubscription() },
                onPlayEpisode = { episodeId -> viewModel.playEpisode(episodeId) },
                onDownloadEpisode = { episodeId -> viewModel.downloadEpisode(episodeId) },
                onDeleteDownload = { episodeId -> viewModel.deleteDownload(episodeId) },
            ),
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

private data class PodcastDetailState(
    val podcast: Podcast,
    val episodes: List<EpisodeDisplayModel>,
    val isSubscribed: Boolean,
    val isLoading: Boolean = false,
    val isSubscriptionLoading: Boolean = false,
    val downloadStates: Map<String, DownloadState> = emptyMap(),
)

private data class PodcastDetailActions(
    val onNavigateBack: () -> Unit,
    val onSubscribe: () -> Unit,
    val onPlayEpisode: (String) -> Unit,
    val onDownloadEpisode: (String) -> Unit,
    val onDeleteDownload: (String) -> Unit,
)

@Composable
private fun PodcastDetailScreenContent(state: PodcastDetailState, actions: PodcastDetailActions) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(16.dp),
    ) {
        // Header with back button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = actions.onNavigateBack) {
                Text("← Back")
            }
            Text(
                text = "Podcast Detail",
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(modifier = Modifier.width(48.dp)) // Balance the layout
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Podcast information section
        PodcastInfoSection(
            podcast = state.podcast,
            isSubscribed = state.isSubscribed,
            isSubscriptionLoading = state.isSubscriptionLoading,
            onSubscribe = actions.onSubscribe,
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Episodes section
        EpisodesSection(
            episodes = state.episodes,
            isLoading = state.isLoading,
            downloadStates = state.downloadStates,
            onPlayEpisode = actions.onPlayEpisode,
            onDownloadEpisode = actions.onDownloadEpisode,
            onDeleteDownload = actions.onDeleteDownload,
        )
    }
}

@Composable
private fun PodcastInfoSection(
    podcast: Podcast,
    isSubscribed: Boolean,
    isSubscriptionLoading: Boolean,
    onSubscribe: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Podcast thumbnail
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = podcast.bestArtworkUrl(),
                contentDescription = "Podcast artwork for ${podcast.trackName}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            if (!podcast.hasArtwork) {
                Text(
                    text = "🎧",
                    style = MaterialTheme.typography.displayMedium,
                )
            }
        }

        // Podcast details
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Title
            Text(
                text = podcast.trackName,
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            // Author
            Text(
                text = podcast.artistName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subscribe button
            Button(
                onClick = onSubscribe,
                enabled = !isSubscribed && !isSubscriptionLoading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isSubscriptionLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(if (isSubscribed) "購読中" else "購読する")
                }
            }
        }
    }
}

@Composable
private fun EpisodesSection(
    episodes: List<EpisodeDisplayModel>,
    isLoading: Boolean,
    downloadStates: Map<String, DownloadState>,
    onPlayEpisode: (String) -> Unit,
    onDownloadEpisode: (String) -> Unit,
    onDeleteDownload: (String) -> Unit,
) {
    Column {
        Text(
            text = "Episodes",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 12.dp),
        )

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(episodes) { episode ->
                    EpisodeItem(
                        episode = episode,
                        downloadState = downloadStates[episode.id],
                        onPlay = { onPlayEpisode(episode.id) },
                        onDownload = { onDownloadEpisode(episode.id) },
                        onDeleteDownload = { onDeleteDownload(episode.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun EpisodeItem(
    episode: EpisodeDisplayModel,
    downloadState: DownloadState?,
    onPlay: () -> Unit,
    onDownload: () -> Unit,
    onDeleteDownload: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            // Episode title
            Text(
                text = episode.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Episode metadata
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = episode.publishedAt,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = episode.duration,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Download button
                    DownloadButton(
                        isDownloaded = episode.isDownloaded,
                        downloadState = downloadState,
                        onDownload = onDownload,
                        onDeleteDownload = onDeleteDownload,
                    )

                    Button(onClick = onPlay) {
                        Text("▶")
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadButton(
    isDownloaded: Boolean,
    downloadState: DownloadState?,
    onDownload: () -> Unit,
    onDeleteDownload: () -> Unit,
) {
    when {
        downloadState is DownloadState.Downloading -> {
            // Show progress indicator
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (downloadState.progress > 0f) {
                    // Determinate progress
                    CircularProgressIndicator(
                        progress = { downloadState.progress },
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    // Indeterminate progress (when progress is 0 or unknown)
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                    )
                }
            }
        }
        isDownloaded -> {
            // Show delete button
            IconButton(onClick = onDeleteDownload) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete download",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
        else -> {
            // Show download button
            IconButton(onClick = onDownload) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Download episode",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Preview
@Composable
fun PodcastDetailScreenPreview() {
    val samplePodcast = Podcast(
        trackId = 1L,
        trackName = "Sample Tech Podcast with a Very Long Title That Should Wrap",
        artistName = "Tech Creator",
        collectionName = "Tech Collection",
        trackViewUrl = "https://example.com/podcast",
        artworkUrl100 = "https://example.com/artwork.jpg",
        primaryGenreName = "Technology",
        trackCount = 150,
    )

    MaterialTheme {
        PodcastDetailScreen(
            podcast = samplePodcast,
            onNavigateBack = {},
            onNavigateToPlayer = { _, _ -> },
        )
    }
}
