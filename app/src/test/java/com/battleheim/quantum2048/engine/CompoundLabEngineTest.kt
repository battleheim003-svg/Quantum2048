package com.battleheim.quantum2048.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CompoundLabEngineTest {
    private class NoRandom : RandomProvider {
        override fun nextInt(bound: Int) = 0
        override fun nextDouble() = 0.99
    }

    private fun mediumState(vararg tiles: Tile?): GameState =
        GameState(
            cells = tiles.toList() + List(16 - tiles.size) { null },
            mode = GameMode.QUANTUM,
            difficulty = Difficulty.MEDIUM,
            nextTileId = 100,
        )

    @Test
    fun matchingCompoundRemovesTilesWithoutSpawningAndReturnsRecipe() {
        val state = mediumState(
            Tile(1, QuantumSpecies.HYDROGEN.scoreValue, species = QuantumSpecies.HYDROGEN),
            Tile(2, QuantumSpecies.HYDROGEN.scoreValue, species = QuantumSpecies.HYDROGEN),
            Tile(3, QuantumSpecies.OXYGEN.scoreValue, species = QuantumSpecies.OXYGEN),
        )

        val result = GameEngine(NoRandom()).combineCompound(state, listOf(1, 2, 3)) as CompoundResult.Success

        assertEquals("H2O", result.recipe.output.symbol)
        assertEquals(0, result.state.cells.count { it != null })
        assertEquals(100, result.state.nextTileId)
        assertEquals(0, result.state.moveCount)
        assertEquals(result.recipe.output.scoreValue.toLong(), result.state.score)
    }

    @Test
    fun unrelatedCompoundLeavesStateUnchanged() {
        val state = mediumState(
            Tile(1, QuantumSpecies.GOLD.scoreValue, species = QuantumSpecies.GOLD),
            Tile(2, QuantumSpecies.NEON.scoreValue, species = QuantumSpecies.NEON),
        )

        val result = GameEngine(NoRandom()).combineCompound(state, listOf(1, 2)) as CompoundResult.Failure

        assertEquals(CompoundFailure.NO_RECIPE, result.reason)
        assertEquals(state, result.state)
    }

    @Test
    fun easyCannotUseCompoundLab() {
        val state = GameState(cells = List(16) { null }, difficulty = Difficulty.EASY, mode = GameMode.CLASSIC)

        val result = GameEngine(NoRandom()).combineCompound(state, listOf(1, 2)) as CompoundResult.Failure

        assertEquals(CompoundFailure.LAB_DISABLED, result.reason)
    }

    @Test
    fun hardCompoundSpendsConfiguredEnergyAndAppliesReward() {
        val state = GameState(
            cells = listOf(
                Tile(1, QuantumSpecies.HYDROGEN.scoreValue, species = QuantumSpecies.HYDROGEN),
                Tile(2, QuantumSpecies.HYDROGEN.scoreValue, species = QuantumSpecies.HYDROGEN),
                Tile(3, QuantumSpecies.OXYGEN.scoreValue, species = QuantumSpecies.OXYGEN),
            ) + List(13) { null },
            mode = GameMode.QUANTUM,
            difficulty = Difficulty.HARD,
            quantumEnergy = 20,
        )

        val result = GameEngine(NoRandom()).combineCompound(state, listOf(1, 2, 3)) as CompoundResult.Success

        assertTrue(result.energySpent > 0)
        assertEquals(16, result.state.quantumEnergy)
    }
}
