package com.battleheim.quantum2048.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class StatisticsEngineTest {
    private class FixedRandom : RandomProvider {
        override fun nextInt(bound: Int): Int = (bound - 1).coerceAtLeast(0)
        override fun nextDouble(): Double = 0.99
    }

    @Test
    fun collapseRatioUsesLowAndHighCollapseCounts() {
        val state = GameState(lowCollapseCount = 3, highCollapseCount = 1)

        assertEquals(0.75, FusionRules.collapseLowRatio(state), 0.001)
    }

    @Test
    fun collapseUpdatesLowHighCounters() {
        val cells = MutableList<Tile?>(16) { null }.apply {
            this[0] = Tile(1, 1, TileKind.ELECTRON, superpositionValues = listOf(1, 2, 4))
        }
        val state = GameState(cells = cells, score = 500, energy = 100, mode = GameMode.QUANTUM, difficulty = Difficulty.QUANTUM)

        val result = GameEngine(FixedRandom()).collapseSuperposition(state, 1, 2) as SuperpositionResult.Success

        assertEquals(0, result.state.lowCollapseCount)
        assertEquals(1, result.state.highCollapseCount)
    }

    @Test
    fun winStoresAverageEnergySample() {
        val cells = MutableList<Tile?>(16) { null }.apply {
            this[0] = Tile(1, 512, TileKind.ELECTRON)
            this[1] = Tile(2, 512, TileKind.ELECTRON)
        }
        val state = GameState(cells = cells, mode = GameMode.QUANTUM, difficulty = Difficulty.QUANTUM, energy = 44, nextTileId = 10)

        val result = GameEngine(FixedRandom()).move(state, Direction.LEFT)

        assertEquals(GameStatus.WON, result.state.status)
        assertEquals(1, result.state.winEnergySamples)
        assertEquals(result.state.energy.toDouble(), FusionRules.averageWinEnergy(result.state), 0.001)
    }

    @Test
    fun chainMergeCounterAddsExtraMergesOnly() {
        val cells = MutableList<Tile?>(16) { null }.apply {
            this[0] = Tile(1, 1, TileKind.ELECTRON)
            this[1] = Tile(2, 1, TileKind.ELECTRON)
            this[2] = Tile(3, 1, TileKind.PROTON)
            this[3] = Tile(4, 1, TileKind.PROTON)
        }
        val state = GameState(cells = cells, mode = GameMode.QUANTUM, difficulty = Difficulty.QUANTUM, nextTileId = 10)

        val result = GameEngine(FixedRandom()).move(state, Direction.LEFT)

        assertEquals(1, result.state.totalChainMergeCount)
    }
}
