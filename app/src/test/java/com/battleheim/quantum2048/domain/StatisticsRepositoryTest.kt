package com.battleheim.quantum2048.domain

import com.battleheim.quantum2048.engine.Difficulty
import com.battleheim.quantum2048.engine.GameMode
import com.battleheim.quantum2048.engine.GameState
import com.battleheim.quantum2048.engine.GameStatus
import com.battleheim.quantum2048.engine.Tile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class StatisticsRepositoryTest {
    @Test
    fun recordMergeUpdatesMergeCountHighScoreAndHighestTileOnlyUpward() = runTest {
        val repository = InMemoryStatisticsRepository()
        val lowBoard = boardWithTile(value = 128, score = 50)
        val highBoard = boardWithTile(value = 512, score = 900)

        repository.recordMerge(GameMode.CLASSIC, count = 2, state = highBoard)
        repository.recordMerge(GameMode.CLASSIC, count = 1, state = lowBoard)

        val stats = repository.observeStatistics(GameMode.CLASSIC).first()
        assertEquals(3L, stats.totalMerges)
        assertEquals(900L, stats.highScore)
        assertEquals(512, stats.highestTile)
    }

    @Test
    fun recordCollapseSeparatesManualLowManualHighAndAuto() = runTest {
        val repository = InMemoryStatisticsRepository()

        repository.recordCollapse(GameMode.QUANTUM, lowValue = true, manual = true)
        repository.recordCollapse(GameMode.QUANTUM, lowValue = false, manual = true)
        repository.recordCollapse(GameMode.QUANTUM, lowValue = false, manual = false)

        val stats = repository.observeStatistics(GameMode.QUANTUM).first()
        assertEquals(1L, stats.manualCollapseLow)
        assertEquals(1L, stats.manualCollapseHigh)
        assertEquals(1L, stats.autoCollapseCount)
    }

    @Test
    fun recordEntangledCollapseTracksChainCounterSeparately() = runTest {
        val repository = InMemoryStatisticsRepository()

        repository.recordEntangledCollapse(GameMode.QUANTUM, count = 2)

        val stats = repository.observeStatistics(GameMode.QUANTUM).first()
        assertEquals(2L, stats.entangledCollapseChainCount)
        assertEquals(0L, stats.autoCollapseCount)
    }

    @Test
    fun recordGameEndedIncrementsGamesAndLongestWinningStreak() = runTest {
        val repository = InMemoryStatisticsRepository()
        val win = boardWithTile(value = 2048, score = 2048, status = GameStatus.WON)
        val loss = boardWithTile(value = 512, score = 512, status = GameStatus.LOST)

        repository.recordGameEnded(GameMode.CLASSIC, win)
        repository.recordGameEnded(GameMode.CLASSIC, win)
        repository.recordGameEnded(GameMode.CLASSIC, loss)

        val stats = repository.observeStatistics(GameMode.CLASSIC).first()
        assertEquals(3, stats.gamesPlayed)
        assertEquals(0, stats.currentWinStreak)
        assertEquals(2, stats.longestWinStreak)
    }

    private fun boardWithTile(value: Int, score: Long, status: GameStatus = GameStatus.PLAYING): GameState {
        val cells = MutableList<Tile?>(16) { null }
        cells[0] = Tile(1, value)
        return GameState(
            cells = cells,
            score = score,
            bestScore = score,
            status = status,
            mode = GameMode.CLASSIC,
            difficulty = Difficulty.EASY,
        )
    }

    private class InMemoryStatisticsRepository : StatisticsRepository {
        private val states = GameMode.entries.associateWith { MutableStateFlow(StatsSnapshot(it)) }

        override fun observeStatistics(mode: GameMode): Flow<StatsSnapshot> = states.getValue(mode)

        override suspend fun recordMerge(mode: GameMode, count: Int, state: GameState) {
            update(mode) { it.recordMerge(count, state) }
        }

        override suspend fun recordCollapse(mode: GameMode, lowValue: Boolean, manual: Boolean) {
            update(mode) { it.recordCollapse(lowValue, manual) }
        }

        override suspend fun recordEntangledCollapse(mode: GameMode, count: Int) {
            update(mode) { it.recordEntangledCollapse(count) }
        }

        override suspend fun recordGameEnded(mode: GameMode, state: GameState) {
            update(mode) { it.recordGameEnded(state) }
        }

        override suspend fun clear() {
            GameMode.entries.forEach { states.getValue(it).value = StatsSnapshot(it) }
        }

        private fun update(mode: GameMode, transform: (StatsSnapshot) -> StatsSnapshot) {
            states.getValue(mode).value = transform(states.getValue(mode).value)
        }
    }
}
