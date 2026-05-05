package jp.kztproject.simplepodcastplayer.data

interface IAppleSearchApiClient {
    suspend fun searchPodcasts(term: String, limit: Int = 30, country: String = "JP"): PodcastSearchResponse
    suspend fun lookupEpisodes(podcastId: Long, limit: Int = 200): PodcastLookupResponse
    fun close()
}
