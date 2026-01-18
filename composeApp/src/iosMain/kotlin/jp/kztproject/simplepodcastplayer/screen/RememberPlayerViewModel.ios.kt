package jp.kztproject.simplepodcastplayer.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import jp.kztproject.simplepodcastplayer.data.Episode
import jp.kztproject.simplepodcastplayer.data.Podcast
import jp.kztproject.simplepodcastplayer.data.repository.IDownloadRepository
import jp.kztproject.simplepodcastplayer.data.repository.PlaybackRepository
import org.koin.compose.koinInject

@Composable
actual fun rememberPlayerViewModel(episode: Episode, podcast: Podcast): PlayerViewModel {
    val playbackRepository: PlaybackRepository = koinInject()
    val downloadRepository: IDownloadRepository = koinInject()

    val viewModel = remember(episode.id) {
        PlayerViewModelImpl(
            playbackRepository = playbackRepository,
            downloadRepository = downloadRepository,
        ).apply {
            loadEpisode(episode, podcast)
        }
    }
    return viewModel
}
