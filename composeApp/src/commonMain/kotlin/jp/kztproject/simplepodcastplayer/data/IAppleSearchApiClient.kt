package jp.kztproject.simplepodcastplayer.data

interface IAppleSearchApiClient {
    suspend fun searchPodcasts(term: String, limit: Int = 30, country: String = "US"): PodcastSearchResponse
    fun close()
}
