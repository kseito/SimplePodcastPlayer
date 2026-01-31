package jp.kztproject.simplepodcastplayer.data

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

// Apple Search API implementation based on documentation:
// https://performance-partners.apple.com/search-api
class AppleSearchApiClient : IAppleSearchApiClient {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    prettyPrint = false
                    encodeDefaults = true
                },
                contentType = ContentType.Any,
            )
        }

        install(HttpTimeout) {
            requestTimeoutMillis = 15_000 // 15秒
            connectTimeoutMillis = 15_000 // 15秒
            socketTimeoutMillis = 15_000 // 15秒
        }

        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    Napier.d("Ktor: $message")
                }
            }
            level = LogLevel.ALL // すべてのログを出力
        }
    }

    private companion object {
        const val SEARCH_URL = "https://itunes.apple.com/search"
        const val LOOKUP_URL = "https://itunes.apple.com/lookup"
    }

    override suspend fun searchPodcasts(term: String, limit: Int, country: String): PodcastSearchResponse =
        client.get(SEARCH_URL) {
            parameter("term", term)
            parameter("country", country)
            parameter("media", "podcast")
            parameter("limit", limit)
            header(HttpHeaders.Accept, ContentType.Application.Json.toString())
            header(HttpHeaders.UserAgent, "SimplePodcastPlayer/1.0")
        }.body()

    override suspend fun lookupEpisodes(podcastId: Long, limit: Int): PodcastLookupResponse =
        client.get(LOOKUP_URL) {
            parameter("id", podcastId)
            parameter("media", "podcast")
            parameter("entity", "podcastEpisode")
            parameter("limit", limit)
            header(HttpHeaders.Accept, ContentType.Application.Json.toString())
            header(HttpHeaders.UserAgent, "SimplePodcastPlayer/1.0")
        }.body()

    override fun close() {
        client.close()
    }
}
