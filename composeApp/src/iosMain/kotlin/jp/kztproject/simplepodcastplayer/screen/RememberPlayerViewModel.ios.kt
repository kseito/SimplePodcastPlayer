package jp.kztproject.simplepodcastplayer.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import jp.kztproject.simplepodcastplayer.data.Episode
import jp.kztproject.simplepodcastplayer.data.Podcast
import jp.kztproject.simplepodcastplayer.data.repository.IEpisodeAudioRepository
import jp.kztproject.simplepodcastplayer.data.repository.IPlaybackRepository
import org.koin.compose.koinInject

@Composable
actual fun rememberPlayerViewModel(episode: Episode, podcast: Podcast): PlayerViewModel {
    val playbackRepository: IPlaybackRepository = koinInject()
    val episodeAudioRepository: IEpisodeAudioRepository = koinInject()

    val viewModel = remember(episode.id) {
        PlayerViewModelImpl(
            playbackRepository = playbackRepository,
            episodeAudioRepository = episodeAudioRepository,
        ).apply {
            loadEpisode(episode, podcast)
        }
    }
    return viewModel
}
