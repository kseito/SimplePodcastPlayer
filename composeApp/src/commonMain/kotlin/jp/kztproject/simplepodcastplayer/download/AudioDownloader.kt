package jp.kztproject.simplepodcastplayer.download

import kotlinx.coroutines.flow.Flow

expect class AudioDownloader {
    /**
     * Download audio file from URL to local storage
     * @param url Remote audio URL
     * @param episodeId Episode ID for file naming
     * @return Flow of download state updates
     */
    suspend fun downloadAudio(url: String, episodeId: String): Flow<DownloadState>

    /**
     * Get local file path for episode
     * @param episodeId Episode ID
     * @return Local file path or null if not downloaded
     */
    fun getLocalFilePath(episodeId: String): String?

    /**
     * Delete downloaded audio file
     * @param episodeId Episode ID
     * @return true if deleted successfully
     */
    suspend fun deleteDownload(episodeId: String): Boolean

    /**
     * Check if episode is downloaded
     * @param episodeId Episode ID
     * @return true if downloaded
     */
    fun isDownloaded(episodeId: String): Boolean
}
