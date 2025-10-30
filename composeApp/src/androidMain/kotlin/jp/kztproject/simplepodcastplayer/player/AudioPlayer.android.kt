package jp.kztproject.simplepodcastplayer.player

import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

actual class AudioPlayer(
    private val player: ExoPlayer,
) {
    actual fun play() {
        player.play()
    }

    actual fun pause() {
        player.pause()
    }

    actual fun seekTo(position: Long) {
        player.seekTo(position)
    }

    actual fun setPlaybackSpeed(speed: Float) {
        player.playbackParameters = PlaybackParameters(speed)
    }

    actual fun loadUrl(url: String) {
        val mediaItem = MediaItem.fromUri(url)
        player.setMediaItem(mediaItem)
        player.prepare()
    }

    actual fun release() {
        player.release()
    }

    actual fun getCurrentPosition(): Long {
        return player.currentPosition
    }

    actual fun getDuration(): Long {
        return if (player.duration == androidx.media3.common.C.TIME_UNSET) 0L else player.duration
    }

    actual fun isPlaying(): Boolean {
        return player.isPlaying
    }
}
