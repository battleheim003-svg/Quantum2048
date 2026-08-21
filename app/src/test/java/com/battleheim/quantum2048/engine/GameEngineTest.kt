package com.battleheim.quantum2048.engine

import org.junit.Assert.*
import org.junit.Test

class GameEngineTest {
    private class FixedRandom(private val ints: MutableList<Int> = mutableListOf(0), private val doubles: MutableList<Double> = mutableListOf(0.0)) : RandomProvider { override fun nextInt(bound: Int) = (if (ints.isEmpty()) 0 else ints.removeAt(0)).mod(bound); override fun nextDouble() = if (doubles.isEmpty()) 0.0 else doubles.removeAt(0) }
    private fun state(vararg values: Int?, score: Long = 0) = GameState(cells = values.mapIndexed { i, v -> v?.let { Tile(i.toLong(), it) } }, score = score, nextTileId = 99)
    private fun values(s: GameState) = s.cells.map { it?.value }

    @Test fun left_compacts_merges_once_and_scores() { val r = GameEngine(FixedRandom()).move(state(2,2,2,2,null,null,null,null,null,null,null,null,null,null,null,null), Direction.LEFT); assertTrue(r.changed); assertEquals(8, r.gainedScore); assertEquals(listOf(4,4), values(r.state).filterNotNull().filter { it != 2 }) }
    @Test fun right_moves_in_correct_direction() { val r = GameEngine(FixedRandom()).move(state(2,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null), Direction.RIGHT); assertEquals(2, r.state[0,3]?.value) }
    @Test fun up_merges_columns() { val r = GameEngine(FixedRandom()).move(state(2,null,null,null,2,null,null,null,null,null,null,null,null,null,null,null), Direction.UP); assertEquals(4, r.state[0,0]?.value) }
    @Test fun down_merges_columns() { val r = GameEngine(FixedRandom()).move(state(2,null,null,null,2,null,null,null,null,null,null,null,null,null,null,null), Direction.DOWN); assertEquals(4, r.state[3,0]?.value) }
    @Test fun invalid_move_does_not_spawn() { val s = state(2,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null); val r = GameEngine(FixedRandom()).move(s, Direction.LEFT); assertFalse(r.changed); assertEquals(1, r.state.cells.count { it != null }) }
    @Test fun valid_move_spawns_exactly_one_tile() { val s = state(null,2,null,null,null,null,null,null,null,null,null,null,null,null,null,null); val r = GameEngine(FixedRandom()).move(s, Direction.LEFT); assertEquals(2, r.state.cells.count { it != null }) }
    @Test fun fixed_random_controls_spawn_value() { val s = GameState(); val two = GameEngine(FixedRandom(doubles= mutableListOf(.89))).spawn(s); val four = GameEngine(FixedRandom(doubles= mutableListOf(.91))).spawn(s); assertEquals(2, two.cells.filterNotNull().single().value); assertEquals(4, four.cells.filterNotNull().single().value) }
    @Test fun detects_win_and_can_continue() { val s = state(1024,1024,null,null,null,null,null,null,null,null,null,null,null,null,null,null); val e = GameEngine(FixedRandom()); val won = e.move(s, Direction.LEFT).state; assertEquals(GameStatus.WON, won.status); assertEquals(GameStatus.PLAYING, e.continueAfterWin(won).status) }
    @Test fun detects_loss_on_full_board() { val v = arrayOf(2,4,2,4,4,2,4,2,2,4,2,4,4,2,4,2); val r = GameEngine(FixedRandom()).move(state(*v), Direction.LEFT); assertFalse(r.changed); assertEquals(GameStatus.LOST, r.state.status) }
    @Test fun classic_and_quantum_modes_are_independent_state() { assertEquals(GameMode.CLASSIC, GameEngine(FixedRandom()).newGame().mode); assertEquals(GameMode.QUANTUM, GameEngine(FixedRandom()).newGame(GameMode.QUANTUM).mode) }
    @Test fun all_swipe_directions_move_to_physical_edges_in_ltr_and_rtl() {
        listOf("LTR", "RTL").forEach {
            assertEquals(2, GameEngine(FixedRandom()).move(state(null, 2, null, null, null, null, null, null, null, null, null, null, null, null, null, null), Direction.LEFT).state[0,0]?.value)
            assertEquals(2, GameEngine(FixedRandom()).move(state(null, 2, null, null, null, null, null, null, null, null, null, null, null, null, null, null), Direction.RIGHT).state[0,3]?.value)
            assertEquals(2, GameEngine(FixedRandom()).move(state(null, null, null, null, null, 2, null, null, null, null, null, null, null, null, null, null), Direction.UP).state[0,1]?.value)
            assertEquals(2, GameEngine(FixedRandom()).move(state(null, null, null, null, null, 2, null, null, null, null, null, null, null, null, null, null), Direction.DOWN).state[3,1]?.value)
        }
    }

    @Test fun balanced_quantum_spawn_uses_equal_electron_proton_split_before_element_spawns() {
        val state = GameState(mode = GameMode.QUANTUM, difficulty = Difficulty.QUANTUM, cells = List(16) { null })
        assertEquals(TileKind.PROTON, FusionRules.quantumSpawnTile(state, 0.49).kind)
        assertEquals(TileKind.ELECTRON, FusionRules.quantumSpawnTile(state, 0.50).kind)
    }

    @Test fun quantum_spawn_balance_stays_within_two_points_across_board_pressure() {
        val states = listOf(
            pressureState(empty = 16, electrons = 0, protons = 0),
            pressureState(empty = 8, electrons = 4, protons = 4),
            pressureState(empty = 3, electrons = 6, protons = 7),
        )
        var totalProtons = 0
        var totalParticles = 0
        states.forEach { state ->
            var protons = 0
            var electrons = 0
            repeat(2_000) { index ->
                when (FusionRules.quantumSpawnTile(state, (index + 0.5) / 2_000.0).kind) {
                    TileKind.PROTON -> protons++
                    TileKind.ELECTRON -> electrons++
                    else -> Unit
                }
            }
            val particles = protons + electrons
            totalProtons += protons
            totalParticles += particles
            assertTrue(kotlin.math.abs(protons.toDouble() / particles - expectedProtonChance(state)) <= 0.001)
        }
        assertTrue(kotlin.math.abs(totalProtons.toDouble() / totalParticles - 0.50) <= 0.02)
    }

    private fun pressureState(empty: Int, electrons: Int, protons: Int): GameState {
        val cells = MutableList<Tile?>(16) { null }
        repeat(electrons) { cells[it] = Tile(it.toLong() + 1, 1, TileKind.ELECTRON) }
        repeat(protons) { cells[electrons + it] = Tile((electrons + it).toLong() + 1, 1, TileKind.PROTON) }
        check(cells.count { it == null } == empty)
        return GameState(mode = GameMode.QUANTUM, difficulty = Difficulty.QUANTUM, cells = cells)
    }

    private fun expectedProtonChance(state: GameState): Double {
        val electrons = state.cells.filter { it?.kind == TileKind.ELECTRON }.sumOf { it?.value ?: 0 }
        val protons = state.cells.filter { it?.kind == TileKind.PROTON }.sumOf { it?.value ?: 0 }
        val emptyRatio = state.cells.count { it == null }.toDouble() / state.cells.size
        val balanceBias = ((electrons - protons).coerceIn(-8, 8)) * 0.035
        val pressureAssist = if (emptyRatio < 0.25) 0.06 else 0.0
        return (0.50 + balanceBias + pressureAssist).coerceIn(0.35, 0.65)
    }
}
