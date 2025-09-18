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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import jp.kztproject.simplepodcastplayer.data.EpisodeDisplayModel
import jp.kztproject.simplepodcastplayer.data.Podcast
import org.jetbrains.compose.ui.tooling.preview.Preview

// TODO: Use ViewModel to manage state and business logic
@Composable
fun PodcastDetailScreen(podcast: Podcast, onNavigateBack: () -> Unit) {
    var isSubscribed by remember { mutableStateOf(false) }
    // TODO: Replace with real episodes fetched from a data source
    val episodes = remember { createSampleEpisodes(podcast.trackId.toString()) }

    PodcastDetailScreenContent(
        state = PodcastDetailState(
            podcast = podcast,
            episodes = episodes,
            isSubscribed = isSubscribed,
        ),
        actions = PodcastDetailActions(
            onNavigateBack = onNavigateBack,
            onSubscribe = { isSubscribed = true },
            onPlayEpisode = { episodeId ->
                // TODO: Navigation to PlayerScreen will be implemented in future
            },
        ),
    )
}

private data class PodcastDetailState(
    val podcast: Podcast,
    val episodes: List<EpisodeDisplayModel>,
    val isSubscribed: Boolean,
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
            onSubscribe = actions.onSubscribe,
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Episodes section
        EpisodesSection(
            episodes = state.episodes,
            onPlayEpisode = actions.onPlayEpisode,
        )
    }
}

@Composable
private fun PodcastInfoSection(podcast: Podcast, isSubscribed: Boolean, onSubscribe: () -> Unit) {
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
                enabled = !isSubscribed,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (isSubscribed) "購読中" else "購読する")
            }
        }
    }
}

@Composable
private fun EpisodesSection(episodes: List<EpisodeDisplayModel>, onPlayEpisode: (String) -> Unit) {
    Column {
        Text(
            text = "Episodes",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 12.dp),
        )

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

private const val SAMPLE_EPISODE_COUNT = 5

private fun createSampleEpisodes(podcastId: String): List<EpisodeDisplayModel> =
    (1..SAMPLE_EPISODE_COUNT).map { index ->
    EpisodeDisplayModel(
        id = "episode_${podcastId}_$index",
        title = "Episode $index: Sample Episode Title That Might Be Long",
        description = "This is a sample episode description for episode $index",
        publishedAt = "Dec ${20 - index}, 2024",
        duration = "${(30 + index * 5)}:${(10 + index * 2).toString().padStart(2, '0')}",
        audioUrl = "https://example.com/episode$index.mp3",
        listened = index <= 2,
    )
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
