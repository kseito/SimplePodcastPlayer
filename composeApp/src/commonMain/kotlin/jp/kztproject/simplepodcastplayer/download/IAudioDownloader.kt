package jp.kztproject.simplepodcastplayer.download

import kotlinx.coroutines.flow.Flow
import okio.ByteString.Companion.encodeUtf8

/**
 * The name of the file that holds the audio of an episode.
 *
 * Episode IDs come from RSS `<guid>` values — URLs, UUIDs, free text — and fall back to the
 * episode title when a feed carries no guid. Hashing turns any of those into a valid, bounded
 * file name that stays distinct per episode. That distinctness is what makes the naming safe:
 * the presence of this file is what marks an episode as downloaded, so two episodes sharing a
 * name would make one of them look downloaded merely because the other one is.
 */
fun audioFileNameOf(episodeId: String): String = "${episodeId.encodeUtf8().sha256().hex()}.mp3"

/**
 * The naming scheme used before hashing, which replaced every non-alphanumeric character with
 * "_" and collapsed unrelated episodes onto the same name. Kept only to find the files it left
 * behind, so they can be renamed instead of being orphaned.
 */
// TODO: Remove once existing installs have migrated their downloads to the hashed file names.
fun legacyAudioFileNameOf(episodeId: String): String = "${episodeId.replace("[^a-zA-Z0-9]".toRegex(), "_")}.mp3"

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
     * The names of every audio file held locally, read in a single pass over the download
     * directory. Answers "which of these episodes are downloaded?" without asking per episode.
     * Files of downloads still in flight are not included.
     */
    suspend fun downloadedFileNames(): Set<String>

    /**
     * Rename the audio file of an episode from [legacyAudioFileNameOf] to [audioFileNameOf].
     * Does nothing when there is no legacy file, or when the episode already has a current one.
     * @return true if a file was renamed
     */
    // TODO: Remove once existing installs have migrated their downloads to the hashed file names.
    suspend fun migrateLegacyFileName(episodeId: String): Boolean

    /**
     * Delete the temporary files left behind by downloads that never completed.
     * A download only publishes its final file name once the whole body has been written,
     * so anything still carrying the temporary suffix is known to be unusable.
     * @return the number of files deleted
     */
    suspend fun deleteIncompleteDownloads(): Int
}
