package jp.kztproject.simplepodcastplayer.data.repository

import io.github.aakira.napier.Napier
import jp.kztproject.simplepodcastplayer.data.database.dao.EpisodeDao
import jp.kztproject.simplepodcastplayer.download.DownloadState
import jp.kztproject.simplepodcastplayer.download.IAudioDownloader
import kotlin.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach

/**
 * Keeps the audio files on disk and the download columns of the episode table in sync.
 * Platform differences are confined to [IAudioDownloader], so these rules stay in commonMain.
 */
class EpisodeAudioRepository(private val audioDownloader: IAudioDownloader, private val episodeDao: EpisodeDao) :
    IEpisodeAudioRepository {

    override suspend fun downloadEpisode(episodeId: String, audioUrl: String): Flow<DownloadState> =
        audioDownloader.downloadAudio(audioUrl, episodeId).onEach { state ->
            if (state is DownloadState.Completed) {
                episodeDao.updateDownloadStatus(
                    episodeId = episodeId,
                    isDownloaded = true,
                    localFilePath = audioDownloader.getAudioFilePath(episodeId),
                    downloadedAt = Clock.System.now().toEpochMilliseconds(),
                )
            }
        }

    /**
     * @return true if the episode held an audio file that is now gone, false if the deletion
     * failed or there was nothing to delete
     */
    override suspend fun deleteAudioFile(episodeId: String): Boolean {
        val deleted = audioDownloader.deleteAudioFile(episodeId)
        if (!deleted && audioDownloader.isDownloaded(episodeId)) {
            // The file is still on disk, so the deletion genuinely failed. Leave the DB alone.
            return false
        }

        // The file is gone: either this call removed it, or it had already disappeared while the
        // DB still said "downloaded". Clear the columns either way, otherwise a stale row keeps
        // being counted by the cleanup flows and can never be cleaned up.
        val hadStaleDownloadState = !deleted && episodeDao.getById(episodeId)?.isDownloaded == true
        episodeDao.updateDownloadStatus(
            episodeId = episodeId,
            isDownloaded = false,
            localFilePath = null,
            downloadedAt = 0L,
        )
        return deleted || hadStaleDownloadState
    }

    override fun getAudioFilePath(episodeId: String): String? = audioDownloader.getAudioFilePath(episodeId)

    override fun isDownloaded(episodeId: String): Boolean = audioDownloader.isDownloaded(episodeId)

    override suspend fun countAudioFilesByPodcast(podcastId: String): Int =
        episodeDao.getDownloadedEpisodesByPodcastId(podcastId).size

    override suspend fun deleteAudioFilesByPodcast(podcastId: String): Int =
        deleteAll(episodeDao.getDownloadedEpisodesByPodcastId(podcastId).map { it.id })

    override suspend fun countListenedAudioFiles(): Int = episodeDao.getListenedDownloadedEpisodes().size

    override suspend fun deleteListenedAudioFiles(): Int =
        deleteAll(episodeDao.getListenedDownloadedEpisodes().map { it.id })

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
