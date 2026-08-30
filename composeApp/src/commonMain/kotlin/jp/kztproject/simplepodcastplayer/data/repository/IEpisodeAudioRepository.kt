package jp.kztproject.simplepodcastplayer.data.repository

import jp.kztproject.simplepodcastplayer.download.DownloadState
import kotlinx.coroutines.flow.Flow

/**
 * Owns the audio files that episodes are downloaded to: acquiring them, locating them,
 * and removing them one by one or in bulk.
 */
interface IEpisodeAudioRepository {
    /**
     * Download the audio file of an episode
     * @param episodeId Episode ID
     * @param audioUrl Remote audio URL
     * @return Flow of download state updates
     */
    suspend fun downloadEpisode(episodeId: String, audioUrl: String): Flow<DownloadState>

    /**
     * Delete the audio file of an episode
     * @param episodeId Episode ID
     * @return true if deleted successfully
     */
    suspend fun deleteAudioFile(episodeId: String): Boolean

    /**
     * Get the local audio file path of an episode
     * @param episodeId Episode ID
     * @return Local file path or null if not downloaded
     */
    fun getAudioFilePath(episodeId: String): String?

    /**
     * Check whether the audio file of an episode exists locally
     * @param episodeId Episode ID
     * @return true if downloaded
     */
    fun isDownloaded(episodeId: String): Boolean

    /** Count the audio files held by a podcast, without deleting anything. */
    suspend fun countAudioFilesByPodcast(podcastId: String): Int

    /**
     * Delete every audio file of a podcast
     * @return the number of audio files actually deleted
     */
    suspend fun deleteAudioFilesByPodcast(podcastId: String): Int

    /** Count the audio files of listened episodes across podcasts, without deleting anything. */
    suspend fun countListenedAudioFiles(): Int

    /**
     * Delete the audio files of listened episodes across podcasts
     * @return the number of audio files actually deleted
     */
    suspend fun deleteListenedAudioFiles(): Int
}
