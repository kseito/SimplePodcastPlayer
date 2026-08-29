package jp.kztproject.simplepodcastplayer.data.repository

import jp.kztproject.simplepodcastplayer.data.database.DatabaseBuilder
import jp.kztproject.simplepodcastplayer.download.AudioDownloader

actual object EpisodeAudioRepositoryBuilder {
    actual fun build(): IEpisodeAudioRepository = EpisodeAudioRepository(
        audioDownloader = AudioDownloader(),
        episodeDao = DatabaseBuilder.build().episodeDao(),
    )
}
