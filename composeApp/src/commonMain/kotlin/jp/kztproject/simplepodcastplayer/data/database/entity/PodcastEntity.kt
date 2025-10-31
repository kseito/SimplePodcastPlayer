package jp.kztproject.simplepodcastplayer.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "podcasts")
data class PodcastEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val artistName: String,
    val description: String,
    val imageUrl: String?,
    val feedUrl: String?,
    val subscribed: Boolean,
    val subscribedAt: Long,
)
