package com.battleheim.quantum2048.engine

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AchievementEngineTest {
    private class FixedRandom : RandomProvider {
        override fun nextInt(bound: Int): Int = (bound - 1).coerceAtLeast(0)
        override fun nextDouble(): Double = 0.99
    }

    @Test
    fun oneHundredSuccessfulCollapsesUnlocksCollapseCentury() {
        val cells = MutableList<Tile?>(16) { null }.apply {
            this[0] = Tile(1, 1, TileKind.ELECTRON, superpositionValues = listOf(1, 2, 4))
        }
        val state = GameState(
            cells = cells,
            score = 500,
            energy = 100,
            mode = GameMode.QUANTUM,
            difficulty = Difficulty.QUANTUM,
            successfulCollapseCount = 99,
        )

        val result = GameEngine(FixedRandom()).collapseSuperposition(state, 1, 0) as SuperpositionResult.Success

        assertTrue(FusionRules.achievementCollapseCentury in result.state.unlockedAchievements)
    }

    @Test
    fun resolved2048WinUnlocksResolvedAndNoUndoAchievements() {
        val cells = MutableList<Tile?>(16) { null }.apply {
            this[0] = Tile(1, 512, TileKind.ELECTRON)
            this[1] = Tile(2, 512, TileKind.ELECTRON)
        }
        val state = GameState(cells = cells, mode = GameMode.QUANTUM, difficulty = Difficulty.QUANTUM, nextTileId = 10)

        val result = GameEngine(FixedRandom()).move(state, Direction.LEFT)

        assertTrue(FusionRules.achievementResolved2048 in result.state.unlockedAchievements)
        assertTrue(FusionRules.achievementNoUndoWin in result.state.unlockedAchievements)
    }

    @Test
    fun undoUsedPreventsNoUndoWinAchievement() {
        val cells = MutableList<Tile?>(16) { null }.apply {
            this[0] = Tile(1, 512, TileKind.ELECTRON)
            this[1] = Tile(2, 512, TileKind.ELECTRON)
        }
        val state = GameState(cells = cells, mode = GameMode.QUANTUM, difficulty = Difficulty.QUANTUM, nextTileId = 10, usedUndo = true)

        val result = GameEngine(FixedRandom()).move(state, Direction.LEFT)

        assertFalse(FusionRules.achievementNoUndoWin in result.state.unlockedAchievements)
        assertTrue(FusionRules.achievementResolved2048 in result.state.unlockedAchievements)
    }
}
