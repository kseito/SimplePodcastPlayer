package jp.kztproject.simplepodcastplayer.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun PodcastListScreen(onNavigateToSearch: () -> Unit) {
    Column {
        Text("Podcast List")
        Button(onClick = onNavigateToSearch) {
            Text("Go to Search")
        }
    }
}
