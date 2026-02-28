package jp.kztproject.simplepodcastplayer.screen

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.media3.exoplayer.ExoPlayer
import jp.kztproject.simplepodcastplayer.data.Episode
import jp.kztproject.simplepodcastplayer.data.Podcast
import jp.kztproject.simplepodcastplayer.data.repository.IDownloadRepository
import jp.kztproject.simplepodcastplayer.data.repository.PlaybackRepository
import jp.kztproject.simplepodcastplayer.service.PlaybackService
import org.koin.compose.koinInject

@Composable
actual fun rememberPlayerViewModel(episode: Episode, podcast: Podcast): PlayerViewModel {
    val context = LocalContext.current
    val playbackRepository = koinInject<PlaybackRepository>()
    val downloadRepository = koinInject<IDownloadRepository>()
    var viewModel by remember { mutableStateOf<PlayerViewModel?>(null) }
    var playbackService by remember { mutableStateOf<PlaybackService?>(null) }

    DisposableEffect(Unit) {
        val serviceConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                val binder = service as? PlaybackService.PlaybackBinder
                playbackService = binder?.getService()

                binder?.getService()?.let { svc ->
                    val exoPlayer = svc.getPlayer() as ExoPlayer
                    val vm = PlayerViewModelImpl(exoPlayer, playbackRepository, downloadRepository)
                    vm.loadEpisode(episode, podcast)
                    viewModel = vm
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                playbackService = null
            }
        }

        val serviceIntent = Intent(context, PlaybackService::class.java)
        context.startService(serviceIntent)
        context.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)

        onDispose {
            context.unbindService(serviceConnection)
        }
    }

    // Return a temporary ViewModel until the service is bound
    return viewModel ?: remember {
        object : PlayerViewModel {
            override val uiState = kotlinx.coroutines.flow.MutableStateFlow(PlayerUiState())
            override fun play() {}
            override fun pause() {}
            override fun seekTo(position: Long) {}
            override fun skipForward(seconds: Int) {}
            override fun skipBackward(seconds: Int) {}
            override fun setPlaybackSpeed(speed: Float) {}
            override fun loadEpisode(episode: Episode, podcast: Podcast) {}
            override fun release() {}
        }
    }
}
