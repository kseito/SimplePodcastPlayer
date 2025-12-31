package jp.kztproject.simplepodcastplayer.data.repository

import jp.kztproject.simplepodcastplayer.download.DownloadState
import kotlinx.coroutines.flow.Flow

expect class DownloadRepository : IDownloadRepository {
    /**
     * Download episode audio file
     * @param episodeId Episode ID
     * @param audioUrl Remote audio URL
     * @return Flow of download state updates
     */
    override suspend fun downloadEpisode(episodeId: String, audioUrl: String): Flow<DownloadState>

    /**
     * Delete downloaded episode
     * @param episodeId Episode ID
     * @return true if deleted successfully
     */
    override suspend fun deleteDownload(episodeId: String): Boolean

    /**
     * Get local file path for episode
     * @param episodeId Episode ID
     * @return Local file path or null if not downloaded
     */
    override fun getLocalFilePath(episodeId: String): String?

    /**
     * Check if episode is downloaded
     * @param episodeId Episode ID
     * @return true if downloaded
     */
    override fun isDownloaded(episodeId: String): Boolean
}
