package jp.kztproject.simplepodcastplayer.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PodcastSearchResponse(
    @SerialName("resultCount")
    val resultCount: Int,
    @SerialName("results")
    val results: List<Podcast>
)

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
    val genres: List<String>? = null
)