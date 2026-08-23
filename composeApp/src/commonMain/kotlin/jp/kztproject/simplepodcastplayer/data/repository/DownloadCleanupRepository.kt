package jp.kztproject.simplepodcastplayer.data.repository

import io.github.aakira.napier.Napier
import jp.kztproject.simplepodcastplayer.data.database.dao.EpisodeDao

interface IDownloadCleanupRepository {
    suspend fun countDownloadsByPodcast(podcastId: String): Int
    suspend fun deleteDownloadsByPodcast(podcastId: String): Int
    suspend fun countListenedDownloads(): Int
    suspend fun deleteListenedDownloads(): Int
}

/**
 * Bulk deletion of downloaded audio files.
 * Lives in commonMain so the cleanup rules stay platform-independent;
 * the actual file removal is delegated to the expect/actual [IDownloadRepository].
 */
class DownloadCleanupRepository(
    private val episodeDao: EpisodeDao,
    private val downloadRepository: IDownloadRepository,
) : IDownloadCleanupRepository {

    override suspend fun countDownloadsByPodcast(podcastId: String): Int =
        episodeDao.getDownloadedEpisodesByPodcastId(podcastId).size

    override suspend fun deleteDownloadsByPodcast(podcastId: String): Int =
        deleteAll(episodeDao.getDownloadedEpisodesByPodcastId(podcastId).map { it.id })

    override suspend fun countListenedDownloads(): Int = episodeDao.getListenedDownloadedEpisodes().size

    override suspend fun deleteListenedDownloads(): Int =
        deleteAll(episodeDao.getListenedDownloadedEpisodes().map { it.id })

    /**
     * Deletes each download independently so one failure does not abort the rest.
     * @return the number of downloads actually deleted
     */
    private suspend fun deleteAll(episodeIds: List<String>): Int = episodeIds.count { episodeId ->
        runCatching { downloadRepository.deleteDownload(episodeId) }
            .onFailure { Napier.e("Failed to delete download: $episodeId", it) }
            .getOrDefault(false)
    }
}
