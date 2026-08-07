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
        assertEquals(GameMode.CLASSIC, restored.state.mode)
        assertEquals(Difficulty.EASY, restored.state.difficulty)
    }

    @Test fun legacy_snapshot_without_difficulty_maps_quantum_to_quantum_difficulty() {
        val old = """{"schemaVersion":1,"state":{"size":4,"cells":[null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null],"score":7,"bestScore":9,"status":"PLAYING","mode":"QUANTUM","hasAcknowledgedWin":false,"moveCount":1,"nextTileId":3}}"""
        val restored = Json { ignoreUnknownKeys = true }.decodeFromString<Snapshot>(old)
        assertEquals(GameMode.QUANTUM, restored.state.mode)
        assertEquals(Difficulty.QUANTUM, restored.state.difficulty)
    }

    @Test fun schema_three_tile_without_entanglement_group_remains_readable() {
        val old = """{"schemaVersion":3,"state":{"size":4,"cells":[{"id":1,"value":1,"kind":"ELECTRON","element":null},null,null,null,null,null,null,null,null,null,null,null,null,null,null,null],"score":7,"bestScore":9,"status":"PLAYING","mode":"QUANTUM","difficulty":"QUANTUM","hasAcknowledgedWin":false,"moveCount":1,"nextTileId":3}}"""
        val restored = Json { ignoreUnknownKeys = true }.decodeFromString<Snapshot>(old)
        assertEquals(TileKind.ELECTRON, restored.state.cells[0]?.kind)
        assertEquals(null, restored.state.cells[0]?.entanglementGroupId)
    }

    @Test fun schema_three_tile_without_superposition_values_remains_readable() {
        val old = """{"schemaVersion":3,"state":{"size":4,"cells":[{"id":1,"value":1,"kind":"PROTON","element":null},null,null,null,null,null,null,null,null,null,null,null,null,null,null,null],"score":7,"bestScore":9,"status":"PLAYING","mode":"QUANTUM","difficulty":"QUANTUM","hasAcknowledgedWin":false,"moveCount":1,"nextTileId":3}}"""
        val restored = Json { ignoreUnknownKeys = true }.decodeFromString<Snapshot>(old)
        assertEquals(TileKind.PROTON, restored.state.cells[0]?.kind)
        assertEquals(emptyList<Int>(), restored.state.cells[0]?.superpositionValues)
    }

    @Test fun schema_three_quantum_snapshot_without_energy_uses_initial_energy() {
        val old = """{"schemaVersion":3,"state":{"size":4,"cells":[null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null],"score":7,"bestScore":9,"status":"PLAYING","mode":"QUANTUM","difficulty":"QUANTUM","hasAcknowledgedWin":false,"moveCount":1,"nextTileId":3}}"""
        val restored = Json { ignoreUnknownKeys = true }.decodeFromString<Snapshot>(old)
        assertEquals(FusionRules.initialEnergy, restored.state.energy)
    }

    @Test fun daily_snapshot_round_trip_preserves_date_and_daily_best() {
        val state = GameState(
            mode = GameMode.QUANTUM,
            difficulty = Difficulty.DAILY,
            dailyChallengeDate = "2026-08-07",
            dailyBestScore = 123,
        )
        val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }
        val restored = json.decodeFromString<Snapshot>(json.encodeToString(Snapshot(state = state)))
        assertEquals("2026-08-07", restored.state.dailyChallengeDate)
        assertEquals(123, restored.state.dailyBestScore)
    }

    @Test fun achievement_progress_round_trip_persists() {
        val state = GameState(
            mode = GameMode.QUANTUM,
            difficulty = Difficulty.QUANTUM,
            successfulCollapseCount = 100,
            usedUndo = true,
            unlockedAchievements = setOf(FusionRules.achievementCollapseCentury),
        )
        val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }
        val restored = json.decodeFromString<Snapshot>(json.encodeToString(Snapshot(state = state)))
        assertEquals(100, restored.state.successfulCollapseCount)
        assertEquals(true, restored.state.usedUndo)
        assertEquals(setOf(FusionRules.achievementCollapseCentury), restored.state.unlockedAchievements)
    }

    @Test fun statistics_round_trip_persists() {
        val state = GameState(
            lowCollapseCount = 2,
            highCollapseCount = 3,
            totalWinEnergy = 120,
            winEnergySamples = 2,
            totalChainMergeCount = 7,
        )
        val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }
        val restored = json.decodeFromString<Snapshot>(json.encodeToString(Snapshot(state = state)))
        assertEquals(2, restored.state.lowCollapseCount)
        assertEquals(3, restored.state.highCollapseCount)
        assertEquals(120, restored.state.totalWinEnergy)
        assertEquals(2, restored.state.winEnergySamples)
        assertEquals(7, restored.state.totalChainMergeCount)
    }

    @Test fun tutorial_completion_round_trip_persists() {
        val state = GameState(tutorialCompleted = true)
        val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }
        val restored = json.decodeFromString<Snapshot>(json.encodeToString(Snapshot(state = state)))
        assertEquals(true, restored.state.tutorialCompleted)
    }

    @Test fun old_snapshot_without_tutorial_completion_defaults_to_false() {
        val old = """{"schemaVersion":3,"state":{"size":4,"cells":[null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null],"score":7,"bestScore":9,"status":"PLAYING","mode":"CLASSIC","difficulty":"EASY","hasAcknowledgedWin":false,"moveCount":1,"nextTileId":3}}"""
        val restored = Json { ignoreUnknownKeys = true }.decodeFromString<Snapshot>(old)
        assertEquals(false, restored.state.tutorialCompleted)
    }
}
