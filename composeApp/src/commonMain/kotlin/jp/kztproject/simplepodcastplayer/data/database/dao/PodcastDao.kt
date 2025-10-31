package jp.kztproject.simplepodcastplayer.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import jp.kztproject.simplepodcastplayer.data.database.entity.PodcastEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PodcastDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(podcast: PodcastEntity)

    @Query("DELETE FROM podcasts WHERE id = :podcastId")
    suspend fun delete(podcastId: Long)

    @Query("SELECT * FROM podcasts ORDER BY subscribedAt DESC")
    fun getAll(): Flow<List<PodcastEntity>>

    @Query("SELECT * FROM podcasts WHERE id = :podcastId")
    suspend fun getById(podcastId: Long): PodcastEntity?

    @Query("UPDATE podcasts SET subscribed = :subscribed WHERE id = :podcastId")
    suspend fun updateSubscription(podcastId: Long, subscribed: Boolean)
}
