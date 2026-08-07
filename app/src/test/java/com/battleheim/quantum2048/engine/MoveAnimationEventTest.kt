package com.battleheim.quantum2048.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MoveAnimationEventTest {
    private class FixedRandom : RandomProvider {
        override fun nextInt(bound: Int) = (bound - 1).coerceAtLeast(0)
        override fun nextDouble() = 0.99
    }

    @Test fun slide_event_records_distinct_source_and_destination() {
        val cells = MutableList<Tile?>(16) { null }.apply {
            this[3] = Tile(1, 2)
        }
        val result = GameEngine(FixedRandom()).move(GameState(cells = cells, nextTileId = 10), Direction.LEFT)

        val slide = result.animations.first { it.tileId == 1L }
        assertEquals(MoveAnimationKind.SLIDE, slide.kind)
        assertEquals(3, slide.fromIndex)
        assertEquals(0, slide.toIndex)
    }

    @Test fun merge_event_marks_winning_tile_for_micro_scale() {
        val cells = MutableList<Tile?>(16) { null }.apply {
            this[0] = Tile(1, 2)
            this[1] = Tile(2, 2)
        }
        val result = GameEngine(FixedRandom()).move(GameState(cells = cells, nextTileId = 10), Direction.LEFT)

        assertTrue(result.animations.any { it.kind == MoveAnimationKind.MERGE && it.toIndex == 0 })
    }

    @Test fun reaction_event_marks_chemical_glow() {
        val cells = MutableList<Tile?>(16) { null }.apply {
            this[0] = Tile(1, 10, TileKind.ELECTRON)
            this[1] = Tile(2, 11, TileKind.PROTON)
        }
        val state = GameState(cells = cells, mode = GameMode.QUANTUM, difficulty = Difficulty.QUANTUM, nextTileId = 10)
        val result = GameEngine(FixedRandom()).move(state, Direction.LEFT)

        assertTrue(result.animations.any { it.kind == MoveAnimationKind.REACTION && it.toIndex == 0 })
    }

    @Test fun spawn_event_marks_new_tile_for_scale_in() {
        val cells = MutableList<Tile?>(16) { null }.apply {
            this[3] = Tile(1, 2)
        }
        val result = GameEngine(FixedRandom()).move(GameState(cells = cells, nextTileId = 10), Direction.LEFT)

        assertTrue(result.animations.any { it.kind == MoveAnimationKind.SPAWN })
    }
}
