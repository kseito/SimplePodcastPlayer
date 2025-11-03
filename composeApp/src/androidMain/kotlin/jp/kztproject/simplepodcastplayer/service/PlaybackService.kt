package jp.kztproject.simplepodcastplayer.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Binder
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.MediaStyleNotificationHelper
import jp.kztproject.simplepodcastplayer.activity.PlayerActivity

@Suppress("TooManyFunctions")
class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer
    private val binder = PlaybackBinder()

    inner class PlaybackBinder : Binder() {
        fun getService(): PlaybackService = this@PlaybackService
    }

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val NOTIFICATION_CHANNEL_ID = "podcast_playback_channel"
        private const val NOTIFICATION_CHANNEL_NAME = "Podcast Playback"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        initializePlayer()
        initializeMediaSession()
        setupPlayerListener()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    NOTIFICATION_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = "Controls for podcast playback"
                    setShowBadge(false)
                }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun setupPlayerListener() {
        player.addListener(
            object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_READY, Player.STATE_BUFFERING -> {
                            if (player.playWhenReady) {
                                startForegroundService()
                            }
                        }
                        Player.STATE_IDLE, Player.STATE_ENDED -> {
                            stopForeground(STOP_FOREGROUND_REMOVE)
                        }
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (isPlaying) {
                        startForegroundService()
                    } else {
                        stopForeground(STOP_FOREGROUND_DETACH)
                    }
                }
            },
        )
    }

    private fun startForegroundService() {
        val notification =
            NotificationCompat
                .Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle("Podcast Player")
                .setContentText("Playing...")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setOngoing(true)
                .setStyle(
                    MediaStyleNotificationHelper.MediaStyle(mediaSession!!)
                        .setShowActionsInCompactView(0, 1, 2),
                ).build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun initializePlayer() {
        player =
            ExoPlayer
                .Builder(this)
                .setAudioAttributes(
                    AudioAttributes
                        .Builder()
                        .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                        .setUsage(C.USAGE_MEDIA)
                        .build(),
                    true,
                ).setHandleAudioBecomingNoisy(true)
                .build()
    }

    private fun initializeMediaSession() {
        val sessionActivityIntent = Intent(this, PlayerActivity::class.java)
        val sessionActivityPendingIntent =
            PendingIntent.getActivity(
                this,
                0,
                sessionActivityIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        mediaSession =
            MediaSession
                .Builder(this, player)
                .setSessionActivity(sessionActivityPendingIntent)
                .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onBind(intent: Intent?) = super.onBind(intent) ?: binder

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    fun getPlayer(): Player = player

    fun loadMediaItem(url: String) {
        val mediaItem = MediaItem.fromUri(url)
        player.setMediaItem(mediaItem)
        player.prepare()
    }
}
