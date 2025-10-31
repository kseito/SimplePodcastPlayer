package jp.kztproject.simplepodcastplayer.data.database

import androidx.room.RoomDatabase

expect object DatabaseBuilder {
    fun build(): AppDatabase
}

fun RoomDatabase.Builder<AppDatabase>.buildDatabase(): AppDatabase = this
    .fallbackToDestructiveMigration(dropAllTables = true)
    .build()
