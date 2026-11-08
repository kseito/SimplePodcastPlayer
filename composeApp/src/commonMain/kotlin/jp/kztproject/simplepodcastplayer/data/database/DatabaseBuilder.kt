package jp.kztproject.simplepodcastplayer.data.database

import androidx.room.RoomDatabase
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

expect object DatabaseBuilder {
    fun build(): AppDatabase
}

expect fun RoomDatabase.Builder<AppDatabase>.buildDatabase(driver: SQLiteDriver = BundledSQLiteDriver()): AppDatabase
