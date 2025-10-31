package jp.kztproject.simplepodcastplayer.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import jp.kztproject.simplepodcastplayer.data.Episode
import jp.kztproject.simplepodcastplayer.data.Podcast

@Composable
actual fun rememberPlayerViewModel(episode: Episode, podcast: Podcast): PlayerViewModel {
    val viewModel = remember(episode.id) {
        PlayerViewModelImpl().apply {
            loadEpisode(episode, podcast)
        }
    }
    return viewModel
}
