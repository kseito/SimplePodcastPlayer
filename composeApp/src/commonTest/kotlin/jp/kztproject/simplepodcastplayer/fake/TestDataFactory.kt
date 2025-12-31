package jp.kztproject.simplepodcastplayer.fake

import jp.kztproject.simplepodcastplayer.data.Episode
import jp.kztproject.simplepodcastplayer.data.ParsedEpisode
import jp.kztproject.simplepodcastplayer.data.Podcast
import jp.kztproject.simplepodcastplayer.data.database.entity.EpisodeEntity
import jp.kztproject.simplepodcastplayer.data.database.entity.PlayHistoryEntity
import jp.kztproject.simplepodcastplayer.data.database.entity.PodcastEntity

object TestDataFactory {
    fun createPodcast(
        trackId: Long = 1L,
        trackName: String = "Test Podcast",
        artistName: String = "Test Artist",
        feedUrl: String = "https://example.com/feed.xml",
    ) = Podcast(
        trackId = trackId,
        trackName = trackName,
        artistName = artistName,
        collectionName = "Test Collection",
        artworkUrl100 = "https://example.com/artwork.jpg",
        feedUrl = feedUrl,
    )

    fun createPodcastEntity(
        id: Long = 1L,
        name: String = "Test Podcast",
        artistName: String = "Test Artist",
        feedUrl: String = "https://example.com/feed.xml",
        subscribed: Boolean = true,
        subscribedAt: Long = 1703001600000L, // 2024-12-15 00:00:00 UTC
    ) = PodcastEntity(
        id = id,
        name = name,
        artistName = artistName,
        description = "Test Description",
        imageUrl = "https://example.com/artwork.jpg",
        feedUrl = feedUrl,
        subscribed = subscribed,
        subscribedAt = subscribedAt,
    )

    fun createEpisode(
        id: String = "ep1",
        podcastId: String = "1",
        title: String = "Test Episode",
        duration: Long = 1800L,
    ) = Episode(
        id = id,
        podcastId = podcastId,
        title = title,
        description = "Test Description",
        audioUrl = "https://example.com/episode.mp3",
        duration = duration,
        publishedAt = "2024-12-15T10:00:00Z",
        listened = false,
    )

    fun createEpisodeEntity(
        id: String = "ep1",
        podcastId: String = "1",
        title: String = "Test Episode",
        duration: Long = 1800L,
        listened: Boolean = false,
        isDownloaded: Boolean = false,
    ) = EpisodeEntity(
        id = id,
        podcastId = podcastId,
        title = title,
        description = "Test Description",
        audioUrl = "https://example.com/episode.mp3",
        duration = duration,
        publishedAt = "2024-12-15T10:00:00Z",
        listened = listened,
        lastPlaybackPosition = 0L,
        isDownloaded = isDownloaded,
        localFilePath = if (isDownloaded) "/fake/path/$id.mp3" else null,
        downloadedAt = if (isDownloaded) 1703001600000L else 0L, // 2024-12-15 00:00:00 UTC
    )

    fun createParsedEpisode(id: String = "ep1", title: String = "Test Episode", duration: Long = 1800L) = ParsedEpisode(
        id = id,
        title = title,
        description = "Test Description",
        publishedAt = "Dec 15, 2024",
        duration = duration,
        audioUrl = "https://example.com/episode.mp3",
    )

    fun createPlayHistoryEntity(
        id: Long = 0L,
        episodeId: String = "ep1",
        playedAt: Long = 1703001600000L, // 2024-12-15 00:00:00 UTC
        position: Long = 300L,
        completed: Boolean = false,
    ) = PlayHistoryEntity(
        id = id,
        episodeId = episodeId,
        playedAt = playedAt,
        position = position,
        completed = completed,
    )
}
