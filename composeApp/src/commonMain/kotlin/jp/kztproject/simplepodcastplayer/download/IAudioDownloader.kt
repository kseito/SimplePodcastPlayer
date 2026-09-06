package jp.kztproject.simplepodcastplayer.download

import kotlinx.coroutines.flow.Flow

/** Raised when the downloaded bytes cannot be written to, or published on, local storage. */
class DownloadDataCreationException(message: String) : IllegalStateException(message)

/**
 * Performs the actual HTTP download and file operations for episode audio.
 * Implemented per platform; [jp.kztproject.simplepodcastplayer.data.repository.EpisodeAudioRepository]
 * builds the DB-aware behaviour on top of it.
 */
interface IAudioDownloader {
    /**
     * Download audio file from URL to local storage
     * @param url Remote audio URL
     * @param episodeId Episode ID for file naming
     * @return Flow of download state updates
     */
    suspend fun downloadAudio(url: String, episodeId: String): Flow<DownloadState>

    /**
     * Get local audio file path for episode
     * @param episodeId Episode ID
     * @return Local file path or null if not downloaded
     */
    fun getAudioFilePath(episodeId: String): String?

    /**
     * Delete downloaded audio file
     * @param episodeId Episode ID
     * @return true if deleted successfully
     */
    suspend fun deleteAudioFile(episodeId: String): Boolean

    /**
     * Check if the audio file of the episode exists locally
     * @param episodeId Episode ID
     * @return true if downloaded
     */
    fun isDownloaded(episodeId: String): Boolean

    /**
     * Delete the temporary files left behind by downloads that never completed.
     * A download only publishes its final file name once the whole body has been written,
     * so anything still carrying the temporary suffix is known to be unusable.
     * @return the number of files deleted
     */
    suspend fun deleteIncompleteDownloads(): Int
}
