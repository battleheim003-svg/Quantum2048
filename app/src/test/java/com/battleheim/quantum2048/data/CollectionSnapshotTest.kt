package com.battleheim.quantum2048.data

import com.battleheim.quantum2048.domain.CollectionState
import com.battleheim.quantum2048.engine.Difficulty
import com.battleheim.quantum2048.engine.FusionRules
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CollectionSnapshotTest {
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }
    private val water = FusionRules.compoundRecipes.first { it.output.symbol == "H2O" }.output
    private val silica = FusionRules.compoundRecipes.first { it.output.symbol == "SiO2" }.output

    @Test
    fun repeatedCompoundIncrementsCounterWithoutAddingDuplicateEntry() {
        val state = CollectionState()
            .record(water, Difficulty.MEDIUM, discoveredAtMillis = 1000)
            .record(water, Difficulty.HARD, discoveredAtMillis = 2500)

        assertEquals(1, state.entries.size)
        assertEquals(2, state.entries.single().discoveryCount)
        assertEquals(Difficulty.MEDIUM, state.entries.single().firstDifficulty)
        assertEquals(Difficulty.HARD, state.entries.single().lastDifficulty)
        assertEquals(1000, state.entries.single().firstDiscoveredAtMillis)
        assertEquals(2500, state.entries.single().lastDiscoveredAtMillis)
    }

    @Test
    fun collectionSnapshotPersistsBetweenJsonSessions() {
        val saved = CollectionState()
            .record(water, Difficulty.MEDIUM, discoveredAtMillis = 1000)
            .record(silica, Difficulty.MEDIUM, discoveredAtMillis = 2000)
            .record(water, Difficulty.QUANTUM, discoveredAtMillis = 3000)

        val encoded = json.encodeToString(CollectionSnapshot(state = saved))
        val restored = json.decodeFromString<CollectionSnapshot>(encoded).state

        assertEquals(saved, restored)
        assertEquals(2, restored.entries.size)
        assertEquals(2, restored.entries.first { it.compoundSymbol == "H2O" }.discoveryCount)
    }

    @Test
    fun codexIncludesLockedEntriesForUndiscoveredRecipes() {
        val state = CollectionState().record(water, Difficulty.MEDIUM, discoveredAtMillis = 1000)
        val codex = state.codex(FusionRules.compoundRecipes)

        val waterEntry = codex.first { it.symbol == "H2O" }
        val silicaEntry = codex.first { it.symbol == "SiO2" }

        assertTrue(waterEntry.discovered)
        assertEquals("Water", waterEntry.englishName)
        assertFalse(silicaEntry.discovered)
        assertEquals(null, silicaEntry.englishName)
        assertEquals(0, silicaEntry.discoveryCount)
    }

    @Test
    fun unrecordDecrementsOrRemovesCompoundForUndo() {
        val state = CollectionState()
            .record(water, Difficulty.MEDIUM, discoveredAtMillis = 1000)
            .record(water, Difficulty.HARD, discoveredAtMillis = 2000)

        val decremented = state.unrecord("H2O")
        val removed = decremented.unrecord("H2O")

        assertEquals(1, decremented.entries.single().discoveryCount)
        assertEquals(emptyList<com.battleheim.quantum2048.domain.CollectionEntry>(), removed.entries)
    }
}
