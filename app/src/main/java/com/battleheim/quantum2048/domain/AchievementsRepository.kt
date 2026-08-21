package com.battleheim.quantum2048.domain

import com.battleheim.quantum2048.engine.GameMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.serialization.Serializable

enum class AchievementProgressType { BOOLEAN, CUMULATIVE }

@Serializable
data class Achievement(
    val id: String,
    val titleKey: String,
    val descriptionKey: String,
    val target: Long,
    val progressType: AchievementProgressType,
)

data class AchievementProgress(
    val achievement: Achievement,
    val current: Long,
    val completedAtMillis: Long? = null,
) {
    val isCompleted: Boolean get() = completedAtMillis != null
    val ratio: Float get() = (current.toFloat() / achievement.target.toFloat()).coerceIn(0f, 1f)
}

@Serializable
data class AchievementsState(
    val completedAtMillis: Map<String, Long> = emptyMap(),
)

data class AchievementEvaluation(
    val achievements: List<AchievementProgress>,
    val newlyCompleted: List<AchievementProgress>,
    val nextState: AchievementsState,
)

interface AchievementsRepository {
    fun observeAchievements(): Flow<List<AchievementProgress>>
    suspend fun refresh(nowMillis: Long = System.currentTimeMillis()): List<AchievementProgress>
    suspend fun clear()
}

object AchievementCatalog {
    const val LOW_COLLAPSE_100 = "low_collapse_100"
    const val HIGH_COLLAPSE_100 = "high_collapse_100"
    const val QUANTUM_2048 = "quantum_2048"
    const val TILE_4096 = "tile_4096"
    const val DAILY_STREAK_5 = "daily_streak_5"
    const val DAILY_STREAK_30 = "daily_streak_30"
    const val CLASSIC_FIRST_GAME = "classic_first_game"
    const val QUANTUM_FIRST_GAME = "quantum_first_game"
    const val MERGES_1000 = "merges_1000"
    const val WIN_STREAK_5 = "win_streak_5"
    const val BOTH_MODES = "both_modes"
    const val ENTANGLED_COLLAPSE_50 = "entangled_collapse_50"

    val all: List<Achievement> = listOf(
        Achievement(LOW_COLLAPSE_100, "achievement_low_collapse_100_title", "achievement_low_collapse_100_desc", 100, AchievementProgressType.CUMULATIVE),
        Achievement(HIGH_COLLAPSE_100, "achievement_high_collapse_100_title", "achievement_high_collapse_100_desc", 100, AchievementProgressType.CUMULATIVE),
        Achievement(QUANTUM_2048, "achievement_quantum_2048_title", "achievement_quantum_2048_desc", 2048, AchievementProgressType.BOOLEAN),
        Achievement(TILE_4096, "achievement_tile_4096_title", "achievement_tile_4096_desc", 4096, AchievementProgressType.BOOLEAN),
        Achievement(DAILY_STREAK_5, "achievement_daily_streak_5_title", "achievement_daily_streak_5_desc", 5, AchievementProgressType.CUMULATIVE),
        Achievement(DAILY_STREAK_30, "achievement_daily_streak_30_title", "achievement_daily_streak_30_desc", 30, AchievementProgressType.CUMULATIVE),
        Achievement(CLASSIC_FIRST_GAME, "achievement_classic_first_game_title", "achievement_classic_first_game_desc", 1, AchievementProgressType.BOOLEAN),
        Achievement(QUANTUM_FIRST_GAME, "achievement_quantum_first_game_title", "achievement_quantum_first_game_desc", 1, AchievementProgressType.BOOLEAN),
        Achievement(MERGES_1000, "achievement_merges_1000_title", "achievement_merges_1000_desc", 1_000, AchievementProgressType.CUMULATIVE),
        Achievement(WIN_STREAK_5, "achievement_win_streak_5_title", "achievement_win_streak_5_desc", 5, AchievementProgressType.CUMULATIVE),
        Achievement(BOTH_MODES, "achievement_both_modes_title", "achievement_both_modes_desc", 2, AchievementProgressType.BOOLEAN),
        Achievement(ENTANGLED_COLLAPSE_50, "achievement_entangled_collapse_50_title", "achievement_entangled_collapse_50_desc", 50, AchievementProgressType.CUMULATIVE),
    )
}

object AchievementEvaluator {
    fun evaluate(
        classic: StatsSnapshot,
        quantum: StatsSnapshot,
        daily: DailyChallengeState,
        stored: AchievementsState,
        nowMillis: Long,
    ): AchievementEvaluation {
        val progressById = mapOf(
            AchievementCatalog.LOW_COLLAPSE_100 to quantum.manualCollapseLow,
            AchievementCatalog.HIGH_COLLAPSE_100 to quantum.manualCollapseHigh,
            AchievementCatalog.QUANTUM_2048 to quantum.highestTile.toLong(),
            AchievementCatalog.TILE_4096 to maxOf(classic.highestTile, quantum.highestTile).toLong(),
            AchievementCatalog.DAILY_STREAK_5 to daily.participationStreak.toLong(),
            AchievementCatalog.DAILY_STREAK_30 to daily.participationStreak.toLong(),
            AchievementCatalog.CLASSIC_FIRST_GAME to classic.gamesPlayed.toLong(),
            AchievementCatalog.QUANTUM_FIRST_GAME to quantum.gamesPlayed.toLong(),
            AchievementCatalog.MERGES_1000 to classic.totalMerges + quantum.totalMerges,
            AchievementCatalog.WIN_STREAK_5 to maxOf(classic.longestWinStreak, quantum.longestWinStreak).toLong(),
            AchievementCatalog.BOTH_MODES to listOf(classic.gamesPlayed, quantum.gamesPlayed).count { it > 0 }.toLong(),
            AchievementCatalog.ENTANGLED_COLLAPSE_50 to quantum.entangledCollapseChainCount,
        )
        var completed = stored.completedAtMillis
        val evaluated = AchievementCatalog.all.map { achievement ->
            val current = progressById.getValue(achievement.id).coerceAtMost(achievement.target)
            val alreadyCompleted = completed[achievement.id]
            val completedAt = alreadyCompleted ?: if (current >= achievement.target) nowMillis else null
            if (alreadyCompleted == null && completedAt != null) completed = completed + (achievement.id to completedAt)
            AchievementProgress(achievement, current, completedAt)
        }
        val newlyCompleted = evaluated.filter { it.achievement.id !in stored.completedAtMillis && it.isCompleted }
        return AchievementEvaluation(evaluated, newlyCompleted, AchievementsState(completed))
    }
}

fun observeEvaluatedAchievements(
    statisticsRepository: StatisticsRepository,
    dailyChallengeRepository: DailyChallengeRepository,
    stored: Flow<AchievementsState>,
): Flow<List<AchievementProgress>> = combine(
    statisticsRepository.observeStatistics(GameMode.CLASSIC),
    statisticsRepository.observeStatistics(GameMode.QUANTUM),
    dailyChallengeRepository.observe(),
    stored,
) { classic, quantum, daily, state ->
    AchievementEvaluator.evaluate(classic, quantum, daily, state, System.currentTimeMillis()).achievements
}
