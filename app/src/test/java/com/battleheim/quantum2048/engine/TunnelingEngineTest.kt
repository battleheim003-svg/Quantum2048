package com.battleheim.quantum2048.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class TunnelingEngineTest {
    private class FixedRandom : RandomProvider {
        override fun nextInt(bound: Int): Int = 0
        override fun nextDouble(): Double = 0.99
    }

    @Test
    fun tunnelMovesTileToEmptyDestinationWithoutSpawningOrCountingMove() {
        val cells = MutableList<Tile?>(16) { null }.apply {
            this[0] = Tile(1, 1, TileKind.ELECTRON)
        }
        val state = GameState(
            cells = cells,
            score = 100,
            energy = FusionRules.tunnelingEnergyCost,
            mode = GameMode.QUANTUM,
            difficulty = Difficulty.QUANTUM,
            moveCount = 5,
            nextTileId = 9,
        )

        val result = GameEngine(FixedRandom()).tunnel(state, tileId = 1, destinationIndex = 5) as TunnelResult.Success

        assertEquals(null, result.state.cells[0])
        assertEquals(1L, result.state.cells[5]?.id)
        assertEquals(100L, result.state.score)
        assertEquals(0, result.state.energy)
        assertEquals(5, result.state.moveCount)
        assertEquals(9, result.state.nextTileId)
        assertEquals(1, result.state.cells.count { it != null })
        assertEquals(MoveAnimationKind.TUNNEL, result.animation.kind)
        assertEquals(0, result.animation.fromIndex)
        assertEquals(5, result.animation.toIndex)
    }

    @Test
    fun tunnelFailsClosedWhenDestinationIsOccupied() {
        val cells = MutableList<Tile?>(16) { null }.apply {
            this[0] = Tile(1, 1, TileKind.ELECTRON)
            this[5] = Tile(2, 1, TileKind.PROTON)
        }
        val state = GameState(cells = cells, score = 500, energy = 100, mode = GameMode.QUANTUM, difficulty = Difficulty.QUANTUM)

        val result = GameEngine(FixedRandom()).tunnel(state, tileId = 1, destinationIndex = 5) as TunnelResult.Failure

        assertEquals(TunnelFailure.DESTINATION_OCCUPIED, result.reason)
        assertSame(state, result.state)
    }

    @Test
    fun tunnelFailsClosedWhenScoreIsInsufficient() {
        val cells = MutableList<Tile?>(16) { null }.apply {
            this[0] = Tile(1, 1, TileKind.ELECTRON)
        }
        val state = GameState(cells = cells, energy = FusionRules.tunnelingEnergyCost - 1, mode = GameMode.QUANTUM, difficulty = Difficulty.QUANTUM)

        val result = GameEngine(FixedRandom()).tunnel(state, tileId = 1, destinationIndex = 5) as TunnelResult.Failure

        assertEquals(TunnelFailure.INSUFFICIENT_SCORE, result.reason)
        assertSame(state, result.state)
    }

    @Test
    fun classicModeCannotTunnel() {
        val state = GameState(cells = listOf(Tile(1, 2)) + List(15) { null }, score = 500, mode = GameMode.CLASSIC, difficulty = Difficulty.EASY)

        val result = GameEngine(FixedRandom()).tunnel(state, tileId = 1, destinationIndex = 5) as TunnelResult.Failure

        assertEquals(TunnelFailure.LAB_DISABLED, result.reason)
        assertSame(state, result.state)
    }

    @Test
    fun tunnelPreservesEntanglementBondOnMovedTile() {
        val cells = MutableList<Tile?>(16) { null }.apply {
            this[0] = Tile(1, 1, TileKind.ELECTRON, entanglementGroupId = 77)
            this[1] = Tile(2, 1, TileKind.PROTON, entanglementGroupId = 77)
        }
        val state = GameState(cells = cells, score = 500, energy = 100, mode = GameMode.QUANTUM, difficulty = Difficulty.QUANTUM)

        val result = GameEngine(FixedRandom()).tunnel(state, tileId = 1, destinationIndex = 5) as TunnelResult.Success

        assertEquals(77L, result.state.cells[5]?.entanglementGroupId)
        assertEquals(77L, result.state.cells[1]?.entanglementGroupId)
        assertTrue(result.state.cells[0] == null)
    }
}
