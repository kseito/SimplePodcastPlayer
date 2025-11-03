package jp.kztproject.simplepodcastplayer.player

expect class AudioPlayer {
    fun play()

    fun pause()

    fun seekTo(position: Long)

    fun setPlaybackSpeed(speed: Float)

    fun loadUrl(url: String)

    fun release()

    fun getCurrentPosition(): Long

    fun getDuration(): Long

    fun isPlaying(): Boolean
}
