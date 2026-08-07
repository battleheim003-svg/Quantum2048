package com.battleheim.quantum2048.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FusionRulesTest {
    private class FixedRandom : RandomProvider {
        override fun nextInt(bound: Int) = (bound - 1).coerceAtLeast(0)
        override fun nextDouble() = 0.0
    }

    @Test fun electron_chain_doubles_like_2048() {
        val product = FusionRules.mergeProduct(Tile(1, 2, TileKind.ELECTRON), Tile(2, 2, TileKind.ELECTRON))!!
        assertEquals(TileKind.ELECTRON, product.tiles.single().kind)
        assertEquals(4, product.tiles.single().value)
        assertEquals(8, FusionRules.gameValueOf(product.tiles.single()))
    }

    @Test fun proton_chain_doubles_like_2048() {
        val product = FusionRules.mergeProduct(Tile(1, 4, TileKind.PROTON), Tile(2, 4, TileKind.PROTON))!!
        assertEquals(TileKind.PROTON, product.tiles.single().kind)
        assertEquals(8, product.tiles.single().value)
    }

    @Test fun element_chain_uses_defined_atomic_sequence() {
        val product = FusionRules.mergeProduct(
            Tile(1, QuantumElement.HELIUM.atomicNumber, TileKind.ELEMENT, QuantumElement.HELIUM),
            Tile(2, QuantumElement.HELIUM.atomicNumber, TileKind.ELEMENT, QuantumElement.HELIUM),
        )!!
        assertEquals(QuantumElement.BERYLLIUM, product.tiles.single().element)
    }

    @Test fun ten_electrons_and_eleven_protons_make_neon_plus_one_proton() {
        val product = FusionRules.mergeProduct(Tile(1, 10, TileKind.ELECTRON), Tile(2, 11, TileKind.PROTON))!!
        assertEquals(true, product.isReaction)
        assertEquals(QuantumElement.NEON, product.tiles[0].element)
        assertEquals(TileKind.PROTON, product.tiles[1].kind)
        assertEquals(1, product.tiles[1].value)
    }

    @Test fun engine_swipe_applies_ten_electron_eleven_proton_reaction() {
        val cells = MutableList<Tile?>(16) { null }.apply {
            this[0] = Tile(1, 10, TileKind.ELECTRON)
            this[1] = Tile(2, 11, TileKind.PROTON)
        }
        val state = GameState(cells = cells, mode = GameMode.QUANTUM, difficulty = Difficulty.QUANTUM, nextTileId = 10)

        val result = GameEngine(FixedRandom()).move(state, Direction.LEFT)

        assertEquals(1, result.reactionCount)
        assertEquals(QuantumElement.NEON, result.state.cells[0]?.element)
        assertEquals(TileKind.PROTON, result.state.cells[1]?.kind)
        assertEquals(1, result.state.cells[1]?.value)
    }


    @Test fun balanced_particles_leave_no_remainder() {
        val product = FusionRules.mergeProduct(Tile(1, 8, TileKind.ELECTRON), Tile(2, 8, TileKind.PROTON))!!
        assertEquals(listOf(QuantumElement.OXYGEN), product.tiles.map { it.element })
    }

    @Test fun undefined_atomic_number_rounds_down_to_defined_element() {
        assertEquals(QuantumElement.OXYGEN, FusionRules.nearestDefinedElementAtOrBelow(9))
        val product = FusionRules.mergeProduct(Tile(1, 9, TileKind.ELECTRON), Tile(2, 9, TileKind.PROTON))!!
        assertEquals(QuantumElement.OXYGEN, product.tiles.single().element)
    }

    @Test fun non_matching_categories_do_not_merge() {
        assertNull(FusionRules.mergeProduct(Tile(1, 2, TileKind.ELECTRON), Tile(2, 4, TileKind.ELECTRON)))
    }
}
