package jp.kztproject.simplepodcastplayer.download

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.test.Test

class AudioFileNameTest {
    @Test
    fun audioFileNameOf_isStableForTheSameEpisodeId() {
        // SHA-256 of "abc", so the naming is pinned to the hash and not just to itself
        audioFileNameOf("abc") shouldBe
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad.mp3"
    }

    @Test
    fun audioFileNameOf_separatesIdsThatDifferOnlyInPunctuation() {
        // The pre-hash scheme mapped both of these onto "https___example_com_ep_1.mp3", which made
        // one episode look downloaded because the other one was
        val slash = "https://example.com/ep/1"
        val hyphen = "https://example.com/ep-1"

        legacyAudioFileNameOf(slash) shouldBe legacyAudioFileNameOf(hyphen)
        audioFileNameOf(slash) shouldNotBe audioFileNameOf(hyphen)
    }

    @Test
    fun audioFileNameOf_separatesIdsWithNoAlphanumericCharacters() {
        // Some feeds use the episode title as its guid. The pre-hash scheme kept nothing but the
        // alphanumeric characters, so two Japanese titles of the same length became one file name
        val first = "第一回 テスト"
        val second = "第二回 テスト"

        legacyAudioFileNameOf(first) shouldBe legacyAudioFileNameOf(second)
        audioFileNameOf(first) shouldNotBe audioFileNameOf(second)
    }

    @Test
    fun audioFileNameOf_keepsTheNameShortEnoughForAFileSystem() {
        val longId = "https://example.com/" + "a".repeat(1000)

        audioFileNameOf(longId).length shouldBe 68
    }
}
