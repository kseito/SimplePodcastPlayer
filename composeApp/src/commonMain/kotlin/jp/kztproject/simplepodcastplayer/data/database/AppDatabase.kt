package jp.kztproject.simplepodcastplayer.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import jp.kztproject.simplepodcastplayer.data.database.dao.EpisodeDao
import jp.kztproject.simplepodcastplayer.data.database.dao.PlayHistoryDao
import jp.kztproject.simplepodcastplayer.data.database.dao.PodcastDao
import jp.kztproject.simplepodcastplayer.data.database.entity.EpisodeEntity
import jp.kztproject.simplepodcastplayer.data.database.entity.PlayHistoryEntity
import jp.kztproject.simplepodcastplayer.data.database.entity.PodcastEntity

@Database(
    entities = [PodcastEntity::class, EpisodeEntity::class, PlayHistoryEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun podcastDao(): PodcastDao

    abstract fun episodeDao(): EpisodeDao

    abstract fun playHistoryDao(): PlayHistoryDao
}
