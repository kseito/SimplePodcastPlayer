package jp.kztproject.simplepodcastplayer.data.database

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.core.app.ApplicationProvider
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DatabaseMigrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun version1_preservesSubscriptionsEpisodesAndHistory() = verifyUpgrade(1)

    @Test
    fun version2_preservesSubscriptionsEpisodesAndHistory() = verifyUpgrade(2)

    @Test
    fun version3_preservesSubscriptionsEpisodesAndHistory() = verifyUpgrade(3)

    @Test
    fun missingMigration_failsWithoutDeletingData() {
        val file = createDatabase(4)
        AndroidSQLiteDriver().open(file.path).use { it.execSQL("PRAGMA user_version = 5") }
        val database = Room.databaseBuilder<AppDatabase>(context, file.path).buildDatabase(AndroidSQLiteDriver())
        try {
            shouldThrow<IllegalStateException> {
                runBlocking { database.episodeDao().getById("episode") }
            }
        } finally {
            database.close()
        }
        AndroidSQLiteDriver().open(file.path).use { connection ->
            connection.prepare("SELECT lastPlaybackPosition FROM episodes WHERE id = 'episode'").use {
                it.step() shouldBe true
                it.getLong(0) shouldBe 12345L
            }
        }
    }

    private fun verifyUpgrade(version: Int): Unit = runBlocking {
        val file = createDatabase(version)
        // Exercise the production builder, including generated 3 -> 4 migration and schema validation.
        val database = Room.databaseBuilder<AppDatabase>(context, file.path).buildDatabase(AndroidSQLiteDriver())
        try {
            val episode = requireNotNull(database.episodeDao().getById("episode"))
            episode.podcastId shouldBe "7"
            episode.title shouldBe "Saved episode"
            episode.audioUrl shouldBe "https://example.test/audio.mp3"
            episode.listened shouldBe true
            episode.lastPlaybackPosition shouldBe 12345L
            episode.trackId shouldBe if (version >= 3) 456L else null
            val podcast = requireNotNull(database.podcastDao().getById(7))
            podcast.name shouldBe "Saved podcast"
            podcast.subscribed shouldBe true
            podcast.subscribedAt shouldBe 1000L
            val history = database.playHistoryDao().getByEpisodeId("episode").first().single()
            history.id shouldBe 9L
            history.position shouldBe 12345L
            history.playedAt shouldBe 2000L
            history.completed shouldBe false
        } finally {
            database.close()
        }
        // Downloads remain on disk when v4 drops the old download metadata columns.
        File(file.parentFile, "saved-audio.mp3").readText() shouldBe "downloaded audio"
    }

    private fun createDatabase(version: Int): File {
        val directory = File(context.cacheDir, "migration-$version").apply { mkdirs() }
        val file = File(directory, "app.db")
        file.delete()
        File(directory, "saved-audio.mp3").writeText("downloaded audio")
        val schema = JSONObject(
            File(
                System.getProperty("database.schemaDir"),
                "jp.kztproject.simplepodcastplayer.data.database.AppDatabase/$version.json",
            ).readText(),
        ).getJSONObject("database")
        AndroidSQLiteDriver().open(file.path).use { connection ->
            val entities = schema.getJSONArray("entities")
            for (index in 0 until entities.length()) {
                val entity = entities.getJSONObject(index)
                connection.execSQL(
                    entity.getString("createSql").replace("\${TABLE_NAME}", entity.getString("tableName")),
                )
            }
            val setup = schema.getJSONArray("setupQueries")
            for (index in 0 until setup.length()) connection.execSQL(setup.getString(index))
            connection.execSQL("PRAGMA user_version = $version")
            connection.execSQL(
                "INSERT INTO podcasts VALUES (7, 'Saved podcast', 'Artist', 'Description', NULL, NULL, 1, 1000)",
            )
            val downloadValues = if (version in 2..3) ", 1, 'saved-audio.mp3', 3000" else ""
            val trackValue = if (version >= 3) ", 456" else ""
            connection.execSQL(
                "INSERT INTO episodes VALUES " +
                    "('episode', '7', 'Saved episode', 'Description', 'https://example.test/audio.mp3', " +
                    "600, '2026-01-01', 1, 12345$downloadValues$trackValue)",
            )
            connection.execSQL("INSERT INTO play_history VALUES (9, 'episode', 2000, 12345, 0)")
        }
        return file
    }
}
