package com.battleheim.quantum2048.domain

import com.battleheim.quantum2048.engine.BotDifficulty
import com.battleheim.quantum2048.engine.Difficulty
import com.battleheim.quantum2048.engine.DuelOpponent
import com.battleheim.quantum2048.engine.DuelPlayer
import com.battleheim.quantum2048.engine.GameState
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

private const val MAX_LEADERBOARD_ENTRIES = 80

@Serializable
data class LeaderboardEntry(
    val difficulty: Difficulty,
    val score: Long,
    val boardSize: Int,
    val achievedAtMillis: Long,
) {
    init {
        require(score >= 0)
        require(boardSize >= 2)
        require(achievedAtMillis > 0)
    }
}

@Serializable
data class DailyStreakState(
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val lastCompletedDate: String? = null,
) {
    init {
        require(currentStreak >= 0)
        require(bestStreak >= 0)
    }
}

@Serializable
data class DuelRecordState(
    val bestWinStreak: Int = 0,
    val currentWinStreak: Int = 0,
    val totalWins: Int = 0,
    val totalLosses: Int = 0,
) {
    init {
        require(bestWinStreak >= 0)
        require(currentWinStreak >= 0)
        require(totalWins >= 0)
        require(totalLosses >= 0)
    }
}

@Serializable
data class CloudSaveState(
    val enabled: Boolean = false,
    val lastLocalSnapshotMillis: Long = 0,
    val lastSyncStatus: String = "offline_stub",
)

@Serializable
data class SocialState(
    val leaderboards: List<LeaderboardEntry> = emptyList(),
    val dailyStreak: DailyStreakState = DailyStreakState(),
    val duelRecord: DuelRecordState = DuelRecordState(),
    val cloudSave: CloudSaveState = CloudSaveState(),
    val syncedAchievements: Set<String> = emptySet(),
) {
    fun recordGame(game: GameState, nowMillis: Long = System.currentTimeMillis()): SocialState {
        val nextLeaderboard = upsertLeaderboard(game, nowMillis)
        val nextStreak = if (game.difficulty == Difficulty.DAILY && game.dailyChallengeDate != null && game.score > 0) {
            dailyStreak.record(game.dailyChallengeDate)
        } else {
            dailyStreak
        }
        return copy(
            leaderboards = nextLeaderboard,
            dailyStreak = nextStreak,
            cloudSave = cloudSave.copy(lastLocalSnapshotMillis = nowMillis),
        )
    }

    fun recordDuelResult(winner: DuelPlayer?, nowMillis: Long = System.currentTimeMillis()): SocialState {
        val won = winner == DuelPlayer.PLAYER_ONE
        val nextCurrent = if (won) duelRecord.currentWinStreak + 1 else 0
        return copy(
            duelRecord = duelRecord.copy(
                currentWinStreak = nextCurrent,
                bestWinStreak = maxOf(duelRecord.bestWinStreak, nextCurrent),
                totalWins = duelRecord.totalWins + if (won) 1 else 0,
                totalLosses = duelRecord.totalLosses + if (won) 0 else 1,
            ),
            cloudSave = cloudSave.copy(lastLocalSnapshotMillis = nowMillis),
        )
    }

    fun syncAchievements(achievementIds: Set<String>): SocialState =
        copy(syncedAchievements = syncedAchievements + achievementIds)

    private fun upsertLeaderboard(game: GameState, nowMillis: Long): List<LeaderboardEntry> {
        val existing = leaderboards.firstOrNull { it.difficulty == game.difficulty && it.boardSize == game.size }
        val bestScore = maxOf(existing?.score ?: 0, game.bestScore, game.score, game.dailyBestScore)
        val entry = LeaderboardEntry(game.difficulty, bestScore, game.size, existing?.achievedAtMillis ?: nowMillis)
        return (leaderboards.filterNot { it.difficulty == game.difficulty && it.boardSize == game.size } + entry)
            .filter { it.score > 0 }
            .sortedWith(compareByDescending<LeaderboardEntry> { it.score }.thenBy { it.difficulty.name })
            .take(MAX_LEADERBOARD_ENTRIES)
    }
}

private fun DailyStreakState.record(date: String): DailyStreakState {
    if (lastCompletedDate == date) return this
    val previous = lastCompletedDate?.let { runCatching { java.time.LocalDate.parse(it) }.getOrNull() }
    val current = runCatching { java.time.LocalDate.parse(date) }.getOrNull() ?: return this
    val nextCurrent = if (previous != null && previous.plusDays(1) == current) currentStreak + 1 else 1
    return copy(
        currentStreak = nextCurrent,
        bestStreak = maxOf(bestStreak, nextCurrent),
        lastCompletedDate = date,
    )
}

interface SocialRepository {
    fun observe(): Flow<SocialState>
    suspend fun recordGame(game: GameState)
    suspend fun recordDuelResult(
        difficulty: Difficulty,
        opponent: DuelOpponent,
        botDifficulty: BotDifficulty,
        winner: DuelPlayer?,
    )
    suspend fun syncAchievements(achievementIds: Set<String>)
    suspend fun clear()
}
