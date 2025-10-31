package jp.kztproject.simplepodcastplayer.data.repository

import android.content.Context
import jp.kztproject.simplepodcastplayer.data.database.DatabaseBuilder
import jp.kztproject.simplepodcastplayer.download.AudioDownloader
import jp.kztproject.simplepodcastplayer.download.DownloadState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach

actual class DownloadRepository(context: Context) {
    private val audioDownloader = AudioDownloader(context)
    private val database = DatabaseBuilder.build()
    private val episodeDao = database.episodeDao()

    actual suspend fun downloadEpisode(episodeId: String, audioUrl: String): Flow<DownloadState> =
        audioDownloader.downloadAudio(audioUrl, episodeId).onEach { state ->
            if (state is DownloadState.Completed) {
                val localFilePath = audioDownloader.getLocalFilePath(episodeId)
                episodeDao.updateDownloadStatus(
                    episodeId = episodeId,
                    isDownloaded = true,
                    localFilePath = localFilePath,
                    downloadedAt = System.currentTimeMillis(),
                )
            }
        }

    actual suspend fun deleteDownload(episodeId: String): Boolean {
        val deleted = audioDownloader.deleteDownload(episodeId)
        if (deleted) {
            episodeDao.updateDownloadStatus(
                episodeId = episodeId,
                isDownloaded = false,
                localFilePath = null,
                downloadedAt = 0L,
            )
        }
        return deleted
    }

    actual fun getLocalFilePath(episodeId: String): String? = audioDownloader.getLocalFilePath(episodeId)

    actual fun isDownloaded(episodeId: String): Boolean = audioDownloader.isDownloaded(episodeId)
}
