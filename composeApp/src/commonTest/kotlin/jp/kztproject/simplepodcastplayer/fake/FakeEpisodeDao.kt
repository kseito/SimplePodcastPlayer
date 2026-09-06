package jp.kztproject.simplepodcastplayer.fake

import jp.kztproject.simplepodcastplayer.data.database.dao.EpisodeDao
import jp.kztproject.simplepodcastplayer.data.database.entity.EpisodeEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeEpisodeDao : EpisodeDao {
    private val episodes = mutableListOf<EpisodeEntity>()
    private val episodesFlow = MutableStateFlow<List<EpisodeEntity>>(emptyList())
    private var downloadedEpisodeQueryError: Exception? = null

    /** Makes the queries that count deletable audio files throw, to cover the failure paths. */
    fun setDownloadedEpisodeQueryError(e: Exception?) {
        downloadedEpisodeQueryError = e
    }

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

    override suspend fun getById(episodeId: String): EpisodeEntity? = episodes.find { it.id == episodeId }

    override fun getByPodcastId(podcastId: String): Flow<List<EpisodeEntity>> = episodesFlow.map { allEpisodes ->
        allEpisodes.filter { it.podcastId == podcastId }.sortedWith(
            compareByDescending<EpisodeEntity> { it.trackId }.thenByDescending { it.publishedAt },
        )
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

    override suspend fun getEpisodeIdsByPodcastId(podcastId: String): List<String> {
        downloadedEpisodeQueryError?.let { throw it }
        return episodes.filter { it.podcastId == podcastId }.map { it.id }
    }

    override suspend fun getListenedEpisodeIds(): List<String> {
        downloadedEpisodeQueryError?.let { throw it }
        return episodes.filter { it.listened }.map { it.id }
    }

    override fun getInProgressEpisodes(): Flow<List<EpisodeEntity>> = episodesFlow.map { list ->
        list.filter { it.lastPlaybackPosition > 0L && !it.listened }
    }

    override suspend fun getAllEpisodeIds(): List<String> = episodes.map { it.id }

    override suspend fun delete(episodeId: String) {
        episodes.removeAll { it.id == episodeId }
        episodesFlow.value = episodes.toList()
    }
}
