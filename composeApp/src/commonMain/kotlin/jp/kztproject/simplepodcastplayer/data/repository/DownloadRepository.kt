package jp.kztproject.simplepodcastplayer.data.repository

import jp.kztproject.simplepodcastplayer.download.DownloadState
import kotlinx.coroutines.flow.Flow

expect class DownloadRepository {
    /**
     * Download episode audio file
     * @param episodeId Episode ID
     * @param audioUrl Remote audio URL
     * @return Flow of download state updates
     */
    suspend fun downloadEpisode(episodeId: String, audioUrl: String): Flow<DownloadState>

    /**
     * Delete downloaded episode
     * @param episodeId Episode ID
     * @return true if deleted successfully
     */
    suspend fun deleteDownload(episodeId: String): Boolean

    /**
     * Get local file path for episode
     * @param episodeId Episode ID
     * @return Local file path or null if not downloaded
     */
    fun getLocalFilePath(episodeId: String): String?

    /**
     * Check if episode is downloaded
     * @param episodeId Episode ID
     * @return true if downloaded
     */
    fun isDownloaded(episodeId: String): Boolean
}
