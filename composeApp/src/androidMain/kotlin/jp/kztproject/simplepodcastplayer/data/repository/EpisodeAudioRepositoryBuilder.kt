package jp.kztproject.simplepodcastplayer.data.repository

import android.content.Context
import jp.kztproject.simplepodcastplayer.data.database.DatabaseBuilder
import jp.kztproject.simplepodcastplayer.download.AudioDownloader

actual object EpisodeAudioRepositoryBuilder {
    private var context: Context? = null

    fun init(context: Context) {
        this.context = context.applicationContext
    }

    actual fun build(): IEpisodeAudioRepository {
        val appContext = context ?: error(
            "EpisodeAudioRepositoryBuilder has not been initialized. Call init() in your Application class.",
        )
        return EpisodeAudioRepository(
            audioDownloader = AudioDownloader(appContext),
            episodeDao = DatabaseBuilder.build().episodeDao(),
        )
    }
}
