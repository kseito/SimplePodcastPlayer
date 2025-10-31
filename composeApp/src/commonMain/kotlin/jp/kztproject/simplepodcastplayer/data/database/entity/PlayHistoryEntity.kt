package jp.kztproject.simplepodcastplayer.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "play_history")
data class PlayHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val episodeId: String,
    val playedAt: Long,
    val position: Long,
    val completed: Boolean,
)
