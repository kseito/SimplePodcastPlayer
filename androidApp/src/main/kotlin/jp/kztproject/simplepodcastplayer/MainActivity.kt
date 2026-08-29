package jp.kztproject.simplepodcastplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import jp.kztproject.simplepodcastplayer.data.database.DatabaseBuilder
import jp.kztproject.simplepodcastplayer.data.repository.EpisodeAudioRepositoryBuilder

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Initialize database
        DatabaseBuilder.init(this)
        EpisodeAudioRepositoryBuilder.init(this)

        setContent {
            App()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
