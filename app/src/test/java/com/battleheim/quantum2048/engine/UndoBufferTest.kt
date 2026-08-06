package com.battleheim.quantum2048.engine

import com.battleheim.quantum2048.domain.UndoBuffer
import org.junit.Assert.*
import org.junit.Test

class UndoBufferTest {
    @Test fun undo_is_single_use_and_restores_exact_state() {
        val original = GameState(score = 42, moveCount = 7)
        val undo = UndoBuffer()
        undo.remember(original)
        assertTrue(undo.canUndo)
        assertEquals(original, undo.consume())
        assertNull(undo.consume())
        assertFalse(undo.canUndo)
    }
}
