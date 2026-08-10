package com.battleheim.quantum2048.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameModesEngineTest {
    private class FixedRandom : RandomProvider {
        override fun nextInt(bound: Int): Int = (bound - 1).coerceAtLeast(0)
        override fun nextDouble(): Double = 0.99
    }

    @Test
    fun zenStartsWithRelaxedEnergyPoolAndIndependentDifficulty() {
        val state = GameEngine(FixedRandom()).newGame(Difficulty.ZEN, 4)

        assertEquals(GameMode.QUANTUM, state.mode)
        assertEquals(Difficulty.ZEN, state.difficulty)
        assertEquals(FusionRules.zenEnergy, state.energy)
    }

    @Test
    fun hardcoreStartsWithZeroEnergyAndDisablesUndo() {
        val state = GameEngine(FixedRandom()).newGame(Difficulty.HARDCORE, 4)

        assertEquals(GameMode.QUANTUM, state.mode)
        assertEquals(0, state.energy)
        assertFalse(FusionRules.isUndoEnabled(state.difficulty))
    }

    @Test
    fun puzzleModeHasAtLeastThreeDesignedBoards() {
        assertTrue(FusionRules.puzzleDefinitions.size >= 3)
        val boards = FusionRules.puzzleDefinitions.indices.map { index ->
            GameEngine(FixedRandom()).puzzleGame(4, index).cells
        }.toSet()

        assertEquals(FusionRules.puzzleDefinitions.size, boards.size)
    }

    @Test
    fun puzzleSelectionIsDeterministicForSeededRandomProvider() {
        val first = GameEngine(SeededRandomProvider(7)).newGame(Difficulty.PUZZLE, 4)
        val second = GameEngine(SeededRandomProvider(7)).newGame(Difficulty.PUZZLE, 4)

        assertEquals(first.cells, second.cells)
    }

    @Test
    fun puzzleCanBeSolvedByReachingTargetWithinMoveLimitWithoutSpawning() {
        val state = GameEngine(FixedRandom()).puzzleGame(4, 0)

        val result = GameEngine(FixedRandom()).move(state, Direction.LEFT)

        assertEquals(GameStatus.WON, result.state.status)
        assertEquals(2, result.state.cells.count { it != null })
    }

    @Test
    fun puzzleLosesAfterMoveLimitWhenTargetIsNotReached() {
        val cells = MutableList<Tile?>(16) { null }.apply {
            this[0] = Tile(1, 1, TileKind.ELECTRON)
            this[3] = Tile(2, 1, TileKind.PROTON)
        }
        val state = GameState(
            cells = cells,
            mode = GameMode.QUANTUM,
            difficulty = Difficulty.PUZZLE,
            moveCount = FusionRules.puzzleMoveLimit - 1,
            nextTileId = 3,
        )

        val result = GameEngine(FixedRandom()).move(state, Direction.LEFT)

        assertTrue(result.changed)
        assertEquals(GameStatus.LOST, result.state.status)
    }
}
