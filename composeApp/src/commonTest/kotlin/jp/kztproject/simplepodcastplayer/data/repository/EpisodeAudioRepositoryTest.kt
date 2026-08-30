package jp.kztproject.simplepodcastplayer.data.repository

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import jp.kztproject.simplepodcastplayer.fake.FakeAudioDownloader
import jp.kztproject.simplepodcastplayer.fake.FakeEpisodeDao
import jp.kztproject.simplepodcastplayer.fake.TestDataFactory
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class EpisodeAudioRepositoryTest {
    private lateinit var episodeDao: FakeEpisodeDao
    private lateinit var audioDownloader: FakeAudioDownloader
    private lateinit var repository: EpisodeAudioRepository

    @BeforeTest
    fun setup() {
        episodeDao = FakeEpisodeDao()
        audioDownloader = FakeAudioDownloader()
        repository = EpisodeAudioRepository(audioDownloader, episodeDao)
    }

    @Test
    fun downloadEpisode_completed_marksEpisodeDownloadedInDb() = runTest {
        addEpisode(id = "ep1", podcastId = "1")

        repository.downloadEpisode("ep1", "https://example.com/ep1.mp3").collect()

        audioDownloader.isDownloaded("ep1") shouldBe true
        episodeDao.getById("ep1")!!.isDownloaded shouldBe true
        episodeDao.getById("ep1")!!.localFilePath shouldBe "/fake/path/ep1.mp3"
    }

    @Test
    fun deleteAudioFile_clearsDownloadColumnsInDb() = runTest {
        addEpisode(id = "ep1", podcastId = "1", isDownloaded = true)

        repository.deleteAudioFile("ep1") shouldBe true

        audioDownloader.isDownloaded("ep1") shouldBe false
        episodeDao.getById("ep1")!!.isDownloaded shouldBe false
        episodeDao.getById("ep1")!!.localFilePath.shouldBeNull()
    }

    @Test
    fun deleteAudioFile_notDownloaded_returnsFalse() = runTest {
        addEpisode(id = "ep1", podcastId = "1")

        repository.deleteAudioFile("ep1") shouldBe false
    }

    @Test
    fun deleteAudioFile_fileAlreadyMissing_clearsStaleDownloadState() = runTest {
        // The DB says downloaded but the file is gone, e.g. it was removed outside the app
        episodeDao.insert(TestDataFactory.createEpisodeEntity(id = "ep1", podcastId = "1", isDownloaded = true))

        repository.deleteAudioFile("ep1") shouldBe true

        // Without clearing the columns the episode would stay "downloaded" forever
        // and keep being counted by the cleanup flows
        episodeDao.getById("ep1")!!.isDownloaded shouldBe false
        episodeDao.getById("ep1")!!.localFilePath.shouldBeNull()
        repository.countAudioFilesByPodcast("1") shouldBe 0
    }

    @Test
    fun countAudioFilesByPodcast_countsOnlyDownloadedEpisodesOfThatPodcast() = runTest {
        addEpisode(id = "ep1", podcastId = "1", isDownloaded = true)
        addEpisode(id = "ep2", podcastId = "1", isDownloaded = false)
        addEpisode(id = "ep3", podcastId = "2", isDownloaded = true)

        repository.countAudioFilesByPodcast("1") shouldBe 1
    }

    @Test
    fun deleteAudioFilesByPodcast_deletesOnlyThatPodcastsAudioFiles() = runTest {
        addEpisode(id = "ep1", podcastId = "1", isDownloaded = true)
        addEpisode(id = "ep2", podcastId = "1", isDownloaded = true)
        addEpisode(id = "ep3", podcastId = "2", isDownloaded = true)

        repository.deleteAudioFilesByPodcast("1") shouldBe 2

        audioDownloader.isDownloaded("ep1") shouldBe false
        audioDownloader.isDownloaded("ep2") shouldBe false
        audioDownloader.isDownloaded("ep3") shouldBe true
        episodeDao.getById("ep1")!!.isDownloaded shouldBe false
        episodeDao.getById("ep3")!!.isDownloaded shouldBe true
    }

    @Test
    fun deleteAudioFilesByPodcast_noAudioFiles_returnsZero() = runTest {
        addEpisode(id = "ep1", podcastId = "1", isDownloaded = false)

        repository.deleteAudioFilesByPodcast("1") shouldBe 0
    }

    @Test
    fun countListenedAudioFiles_countsOnlyListenedAndDownloaded() = runTest {
        addEpisode(id = "ep1", podcastId = "1", listened = true, isDownloaded = true)
        addEpisode(id = "ep2", podcastId = "1", listened = true, isDownloaded = false)
        addEpisode(id = "ep3", podcastId = "1", listened = false, isDownloaded = true)

        repository.countListenedAudioFiles() shouldBe 1
    }

    @Test
    fun deleteListenedAudioFiles_keepsUnlistenedAudioFiles() = runTest {
        addEpisode(id = "ep1", podcastId = "1", listened = true, isDownloaded = true)
        addEpisode(id = "ep2", podcastId = "2", listened = true, isDownloaded = true)
        addEpisode(id = "ep3", podcastId = "1", listened = false, isDownloaded = true)

        // Spans podcasts, but never touches episodes that are not listened yet
        repository.deleteListenedAudioFiles() shouldBe 2

        audioDownloader.isDownloaded("ep1") shouldBe false
        audioDownloader.isDownloaded("ep2") shouldBe false
        audioDownloader.isDownloaded("ep3") shouldBe true
        episodeDao.getById("ep1")!!.isDownloaded shouldBe false
        episodeDao.getById("ep3")!!.isDownloaded shouldBe true
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
            audioDownloader.setDownloadedEpisode(id, "/fake/path/$id.mp3")
        }
    }
}
