package jp.kztproject.simplepodcastplayer.fake

import jp.kztproject.simplepodcastplayer.data.database.dao.PodcastDao
import jp.kztproject.simplepodcastplayer.data.database.entity.PodcastEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakePodcastDao : PodcastDao {
    private val podcasts = mutableListOf<PodcastEntity>()
    private val podcastsFlow = MutableStateFlow<List<PodcastEntity>>(emptyList())

    override suspend fun insert(podcast: PodcastEntity) {
        podcasts.removeAll { it.id == podcast.id }
        podcasts.add(podcast)
        podcastsFlow.value = podcasts.toList()
    }

    override suspend fun delete(podcastId: Long) {
        podcasts.removeAll { it.id == podcastId }
        podcastsFlow.value = podcasts.toList()
    }

    override fun getAll(): Flow<List<PodcastEntity>> = podcastsFlow.map { allPodcasts ->
        allPodcasts.filter { it.subscribed }.sortedByDescending { it.subscribedAt }
    }

    override suspend fun getById(podcastId: Long): PodcastEntity? =
        podcasts.find { it.id == podcastId }

    override suspend fun updateSubscription(podcastId: Long, subscribed: Boolean) {
        val index = podcasts.indexOfFirst { it.id == podcastId }
        if (index != -1) {
            podcasts[index] = podcasts[index].copy(subscribed = subscribed)
            podcastsFlow.value = podcasts.toList()
        }
    }
}
