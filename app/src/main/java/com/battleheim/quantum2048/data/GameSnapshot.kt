package com.battleheim.quantum2048.data

import com.battleheim.quantum2048.engine.GameState
import kotlinx.serialization.Serializable

const val GAME_SNAPSHOT_SCHEMA_VERSION = 3

/** Versioned disk envelope. New GameState fields must keep safe defaults for old saves. */
@Serializable
data class Snapshot(
    val schemaVersion: Int = GAME_SNAPSHOT_SCHEMA_VERSION,
    val state: GameState,
)
