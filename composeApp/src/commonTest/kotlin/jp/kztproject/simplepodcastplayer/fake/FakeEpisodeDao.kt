package jp.kztproject.simplepodcastplayer.fake

import jp.kztproject.simplepodcastplayer.data.database.dao.EpisodeDao
import jp.kztproject.simplepodcastplayer.data.database.entity.EpisodeEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeEpisodeDao : EpisodeDao {
    private val episodes = mutableListOf<EpisodeEntity>()
    private val episodesFlow = MutableStateFlow<List<EpisodeEntity>>(emptyList())

    override suspend fun insert(episode: EpisodeEntity) {
        episodes.removeAll { it.id == episode.id }
        episodes.add(episode)
        episodesFlow.value = episodes.toList()
    }

    override suspend fun update(episode: EpisodeEntity) {
        val index = episodes.indexOfFirst { it.id == episode.id }
        if (index != -1) {
            episodes[index] = episode
            episodesFlow.value = episodes.toList()
        }
    }

    override suspend fun getById(episodeId: String): EpisodeEntity? =
        episodes.find { it.id == episodeId }

    override fun getByPodcastId(podcastId: String): Flow<List<EpisodeEntity>> =
        episodesFlow.map { allEpisodes ->
            allEpisodes.filter { it.podcastId == podcastId }.sortedByDescending { it.publishedAt }
        }

    override suspend fun updateListenedStatus(episodeId: String, listened: Boolean) {
        val index = episodes.indexOfFirst { it.id == episodeId }
        if (index != -1) {
            episodes[index] = episodes[index].copy(listened = listened)
            episodesFlow.value = episodes.toList()
        }
    }

    override suspend fun updatePlaybackPosition(episodeId: String, position: Long) {
        val index = episodes.indexOfFirst { it.id == episodeId }
        if (index != -1) {
            episodes[index] = episodes[index].copy(lastPlaybackPosition = position)
            episodesFlow.value = episodes.toList()
        }
    }

    override suspend fun updateDownloadStatus(
        episodeId: String,
        isDownloaded: Boolean,
        localFilePath: String?,
        downloadedAt: Long,
    ) {
        val index = episodes.indexOfFirst { it.id == episodeId }
        if (index != -1) {
            episodes[index] = episodes[index].copy(
                isDownloaded = isDownloaded,
                localFilePath = localFilePath,
                downloadedAt = downloadedAt,
            )
            episodesFlow.value = episodes.toList()
        }
    }

    override fun getDownloadedEpisodes(): Flow<List<EpisodeEntity>> =
        episodesFlow.map { allEpisodes ->
            allEpisodes.filter { it.isDownloaded }
        }

    override suspend fun delete(episodeId: String) {
        episodes.removeAll { it.id == episodeId }
        episodesFlow.value = episodes.toList()
    }
}
