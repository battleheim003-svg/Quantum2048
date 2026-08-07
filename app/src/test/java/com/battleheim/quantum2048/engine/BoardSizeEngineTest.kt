package com.battleheim.quantum2048.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class BoardSizeEngineTest {
    private class FixedRandom : RandomProvider {
        override fun nextInt(bound: Int) = 0
        override fun nextDouble() = 0.99
    }

    @Test fun new_games_use_configured_size_and_spawn_density() {
        val engine = GameEngine(FixedRandom())

        FusionRules.supportedBoardSizes.forEach { size ->
            val state = engine.newGame(GameMode.QUANTUM, size)

            assertEquals(size, state.size)
            assertEquals(size * size, state.cells.size)
            assertEquals(FusionRules.spawnCount(size), state.cells.count { it != null })
        }
    }

    @Test fun six_by_six_move_and_merge_uses_dynamic_board_size() {
        val cells = MutableList<Tile?>(36) { null }.apply {
            this[0] = Tile(1, 2)
            this[1] = Tile(2, 2)
        }
        val state = GameState(size = 6, cells = cells, nextTileId = 10)

        val result = GameEngine(FixedRandom()).move(state, Direction.LEFT)

        assertEquals(6, result.state.size)
        assertEquals(4, result.state[0, 0]?.value)
        assertEquals(2, result.state.cells.count { it != null })
    }

    @Test fun eight_by_eight_loss_detection_checks_all_cells() {
        val values = List(64) { index -> if ((index + index / 8) % 2 == 0) 2 else 4 }
        val state = GameState(size = 8, cells = values.mapIndexed { index, value -> Tile(index.toLong(), value) })

        val result = GameEngine(FixedRandom()).move(state, Direction.LEFT)

        assertFalse(result.changed)
        assertEquals(GameStatus.LOST, result.state.status)
    }
}
