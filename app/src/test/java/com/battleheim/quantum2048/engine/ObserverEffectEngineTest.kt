package com.battleheim.quantum2048.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class ObserverEffectEngineTest {
    private class FixedRandom : RandomProvider {
        override fun nextInt(bound: Int): Int = 0
        override fun nextDouble(): Double = 0.99
    }

    @Test
    fun observePreviewDoesNotChangeTileStateButSpendsScore() {
        val tile = Tile(1, 1, TileKind.ELECTRON, superpositionValues = listOf(1, 2, 4))
        val cells = MutableList<Tile?>(16) { null }.apply { this[0] = tile }
        val state = GameState(cells = cells, score = 100, energy = 30, mode = GameMode.QUANTUM, difficulty = Difficulty.QUANTUM)

        val result = GameEngine(FixedRandom()).observeSuperposition(state, 1) as ObserverResult.Success

        assertEquals(2, result.previewValue)
        assertEquals(100L, result.state.score)
        assertEquals(30 - FusionRules.observerPreviewEnergyCost, result.state.energy)
        assertEquals(tile, result.state.cells[0])
    }

    @Test
    fun observeFailsClosedWhenScoreIsInsufficient() {
        val cells = MutableList<Tile?>(16) { null }.apply {
            this[0] = Tile(1, 1, TileKind.ELECTRON, superpositionValues = listOf(1, 2, 4))
        }
        val state = GameState(cells = cells, energy = FusionRules.observerPreviewEnergyCost - 1, mode = GameMode.QUANTUM, difficulty = Difficulty.QUANTUM)

        val result = GameEngine(FixedRandom()).observeSuperposition(state, 1) as ObserverResult.Failure

        assertEquals(ObserverFailure.INSUFFICIENT_SCORE, result.reason)
        assertSame(state, result.state)
    }

    @Test
    fun observeFailsClosedForStableTile() {
        val cells = MutableList<Tile?>(16) { null }.apply {
            this[0] = Tile(1, 1, TileKind.ELECTRON)
        }
        val state = GameState(cells = cells, score = 100, mode = GameMode.QUANTUM, difficulty = Difficulty.QUANTUM)

        val result = GameEngine(FixedRandom()).observeSuperposition(state, 1) as ObserverResult.Failure

        assertEquals(ObserverFailure.NOT_SUPERPOSITION, result.reason)
        assertSame(state, result.state)
    }
}
