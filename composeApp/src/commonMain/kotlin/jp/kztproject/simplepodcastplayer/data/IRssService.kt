package jp.kztproject.simplepodcastplayer.data

interface IRssService {
    suspend fun fetchEpisodes(feedUrl: String): Result<List<ParsedEpisode>>
    fun close()
}
