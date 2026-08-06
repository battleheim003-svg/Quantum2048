package com.battleheim.quantum2048.data

import com.battleheim.quantum2048.engine.GameState
import kotlinx.serialization.Serializable

/** Versioned disk envelope. New GameState fields must keep safe defaults for old saves. */
@Serializable data class Snapshot(val schemaVersion: Int = CURRENT_SCHEMA_VERSION, val state: GameState) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 3
    }
}
