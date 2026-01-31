package jp.kztproject.simplepodcastplayer.fake

import jp.kztproject.simplepodcastplayer.data.IAppleSearchApiClient
import jp.kztproject.simplepodcastplayer.data.Podcast
import jp.kztproject.simplepodcastplayer.data.PodcastLookupResponse
import jp.kztproject.simplepodcastplayer.data.PodcastSearchResponse

class FakeAppleSearchApiClient : IAppleSearchApiClient {
    private var searchResult: PodcastSearchResponse = PodcastSearchResponse(
        resultCount = 0,
        results = emptyList(),
    )
    private var lookupResult: PodcastLookupResponse = PodcastLookupResponse(
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

    fun setLookupResult(response: PodcastLookupResponse) {
        lookupResult = response
    }

    fun setShouldThrowError(error: Exception) {
        shouldThrowError = true
        errorToThrow = error
    }

    fun clearError() {
        shouldThrowError = false
        errorToThrow = null
    }

    override suspend fun searchPodcasts(term: String, limit: Int, country: String): PodcastSearchResponse {
        if (shouldThrowError) {
            throw errorToThrow ?: Exception("Unknown error")
        }
        return searchResult
    }

    override suspend fun lookupEpisodes(podcastId: Long, limit: Int): PodcastLookupResponse {
        if (shouldThrowError) {
            throw errorToThrow ?: Exception("Unknown error")
        }
        return lookupResult
    }

    override fun close() {
        // No-op for fake
    }
}
