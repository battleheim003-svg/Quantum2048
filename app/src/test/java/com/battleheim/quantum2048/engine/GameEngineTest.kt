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
}
