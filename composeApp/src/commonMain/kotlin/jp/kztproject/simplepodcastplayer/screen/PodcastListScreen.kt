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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
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
import jp.kztproject.simplepodcastplayer.data.database.entity.PodcastEntity
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PodcastListScreen(onNavigateToSearch: () -> Unit, onPodcastClick: (Long) -> Unit) {
    val viewModel: PodcastListViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    PodcastListContent(uiState = uiState, onNavigateToSearch = onNavigateToSearch, onPodcastClick = onPodcastClick)
}

@Composable
private fun PodcastListContent(
    uiState: PodcastListUiState,
    onNavigateToSearch: () -> Unit,
    onPodcastClick: (Long) -> Unit,
) {
    Column(
        modifier =
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(16.dp),
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "My Podcasts",
                style = MaterialTheme.typography.headlineMedium,
            )
            Button(onClick = onNavigateToSearch) {
                Text("Search")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Content
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.subscribedPodcasts.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "No subscribed podcasts",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Search for podcasts to subscribe",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(uiState.subscribedPodcasts) { podcast ->
                        PodcastListItem(
                            podcast = podcast,
                            onClick = { onPodcastClick(podcast.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PodcastListItem(podcast: PodcastEntity, onClick: () -> Unit) {
    Card(
        modifier =
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier =
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Podcast thumbnail
            Box(
                modifier =
                Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                if (podcast.imageUrl != null) {
                    AsyncImage(
                        model = podcast.imageUrl,
                        contentDescription = "Podcast artwork for ${podcast.name}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Text(
                        text = "🎧",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                }
            }

            // Podcast info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = podcast.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = podcast.artistName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                if (podcast.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = podcast.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun PodcastListScreenPreview() {
    MaterialTheme {
        PodcastListContent(
            uiState = PodcastListUiState(
                subscribedPodcasts = listOf(
                    PodcastEntity(
                        id = 1L,
                        name = "Sample Tech Podcast",
                        artistName = "Tech Creator",
                        description = "A great podcast about technology and programming.",
                        imageUrl = null,
                        feedUrl = "https://example.com/feed.xml",
                        subscribed = true,
                        subscribedAt = 0L,
                    ),
                    PodcastEntity(
                        id = 2L,
                        name = "Another Podcast with a Very Long Title That Should Wrap to the Next Line",
                        artistName = "Another Creator",
                        description = "Another great podcast about design and creativity.",
                        imageUrl = null,
                        feedUrl = "https://example.com/feed2.xml",
                        subscribed = true,
                        subscribedAt = 0L,
                    ),
                ),
                isLoading = false,
            ),
            onNavigateToSearch = {},
            onPodcastClick = {},
        )
    }
}

@Preview
@Composable
fun PodcastListScreenEmptyPreview() {
    MaterialTheme {
        PodcastListContent(
            uiState = PodcastListUiState(
                subscribedPodcasts = emptyList(),
                isLoading = false,
            ),
            onNavigateToSearch = {},
            onPodcastClick = {},
        )
    }
}
