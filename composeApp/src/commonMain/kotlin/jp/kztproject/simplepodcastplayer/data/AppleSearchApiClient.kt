package jp.kztproject.simplepodcastplayer.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

// Apple Search API implementation based on documentation:
// https://performance-partners.apple.com/search-api
class AppleSearchApiClient {
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
    }

    private companion object {
        const val BASE_URL = "https://itunes.apple.com/search"
    }

    suspend fun searchPodcasts(term: String, limit: Int = 30, country: String = "US"): PodcastSearchResponse =
        client.get(BASE_URL) {
            parameter("term", term)
            parameter("country", country)
            parameter("media", "podcast")
            parameter("limit", limit)
            header(HttpHeaders.Accept, ContentType.Application.Json.toString())
            header(HttpHeaders.UserAgent, "SimplePodcastPlayer/1.0")
        }.body()

    fun close() {
        client.close()
    }
}
