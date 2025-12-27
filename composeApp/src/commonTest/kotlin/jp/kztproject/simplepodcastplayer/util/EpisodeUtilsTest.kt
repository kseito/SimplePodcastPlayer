package jp.kztproject.simplepodcastplayer.util

import jp.kztproject.simplepodcastplayer.data.Episode
import jp.kztproject.simplepodcastplayer.data.ParsedEpisode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EpisodeUtilsTest {
    @Test
    fun formatDuration_overOneHour_returnsHmsFormat() {
        val duration = 3661L // 1:01:01

        val result = formatDuration(duration)

        assertEquals("1:01:01", result)
    }

    @Test
    fun formatDuration_exactlyOneHour_returnsHmsFormat() {
        val duration = 3600L // 1:00:00

        val result = formatDuration(duration)

        assertEquals("1:00:00", result)
    }

    @Test
    fun formatDuration_underOneHour_returnsMsFormat() {
        val duration = 125L // 2:05

        val result = formatDuration(duration)

        assertEquals("2:05", result)
    }

    @Test
    fun formatDuration_zeroSeconds_returnsZeroMinutes() {
        val duration = 0L

        val result = formatDuration(duration)

        assertEquals("0:00", result)
    }

    @Test
    fun formatDuration_boundaryValue59Minutes59Seconds_returnsMsFormat() {
        val duration = 3599L // 59:59

        val result = formatDuration(duration)

        assertEquals("59:59", result)
    }

    @Test
    fun formatDuration_multipleHours_returnsHmsFormat() {
        val duration = 7384L // 2:03:04

        val result = formatDuration(duration)

        assertEquals("2:03:04", result)
    }

    @Test
    fun formatPublishedDate_isoFormat_returnsFormattedDate() {
        val dateString = "2024-12-15T10:00:00Z"

        val result = formatPublishedDate(dateString)

        assertEquals("Dec 15, 2024", result)
    }

    @Test
    fun formatPublishedDate_invalidFormat_returnsOriginalString() {
        val dateString = "Invalid Date"

        val result = formatPublishedDate(dateString)

        assertEquals("Invalid Date", result)
    }

    @Test
    fun formatPublishedDate_emptyString_returnsEmptyString() {
        val dateString = ""

        val result = formatPublishedDate(dateString)

        assertEquals("", result)
    }

    @Test
    fun formatPublishedDate_variousMonths_returnsCorrectMonthNames() {
        assertEquals("Jan 01, 2024", formatPublishedDate("2024-01-01"))
        assertEquals("Feb 14, 2024", formatPublishedDate("2024-02-14"))
        assertEquals("Mar 31, 2024", formatPublishedDate("2024-03-31"))
        assertEquals("Apr 15, 2024", formatPublishedDate("2024-04-15"))
        assertEquals("May 20, 2024", formatPublishedDate("2024-05-20"))
        assertEquals("Jun 30, 2024", formatPublishedDate("2024-06-30"))
        assertEquals("Jul 04, 2024", formatPublishedDate("2024-07-04"))
        assertEquals("Aug 25, 2024", formatPublishedDate("2024-08-25"))
        assertEquals("Sep 10, 2024", formatPublishedDate("2024-09-10"))
        assertEquals("Oct 31, 2024", formatPublishedDate("2024-10-31"))
        assertEquals("Nov 11, 2024", formatPublishedDate("2024-11-11"))
        assertEquals("Dec 25, 2024", formatPublishedDate("2024-12-25"))
    }

    @Test
    fun episodeToDisplayModel_normalConversion_returnsCorrectDisplayModel() {
        val episode = Episode(
            id = "ep1",
            title = "Test Episode",
            description = "Test Description",
            publishedAt = "2024-12-15T10:00:00Z",
            duration = 3661L,
            audioUrl = "https://example.com/audio.mp3",
            listened = true,
            podcastId = "1",
        )

        val displayModel = episode.toDisplayModel(isDownloaded = false)

        assertEquals("ep1", displayModel.id)
        assertEquals("Test Episode", displayModel.title)
        assertEquals("Test Description", displayModel.description)
        assertEquals("Dec 15, 2024", displayModel.publishedAt)
        assertEquals("1:01:01", displayModel.duration)
        assertEquals("https://example.com/audio.mp3", displayModel.audioUrl)
        assertTrue(displayModel.listened)
        assertFalse(displayModel.isDownloaded)
    }

    @Test
    fun episodeToDisplayModel_withDownloadedFlag_setsDownloadedTrue() {
        val episode = Episode(
            id = "ep1",
            title = "Test Episode",
            description = "Test Description",
            publishedAt = "2024-12-15T10:00:00Z",
            duration = 1800L,
            audioUrl = "https://example.com/audio.mp3",
            listened = false,
            podcastId = "1",
        )

        val displayModel = episode.toDisplayModel(isDownloaded = true)

        assertTrue(displayModel.isDownloaded)
    }

    @Test
    fun parsedEpisodeToDisplayModel_normalConversion_returnsCorrectDisplayModel() {
        val parsedEpisode = ParsedEpisode(
            id = "ep1",
            title = "Parsed Episode",
            description = "Parsed Description",
            publishedAt = "Dec 15, 2024",
            duration = 1800L,
            audioUrl = "https://example.com/audio.mp3",
        )

        val displayModel = parsedEpisode.toDisplayModel(isDownloaded = false)

        assertEquals("ep1", displayModel.id)
        assertEquals("Parsed Episode", displayModel.title)
        assertEquals("Parsed Description", displayModel.description)
        assertEquals("Dec 15, 2024", displayModel.publishedAt) // Already formatted
        assertEquals("30:00", displayModel.duration)
        assertEquals("https://example.com/audio.mp3", displayModel.audioUrl)
        assertFalse(displayModel.listened) // Default to false for RSS episodes
        assertFalse(displayModel.isDownloaded)
    }

    @Test
    fun parsedEpisodeToDisplayModel_withDownloadedFlag_setsDownloadedTrue() {
        val parsedEpisode = ParsedEpisode(
            id = "ep1",
            title = "Parsed Episode",
            description = "Parsed Description",
            publishedAt = "Dec 15, 2024",
            duration = 1800L,
            audioUrl = "https://example.com/audio.mp3",
        )

        val displayModel = parsedEpisode.toDisplayModel(isDownloaded = true)

        assertTrue(displayModel.isDownloaded)
    }
}
