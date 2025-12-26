package jp.kztproject.simplepodcastplayer.fake

import jp.kztproject.simplepodcastplayer.download.DownloadState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class FakeDownloadRepository {
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

    suspend fun downloadEpisode(episodeId: String, audioUrl: String): Flow<DownloadState> = flow {
        emit(DownloadState.Idle)

        if (shouldFailDownload) {
            emit(DownloadState.Failed(downloadError))
            return@flow
        }

        emit(DownloadState.Downloading(0.5f))
        emit(DownloadState.Downloading(1.0f))

        val localPath = "/fake/path/$episodeId.mp3"
        downloadedEpisodes[episodeId] = localPath
        emit(DownloadState.Completed)
    }

    suspend fun deleteDownload(episodeId: String): Boolean {
        return downloadedEpisodes.remove(episodeId) != null
    }

    fun getLocalFilePath(episodeId: String): String? {
        return downloadedEpisodes[episodeId]
    }

    fun isDownloaded(episodeId: String): Boolean {
        return downloadedEpisodes.containsKey(episodeId)
    }
}
