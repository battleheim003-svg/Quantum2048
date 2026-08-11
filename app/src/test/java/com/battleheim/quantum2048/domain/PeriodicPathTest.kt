package com.battleheim.quantum2048.domain

import com.battleheim.quantum2048.engine.Difficulty
import com.battleheim.quantum2048.engine.GameState
import com.battleheim.quantum2048.engine.QuantumElement
import com.battleheim.quantum2048.engine.Tile
import com.battleheim.quantum2048.engine.TileKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PeriodicPathTest {
    @Test
    fun reachElementGoalCompletesAndAwardsStars() {
        val level = sampleLevel()
        val state = GameState(
            difficulty = Difficulty.MEDIUM,
            moveCount = 11,
            cells = listOf(
                Tile(1, 2, TileKind.ELEMENT, QuantumElement.HYDROGEN),
                Tile(2, 4, TileKind.ELEMENT, QuantumElement.HELIUM),
                null,
                null,
            ),
            size = 2,
        )

        val result = LevelGoalTracker.evaluate(level, state)

        assertEquals(LevelRunStatus.COMPLETE, result.status)
        assertEquals(2, result.stars)
        assertTrue(result.goals.all { it.complete })
    }

    @Test
    fun repeatedFailuresUnlockMercyMoveBonus() {
        val progress = (1..3).fold(PlayerProgress()) { current, _ ->
            PeriodicPathProgression.recordFailure(current, "z01-l01")
        }

        assertEquals(3, progress.mercyFor("z01-l01").consecutiveFailures)
        assertEquals(2, progress.mercyFor("z01-l01").assistMoveBonus)
    }

    @Test
    fun completionUnlocksNextLevelAndKeepsBestStars() {
        val level = sampleLevel()
        val next = level.copy(id = "z01-l02", indexInZone = 2)
        val catalog = LevelCatalog(
            zones = listOf(
                ZoneDefinition(id = "z01", title = "Alkali Spark", subtitle = "Test", levels = listOf(level, next)),
            ),
        )
        val state = GameState(difficulty = Difficulty.MEDIUM, moveCount = 8, score = 500)

        val progress = PeriodicPathProgression.recordCompletion(catalog, PlayerProgress(), level, state)

        assertTrue("z01-l02" in progress.unlockedLevelIds)
        assertEquals(3, progress.completion("z01-l01")?.bestStars)
    }

    private fun sampleLevel(): LevelDefinition = LevelDefinition(
        id = "z01-l01",
        zoneId = "z01",
        indexInZone = 1,
        title = "Alkali Spark 1",
        difficulty = Difficulty.MEDIUM,
        boardSize = 2,
        moveLimit = 18,
        goals = listOf(
            LevelGoalDefinition(id = "reach_he", type = LevelGoalType.REACH_ELEMENT, element = QuantumElement.HELIUM),
        ),
        starRules = StarRules(twoStarMoveLimit = 14, threeStarMoveLimit = 10),
    )
}
