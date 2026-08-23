package jp.kztproject.simplepodcastplayer.data.repository

import jp.kztproject.simplepodcastplayer.fake.FakeDownloadRepository
import jp.kztproject.simplepodcastplayer.fake.FakeEpisodeDao
import jp.kztproject.simplepodcastplayer.fake.TestDataFactory
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DownloadCleanupRepositoryTest {
    private lateinit var episodeDao: FakeEpisodeDao
    private lateinit var downloadRepository: FakeDownloadRepository
    private lateinit var cleanupRepository: DownloadCleanupRepository

    @BeforeTest
    fun setup() {
        episodeDao = FakeEpisodeDao()
        downloadRepository = FakeDownloadRepository(episodeDao)
        cleanupRepository = DownloadCleanupRepository(episodeDao, downloadRepository)
    }

    @Test
    fun countDownloadsByPodcast_countsOnlyDownloadedEpisodesOfThatPodcast() = runTest {
        addEpisode(id = "ep1", podcastId = "1", isDownloaded = true)
        addEpisode(id = "ep2", podcastId = "1", isDownloaded = false)
        addEpisode(id = "ep3", podcastId = "2", isDownloaded = true)

        assertEquals(1, cleanupRepository.countDownloadsByPodcast("1"))
    }

    @Test
    fun deleteDownloadsByPodcast_deletesOnlyThatPodcastsDownloads() = runTest {
        addEpisode(id = "ep1", podcastId = "1", isDownloaded = true)
        addEpisode(id = "ep2", podcastId = "1", isDownloaded = true)
        addEpisode(id = "ep3", podcastId = "2", isDownloaded = true)

        val deleted = cleanupRepository.deleteDownloadsByPodcast("1")

        assertEquals(2, deleted)
        assertFalse(downloadRepository.isDownloaded("ep1"))
        assertFalse(downloadRepository.isDownloaded("ep2"))
        assertTrue(downloadRepository.isDownloaded("ep3"))
        assertFalse(episodeDao.getById("ep1")!!.isDownloaded)
        assertTrue(episodeDao.getById("ep3")!!.isDownloaded)
    }

    @Test
    fun deleteDownloadsByPodcast_noDownloads_returnsZero() = runTest {
        addEpisode(id = "ep1", podcastId = "1", isDownloaded = false)

        assertEquals(0, cleanupRepository.deleteDownloadsByPodcast("1"))
    }

    @Test
    fun countListenedDownloads_countsOnlyListenedAndDownloaded() = runTest {
        addEpisode(id = "ep1", podcastId = "1", listened = true, isDownloaded = true)
        addEpisode(id = "ep2", podcastId = "1", listened = true, isDownloaded = false)
        addEpisode(id = "ep3", podcastId = "1", listened = false, isDownloaded = true)

        assertEquals(1, cleanupRepository.countListenedDownloads())
    }

    @Test
    fun deleteListenedDownloads_keepsUnlistenedDownloads() = runTest {
        addEpisode(id = "ep1", podcastId = "1", listened = true, isDownloaded = true)
        addEpisode(id = "ep2", podcastId = "2", listened = true, isDownloaded = true)
        addEpisode(id = "ep3", podcastId = "1", listened = false, isDownloaded = true)

        val deleted = cleanupRepository.deleteListenedDownloads()

        // Spans podcasts, but never touches episodes that are not listened yet
        assertEquals(2, deleted)
        assertFalse(downloadRepository.isDownloaded("ep1"))
        assertFalse(downloadRepository.isDownloaded("ep2"))
        assertTrue(downloadRepository.isDownloaded("ep3"))
        assertFalse(episodeDao.getById("ep1")!!.isDownloaded)
        assertTrue(episodeDao.getById("ep3")!!.isDownloaded)
    }

    private suspend fun addEpisode(
        id: String,
        podcastId: String,
        listened: Boolean = false,
        isDownloaded: Boolean = false,
    ) {
        episodeDao.insert(
            TestDataFactory.createEpisodeEntity(
                id = id,
                podcastId = podcastId,
                listened = listened,
                isDownloaded = isDownloaded,
            ),
        )
        if (isDownloaded) {
            downloadRepository.setDownloadedEpisode(id, "/fake/path/$id.mp3")
        }
    }
}
