package jp.kztproject.simplepodcastplayer.data.repository

import io.github.aakira.napier.Napier
import jp.kztproject.simplepodcastplayer.data.database.dao.EpisodeDao
import jp.kztproject.simplepodcastplayer.download.DownloadState
import jp.kztproject.simplepodcastplayer.download.IAudioDownloader
import jp.kztproject.simplepodcastplayer.download.audioFileNameOf
import kotlinx.coroutines.flow.Flow

/**
 * Owns the audio files that episodes are downloaded to.
 *
 * The presence of a file is the only record that an episode is downloaded: nothing in the
 * database mirrors it, so the two cannot drift apart. The episode table is consulted only to
 * learn which episodes exist and which of them have been listened to.
 * Platform differences are confined to [IAudioDownloader], so these rules stay in commonMain.
 */
class EpisodeAudioRepository(private val audioDownloader: IAudioDownloader, private val episodeDao: EpisodeDao) :
    IEpisodeAudioRepository {

    override suspend fun downloadEpisode(episodeId: String, audioUrl: String): Flow<DownloadState> =
        audioDownloader.downloadAudio(audioUrl, episodeId)

    /**
     * @return true if the episode held an audio file that is now gone, false if the deletion
     * failed or there was nothing to delete
     */
    override suspend fun deleteAudioFile(episodeId: String): Boolean = audioDownloader.deleteAudioFile(episodeId)

    override fun getAudioFilePath(episodeId: String): String? = audioDownloader.getAudioFilePath(episodeId)

    override fun isDownloaded(episodeId: String): Boolean = audioDownloader.isDownloaded(episodeId)

    override suspend fun countAudioFilesByPodcast(podcastId: String): Int =
        downloadedAmong(episodeDao.getEpisodeIdsByPodcastId(podcastId)).size

    override suspend fun deleteAudioFilesByPodcast(podcastId: String): Int =
        deleteAll(downloadedAmong(episodeDao.getEpisodeIdsByPodcastId(podcastId)))

    override suspend fun countListenedAudioFiles(): Int = downloadedAmong(episodeDao.getListenedEpisodeIds()).size

    override suspend fun deleteListenedAudioFiles(): Int =
        deleteAll(downloadedAmong(episodeDao.getListenedEpisodeIds()))

    override suspend fun migrateLegacyAudioFileNames(): Int = episodeDao.getAllEpisodeIds().count { episodeId ->
        runCatching { audioDownloader.migrateLegacyFileName(episodeId) }
            .onFailure { Napier.e("Failed to migrate audio file name: $episodeId", it) }
            .getOrDefault(false)
    }

    override suspend fun deleteIncompleteDownloads(): Int = audioDownloader.deleteIncompleteDownloads()

    /**
     * Narrows episode IDs down to the ones that actually hold an audio file, reading the
     * download directory once rather than asking about each episode in turn.
     */
    private suspend fun downloadedAmong(episodeIds: List<String>): List<String> {
        val downloadedFiles = audioDownloader.downloadedFileNames()
        return episodeIds.filter { audioFileNameOf(it) in downloadedFiles }
    }

    /**
     * Deletes each audio file independently so one failure does not abort the rest.
     * @return the number of audio files actually deleted
     */
    private suspend fun deleteAll(episodeIds: List<String>): Int = episodeIds.count { episodeId ->
        runCatching { deleteAudioFile(episodeId) }
            .onFailure { Napier.e("Failed to delete audio file: $episodeId", it) }
            .getOrDefault(false)
    }
}
