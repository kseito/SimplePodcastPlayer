package jp.kztproject.simplepodcastplayer.data.database

import androidx.room.RoomDatabase

expect object DatabaseBuilder {
    fun build(): AppDatabase
}

expect fun RoomDatabase.Builder<AppDatabase>.buildDatabase(): AppDatabase
