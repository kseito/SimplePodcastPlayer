package jp.kztproject.simplepodcastplayer.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import jp.kztproject.simplepodcastplayer.data.database.entity.EpisodeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EpisodeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(episode: EpisodeEntity)

    @Update
    suspend fun update(episode: EpisodeEntity)

    @Query("SELECT * FROM episodes WHERE id = :episodeId")
    suspend fun getById(episodeId: String): EpisodeEntity?

    @Query("SELECT * FROM episodes WHERE podcastId = :podcastId ORDER BY trackId DESC, publishedAt DESC")
    fun getByPodcastId(podcastId: String): Flow<List<EpisodeEntity>>

    @Query("UPDATE episodes SET listened = :listened WHERE id = :episodeId")
    suspend fun updateListenedStatus(episodeId: String, listened: Boolean)

    @Query("UPDATE episodes SET lastPlaybackPosition = :position WHERE id = :episodeId")
    suspend fun updatePlaybackPosition(episodeId: String, position: Long)

    // Whether an episode holds an audio file is answered by the file system, not by this table,
    // so these queries only narrow down which episodes are worth asking about.
    @Query("SELECT id FROM episodes WHERE podcastId = :podcastId")
    suspend fun getEpisodeIdsByPodcastId(podcastId: String): List<String>

    @Query("SELECT id FROM episodes WHERE listened = 1")
    suspend fun getListenedEpisodeIds(): List<String>

    @Query("SELECT * FROM episodes WHERE lastPlaybackPosition > 0 AND listened = 0 ORDER BY lastPlaybackPosition DESC")
    fun getInProgressEpisodes(): Flow<List<EpisodeEntity>>

    // TODO: Remove once existing installs have migrated their downloads to the hashed file names.
    @Query("SELECT id FROM episodes")
    suspend fun getAllEpisodeIds(): List<String>

    @Query("DELETE FROM episodes WHERE id = :episodeId")
    suspend fun delete(episodeId: String)
}
