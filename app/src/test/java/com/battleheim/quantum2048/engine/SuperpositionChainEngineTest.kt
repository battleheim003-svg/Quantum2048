package com.battleheim.quantum2048.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class SuperpositionChainEngineTest {
    private class ScriptedRandom(
        private val ints: MutableList<Int> = mutableListOf(0),
        private val doubles: MutableList<Double> = mutableListOf(0.0),
    ) : RandomProvider {
        override fun nextInt(bound: Int): Int = (if (ints.isEmpty()) 0 else ints.removeAt(0)).mod(bound)
        override fun nextDouble(): Double = if (doubles.isEmpty()) 0.99 else doubles.removeAt(0)
    }

    @Test
    fun quantumSpawnCanCreateThreeStateTileAfterScoreThreshold() {
        val state = GameState(
            cells = List(16) { null },
            score = FusionRules.superpositionScoreThreshold,
            mode = GameMode.QUANTUM,
            difficulty = Difficulty.QUANTUM,
        )
        val engine = GameEngine(ScriptedRandom(doubles = mutableListOf(0.0, 0.0, 0.99)))

        val spawned = engine.spawn(state).cells.filterNotNull().single()

        assertEquals(listOf(1, 2, 4), spawned.superpositionValues)
    }

    @Test
    fun superpositionTilesDoNotMergeBeforeCollapse() {
        val a = Tile(1, 1, TileKind.ELECTRON, superpositionValues = listOf(1, 2, 4))
        val b = Tile(2, 1, TileKind.ELECTRON)

        assertNull(FusionRules.mergeProduct(a, b))
    }

    @Test
    fun collapseCanResolveEachOfThreeValuesWithConfiguredCost() {
        FusionRules.superpositionCollapseEnergyCosts.forEachIndexed { index, cost ->
            val cells = MutableList<Tile?>(16) { null }.apply {
                this[0] = Tile(1, 1, TileKind.ELECTRON, superpositionValues = listOf(1, 2, 4))
            }
            val state = GameState(cells = cells, score = 500, energy = 100, mode = GameMode.QUANTUM, difficulty = Difficulty.QUANTUM)

            val result = GameEngine(ScriptedRandom()).collapseSuperposition(state, 1, index) as SuperpositionResult.Success

            assertEquals(listOf(1, 2, 4)[index], result.state.cells[0]?.value)
            assertEquals(emptyList<Int>(), result.state.cells[0]?.superpositionValues)
            assertEquals(500L, result.state.score)
            assertEquals(100 - cost, result.state.energy)
        }
    }

    @Test
    fun collapseLowAndHighUseDistinctAnimationKinds() {
        fun state(): GameState {
            val cells = MutableList<Tile?>(16) { null }.apply {
                this[0] = Tile(1, 1, TileKind.ELECTRON, superpositionValues = listOf(1, 2, 4))
            }
            return GameState(cells = cells, score = 500, energy = 100, mode = GameMode.QUANTUM, difficulty = Difficulty.QUANTUM)
        }

        val low = GameEngine(ScriptedRandom()).collapseSuperposition(state(), 1, 0) as SuperpositionResult.Success
        val high = GameEngine(ScriptedRandom()).collapseSuperposition(state(), 1, 2) as SuperpositionResult.Success

        assertEquals(MoveAnimationKind.COLLAPSE_LOW, low.animation.kind)
        assertEquals(MoveAnimationKind.COLLAPSE_HIGH, high.animation.kind)
    }

    @Test
    fun collapseFailsClosedWhenScoreIsInsufficient() {
        val cells = MutableList<Tile?>(16) { null }.apply {
            this[0] = Tile(1, 1, TileKind.ELECTRON, superpositionValues = listOf(1, 2, 4))
        }
        val state = GameState(cells = cells, energy = FusionRules.superpositionCollapseEnergyCosts[0] - 1, mode = GameMode.QUANTUM, difficulty = Difficulty.QUANTUM)

        val result = GameEngine(ScriptedRandom()).collapseSuperposition(state, 1, 0) as SuperpositionResult.Failure

        assertEquals(SuperpositionFailure.INSUFFICIENT_SCORE, result.reason)
        assertSame(state, result.state)
    }
}
