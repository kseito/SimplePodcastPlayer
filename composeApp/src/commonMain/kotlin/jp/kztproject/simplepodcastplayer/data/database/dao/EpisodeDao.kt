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

    @Query(
        """
        UPDATE episodes
        SET isDownloaded = :isDownloaded,
            localFilePath = :localFilePath,
            downloadedAt = :downloadedAt
        WHERE id = :episodeId
        """,
    )
    suspend fun updateDownloadStatus(
        episodeId: String,
        isDownloaded: Boolean,
        localFilePath: String?,
        downloadedAt: Long,
    )

    @Query("SELECT * FROM episodes WHERE isDownloaded = 1")
    fun getDownloadedEpisodes(): Flow<List<EpisodeEntity>>

    @Query("DELETE FROM episodes WHERE id = :episodeId")
    suspend fun delete(episodeId: String)
}
