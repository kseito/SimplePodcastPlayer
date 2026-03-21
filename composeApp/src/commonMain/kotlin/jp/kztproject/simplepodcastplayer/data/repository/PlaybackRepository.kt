package jp.kztproject.simplepodcastplayer.data.repository

import jp.kztproject.simplepodcastplayer.data.database.dao.EpisodeDao
import jp.kztproject.simplepodcastplayer.data.database.dao.PlayHistoryDao
import jp.kztproject.simplepodcastplayer.data.database.entity.EpisodeEntity
import jp.kztproject.simplepodcastplayer.data.database.entity.PlayHistoryEntity
import kotlinx.coroutines.flow.Flow
import kotlin.time.Clock

interface IPlaybackRepository {
    suspend fun savePlaybackPosition(episodeId: String, position: Long)
    suspend fun getEpisode(episodeId: String): EpisodeEntity?
    suspend fun getPlaybackPosition(episodeId: String): Long
    suspend fun markAsListened(episodeId: String)
    suspend fun saveEpisode(episode: EpisodeEntity)
    suspend fun recordPlayHistory(episodeId: String, position: Long, completed: Boolean)
    fun getPlayHistory(episodeId: String): Flow<List<PlayHistoryEntity>>
    fun getRecentPlayHistory(limit: Int = 50): Flow<List<PlayHistoryEntity>>
    fun getInProgressEpisodes(): Flow<List<EpisodeEntity>>
}

class PlaybackRepository(private val episodeDao: EpisodeDao, private val playHistoryDao: PlayHistoryDao) :
    IPlaybackRepository {

    /**
     * Save playback position for an episode
     */
    override suspend fun savePlaybackPosition(episodeId: String, position: Long) {
        episodeDao.updatePlaybackPosition(episodeId, position)
    }

    /**
     * Get episode from database
     */
    override suspend fun getEpisode(episodeId: String): EpisodeEntity? = episodeDao.getById(episodeId)

    /**
     * Get saved playback position for an episode
     */
    override suspend fun getPlaybackPosition(episodeId: String): Long =
        episodeDao.getById(episodeId)?.lastPlaybackPosition ?: 0L

    /**
     * Mark episode as listened
     */
    override suspend fun markAsListened(episodeId: String) {
        episodeDao.updateListenedStatus(episodeId, true)
    }

    /**
     * Save or update episode in database
     */
    override suspend fun saveEpisode(episode: EpisodeEntity) {
        episodeDao.insert(episode)
    }

    /**
     * Record play history
     */
    override suspend fun recordPlayHistory(episodeId: String, position: Long, completed: Boolean) {
        val playHistory =
            PlayHistoryEntity(
                episodeId = episodeId,
                playedAt = Clock.System.now().toEpochMilliseconds(),
                position = position,
                completed = completed,
            )
        playHistoryDao.insert(playHistory)
    }

    /**
     * Get play history for an episode
     */
    override fun getPlayHistory(episodeId: String): Flow<List<PlayHistoryEntity>> =
        playHistoryDao.getByEpisodeId(episodeId)

    /**
     * Get recent play history
     */
    override fun getRecentPlayHistory(limit: Int): Flow<List<PlayHistoryEntity>> = playHistoryDao.getRecent(limit)

    /**
     * Get episodes that are in progress (started but not completed)
     */
    override fun getInProgressEpisodes(): Flow<List<EpisodeEntity>> = episodeDao.getInProgressEpisodes()
}
