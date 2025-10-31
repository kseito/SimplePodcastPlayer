package jp.kztproject.simplepodcastplayer.data.database

import androidx.room.Room
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

actual object DatabaseBuilder {
    private var database: AppDatabase? = null

    actual fun build(): AppDatabase = database ?: synchronized(this) {
        database ?: Room
            .databaseBuilder<AppDatabase>(
                name = getDatabasePath(),
            ).buildDatabase()
            .also { database = it }
    }

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
