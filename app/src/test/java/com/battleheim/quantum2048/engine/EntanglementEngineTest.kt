package com.battleheim.quantum2048.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EntanglementEngineTest {
    private class ScriptedRandom(
        private val ints: MutableList<Int> = mutableListOf(0),
        private val doubles: MutableList<Double> = mutableListOf(0.0),
    ) : RandomProvider {
        override fun nextInt(bound: Int): Int = (if (ints.isEmpty()) 0 else ints.removeAt(0)).mod(bound)
        override fun nextDouble(): Double = if (doubles.isEmpty()) 0.99 else doubles.removeAt(0)
    }

    @Test
    fun quantumSpawnCanPairWithAdjacentUnpairedQuantumTile() {
        val cells = MutableList<Tile?>(16) { null }.apply {
            this[0] = Tile(1, 1, TileKind.ELECTRON)
        }
        val state = GameState(
            cells = cells,
            mode = GameMode.QUANTUM,
            difficulty = Difficulty.QUANTUM,
            nextTileId = 2,
        )
        val engine = GameEngine(ScriptedRandom(ints = mutableListOf(0, 0), doubles = mutableListOf(0.0, 0.0)))

        val spawned = engine.spawn(state)

        val firstGroup = spawned.cells[0]?.entanglementGroupId
        val secondGroup = spawned.cells[1]?.entanglementGroupId
        assertNotNull(firstGroup)
        assertEquals(firstGroup, secondGroup)
    }

    @Test
    fun entangledPartnerCollapsesToPrimaryFusionOutputWhenPairMemberMerges() {
        val cells = MutableList<Tile?>(16) { null }.apply {
            this[0] = Tile(1, 1, TileKind.ELECTRON, entanglementGroupId = 42)
            this[1] = Tile(2, 1, TileKind.ELECTRON)
            this[3] = Tile(3, 1, TileKind.PROTON, entanglementGroupId = 42)
        }
        val state = GameState(
            cells = cells,
            mode = GameMode.QUANTUM,
            difficulty = Difficulty.QUANTUM,
            nextTileId = 10,
        )

        val result = GameEngine(ScriptedRandom()).move(state, Direction.LEFT)

        val partner = result.state.cells[1]
        assertTrue(result.changed)
        assertEquals(1, result.entanglementCollapseCount)
        assertEquals(TileKind.ELECTRON, partner?.kind)
        assertEquals(2, partner?.value)
        assertNull(partner?.entanglementGroupId)
        assertTrue(result.animations.any { it.kind == MoveAnimationKind.ENTANGLEMENT && it.tileId == 3L })
    }

    @Test
    fun nonEntangledTilesAreNotChangedByEntanglementCollapse() {
        val cells = MutableList<Tile?>(16) { null }.apply {
            this[0] = Tile(1, 1, TileKind.ELECTRON, entanglementGroupId = 42)
            this[1] = Tile(2, 1, TileKind.ELECTRON)
            this[3] = Tile(3, 1, TileKind.PROTON)
        }
        val state = GameState(
            cells = cells,
            mode = GameMode.QUANTUM,
            difficulty = Difficulty.QUANTUM,
            nextTileId = 10,
        )

        val result = GameEngine(ScriptedRandom()).move(state, Direction.LEFT)

        assertEquals(0, result.entanglementCollapseCount)
        assertEquals(TileKind.PROTON, result.state.cells[1]?.kind)
        assertEquals(1, result.state.cells[1]?.value)
    }
}
