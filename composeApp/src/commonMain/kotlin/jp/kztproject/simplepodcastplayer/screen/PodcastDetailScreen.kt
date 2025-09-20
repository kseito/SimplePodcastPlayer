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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import jp.kztproject.simplepodcastplayer.data.EpisodeDisplayModel
import jp.kztproject.simplepodcastplayer.data.Podcast
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun PodcastDetailScreen(
    podcast: Podcast,
    onNavigateBack: () -> Unit,
    viewModel: PodcastDetailViewModel = viewModel { PodcastDetailViewModel() },
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
            ),
            actions = PodcastDetailActions(
                onNavigateBack = onNavigateBack,
                onSubscribe = { viewModel.toggleSubscription() },
                onPlayEpisode = { episodeId -> viewModel.playEpisode(episodeId) },
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
)

private data class PodcastDetailActions(
    val onNavigateBack: () -> Unit,
    val onSubscribe: () -> Unit,
    val onPlayEpisode: (String) -> Unit,
)

@Composable
private fun PodcastDetailScreenContent(
    state: PodcastDetailState,
    actions: PodcastDetailActions,
) {
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
            onPlayEpisode = actions.onPlayEpisode,
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
    onPlayEpisode: (String) -> Unit,
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
                        onPlay = { onPlayEpisode(episode.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun EpisodeItem(episode: EpisodeDisplayModel, onPlay: () -> Unit) {
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

                Button(onClick = onPlay) {
                    Text("▶")
                }
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
        )
    }
}
