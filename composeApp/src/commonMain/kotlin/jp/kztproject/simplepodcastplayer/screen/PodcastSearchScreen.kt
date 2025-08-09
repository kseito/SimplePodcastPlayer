package jp.kztproject.simplepodcastplayer.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun PodcastSearchScreen(
    onNavigateToList: () -> Unit,
    viewModel: PodcastSearchViewModel = viewModel { PodcastSearchViewModel() }
) {
    val text by viewModel.text.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(16.dp),
    ) {
        Text("Search Podcast")
        Text(text)
        Button(onClick = onNavigateToList) {
            Text("Go to List")
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
