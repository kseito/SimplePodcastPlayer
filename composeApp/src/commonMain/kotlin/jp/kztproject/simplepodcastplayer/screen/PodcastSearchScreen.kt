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
import androidx.compose.material3.OutlinedTextField
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import jp.kztproject.simplepodcastplayer.data.Podcast
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun PodcastSearchScreen(
    onNavigateToList: () -> Unit,
    viewModel: PodcastSearchViewModel = viewModel { PodcastSearchViewModel() },
) {
    val podcasts by viewModel.podcasts.collectAsStateWithLifecycle()

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
            TextButton(onClick = onNavigateToList) {
                Text("← Back")
            }
            Text(
                text = "Podcast Search",
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(modifier = Modifier.width(48.dp)) // Balance the layout
        }

        Spacer(modifier = Modifier.height(16.dp))

        var searchText by remember { mutableStateOf("") }

        // Search input field
        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            label = { Text("Search podcasts...") },
            placeholder = { Text("Enter podcast name or topic") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Search button
        Button(
            onClick = {
                viewModel.updateSearchQuery(searchText)
                viewModel.searchPodcasts()
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Search")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Podcast results
        if (podcasts.isNotEmpty()) {
            LazyColumn {
                items(podcasts) { podcast ->
                    PodcastItem(
                        podcast = podcast,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
            }
        } else {
            Text(
                text = "No podcasts found. Try searching for a topic or podcast name.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun PodcastItem(podcast: Podcast, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PodcastImage(podcast)

            Spacer(modifier = Modifier.width(12.dp))

            // Podcast info
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = podcast.trackName,
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

                podcast.primaryGenreName?.let { genre ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = genre,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun PodcastImage(podcast: Podcast) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = podcast.artworkUrl100 ?: podcast.artworkUrl60 ?: podcast.artworkUrl30,
            contentDescription = "Podcast artwork for ${podcast.trackName}",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        if (podcast.artworkUrl100 == null && podcast.artworkUrl60 == null && podcast.artworkUrl30 == null) {
            Text(
                text = "🎧",
                style = MaterialTheme.typography.headlineLarge,
            )
        }
    }
}

@Preview
@Composable
fun PodcastSearchScreenPreview() {
    MaterialTheme {
        PodcastSearchScreen(onNavigateToList = {})
    }
}
