package com.battleheim.quantum2048.domain

import com.battleheim.quantum2048.engine.Difficulty
import com.battleheim.quantum2048.engine.FusionRules
import com.battleheim.quantum2048.engine.GameMode
import com.battleheim.quantum2048.engine.GameState
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Serializable
data class DailyScoreEntry(
    val date: String,
    val bestScore: Long,
) {
    init {
        require(date.isNotBlank())
        require(bestScore >= 0)
    }
}

@Serializable
data class ProfileState(
    val bestScores: Map<Difficulty, Long> = emptyMap(),
    val bestDailyScores: List<DailyScoreEntry> = emptyList(),
    val dailyBestScores: Map<String, Long> = emptyMap(),
    val bestDailyScore: Long = derivedBestDailyScore(bestDailyScores, dailyBestScores),
    val dailyChallengeCount: Int = derivedDailyChallengeCount(bestDailyScores, dailyBestScores),
    val successfulCollapseCount: Int = 0,
    val lowCollapseCount: Int = 0,
    val highCollapseCount: Int = 0,
    val totalWinEnergy: Int = 0,
    val winEnergySamples: Int = 0,
    val totalChainMergeCount: Int = 0,
    val unlockedAchievements: Set<String> = emptySet(),
) {
    val collapseLowRatio: Double
        get() {
            val total = lowCollapseCount + highCollapseCount
            return if (total == 0) 0.0 else lowCollapseCount.toDouble() / total
        }

    val averageWinEnergy: Double
        get() = if (winEnergySamples == 0) 0.0 else totalWinEnergy.toDouble() / winEnergySamples

    fun dailyBestScore(date: String?): Long {
        if (date.isNullOrBlank()) return 0
        return normalizedDailyScores().firstOrNull { it.date == date }?.bestScore ?: 0
    }

    fun record(game: GameState): ProfileState {
        val currentBest = bestScores[game.difficulty] ?: 0
        val nextBestScores = bestScores + (game.difficulty to maxOf(currentBest, game.bestScore, game.score))
        val dailyScore = maxOf(game.dailyBestScore, game.bestScore, game.score)
        val nextDailyScores = if (!game.dailyChallengeDate.isNullOrBlank() && dailyScore > 0) {
            upsertDailyScore(game.dailyChallengeDate, dailyScore)
        } else {
            normalizedDailyScores()
        }
        val nextDailyScoreMap = nextDailyScores.associate { it.date to it.bestScore }
        val dailyAttempts = maxOf(dailyChallengeCount, nextDailyScores.size)
        val unlocked = unlockedAchievements + game.unlockedAchievements + FusionRules.unlockedAchievementsFor(game)
        return copy(
            bestScores = nextBestScores,
            bestDailyScores = nextDailyScores,
            dailyBestScores = dailyBestScores + nextDailyScoreMap,
            bestDailyScore = maxOf(bestDailyScore, nextDailyScores.maxOfOrNull { it.bestScore } ?: 0),
            dailyChallengeCount = dailyAttempts,
            successfulCollapseCount = maxOf(successfulCollapseCount, game.successfulCollapseCount),
            lowCollapseCount = maxOf(lowCollapseCount, game.lowCollapseCount),
            highCollapseCount = maxOf(highCollapseCount, game.highCollapseCount),
            totalWinEnergy = maxOf(totalWinEnergy, game.totalWinEnergy),
            winEnergySamples = maxOf(winEnergySamples, game.winEnergySamples),
            totalChainMergeCount = maxOf(totalChainMergeCount, game.totalChainMergeCount),
            unlockedAchievements = unlocked,
        )
    }

    private fun upsertDailyScore(date: String, score: Long): List<DailyScoreEntry> {
        val existing = normalizedDailyScores().firstOrNull { it.date == date }
        val entry = DailyScoreEntry(date, maxOf(existing?.bestScore ?: 0, score))
        return (normalizedDailyScores().filterNot { it.date == date } + entry)
            .filter { it.bestScore > 0 }
            .sortedByDescending { it.date }
            .take(MAX_DAILY_ENTRIES)
    }

    private fun normalizedDailyScores(): List<DailyScoreEntry> {
        val fromList = bestDailyScores.associate { it.date to it.bestScore }
        val merged = fromList + dailyBestScores
        return merged
            .filter { (date, score) -> date.isNotBlank() && score > 0 }
            .map { (date, score) -> DailyScoreEntry(date, score) }
            .sortedByDescending { it.date }
            .take(MAX_DAILY_ENTRIES)
    }

    companion object {
        private const val MAX_DAILY_ENTRIES = 120
    }
}

private fun derivedBestDailyScore(
    entries: List<DailyScoreEntry>,
    scores: Map<String, Long>,
): Long =
    normalizedDailyScoreEntries(entries, scores).maxOfOrNull { it.bestScore } ?: 0

private fun derivedDailyChallengeCount(
    entries: List<DailyScoreEntry>,
    scores: Map<String, Long>,
): Int =
    normalizedDailyScoreEntries(entries, scores).size

private fun normalizedDailyScoreEntries(
    entries: List<DailyScoreEntry>,
    scores: Map<String, Long>,
): List<DailyScoreEntry> {
    val fromList = entries.associate { it.date to it.bestScore }
    val merged = fromList + scores
    return merged
        .filter { (date, score) -> date.isNotBlank() && score > 0 }
        .map { (date, score) -> DailyScoreEntry(date, score) }
        .sortedByDescending { it.date }
        .take(120)
}

interface ProfileRepository {
    fun observe(): Flow<ProfileState>
    suspend fun record(game: GameState)
    suspend fun clear()
}
