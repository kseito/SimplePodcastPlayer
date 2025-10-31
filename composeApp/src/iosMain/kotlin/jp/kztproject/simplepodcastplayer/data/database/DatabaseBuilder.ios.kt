package jp.kztproject.simplepodcastplayer.data.database

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

actual object DatabaseBuilder {
    private var database: AppDatabase? = null

    actual fun build(): AppDatabase = database ?: Room
        .databaseBuilder<AppDatabase>(
            name = getDatabasePath(),
        ).buildDatabase()
        .also { database = it }

    @OptIn(ExperimentalForeignApi::class)
    private fun getDatabasePath(): String {
        val documentDirectory =
            NSFileManager.defaultManager.URLForDirectory(
                directory = NSDocumentDirectory,
                inDomain = NSUserDomainMask,
                appropriateForURL = null,
                create = false,
                error = null,
            )
        return "${documentDirectory?.path}/simplepodcastplayer.db"
    }
}

actual fun RoomDatabase.Builder<AppDatabase>.buildDatabase(): AppDatabase = this
    .setDriver(BundledSQLiteDriver())
    .setQueryCoroutineContext(Dispatchers.IO)
    .fallbackToDestructiveMigration(dropAllTables = true)
    .build()
