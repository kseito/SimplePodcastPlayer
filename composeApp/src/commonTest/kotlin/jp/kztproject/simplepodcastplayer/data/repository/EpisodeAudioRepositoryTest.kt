package jp.kztproject.simplepodcastplayer.data.repository

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
    fun downloadEpisode_completed_leavesAnAudioFileBehind() = runTest {
        addEpisode(id = "ep1", podcastId = "1")

        repository.downloadEpisode("ep1", "https://example.com/ep1.mp3").collect()

        audioDownloader.isDownloaded("ep1") shouldBe true
        repository.countAudioFilesByPodcast("1") shouldBe 1
    }

    @Test
    fun deleteAudioFile_removesTheAudioFile() = runTest {
        addEpisode(id = "ep1", podcastId = "1", isDownloaded = true)

        repository.deleteAudioFile("ep1") shouldBe true

        audioDownloader.isDownloaded("ep1") shouldBe false
        repository.countAudioFilesByPodcast("1") shouldBe 0
    }

    @Test
    fun deleteAudioFile_notDownloaded_returnsFalse() = runTest {
        addEpisode(id = "ep1", podcastId = "1")

        repository.deleteAudioFile("ep1") shouldBe false
    }

    @Test
    fun countAudioFilesByPodcast_ignoresEpisodesWhoseFileWasRemovedOutsideTheApp() = runTest {
        addEpisode(id = "ep1", podcastId = "1", isDownloaded = true)
        audioDownloader.clearDownloads()

        // Nothing in the DB claims the episode is downloaded, so a file that disappears
        // behind the app's back cannot leave a count that can never be worked off
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
        repository.countAudioFilesByPodcast("1") shouldBe 0
        repository.countAudioFilesByPodcast("2") shouldBe 1
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
        repository.countListenedAudioFiles() shouldBe 0
    }

    @Test
    fun migrateLegacyAudioFileNames_movesFilesLeftUnderTheOldName() = runTest {
        addEpisode(id = "ep1", podcastId = "1")
        addEpisode(id = "ep2", podcastId = "1")
        addEpisode(id = "ep3", podcastId = "1", isDownloaded = true)
        audioDownloader.setLegacyFileName("ep1")
        audioDownloader.setLegacyFileName("ep2")

        repository.migrateLegacyAudioFileNames() shouldBe 2

        // A migrated file is reachable again, which is what makes the episode count as downloaded
        audioDownloader.isDownloaded("ep1") shouldBe true
        audioDownloader.isDownloaded("ep2") shouldBe true
        audioDownloader.isDownloaded("ep3") shouldBe true
    }

    @Test
    fun migrateLegacyAudioFileNames_withoutLegacyFiles_doesNothing() = runTest {
        addEpisode(id = "ep1", podcastId = "1", isDownloaded = true)

        repository.migrateLegacyAudioFileNames() shouldBe 0
    }

    @Test
    fun deleteIncompleteDownloads_clearsLeftoversOfInterruptedDownloads() = runTest {
        audioDownloader.setIncompleteDownloads(2)

        repository.deleteIncompleteDownloads() shouldBe 2
        repository.deleteIncompleteDownloads() shouldBe 0
    }

    private suspend fun addEpisode(
        id: String,
        podcastId: String,
        listened: Boolean = false,
        isDownloaded: Boolean = false,
    ) {
        episodeDao.insert(
            TestDataFactory.createEpisodeEntity(id = id, podcastId = podcastId, listened = listened),
        )
        if (isDownloaded) {
            audioDownloader.setDownloadedEpisode(id, "/fake/path/$id.mp3")
        }
    }
}
