package jp.kztproject.simplepodcastplayer.player

import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.play
import platform.AVFoundation.pause
import platform.AVFoundation.seekToTime
import platform.AVFoundation.currentTime
import platform.AVFoundation.rate
import platform.Foundation.NSURL
import platform.CoreMedia.CMTimeMake
import platform.CoreMedia.CMTimeGetSeconds

@OptIn(ExperimentalForeignApi::class)
actual class AudioPlayer {
    private var avPlayer: AVPlayer? = null
    private var currentPlayerItem: AVPlayerItem? = null

    actual fun play() {
        avPlayer?.play()
    }

    actual fun pause() {
        avPlayer?.pause()
    }

    actual fun seekTo(position: Long) {
        val time = CMTimeMake(value = position, timescale = 1000)
        avPlayer?.seekToTime(time)
    }

    actual fun setPlaybackSpeed(speed: Float) {
        avPlayer?.rate = speed
    }

    actual fun loadUrl(url: String) {
        val nsUrl = NSURL.URLWithString(url) ?: return
        currentPlayerItem = AVPlayerItem(uRL = nsUrl)
        avPlayer = AVPlayer(playerItem = currentPlayerItem)
    }

    actual fun release() {
        avPlayer?.pause()
        avPlayer = null
        currentPlayerItem = null
    }

    actual fun getCurrentPosition(): Long {
        val time = avPlayer?.currentTime() ?: return 0L
        return (CMTimeGetSeconds(time) * 1000).toLong()
    }

    actual fun getDuration(): Long {
        // TODO: Implement proper duration retrieval
        // AVPlayerItem duration is not directly accessible in Kotlin/Native
        return 0L
    }

    actual fun isPlaying(): Boolean = (avPlayer?.rate ?: 0f) > 0f
}
