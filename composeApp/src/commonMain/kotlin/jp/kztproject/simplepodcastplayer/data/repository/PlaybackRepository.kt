package jp.kztproject.simplepodcastplayer.data.repository

import jp.kztproject.simplepodcastplayer.data.database.DatabaseBuilder
import jp.kztproject.simplepodcastplayer.data.database.entity.EpisodeEntity
import jp.kztproject.simplepodcastplayer.data.database.entity.PlayHistoryEntity
import kotlinx.coroutines.flow.Flow

class PlaybackRepository {
    private val database = DatabaseBuilder.build()
    private val episodeDao = database.episodeDao()
    private val playHistoryDao = database.playHistoryDao()

    /**
     * Save playback position for an episode
     */
    suspend fun savePlaybackPosition(episodeId: String, position: Long) {
        episodeDao.updatePlaybackPosition(episodeId, position)
    }

    /**
     * Get episode from database
     */
    suspend fun getEpisode(episodeId: String): EpisodeEntity? = episodeDao.getById(episodeId)

    /**
     * Get saved playback position for an episode
     */
    suspend fun getPlaybackPosition(episodeId: String): Long = episodeDao.getById(episodeId)?.lastPlaybackPosition ?: 0L

    /**
     * Mark episode as listened
     */
    suspend fun markAsListened(episodeId: String) {
        episodeDao.updateListenedStatus(episodeId, true)
    }

    /**
     * Save or update episode in database
     */
    suspend fun saveEpisode(episode: EpisodeEntity) {
        episodeDao.insert(episode)
    }

    /**
     * Record play history
     */
    suspend fun recordPlayHistory(episodeId: String, position: Long, completed: Boolean) {
        val playHistory =
            PlayHistoryEntity(
                episodeId = episodeId,
                playedAt = System.currentTimeMillis(),
                position = position,
                completed = completed,
            )
        playHistoryDao.insert(playHistory)
    }

    /**
     * Get play history for an episode
     */
    fun getPlayHistory(episodeId: String): Flow<List<PlayHistoryEntity>> = playHistoryDao.getByEpisodeId(episodeId)

    /**
     * Get recent play history
     */
    fun getRecentPlayHistory(limit: Int = 50): Flow<List<PlayHistoryEntity>> = playHistoryDao.getRecent(limit)
}
