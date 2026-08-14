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
        val engine = GameEngine(ScriptedRandom(ints = mutableListOf(0, 0), doubles = mutableListOf(0.0, 0.0)), entanglementEnabled = true)

        val spawned = engine.spawn(state)

        val firstGroup = spawned.cells[0]?.entanglementGroupId
        val secondGroup = spawned.cells[1]?.entanglementGroupId
        assertNotNull(firstGroup)
        assertEquals(firstGroup, secondGroup)
        assertEquals(1, spawned.entangledPairs.size)
        assertEquals(2L, spawned.entangledPairs.single().firstTileId)
        assertEquals(1L, spawned.entangledPairs.single().secondTileId)
    }

    @Test
    fun featureFlagOffDoesNotCreateEntangledPair() {
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

        assertNull(spawned.cells[0]?.entanglementGroupId)
        assertNull(spawned.cells[1]?.entanglementGroupId)
        assertTrue(spawned.entangledPairs.isEmpty())
    }

    @Test
    fun featureFlagOffDoesNotApplyEntanglementSideEffectsDuringMerge() {
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
            entangledPairs = listOf(EntangledPair(42, 1, 3)),
        )

        val result = GameEngine(ScriptedRandom()).move(state, Direction.LEFT)

        assertTrue(result.changed)
        assertEquals(0, result.entanglementCollapseCount)
        assertEquals(42L, result.state.cells[1]?.entanglementGroupId)
        assertEquals(state.entangledPairs, result.state.entangledPairs)
    }

    @Test
    fun entangledPartnerCollapsesWhenPairMemberManuallyCollapses() {
        val cells = MutableList<Tile?>(16) { null }.apply {
            this[0] = Tile(1, 1, TileKind.ELECTRON, entanglementGroupId = 42, superpositionValues = listOf(1, 2, 4))
            this[1] = Tile(2, 1, TileKind.PROTON, entanglementGroupId = 42, superpositionValues = listOf(1, 2, 4))
        }
        val state = GameState(
            cells = cells,
            mode = GameMode.QUANTUM,
            difficulty = Difficulty.QUANTUM,
            nextTileId = 10,
            energy = 100,
            entangledPairs = listOf(EntangledPair(42, 1, 2, EntanglementRelation.SAME_CHOICE)),
        )

        val result = GameEngine(ScriptedRandom(), entanglementEnabled = true)
            .collapseSuperposition(state, 1, 2) as SuperpositionResult.Success

        assertEquals(4, result.state.cells[0]?.value)
        assertEquals(4, result.state.cells[1]?.value)
        assertEquals(emptyList<Int>(), result.state.cells[0]?.superpositionValues)
        assertEquals(emptyList<Int>(), result.state.cells[1]?.superpositionValues)
        assertNull(result.state.cells[0]?.entanglementGroupId)
        assertNull(result.state.cells[1]?.entanglementGroupId)
        assertTrue(result.state.entangledPairs.isEmpty())
        assertEquals(FusionRules.superpositionCollapseEnergyCosts[2], 100 - result.state.energy)
    }

    @Test
    fun inverseEntangledPartnerUsesOppositeChoiceWhenCollapsed() {
        val cells = MutableList<Tile?>(16) { null }.apply {
            this[0] = Tile(1, 1, TileKind.ELECTRON, entanglementGroupId = 42, superpositionValues = listOf(1, 2, 4))
            this[1] = Tile(2, 1, TileKind.PROTON, entanglementGroupId = 42, superpositionValues = listOf(1, 2, 4))
        }
        val state = GameState(
            cells = cells,
            mode = GameMode.QUANTUM,
            difficulty = Difficulty.QUANTUM,
            nextTileId = 10,
            energy = 100,
            entangledPairs = listOf(EntangledPair(42, 1, 2, EntanglementRelation.INVERSE_CHOICE)),
        )

        val result = GameEngine(ScriptedRandom(), entanglementEnabled = true)
            .collapseSuperposition(state, 1, 0) as SuperpositionResult.Success

        assertEquals(1, result.state.cells[0]?.value)
        assertEquals(4, result.state.cells[1]?.value)
        assertTrue(result.state.entangledPairs.isEmpty())
    }

    @Test
    fun entangledPairInvalidatesWhenPairMemberMergesBeforeCollapse() {
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
            entangledPairs = listOf(EntangledPair(42, 1, 3)),
        )

        val result = GameEngine(ScriptedRandom(), entanglementEnabled = true).move(state, Direction.LEFT)

        val partner = result.state.cells[1]
        assertTrue(result.changed)
        assertEquals(0, result.entanglementCollapseCount)
        assertEquals(TileKind.PROTON, partner?.kind)
        assertEquals(1, partner?.value)
        assertNull(partner?.entanglementGroupId)
        assertTrue(result.state.entangledPairs.isEmpty())
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

        val result = GameEngine(ScriptedRandom(), entanglementEnabled = true).move(state, Direction.LEFT)

        assertEquals(0, result.entanglementCollapseCount)
        assertEquals(TileKind.PROTON, result.state.cells[1]?.kind)
        assertEquals(1, result.state.cells[1]?.value)
    }
}
