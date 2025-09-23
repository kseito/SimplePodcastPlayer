package jp.kztproject.simplepodcastplayer.data

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import jp.kztproject.simplepodcastplayer.util.RssParser
import jp.kztproject.simplepodcastplayer.util.toEpisodes

// TODO: Move to the infrastructure layer
class RssService {
    private val httpClient = HttpClient()
    private val rssParser = RssParser()

    @Suppress("ReturnCount")
    suspend fun fetchEpisodes(feedUrl: String): Result<List<ParsedEpisode>> {
        return try {
            if (feedUrl.isBlank()) {
                return Result.failure(IllegalArgumentException("Feed URL is empty"))
            }

            val response = httpClient.get(feedUrl)

            if (!response.status.value.toString().startsWith("2")) {
                return Result.failure(
                    Exception("Failed to fetch feed: HTTP ${response.status.value}")
                )
            }

            val xmlContent = response.bodyAsText()

            if (xmlContent.isBlank()) {
                return Result.failure(Exception("Feed content is empty"))
            }

            val channel = rssParser.parseRssFeed(xmlContent)
            val episodes = channel.toEpisodes()

            Result.success(episodes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun close() {
        httpClient.close()
    }
}
