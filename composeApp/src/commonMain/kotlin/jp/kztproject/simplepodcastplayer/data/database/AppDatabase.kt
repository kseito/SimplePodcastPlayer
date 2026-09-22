package jp.kztproject.simplepodcastplayer.data.database

import androidx.room.AutoMigration
import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.DeleteColumn
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.migration.AutoMigrationSpec
import jp.kztproject.simplepodcastplayer.data.database.dao.EpisodeDao
import jp.kztproject.simplepodcastplayer.data.database.dao.PlayHistoryDao
import jp.kztproject.simplepodcastplayer.data.database.dao.PodcastDao
import jp.kztproject.simplepodcastplayer.data.database.entity.EpisodeEntity
import jp.kztproject.simplepodcastplayer.data.database.entity.PlayHistoryEntity
import jp.kztproject.simplepodcastplayer.data.database.entity.PodcastEntity

@Database(
    entities = [PodcastEntity::class, EpisodeEntity::class, PlayHistoryEntity::class],
    version = 4,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 3, to = 4, spec = DropDownloadColumns::class)],
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun podcastDao(): PodcastDao

    abstract fun episodeDao(): EpisodeDao

    abstract fun playHistoryDao(): PlayHistoryDao
}

/**
 * Whether an episode holds an audio file is now answered by the file system alone, so the
 * columns that used to mirror it are gone. Subscriptions, playback positions and listened
 * flags are kept.
 */
@DeleteColumn(tableName = "episodes", columnName = "isDownloaded")
@DeleteColumn(tableName = "episodes", columnName = "localFilePath")
@DeleteColumn(tableName = "episodes", columnName = "downloadedAt")
class DropDownloadColumns : AutoMigrationSpec

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}
