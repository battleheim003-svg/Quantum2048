package com.battleheim.quantum2048.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class QuantumEngineTest {
    private class ScriptedRandom(
        private val ints: ArrayDeque<Int> = ArrayDeque(),
        private val doubles: ArrayDeque<Double> = ArrayDeque(),
    ) : RandomProvider {
        override fun nextInt(bound: Int) = (ints.removeFirstOrNull() ?: 0).mod(bound)
        override fun nextDouble() = doubles.removeFirstOrNull() ?: 0.99
    }

    private val balance = QuantumBalance(quantumSpawnChance = 0.0, autoCollapseChance = 0.0)

    private fun quantumState(
        cells: List<Tile?> = List(16) { null },
        energy: Int = 50,
    ) = GameState(cells = cells, mode = GameMode.QUANTUM, quantumEnergy = energy, nextTileId = 100)

    @Test
    fun quantum_spawn_can_create_unresolved_particle_tile() {
        val engine = GameEngine(ScriptedRandom(doubles = ArrayDeque(listOf(0.0, 0.0))), QuantumBalance(quantumSpawnChance = 1.0, autoCollapseChance = 0.0))
        val tile = engine.spawn(quantumState()).cells.filterNotNull().single()

        assertEquals(QuantumSpecies.ELECTRON, tile.species)
        assertEquals(listOf(QuantumSpecies.ELECTRON, QuantumSpecies.PROTON), tile.speciesOptions())
    }

    @Test
    fun quantum_spawn_can_create_proton() {
        val engine = GameEngine(ScriptedRandom(doubles = ArrayDeque(listOf(0.99, 0.99))), balance)
        val tile = engine.spawn(quantumState()).cells.filterNotNull().single()

        assertEquals(QuantumSpecies.PROTON, tile.species)
        assertFalse(tile.isUnstable)
    }

    @Test
    fun classic_mode_never_spawns_particle_tile() {
        val engine = GameEngine(ScriptedRandom(doubles = ArrayDeque(listOf(0.0))), balance)
        val tile = engine.spawn(GameState()).cells.filterNotNull().single()

        assertEquals(null, tile.species)
    }

    @Test
    fun electron_and_proton_synthesize_hydrogen() {
        val cells = MutableList<Tile?>(16) { null }.apply {
            this[0] = Tile(1, QuantumSpecies.ELECTRON.scoreValue, species = QuantumSpecies.ELECTRON)
            this[1] = Tile(2, QuantumSpecies.PROTON.scoreValue, species = QuantumSpecies.PROTON)
        }

        val result = GameEngine(ScriptedRandom(), balance).move(quantumState(cells), Direction.LEFT)

        assertEquals(1, result.mergeCount)
        assertEquals(QuantumSpecies.HYDROGEN, result.state.cells[0]?.species)
    }

    @Test
    fun paired_hydrogen_synthesizes_helium() {
        val cells = MutableList<Tile?>(16) { null }.apply {
            this[0] = Tile(1, QuantumSpecies.HYDROGEN.scoreValue, species = QuantumSpecies.HYDROGEN)
            this[1] = Tile(2, QuantumSpecies.HYDROGEN.scoreValue, species = QuantumSpecies.HYDROGEN)
        }

        val result = GameEngine(ScriptedRandom(), balance).move(quantumState(cells), Direction.LEFT)

        assertEquals(QuantumSpecies.HELIUM, result.state.cells[0]?.species)
        assertEquals(QuantumSpecies.HELIUM.scoreValue, result.gainedScore)
    }

    @Test
    fun merges_generate_capped_energy_with_chain_bonus() {
        val cells = MutableList<Tile?>(16) { null }.apply {
            this[0] = Tile(1, QuantumSpecies.HYDROGEN.scoreValue, species = QuantumSpecies.HYDROGEN)
            this[1] = Tile(2, QuantumSpecies.HYDROGEN.scoreValue, species = QuantumSpecies.HYDROGEN)
            this[2] = Tile(3, QuantumSpecies.HELIUM.scoreValue, species = QuantumSpecies.HELIUM)
            this[3] = Tile(4, QuantumSpecies.HELIUM.scoreValue, species = QuantumSpecies.HELIUM)
        }

        val result = GameEngine(ScriptedRandom(), balance).move(quantumState(cells, energy = 90), Direction.LEFT)

        assertEquals(2, result.mergeCount)
        assertEquals(15, result.energyGained)
        assertEquals(100, result.state.quantumEnergy)
    }

    @Test
    fun seeded_random_replays_identical_quantum_games() {
        val a = GameEngine(SeededRandomProvider(2048), balance).newGame(GameMode.QUANTUM)
        val b = GameEngine(SeededRandomProvider(2048), balance).newGame(GameMode.QUANTUM)

        assertEquals(a, b)
    }

    @Test
    fun unresolved_particle_tiles_do_not_merge_until_collapsed() {
        val cells = MutableList<Tile?>(16) { null }.apply {
            this[0] = Tile(
                id = 1,
                value = QuantumSpecies.ELECTRON.scoreValue,
                quantumAlternative = QuantumSpecies.PROTON.scoreValue,
                species = QuantumSpecies.ELECTRON,
                quantumAlternativeSpecies = QuantumSpecies.PROTON,
            )
            this[1] = Tile(2, QuantumSpecies.PROTON.scoreValue, species = QuantumSpecies.PROTON)
        }

        val result = GameEngine(ScriptedRandom(), balance).move(quantumState(cells), Direction.LEFT)

        assertEquals(0, result.mergeCount)
        assertEquals(2, result.state.cells.count { it != null })
    }
}
