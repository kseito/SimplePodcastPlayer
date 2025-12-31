package jp.kztproject.simplepodcastplayer.data.repository

actual object DownloadRepositoryBuilder {
    actual fun build(): IDownloadRepository = DownloadRepository()
}
