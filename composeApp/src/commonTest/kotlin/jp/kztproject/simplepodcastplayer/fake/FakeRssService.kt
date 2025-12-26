package jp.kztproject.simplepodcastplayer.fake

import jp.kztproject.simplepodcastplayer.data.ParsedEpisode

class FakeRssService {
    private var episodes: List<ParsedEpisode> = emptyList()
    private var shouldReturnError = false
    private var errorToReturn: Exception? = null

    fun setEpisodes(episodesList: List<ParsedEpisode>) {
        episodes = episodesList
        shouldReturnError = false
    }

    fun setError(error: Exception) {
        shouldReturnError = true
        errorToReturn = error
    }

    fun clearError() {
        shouldReturnError = false
        errorToReturn = null
    }

    suspend fun fetchEpisodes(feedUrl: String): Result<List<ParsedEpisode>> {
        if (feedUrl.isBlank()) {
            return Result.failure(IllegalArgumentException("Feed URL is empty"))
        }

        return if (shouldReturnError) {
            Result.failure(errorToReturn ?: Exception("Unknown error"))
        } else {
            Result.success(episodes)
        }
    }

    fun close() {
        // No-op for fake
    }
}
