package com.battleheim.quantum2048.domain

import com.battleheim.quantum2048.engine.GameState

/** One-level, session-only undo. A restored session starts without an undo credit. */
class UndoBuffer {
    private var previous: GameState? = null
    val canUndo get() = previous != null
    fun remember(state: GameState) { previous = state }
    fun consume(): GameState? = previous.also { previous = null }
    fun clear() { previous = null }
}
