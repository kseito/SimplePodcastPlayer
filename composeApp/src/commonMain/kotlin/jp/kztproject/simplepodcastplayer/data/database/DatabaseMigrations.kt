package jp.kztproject.simplepodcastplayer.data.database

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        // Rebuild rather than adding SQL defaults: the exported v2 schema has no defaults.
        connection.execSQL(
            """
            CREATE TABLE episodes_v2 (
                id TEXT NOT NULL PRIMARY KEY, podcastId TEXT NOT NULL, title TEXT NOT NULL,
                description TEXT NOT NULL, audioUrl TEXT NOT NULL, duration INTEGER NOT NULL,
                publishedAt TEXT NOT NULL, listened INTEGER NOT NULL, lastPlaybackPosition INTEGER NOT NULL,
                isDownloaded INTEGER NOT NULL, localFilePath TEXT, downloadedAt INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        connection.execSQL(
            """
            INSERT INTO episodes_v2
            SELECT id, podcastId, title, description, audioUrl, duration, publishedAt,
                   listened, lastPlaybackPosition, 0, NULL, 0 FROM episodes
            """.trimIndent(),
        )
        connection.execSQL("DROP TABLE episodes")
        connection.execSQL("ALTER TABLE episodes_v2 RENAME TO episodes")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE episodes ADD COLUMN trackId INTEGER")
    }
}
