package jp.kztproject.simplepodcastplayer.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "episodes")
data class EpisodeEntity(
    @PrimaryKey val id: String,
    val podcastId: String,
    val title: String,
    val description: String,
    val audioUrl: String,
    val duration: Long,
    val publishedAt: String,
    val listened: Boolean = false,
    val lastPlaybackPosition: Long = 0L,
    val isDownloaded: Boolean = false,
    val localFilePath: String? = null,
    val downloadedAt: Long = 0L,
    val trackId: Long? = null,
)
