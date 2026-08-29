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

    override suspend fun deleteAudioFile(episodeId: String): Boolean {
        val deleted = audioDownloader.deleteAudioFile(episodeId)
        if (deleted) {
            episodeDao.updateDownloadStatus(
                episodeId = episodeId,
                isDownloaded = false,
                localFilePath = null,
                downloadedAt = 0L,
            )
        }
        return deleted
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
