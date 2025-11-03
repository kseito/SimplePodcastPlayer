package jp.kztproject.simplepodcastplayer.download

sealed class DownloadState {
    data object Idle : DownloadState()

    data class Downloading(val progress: Float) : DownloadState()

    data object Completed : DownloadState()

    data class Failed(val error: String) : DownloadState()
}
