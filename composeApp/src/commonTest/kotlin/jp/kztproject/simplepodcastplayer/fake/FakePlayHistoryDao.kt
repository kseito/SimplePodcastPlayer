package jp.kztproject.simplepodcastplayer.fake

import jp.kztproject.simplepodcastplayer.data.database.dao.PlayHistoryDao
import jp.kztproject.simplepodcastplayer.data.database.entity.PlayHistoryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakePlayHistoryDao : PlayHistoryDao {
    private val playHistories = mutableListOf<PlayHistoryEntity>()
    private val playHistoriesFlow = MutableStateFlow<List<PlayHistoryEntity>>(emptyList())

    override suspend fun insert(playHistory: PlayHistoryEntity) {
        playHistories.add(playHistory)
        playHistoriesFlow.value = playHistories.toList()
    }

    override fun getByEpisodeId(episodeId: String): Flow<List<PlayHistoryEntity>> =
        playHistoriesFlow.map { allHistories ->
            allHistories.filter { it.episodeId == episodeId }.sortedByDescending { it.playedAt }
        }

    override fun getRecent(limit: Int): Flow<List<PlayHistoryEntity>> = playHistoriesFlow.map { allHistories ->
        allHistories.sortedByDescending { it.playedAt }.take(limit)
    }
}
