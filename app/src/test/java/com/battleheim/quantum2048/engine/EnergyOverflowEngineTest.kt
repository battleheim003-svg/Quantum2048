package com.battleheim.quantum2048.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class EnergyOverflowEngineTest {
    private class FixedRandom : RandomProvider {
        override fun nextInt(bound: Int): Int = (bound - 1).coerceAtLeast(0)
        override fun nextDouble(): Double = 0.99
    }

    @Test
    fun quantumMergeAddsEnergyWithoutOverflowWhenBelowCap() {
        val cells = MutableList<Tile?>(16) { null }.apply {
            this[0] = Tile(1, 1, TileKind.ELECTRON)
            this[1] = Tile(2, 1, TileKind.ELECTRON)
        }
        val state = GameState(cells = cells, mode = GameMode.QUANTUM, difficulty = Difficulty.QUANTUM, energy = 30, nextTileId = 10)

        val result = GameEngine(FixedRandom()).move(state, Direction.LEFT)

        assertEquals(36, result.state.energy)
        assertEquals(0, result.energyOverflowBonus)
    }

    @Test
    fun chainMergeAddsBaseAndChainEnergy() {
        val cells = MutableList<Tile?>(16) { null }.apply {
            this[0] = Tile(1, 1, TileKind.ELECTRON)
            this[1] = Tile(2, 1, TileKind.ELECTRON)
            this[2] = Tile(3, 1, TileKind.PROTON)
            this[3] = Tile(4, 1, TileKind.PROTON)
        }
        val state = GameState(cells = cells, mode = GameMode.QUANTUM, difficulty = Difficulty.QUANTUM, energy = 30, nextTileId = 10)

        val result = GameEngine(FixedRandom()).move(state, Direction.LEFT)

        assertEquals(45, result.state.energy)
    }

    @Test
    fun overflowEnergyConvertsToScoreBonus() {
        val cells = MutableList<Tile?>(16) { null }.apply {
            this[0] = Tile(1, 1, TileKind.ELECTRON)
            this[1] = Tile(2, 1, TileKind.ELECTRON)
        }
        val state = GameState(cells = cells, score = 10, mode = GameMode.QUANTUM, difficulty = Difficulty.QUANTUM, energy = 98, nextTileId = 10)

        val result = GameEngine(FixedRandom()).move(state, Direction.LEFT)

        assertEquals(FusionRules.maxEnergy, result.state.energy)
        assertEquals(16, result.energyOverflowBonus)
        assertEquals(30L, result.state.score)
    }

    @Test
    fun classicMergeDoesNotUseQuantumEnergyOverflow() {
        val cells = MutableList<Tile?>(16) { null }.apply {
            this[0] = Tile(1, 2)
            this[1] = Tile(2, 2)
        }
        val state = GameState(cells = cells, score = 10, mode = GameMode.CLASSIC, difficulty = Difficulty.EASY, energy = 0, nextTileId = 10)

        val result = GameEngine(FixedRandom()).move(state, Direction.LEFT)

        assertEquals(0, result.state.energy)
        assertEquals(0, result.energyOverflowBonus)
        assertEquals(14L, result.state.score)
    }
}
