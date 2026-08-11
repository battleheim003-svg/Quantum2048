package com.battleheim.quantum2048.domain

import com.battleheim.quantum2048.engine.Difficulty
import com.battleheim.quantum2048.engine.GameState
import com.battleheim.quantum2048.engine.GameStatus
import com.battleheim.quantum2048.engine.QuantumElement
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Serializable
data class LevelCatalog(
    val schemaVersion: Int = 1,
    val catalogVersion: String = "periodic_path_v1",
    val zones: List<ZoneDefinition> = emptyList(),
) {
    val levels: List<LevelDefinition> get() = zones.flatMap { it.levels }
    fun findLevel(levelId: String): LevelDefinition? = levels.firstOrNull { it.id == levelId }
    fun zoneFor(levelId: String): ZoneDefinition? = zones.firstOrNull { zone -> zone.levels.any { it.id == levelId } }
}

@Serializable
data class ZoneDefinition(
    val id: String,
    val title: String,
    val subtitle: String,
    val unlockAfterLevelId: String? = null,
    val levels: List<LevelDefinition> = emptyList(),
)

@Serializable
data class LevelDefinition(
    val id: String,
    val zoneId: String,
    val indexInZone: Int,
    val title: String,
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val boardSize: Int = 4,
    val seed: Long? = null,
    val moveLimit: Int? = null,
    val startingEnergy: Int? = null,
    val goals: List<LevelGoalDefinition> = emptyList(),
    val starRules: StarRules = StarRules(),
)

@Serializable
data class LevelGoalDefinition(
    val id: String,
    val type: LevelGoalType,
    val element: QuantumElement? = null,
    val score: Long? = null,
    val count: Int? = null,
    val moves: Int? = null,
    val energy: Int? = null,
)

@Serializable
enum class LevelGoalType {
    REACH_ELEMENT,
    REACH_SCORE,
    SURVIVE_MOVES,
    CHAIN_MERGES,
    ENERGY_AT_LEAST,
    CLEAR_ELEMENT,
}

@Serializable
data class StarRules(
    val twoStarMoveLimit: Int? = null,
    val threeStarMoveLimit: Int? = null,
    val requireNoUndoForThreeStars: Boolean = true,
)

@Serializable
data class PlayerProgress(
    val schemaVersion: Int = 1,
    val catalogVersion: String = "periodic_path_v1",
    val unlockedLevelIds: Set<String> = setOf("z01-l01"),
    val completions: Map<String, LevelCompletion> = emptyMap(),
    val mercy: Map<String, MercyState> = emptyMap(),
    val lastPlayedLevelId: String? = null,
) {
    val completedLevelCount: Int get() = completions.size
    val totalStars: Int get() = completions.values.sumOf { it.bestStars }
    fun isUnlocked(levelId: String): Boolean = levelId in unlockedLevelIds
    fun completion(levelId: String): LevelCompletion? = completions[levelId]
    fun mercyFor(levelId: String): MercyState = mercy[levelId] ?: MercyState()
}

@Serializable
data class LevelCompletion(
    val bestStars: Int = 0,
    val bestScore: Long = 0,
    val bestMoves: Int = Int.MAX_VALUE,
    val completedAtMillis: Long = 0,
)

@Serializable
data class MercyState(
    val consecutiveFailures: Int = 0,
    val assistMoveBonus: Int = 0,
) {
    val active: Boolean get() = assistMoveBonus > 0
}

data class GoalProgress(
    val label: String,
    val current: Int,
    val target: Int,
    val complete: Boolean,
)

enum class LevelRunStatus { ACTIVE, COMPLETE, FAILED }

data class LevelRunUiState(
    val levelId: String,
    val title: String,
    val zoneTitle: String,
    val goals: List<GoalProgress>,
    val movesRemaining: Int?,
    val stars: Int = 0,
    val mercy: MercyState = MercyState(),
    val status: LevelRunStatus = LevelRunStatus.ACTIVE,
)

interface LevelCatalogRepository {
    suspend fun catalog(): LevelCatalog
}

interface LevelProgressRepository {
    fun observe(): Flow<PlayerProgress>
    suspend fun save(progress: PlayerProgress)
    suspend fun clear()
}

object LevelGoalTracker {
    fun evaluate(level: LevelDefinition, state: GameState, mercy: MercyState = MercyState()): LevelRunUiState {
        val effectiveMoveLimit = level.moveLimit?.plus(mercy.assistMoveBonus)
        val goals = level.goals.map { goal ->
            val target = goalTarget(goal)
            val current = goalCurrent(goal, state)
            GoalProgress(
                label = goalLabel(goal),
                current = current.coerceAtMost(target),
                target = target,
                complete = current >= target,
            )
        }
        val complete = goals.isNotEmpty() && goals.all { it.complete }
        val failedByMoves = effectiveMoveLimit != null && state.moveCount >= effectiveMoveLimit && !complete
        val failedByBoard = state.status == GameStatus.LOST && !complete
        val status = when {
            complete -> LevelRunStatus.COMPLETE
            failedByMoves || failedByBoard -> LevelRunStatus.FAILED
            else -> LevelRunStatus.ACTIVE
        }
        return LevelRunUiState(
            levelId = level.id,
            title = level.title,
            zoneTitle = level.zoneId,
            goals = goals,
            movesRemaining = effectiveMoveLimit?.minus(state.moveCount)?.coerceAtLeast(0),
            stars = if (complete) starsFor(level, state) else 0,
            mercy = mercy,
            status = status,
        )
    }

    fun starsFor(level: LevelDefinition, state: GameState): Int {
        var stars = 1
        if (level.starRules.twoStarMoveLimit == null || state.moveCount <= level.starRules.twoStarMoveLimit) stars++
        val withinThreeStarLimit = level.starRules.threeStarMoveLimit == null || state.moveCount <= level.starRules.threeStarMoveLimit
        val noUndoOk = !level.starRules.requireNoUndoForThreeStars || !state.usedUndo
        if (withinThreeStarLimit && noUndoOk) stars++
        return stars.coerceIn(1, 3)
    }

    private fun goalTarget(goal: LevelGoalDefinition): Int = when (goal.type) {
        LevelGoalType.REACH_ELEMENT -> goal.element?.rank ?: 1
        LevelGoalType.REACH_SCORE -> (goal.score ?: 0L).toInt()
        LevelGoalType.SURVIVE_MOVES -> goal.moves ?: 0
        LevelGoalType.CHAIN_MERGES -> goal.count ?: 0
        LevelGoalType.ENERGY_AT_LEAST -> goal.energy ?: 0
        LevelGoalType.CLEAR_ELEMENT -> 1
    }

    private fun goalCurrent(goal: LevelGoalDefinition, state: GameState): Int = when (goal.type) {
        LevelGoalType.REACH_ELEMENT -> state.cells.mapNotNull { it?.element?.rank }.maxOrNull() ?: 0
        LevelGoalType.REACH_SCORE -> state.score.toInt()
        LevelGoalType.SURVIVE_MOVES -> state.moveCount
        LevelGoalType.CHAIN_MERGES -> state.totalChainMergeCount
        LevelGoalType.ENERGY_AT_LEAST -> state.energy
        LevelGoalType.CLEAR_ELEMENT -> if (state.moveCount > 0 && state.cells.none { it?.element == goal.element }) 1 else 0
    }

    private fun goalLabel(goal: LevelGoalDefinition): String = when (goal.type) {
        LevelGoalType.REACH_ELEMENT -> "Reach ${goal.element?.symbol ?: "element"}"
        LevelGoalType.REACH_SCORE -> "Score ${goal.score ?: 0}"
        LevelGoalType.SURVIVE_MOVES -> "Survive ${goal.moves ?: 0} moves"
        LevelGoalType.CHAIN_MERGES -> "Chain merges ${goal.count ?: 0}"
        LevelGoalType.ENERGY_AT_LEAST -> "Energy ${goal.energy ?: 0}%"
        LevelGoalType.CLEAR_ELEMENT -> "Clear ${goal.element?.symbol ?: "target"}"
    }
}

object PeriodicPathProgression {
    fun recordCompletion(catalog: LevelCatalog, progress: PlayerProgress, level: LevelDefinition, state: GameState): PlayerProgress {
        val stars = LevelGoalTracker.starsFor(level, state)
        val previous = progress.completion(level.id)
        val best = LevelCompletion(
            bestStars = maxOf(previous?.bestStars ?: 0, stars),
            bestScore = maxOf(previous?.bestScore ?: 0L, state.score),
            bestMoves = minOf(previous?.bestMoves ?: Int.MAX_VALUE, state.moveCount),
            completedAtMillis = System.currentTimeMillis(),
        )
        return progress.copy(
            unlockedLevelIds = progress.unlockedLevelIds + nextUnlocks(catalog, level.id),
            completions = progress.completions + (level.id to best),
            mercy = progress.mercy - level.id,
            lastPlayedLevelId = level.id,
        )
    }

    fun recordFailure(progress: PlayerProgress, levelId: String): PlayerProgress {
        val current = progress.mercyFor(levelId)
        val failures = current.consecutiveFailures + 1
        val bonus = when {
            failures >= 6 -> 4
            failures >= 3 -> 2
            else -> 0
        }
        return progress.copy(
            mercy = progress.mercy + (levelId to MercyState(failures, bonus)),
            lastPlayedLevelId = levelId,
        )
    }

    private fun nextUnlocks(catalog: LevelCatalog, levelId: String): Set<String> {
        val flat = catalog.levels
        val index = flat.indexOfFirst { it.id == levelId }
        return if (index >= 0 && index + 1 < flat.size) setOf(flat[index + 1].id) else emptySet()
    }
}
