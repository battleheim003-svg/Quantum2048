package com.battleheim.quantum2048.domain

import com.battleheim.quantum2048.engine.FusionRules
import com.battleheim.quantum2048.engine.GameMode
import com.battleheim.quantum2048.engine.GameState
import com.battleheim.quantum2048.engine.GameStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Serializable
data class StatsSnapshot(
    val mode: GameMode,
    val highestTile: Int = 0,
    val highScore: Long = 0,
    val gamesPlayed: Int = 0,
    val totalMerges: Long = 0,
    val manualCollapseLow: Long = 0,
    val manualCollapseHigh: Long = 0,
    val autoCollapseCount: Long = 0,
    val currentWinStreak: Int = 0,
    val longestWinStreak: Int = 0,
) {
    val manualCollapseTotal: Long get() = manualCollapseLow + manualCollapseHigh
    val isEmpty: Boolean
        get() = highestTile == 0 &&
            highScore == 0L &&
            gamesPlayed == 0 &&
            totalMerges == 0L &&
            manualCollapseTotal == 0L &&
            autoCollapseCount == 0L

    fun recordMerge(count: Int, state: GameState): StatsSnapshot {
        if (count <= 0) return recordBoard(state)
        return recordBoard(state).copy(totalMerges = totalMerges + count)
    }

    fun recordCollapse(lowValue: Boolean, manual: Boolean): StatsSnapshot {
        if (!manual) return copy(autoCollapseCount = autoCollapseCount + 1)
        return if (lowValue) {
            copy(manualCollapseLow = manualCollapseLow + 1)
        } else {
            copy(manualCollapseHigh = manualCollapseHigh + 1)
        }
    }

    fun recordGameEnded(state: GameState): StatsSnapshot {
        val withBoard = recordBoard(state)
        val endedGameHighestTile = highestTileOf(state)
        val won = state.status == GameStatus.WON || endedGameHighestTile >= WIN_TILE
        val nextCurrent = if (won) withBoard.currentWinStreak + 1 else 0
        return withBoard.copy(
            gamesPlayed = withBoard.gamesPlayed + 1,
            currentWinStreak = nextCurrent,
            longestWinStreak = maxOf(withBoard.longestWinStreak, nextCurrent),
        )
    }

    fun recordBoard(state: GameState): StatsSnapshot {
        val bestTile = highestTileOf(state)
        return copy(
            highestTile = maxOf(highestTile, bestTile),
            highScore = maxOf(highScore, state.bestScore, state.score, state.dailyBestScore),
        )
    }

    private companion object {
        const val WIN_TILE = 2048
    }
}

private fun highestTileOf(state: GameState): Int =
    state.cells.mapNotNull { it?.let(FusionRules::gameValueOf) }.maxOrNull() ?: 0

interface StatisticsRepository {
    fun observeStatistics(mode: GameMode): Flow<StatsSnapshot>
    suspend fun recordMerge(mode: GameMode, count: Int, state: GameState)
    suspend fun recordCollapse(mode: GameMode, lowValue: Boolean, manual: Boolean)
    suspend fun recordGameEnded(mode: GameMode, state: GameState)
    suspend fun clear()
}
