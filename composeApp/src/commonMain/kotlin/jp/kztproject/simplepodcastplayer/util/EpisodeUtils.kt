package jp.kztproject.simplepodcastplayer.util

import jp.kztproject.simplepodcastplayer.data.Episode
import jp.kztproject.simplepodcastplayer.data.EpisodeDisplayModel
import jp.kztproject.simplepodcastplayer.data.ParsedEpisode
import jp.kztproject.simplepodcastplayer.data.PodcastLookupResult

private const val SECONDS_PER_HOUR = 3600L
private const val SECONDS_PER_MINUTE = 60L
private const val TIME_FORMAT_PADDING = 2
private const val DATE_PARTS_COUNT = 3
private const val DAY_LENGTH = 2

fun Episode.toDisplayModel(isDownloaded: Boolean = false): EpisodeDisplayModel = EpisodeDisplayModel(
    id = id,
    title = title,
    description = description,
    publishedAt = formatPublishedDate(publishedAt),
    duration = formatDuration(duration),
    audioUrl = audioUrl,
    listened = listened,
    isDownloaded = isDownloaded,
    trackId = trackId,
)

fun ParsedEpisode.toDisplayModel(isDownloaded: Boolean = false): EpisodeDisplayModel = EpisodeDisplayModel(
    id = id,
    title = title,
    description = description,
    publishedAt = publishedAt, // Already formatted by RSS parser
    duration = formatDuration(duration),
    audioUrl = audioUrl,
    listened = false, // Default to not listened for RSS episodes
    isDownloaded = isDownloaded,
    trackId = trackId,
)

fun PodcastLookupResult.toParsedEpisode(): ParsedEpisode? {
    if (!isPodcastEpisode() || !hasRequiredFields()) {
        return null
    }

    return ParsedEpisode(
        id = episodeGuid!!,
        title = trackName!!,
        description = description ?: "",
        publishedAt = formatReleaseDate(releaseDate!!),
        audioUrl = episodeUrl!!,
        duration = (trackTimeMillis ?: 0L) / 1000, // Convert milliseconds to seconds
        trackId = trackId,
    )
}

private fun formatReleaseDate(isoDate: String): String = try {
    // ISO 8601 format: 2024-12-15T10:00:00Z
    val parts = isoDate.split("T")[0].split("-")
    if (parts.size >= DATE_PARTS_COUNT) {
        val year = parts[0]
        val month = parts[1].toInt()
        val day = parts[2].take(DAY_LENGTH)
        val monthName = getMonthName(month)
        "$monthName $day, $year"
    } else {
        isoDate
    }
} catch (_: Exception) {
    isoDate
}

fun formatDuration(durationInSeconds: Long): String {
    val hours = durationInSeconds / SECONDS_PER_HOUR
    val minutes = (durationInSeconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE
    val seconds = durationInSeconds % SECONDS_PER_MINUTE

    return when {
        hours > 0 -> {
            val minutesStr = minutes.toString().padStart(TIME_FORMAT_PADDING, '0')
            val secondsStr = seconds.toString().padStart(TIME_FORMAT_PADDING, '0')
            "$hours:$minutesStr:$secondsStr"
        }
        else -> "$minutes:${seconds.toString().padStart(TIME_FORMAT_PADDING, '0')}"
    }
}

@Suppress("MagicNumber")
private val monthNames = mapOf(
    1 to "Jan", 2 to "Feb", 3 to "Mar", 4 to "Apr",
    5 to "May", 6 to "Jun", 7 to "Jul", 8 to "Aug",
    9 to "Sep", 10 to "Oct", 11 to "Nov", 12 to "Dec",
)

private fun getMonthName(month: Int): String = monthNames[month] ?: "Unknown"

fun formatPublishedDate(dateString: String): String = try {
    val parts = dateString.split("-")
    if (parts.size >= DATE_PARTS_COUNT) {
        val year = parts[0]
        val month = parts[1].toInt()
        val day = parts[2].take(DAY_LENGTH)
        val monthName = getMonthName(month)
        "$monthName $day, $year"
    } else {
        dateString
    }
} catch (_: Exception) {
    dateString
}
