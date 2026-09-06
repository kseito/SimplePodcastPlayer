package jp.kztproject.simplepodcastplayer.fake

import jp.kztproject.simplepodcastplayer.download.DownloadState
import jp.kztproject.simplepodcastplayer.download.IAudioDownloader
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * In-memory stand-in for the platform downloader.
 * Tests drive it and let the real [jp.kztproject.simplepodcastplayer.data.repository.EpisodeAudioRepository]
 * keep the DB in sync, so the DB write-back is never duplicated here.
 */
class FakeAudioDownloader : IAudioDownloader {
    private val audioFiles = mutableMapOf<String, String>()
    private var shouldFailDownload = false
    private var downloadError: String = "Download failed"
    private var incompleteDownloads = 0
    private val legacyFileNames = mutableSetOf<String>()

    fun setDownloadedEpisode(episodeId: String, localPath: String) {
        audioFiles[episodeId] = localPath
    }

    fun setShouldFailDownload(shouldFail: Boolean, error: String = "Download failed") {
        shouldFailDownload = shouldFail
        downloadError = error
    }

    fun clearDownloads() {
        audioFiles.clear()
    }

    /** Stands in for an audio file still stored under the pre-hash file name. */
    fun setLegacyFileName(episodeId: String) {
        legacyFileNames.add(episodeId)
    }

    /** Stands in for temporary files left behind on disk by interrupted downloads. */
    fun setIncompleteDownloads(count: Int) {
        incompleteDownloads = count
    }

    override suspend fun downloadAudio(url: String, episodeId: String): Flow<DownloadState> = flow {
        emit(DownloadState.Idle)

        if (shouldFailDownload) {
            emit(DownloadState.Failed(downloadError))
            return@flow
        }

        emit(DownloadState.Downloading(0.5f))
        emit(DownloadState.Downloading(1.0f))

        audioFiles[episodeId] = "/fake/path/$episodeId.mp3"
        emit(DownloadState.Completed)
    }

    override fun getAudioFilePath(episodeId: String): String? = audioFiles[episodeId]

    override suspend fun deleteAudioFile(episodeId: String): Boolean = audioFiles.remove(episodeId) != null

    override fun isDownloaded(episodeId: String): Boolean = audioFiles.containsKey(episodeId)

    override suspend fun migrateLegacyFileName(episodeId: String): Boolean =
        legacyFileNames.remove(episodeId).also { if (it) audioFiles[episodeId] = "/fake/path/$episodeId.mp3" }

    override suspend fun deleteIncompleteDownloads(): Int = incompleteDownloads.also { incompleteDownloads = 0 }
}
