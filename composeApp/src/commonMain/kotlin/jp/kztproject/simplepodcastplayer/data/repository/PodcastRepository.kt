package jp.kztproject.simplepodcastplayer.data.repository

import jp.kztproject.simplepodcastplayer.data.Episode
import jp.kztproject.simplepodcastplayer.data.Podcast
import jp.kztproject.simplepodcastplayer.data.database.dao.EpisodeDao
import jp.kztproject.simplepodcastplayer.data.database.dao.PodcastDao
import jp.kztproject.simplepodcastplayer.data.database.entity.EpisodeEntity
import jp.kztproject.simplepodcastplayer.data.database.entity.PodcastEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlin.time.Clock

class PodcastRepository(private val podcastDao: PodcastDao, private val episodeDao: EpisodeDao) {

    /**
     * Subscribe to a podcast and save it with its episodes
     */
    suspend fun subscribeToPodcast(podcast: Podcast, episodes: List<Episode>) {
        // Save podcast
        val podcastEntity =
            PodcastEntity(
                id = podcast.trackId,
                name = podcast.trackName,
                artistName = podcast.artistName,
                description = podcast.collectionName ?: "",
                imageUrl = podcast.bestArtworkUrl(),
                feedUrl = podcast.feedUrl,
                subscribed = true,
                subscribedAt = Clock.System.now().toEpochMilliseconds(),
            )
        podcastDao.insert(podcastEntity)

        // Save all episodes
        episodes.forEach { episode ->
            val episodeEntity =
                EpisodeEntity(
                    id = episode.id,
                    podcastId = episode.podcastId,
                    title = episode.title,
                    description = episode.description,
                    audioUrl = episode.audioUrl,
                    duration = episode.duration,
                    publishedAt = episode.publishedAt,
                    listened = episode.listened,
                )
            episodeDao.insert(episodeEntity)
        }
    }

    /**
     * Unsubscribe from a podcast
     */
    suspend fun unsubscribeFromPodcast(podcastId: Long) {
        podcastDao.updateSubscription(podcastId, false)
    }

    /**
     * Check if podcast is subscribed
     */
    suspend fun isSubscribed(podcastId: Long): Boolean = podcastDao.getById(podcastId)?.subscribed ?: false

    /**
     * Get all subscribed podcasts
     */
    fun getSubscribedPodcasts(): Flow<List<PodcastEntity>> = podcastDao.getAll()

    /**
     * Get podcast by ID
     */
    suspend fun getPodcast(podcastId: Long): PodcastEntity? = podcastDao.getById(podcastId)

    /**
     * Get episodes by podcast ID (for offline access)
     */
    suspend fun getEpisodesByPodcastId(podcastId: String): List<Episode> {
        val entities = episodeDao.getByPodcastId(podcastId).first()
        return entities.map { entity ->
            Episode(
                id = entity.id,
                podcastId = entity.podcastId,
                title = entity.title,
                description = entity.description,
                audioUrl = entity.audioUrl,
                duration = entity.duration,
                publishedAt = entity.publishedAt,
                listened = entity.listened,
            )
        }
    }
}
