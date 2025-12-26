package jp.kztproject.simplepodcastplayer.fake

import jp.kztproject.simplepodcastplayer.data.Podcast
import jp.kztproject.simplepodcastplayer.data.PodcastSearchResponse

class FakeAppleSearchApiClient {
    private var searchResult: PodcastSearchResponse = PodcastSearchResponse(
        resultCount = 0,
        results = emptyList(),
    )
    private var shouldThrowError = false
    private var errorToThrow: Exception? = null

    fun setSearchResult(podcasts: List<Podcast>) {
        searchResult = PodcastSearchResponse(
            resultCount = podcasts.size,
            results = podcasts,
        )
    }

    fun setShouldThrowError(error: Exception) {
        shouldThrowError = true
        errorToThrow = error
    }

    fun clearError() {
        shouldThrowError = false
        errorToThrow = null
    }

    suspend fun searchPodcasts(term: String, limit: Int = 30, country: String = "US"): PodcastSearchResponse {
        if (shouldThrowError) {
            throw errorToThrow ?: Exception("Unknown error")
        }
        return searchResult
    }

    fun close() {
        // No-op for fake
    }
}
