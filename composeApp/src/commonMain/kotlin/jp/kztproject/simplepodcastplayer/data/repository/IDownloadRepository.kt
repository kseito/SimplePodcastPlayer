package jp.kztproject.simplepodcastplayer.data.repository

import jp.kztproject.simplepodcastplayer.download.DownloadState
import kotlinx.coroutines.flow.Flow

interface IDownloadRepository {
    suspend fun downloadEpisode(episodeId: String, audioUrl: String): Flow<DownloadState>
    suspend fun deleteDownload(episodeId: String): Boolean
    fun getLocalFilePath(episodeId: String): String?
    fun isDownloaded(episodeId: String): Boolean
}
