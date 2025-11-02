package jp.kztproject.simplepodcastplayer.player

import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.play
import platform.AVFoundation.pause
import platform.AVFoundation.seekToTime
import platform.AVFoundation.currentTime
import platform.AVFoundation.rate
import platform.AVFoundation.duration
import platform.Foundation.NSURL
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSNotification
import platform.Foundation.NSNumber
import platform.CoreMedia.CMTimeMake
import platform.CoreMedia.CMTimeGetSeconds
import platform.MediaPlayer.MPRemoteCommandCenter
import platform.MediaPlayer.MPRemoteCommandHandlerStatusSuccess
import platform.MediaPlayer.MPNowPlayingInfoCenter
import platform.MediaPlayer.MPMediaItemPropertyTitle
import platform.MediaPlayer.MPMediaItemPropertyArtist
import platform.MediaPlayer.MPMediaItemPropertyPlaybackDuration
import platform.MediaPlayer.MPNowPlayingInfoPropertyElapsedPlaybackTime
import platform.MediaPlayer.MPNowPlayingInfoPropertyPlaybackRate
import platform.MediaPlayer.MPChangePlaybackPositionCommandEvent

@OptIn(ExperimentalForeignApi::class)
actual class AudioPlayer {
    private var avPlayer: AVPlayer? = null
    private var currentPlayerItem: AVPlayerItem? = null

    // Now Playing Info
    private var currentEpisodeTitle: String = ""
    private var currentPodcastName: String = ""

    // Playback state tracking
    private var wasPlayingBeforeInterruption = false

    init {
        setupAudioSession()
        setupRemoteCommandCenter()
        setupInterruptionHandling()
    }

    actual fun play() {
        avPlayer?.play()
        updateNowPlayingInfoInternal()
    }

    actual fun pause() {
        avPlayer?.pause()
        updateNowPlayingInfoInternal()
    }

    actual fun seekTo(position: Long) {
        // Convert milliseconds to seconds with microsecond precision
        // position is in milliseconds, so multiply by 1000 to get microseconds
        val time = CMTimeMake(value = position * 1000, timescale = 1000000)
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
        val durationTime = currentPlayerItem?.duration ?: return 0L
        val seconds = CMTimeGetSeconds(durationTime)
        // Check if duration is valid (not NaN or infinite)
        if (seconds.isNaN() || seconds.isInfinite()) {
            return 0L
        }
        return (seconds * 1000).toLong()
    }

    actual fun isPlaying(): Boolean = (avPlayer?.rate ?: 0f) > 0f

    /**
     * Update Now Playing Info for lock screen and control center display
     */
    fun updateNowPlayingInfo(episodeTitle: String, podcastName: String) {
        currentEpisodeTitle = episodeTitle
        currentPodcastName = podcastName
        updateNowPlayingInfoInternal()
    }

    private fun setupAudioSession() {
        // AVAudioSession is configured in iOSApp.swift at app launch
        // No additional setup needed here
    }

    private fun setupRemoteCommandCenter() {
        val commandCenter = MPRemoteCommandCenter.sharedCommandCenter()

        // Play command
        commandCenter.playCommand.setEnabled(true)
        commandCenter.playCommand.addTargetWithHandler { _ ->
            play()
            updateNowPlayingInfoInternal()
            MPRemoteCommandHandlerStatusSuccess
        }

        // Pause command
        commandCenter.pauseCommand.setEnabled(true)
        commandCenter.pauseCommand.addTargetWithHandler { _ ->
            pause()
            updateNowPlayingInfoInternal()
            MPRemoteCommandHandlerStatusSuccess
        }

        // Skip forward command (15 seconds)
        commandCenter.skipForwardCommand.setEnabled(true)
        commandCenter.skipForwardCommand.preferredIntervals = listOf(15)
        commandCenter.skipForwardCommand.addTargetWithHandler { _ ->
            val newPosition = (getCurrentPosition() + 15000).coerceAtMost(getDuration())
            seekTo(newPosition)
            updateNowPlayingInfoInternal()
            MPRemoteCommandHandlerStatusSuccess
        }

        // Skip backward command (15 seconds)
        commandCenter.skipBackwardCommand.setEnabled(true)
        commandCenter.skipBackwardCommand.preferredIntervals = listOf(15)
        commandCenter.skipBackwardCommand.addTargetWithHandler { _ ->
            val newPosition = (getCurrentPosition() - 15000).coerceAtLeast(0)
            seekTo(newPosition)
            updateNowPlayingInfoInternal()
            MPRemoteCommandHandlerStatusSuccess
        }

        // Change playback position command
        commandCenter.changePlaybackPositionCommand.setEnabled(true)
        commandCenter.changePlaybackPositionCommand.addTargetWithHandler { event ->
            val positionEvent = event as? platform.MediaPlayer.MPChangePlaybackPositionCommandEvent
            positionEvent?.let {
                val positionInMillis = (it.positionTime * 1000).toLong()
                seekTo(positionInMillis)
                updateNowPlayingInfoInternal()
            }
            MPRemoteCommandHandlerStatusSuccess
        }
    }

    private fun setupInterruptionHandling() {
        // Add observer for audio interruptions
        NSNotificationCenter.defaultCenter.addObserverForName(
            name = "AVAudioSessionInterruptionNotification",
            `object` = null,
            queue = null,
        ) { notification: NSNotification? ->
            notification?.userInfo?.let { userInfo ->
                val interruptionType = (userInfo["AVAudioSessionInterruptionTypeKey"] as? NSNumber)?.unsignedLongValue

                when (interruptionType) {
                    1UL -> { // AVAudioSessionInterruptionTypeBegan = 1
                        // Audio interruption began (e.g., phone call)
                        wasPlayingBeforeInterruption = isPlaying()
                        if (wasPlayingBeforeInterruption) {
                            pause()
                        }
                    }

                    0UL -> { // AVAudioSessionInterruptionTypeEnded = 0
                        // Audio interruption ended
                        val options = (userInfo["AVAudioSessionInterruptionOptionKey"] as? NSNumber)?.unsignedLongValue
                        val shouldResume = options == 1UL // AVAudioSessionInterruptionOptionShouldResume = 1

                        // Resume playback if it was playing before and system suggests resuming
                        if (wasPlayingBeforeInterruption && shouldResume) {
                            play()
                        }
                    }
                }
            }
        }
    }

    private fun updateNowPlayingInfoInternal() {
        val nowPlayingInfoCenter = MPNowPlayingInfoCenter.defaultCenter()
        val duration = getDuration()
        val currentPosition = getCurrentPosition()
        val playbackRate = avPlayer?.rate ?: 0f

        val nowPlayingInfo = mutableMapOf<Any?, Any?>()
        nowPlayingInfo[MPMediaItemPropertyTitle] = currentEpisodeTitle
        nowPlayingInfo[MPMediaItemPropertyArtist] = currentPodcastName

        if (duration > 0) {
            nowPlayingInfo[MPMediaItemPropertyPlaybackDuration] = duration / 1000.0
        }

        nowPlayingInfo[MPNowPlayingInfoPropertyElapsedPlaybackTime] = currentPosition / 1000.0
        nowPlayingInfo[MPNowPlayingInfoPropertyPlaybackRate] = playbackRate

        nowPlayingInfoCenter.nowPlayingInfo = nowPlayingInfo
    }
}
