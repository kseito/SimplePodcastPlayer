package jp.kztproject.simplepodcastplayer.activity

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.exoplayer.ExoPlayer
import jp.kztproject.simplepodcastplayer.data.Episode
import jp.kztproject.simplepodcastplayer.data.Podcast
import jp.kztproject.simplepodcastplayer.screen.PlayerScreen
import jp.kztproject.simplepodcastplayer.screen.PlayerViewModelImpl
import jp.kztproject.simplepodcastplayer.service.PlaybackService
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class PlayerActivity : ComponentActivity() {
    private var playbackService: PlaybackService? = null
    private var viewModel by mutableStateOf<PlayerViewModelImpl?>(null)
    private var isBound = false

    private val serviceConnection =
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                val binder = service as? PlaybackService.PlaybackBinder
                playbackService = binder?.getService()
                isBound = true

                binder?.getService()?.let { setupViewModel(it.getPlayer() as ExoPlayer, this@PlayerActivity) }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                playbackService = null
                isBound = false
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Set up UI immediately
        setContent {
            viewModel?.let { vm ->
                PlayerScreen(
                    viewModel = vm,
                    onNavigateBack = { finish() },
                )
            }
        }

        // Start and bind to PlaybackService
        val serviceIntent = Intent(this, PlaybackService::class.java)
        startService(serviceIntent)
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun setupViewModel(exoPlayer: ExoPlayer, context: Context) {
        // Get Episode and Podcast from intent
        val episodeJson = intent.getStringExtra(EXTRA_EPISODE)
        val podcastJson = intent.getStringExtra(EXTRA_PODCAST)

        if (episodeJson != null && podcastJson != null) {
            val episode = Json.decodeFromString<Episode>(episodeJson)
            val podcast = Json.decodeFromString<Podcast>(podcastJson)

            viewModel = PlayerViewModelImpl(exoPlayer, context)
            viewModel?.loadEpisode(episode, podcast)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
    }

    companion object {
        private const val EXTRA_EPISODE = "extra_episode"
        private const val EXTRA_PODCAST = "extra_podcast"

        fun createIntent(context: Context, episode: Episode, podcast: Podcast): Intent =
            Intent(context, PlayerActivity::class.java).apply {
                putExtra(EXTRA_EPISODE, Json.encodeToString(episode))
                putExtra(EXTRA_PODCAST, Json.encodeToString(podcast))
            }
    }
}
