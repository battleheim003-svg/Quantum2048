package com.battleheim.quantum2048.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DifficultyEngineTest {
    private class ScriptedRandom(
        private val ints: ArrayDeque<Int> = ArrayDeque(),
        private val doubles: ArrayDeque<Double> = ArrayDeque(),
    ) : RandomProvider {
        override fun nextInt(bound: Int) = (ints.removeFirstOrNull() ?: 0).mod(bound)
        override fun nextDouble() = doubles.removeFirstOrNull() ?: 0.99
    }

    private val balance = QuantumBalance()

    @Test
    fun easyIsClassic2048WithoutParticlesOrEnergy() {
        val state = GameState(
            cells = listOf(Tile(1, 2), Tile(2, 2)) + List(14) { null },
            difficulty = Difficulty.EASY,
            mode = GameMode.CLASSIC,
            nextTileId = 10,
        )

        val result = GameEngine(ScriptedRandom(), balance).move(state, Direction.LEFT)

        assertEquals(4, result.state.cells[0]?.value)
        assertNull(result.state.cells[0]?.species)
        assertEquals(0, result.state.quantumEnergy)
    }

    @Test
    fun mediumSpawnsStableParticlesAndDisablesCollapseEnergy() {
        val engine = GameEngine(ScriptedRandom(doubles = ArrayDeque(listOf(0.0, 0.0))), balance)
        val state = engine.newGame(Difficulty.MEDIUM)
        val tile = state.cells.filterNotNull().first()

        assertEquals(Difficulty.MEDIUM, state.difficulty)
        assertNotNull(tile.species)
        assertFalse(tile.isUnstable)
        assertEquals(0, state.quantumEnergy)
        assertEquals(CollapseFailure.NOT_QUANTUM, engine.collapse(state, tile.id, tile.value).let { (it as CollapseResult.Failure).reason })
    }

    @Test
    fun hardUsesFullFusionChainAndEnergyWithoutCollapse() {
        val cells = MutableList<Tile?>(16) { null }.apply {
            this[0] = Tile(1, QuantumSpecies.IRON.scoreValue, species = QuantumSpecies.IRON)
            this[1] = Tile(2, QuantumSpecies.IRON.scoreValue, species = QuantumSpecies.IRON)
        }
        val state = GameState(cells = cells, mode = GameMode.QUANTUM, difficulty = Difficulty.HARD, quantumEnergy = 20, nextTileId = 20)

        val result = GameEngine(ScriptedRandom(), balance).move(state, Direction.LEFT)

        assertEquals(QuantumSpecies.GOLD, result.state.cells[0]?.species)
        assertTrue(result.energyGained > 0)
        assertEquals(CollapseFailure.NOT_QUANTUM, GameEngine(ScriptedRandom(), balance).collapse(state, 1, QuantumSpecies.IRON.scoreValue).let { (it as CollapseResult.Failure).reason })
    }

    @Test
    fun quantumKeepsUnresolvedSpawnCollapseAndEnergyRules() {
        val engine = GameEngine(
            ScriptedRandom(doubles = ArrayDeque(listOf(0.0, 0.0, 0.0, 0.0))),
            QuantumBalance(quantumSpawnChance = 1.0, autoCollapseChance = 0.0),
        )
        val state = engine.newGame(Difficulty.QUANTUM)
        val tile = state.cells.filterNotNull().first { it.isUnstable }

        assertEquals(Difficulty.QUANTUM, state.difficulty)
        assertTrue(tile.isUnstable)
        assertEquals(CollapseResult.Success::class, engine.collapse(state, tile.id, tile.value)::class)
    }

    @Test
    fun mediumFusionStopsAtNeon() {
        val cells = MutableList<Tile?>(16) { null }.apply {
            this[0] = Tile(1, QuantumSpecies.NEON.scoreValue, species = QuantumSpecies.NEON)
            this[1] = Tile(2, QuantumSpecies.NEON.scoreValue, species = QuantumSpecies.NEON)
        }
        val state = GameState(cells = cells, mode = GameMode.QUANTUM, difficulty = Difficulty.MEDIUM, nextTileId = 20)

        val result = GameEngine(ScriptedRandom(), balance).move(state, Direction.LEFT)

        assertEquals(0, result.mergeCount)
        assertEquals(2, result.state.cells.count { it != null })
    }

    @Test
    fun recipeAccessIsExplicitPerDifficulty() {
        assertFalse(balance.rulesFor(Difficulty.EASY).compoundLabEnabled)
        assertEquals("H2O", Chemistry.findRecipe(
            listOf(ElementTile(QuantumSpecies.HYDROGEN), ElementTile(QuantumSpecies.HYDROGEN), ElementTile(QuantumSpecies.OXYGEN)),
            balance.compoundRecipes,
            balance.rulesFor(Difficulty.MEDIUM).allowedRecipeLevel,
        )?.output?.symbol)
        assertNull(Chemistry.findRecipe(
            listOf(ElementTile(QuantumSpecies.SILICON), ElementTile(QuantumSpecies.OXYGEN), ElementTile(QuantumSpecies.OXYGEN)),
            balance.compoundRecipes,
            balance.rulesFor(Difficulty.HARD).allowedRecipeLevel,
        ))
        assertEquals("SiO2", Chemistry.findRecipe(
            listOf(ElementTile(QuantumSpecies.SILICON), ElementTile(QuantumSpecies.OXYGEN), ElementTile(QuantumSpecies.OXYGEN)),
            balance.compoundRecipes,
            balance.rulesFor(Difficulty.QUANTUM).allowedRecipeLevel,
        )?.output?.symbol)
    }
}
