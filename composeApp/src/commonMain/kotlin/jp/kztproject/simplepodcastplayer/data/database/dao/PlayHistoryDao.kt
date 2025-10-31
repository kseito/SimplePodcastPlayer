package jp.kztproject.simplepodcastplayer.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import jp.kztproject.simplepodcastplayer.data.database.entity.PlayHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(playHistory: PlayHistoryEntity)

    @Query("SELECT * FROM play_history WHERE episodeId = :episodeId ORDER BY playedAt DESC")
    fun getByEpisodeId(episodeId: String): Flow<List<PlayHistoryEntity>>

    @Query("SELECT * FROM play_history ORDER BY playedAt DESC LIMIT :limit")
    fun getRecent(limit: Int = 50): Flow<List<PlayHistoryEntity>>
}
