package jp.kztproject.simplepodcastplayer.fake

import jp.kztproject.simplepodcastplayer.data.database.dao.EpisodeDao
import jp.kztproject.simplepodcastplayer.data.repository.IDownloadRepository
import jp.kztproject.simplepodcastplayer.download.DownloadState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

private const val DOWNLOADED_AT = 1703001600000L // 2024-12-15 00:00:00 UTC

/**
 * @param episodeDao when provided, download status is written back to it the way the real
 * DownloadRepository does. Pass it whenever a test asserts on the DB state.
 */
class FakeDownloadRepository(private val episodeDao: EpisodeDao? = null) : IDownloadRepository {
    private val downloadedEpisodes = mutableMapOf<String, String>()
    private var shouldFailDownload = false
    private var downloadError: String = "Download failed"

    fun setDownloadedEpisode(episodeId: String, localPath: String) {
        downloadedEpisodes[episodeId] = localPath
    }

    fun setShouldFailDownload(shouldFail: Boolean, error: String = "Download failed") {
        shouldFailDownload = shouldFail
        downloadError = error
    }

    fun clearDownloads() {
        downloadedEpisodes.clear()
    }

    override suspend fun downloadEpisode(episodeId: String, audioUrl: String): Flow<DownloadState> = flow {
        emit(DownloadState.Idle)

        if (shouldFailDownload) {
            emit(DownloadState.Failed(downloadError))
            return@flow
        }

        emit(DownloadState.Downloading(0.5f))
        emit(DownloadState.Downloading(1.0f))

        val localPath = "/fake/path/$episodeId.mp3"
        downloadedEpisodes[episodeId] = localPath
        episodeDao?.updateDownloadStatus(
            episodeId = episodeId,
            isDownloaded = true,
            localFilePath = localPath,
            downloadedAt = DOWNLOADED_AT,
        )
        emit(DownloadState.Completed)
    }

    override suspend fun deleteDownload(episodeId: String): Boolean {
        val deleted = downloadedEpisodes.remove(episodeId) != null
        if (deleted) {
            episodeDao?.updateDownloadStatus(
                episodeId = episodeId,
                isDownloaded = false,
                localFilePath = null,
                downloadedAt = 0L,
            )
        }
        return deleted
    }

    override fun getLocalFilePath(episodeId: String): String? = downloadedEpisodes[episodeId]

    override fun isDownloaded(episodeId: String): Boolean = downloadedEpisodes.containsKey(episodeId)
}
