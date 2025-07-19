package jp.kztproject.simplepodcastplayer.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun PodcastSearchScreen(onNavigateToList: () -> Unit) {
    Column {
        Text("Search Podcast")
        Button(onClick = onNavigateToList) {
            Text("Go to List")
        }
    }
}
