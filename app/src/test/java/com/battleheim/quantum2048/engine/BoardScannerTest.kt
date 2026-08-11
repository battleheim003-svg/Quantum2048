package com.battleheim.quantum2048.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BoardScannerTest {
    private val engine = GameEngine(SeededRandomProvider(12))

    @Test
    fun hardModeHighlightsTilesThatCanSynthesizeCompound() {
        val state = quantumState(Difficulty.HARD, hydrogen(1), hydrogen(2), oxygen(3))

        val scanned = engine.scanBoard(state).state

        assertEquals(3, scanned.cells.filterNotNull().count { it.isHighlightedForSynthesis })
        assertTrue(scanned.cells.filterNotNull().all { it.isHighlightedForSynthesis })
    }

    @Test
    fun mediumModeAutoConsumesCompoundAndAwardsScore() {
        val state = quantumState(Difficulty.MEDIUM, hydrogen(1), hydrogen(2), oxygen(3))

        val scan = engine.scanBoard(state)

        assertEquals("H2O", scan.synthesizedCompound?.symbol)
        assertEquals(0, scan.state.cells.filterNotNull().size)
        assertEquals(240L, scan.state.score)
        assertEquals(240L, scan.state.bestScore)
    }

    private fun quantumState(difficulty: Difficulty, vararg tiles: Tile): GameState =
        GameState(
            cells = tiles.toList() + List(16 - tiles.size) { null },
            mode = GameMode.QUANTUM,
            difficulty = difficulty,
        )

    private fun hydrogen(id: Long) = Tile(id, QuantumElement.HYDROGEN.atomicNumber, TileKind.ELEMENT, QuantumElement.HYDROGEN)
    private fun oxygen(id: Long) = Tile(id, QuantumElement.OXYGEN.atomicNumber, TileKind.ELEMENT, QuantumElement.OXYGEN)
}
