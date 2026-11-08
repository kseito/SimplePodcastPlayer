package jp.kztproject.simplepodcastplayer.data.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers

actual object DatabaseBuilder {
    private lateinit var context: Context
    private var database: AppDatabase? = null

    fun init(context: Context) {
        this.context = context.applicationContext
    }

    actual fun build(): AppDatabase = database ?: synchronized(this) {
        database ?: Room
            .databaseBuilder<AppDatabase>(
                context = context,
                name = "simplepodcastplayer.db",
            ).buildDatabase()
            .also { database = it }
    }
}

actual fun RoomDatabase.Builder<AppDatabase>.buildDatabase(): AppDatabase = this
    .setDriver(BundledSQLiteDriver())
    .setQueryCoroutineContext(Dispatchers.IO)
    .fallbackToDestructiveMigration(dropAllTables = true)
    .build()
