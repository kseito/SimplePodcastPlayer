package jp.kztproject.simplepodcastplayer.screen

import androidx.compose.runtime.Composable
import jp.kztproject.simplepodcastplayer.data.Episode
import jp.kztproject.simplepodcastplayer.data.Podcast

@Composable
expect fun rememberPlayerViewModel(episode: Episode, podcast: Podcast): PlayerViewModel
