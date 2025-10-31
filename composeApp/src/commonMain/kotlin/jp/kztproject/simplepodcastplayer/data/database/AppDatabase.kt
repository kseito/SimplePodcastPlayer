package jp.kztproject.simplepodcastplayer.data.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
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
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun podcastDao(): PodcastDao

    abstract fun episodeDao(): EpisodeDao

    abstract fun playHistoryDao(): PlayHistoryDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}
