package com.battleheim.quantum2048.data

import com.battleheim.quantum2048.engine.*
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class SnapshotTest {
    @Test fun snapshot_round_trip_preserves_game_and_mode() {
        val state = GameState(cells = listOf(Tile(1, 2)) + List(15) { null }, score = 12, bestScore = 64, mode = GameMode.QUANTUM, nextTileId = 2)
        val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }
        val restored = json.decodeFromString<Snapshot>(json.encodeToString(Snapshot(state = state)))
        assertEquals(3, restored.schemaVersion)
        assertEquals(state, restored.state)
    }

    @Test fun phase_one_snapshot_without_quantum_fields_remains_readable() {
        val old = """{"schemaVersion":1,"state":{"size":4,"cells":[null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null],"score":7,"bestScore":9,"status":"PLAYING","mode":"CLASSIC","hasAcknowledgedWin":false,"moveCount":1,"nextTileId":3}}"""
        val restored = Json { ignoreUnknownKeys = true }.decodeFromString<Snapshot>(old)
        assertEquals(7, restored.state.score)
        assertEquals(0, restored.state.quantumEnergy)
        assertEquals(GameMode.CLASSIC, restored.state.mode)
        assertEquals(Difficulty.EASY, restored.state.difficulty)
    }

    @Test fun phase_two_snapshot_without_difficulty_maps_quantum_to_quantum_difficulty() {
        val old = """{"schemaVersion":2,"state":{"size":4,"cells":[null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null],"score":7,"bestScore":9,"status":"PLAYING","mode":"QUANTUM","hasAcknowledgedWin":false,"moveCount":1,"nextTileId":3,"quantumEnergy":30}}"""
        val restored = Json { ignoreUnknownKeys = true }.decodeFromString<Snapshot>(old)
        assertEquals(GameMode.QUANTUM, restored.state.mode)
        assertEquals(Difficulty.QUANTUM, restored.state.difficulty)
    }
}
