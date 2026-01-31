package jp.kztproject.simplepodcastplayer.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PodcastSearchResponse(
    @SerialName("resultCount")
    val resultCount: Int,
    @SerialName("results")
    val results: List<Podcast>,
)

@Serializable
data class PodcastLookupResponse(
    @SerialName("resultCount")
    val resultCount: Int,
    @SerialName("results")
    val results: List<PodcastLookupResult>,
)

@Serializable
data class PodcastLookupResult(
    @SerialName("wrapperType")
    val wrapperType: String,
    @SerialName("kind")
    val kind: String? = null,
    @SerialName("trackId")
    val trackId: Long? = null,
    @SerialName("trackName")
    val trackName: String? = null,
    @SerialName("episodeGuid")
    val episodeGuid: String? = null,
    @SerialName("releaseDate")
    val releaseDate: String? = null,
    @SerialName("trackTimeMillis")
    val trackTimeMillis: Long? = null,
    @SerialName("description")
    val description: String? = null,
    @SerialName("episodeUrl")
    val episodeUrl: String? = null,
) {
    fun isPodcastEpisode(): Boolean = wrapperType == "podcastEpisode"

    fun hasRequiredFields(): Boolean = trackId != null &&
        trackName != null &&
        episodeGuid != null &&
        episodeUrl != null &&
        releaseDate != null
}

@Serializable
data class Podcast(
    @SerialName("trackId")
    val trackId: Long,
    @SerialName("trackName")
    val trackName: String,
    @SerialName("artistName")
    val artistName: String,
    @SerialName("collectionName")
    val collectionName: String? = null,
    @SerialName("trackViewUrl")
    val trackViewUrl: String? = null,
    @SerialName("artworkUrl30")
    val artworkUrl30: String? = null,
    @SerialName("artworkUrl60")
    val artworkUrl60: String? = null,
    @SerialName("artworkUrl100")
    val artworkUrl100: String? = null,
    @SerialName("collectionPrice")
    val collectionPrice: Double? = null,
    @SerialName("trackPrice")
    val trackPrice: Double? = null,
    @SerialName("releaseDate")
    val releaseDate: String? = null,
    @SerialName("collectionExplicitness")
    val collectionExplicitness: String? = null,
    @SerialName("trackExplicitness")
    val trackExplicitness: String? = null,
    @SerialName("trackCount")
    val trackCount: Int? = null,
    @SerialName("trackTimeMillis")
    val trackTimeMillis: Long? = null,
    @SerialName("country")
    val country: String? = null,
    @SerialName("currency")
    val currency: String? = null,
    @SerialName("primaryGenreName")
    val primaryGenreName: String? = null,
    @SerialName("contentAdvisoryRating")
    val contentAdvisoryRating: String? = null,
    @SerialName("feedUrl")
    val feedUrl: String? = null,
    @SerialName("genreIds")
    val genreIds: List<String>? = null,
    @SerialName("genres")
    val genres: List<String>? = null,
) {
    fun bestArtworkUrl(): String? = artworkUrl100 ?: artworkUrl60 ?: artworkUrl30

    val hasArtwork: Boolean
        get() = listOf(artworkUrl100, artworkUrl60, artworkUrl30).any { it != null }
}

@Serializable
data class Episode(
    @SerialName("id")
    val id: String,
    @SerialName("podcastId")
    val podcastId: String,
    @SerialName("title")
    val title: String,
    @SerialName("description")
    val description: String,
    @SerialName("audioUrl")
    val audioUrl: String,
    @SerialName("duration")
    val duration: Long,
    @SerialName("publishedAt")
    val publishedAt: String,
    @SerialName("listened")
    val listened: Boolean = false,
    @SerialName("trackId")
    val trackId: Long? = null,
)

data class EpisodeDisplayModel(
    val id: String,
    val title: String,
    val description: String,
    val publishedAt: String,
    val duration: String,
    val audioUrl: String,
    val listened: Boolean,
    val isDownloaded: Boolean = false,
    val trackId: Long? = null,
)
