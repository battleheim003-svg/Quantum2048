package com.battleheim.quantum2048.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.battleheim.quantum2048.domain.CollectionRepository
import com.battleheim.quantum2048.domain.CollectionState
import com.battleheim.quantum2048.engine.Compound
import com.battleheim.quantum2048.engine.Difficulty
import com.battleheim.quantum2048.engine.QuantumElement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.collectionDataStore by preferencesDataStore("collection_state_v1")

class DataStoreCollectionRepository(private val context: Context) : CollectionRepository {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val key = stringPreferencesKey("collection_snapshot_v1")

    override fun observe(): Flow<CollectionState> = context.collectionDataStore.data.map { prefs ->
        prefs[key]?.let { encoded ->
            runCatching { json.decodeFromString<CollectionSnapshot>(encoded).state }.getOrNull()
        } ?: CollectionState()
    }

    override suspend fun record(compound: Compound, difficulty: Difficulty, discoveredAtMillis: Long) {
        context.collectionDataStore.edit { prefs ->
            val current = prefs[key]?.let { encoded ->
                runCatching { json.decodeFromString<CollectionSnapshot>(encoded).state }.getOrNull()
            } ?: CollectionState()
            val next = current.record(compound, difficulty, discoveredAtMillis)
            prefs[key] = json.encodeToString(CollectionSnapshot(state = next))
        }
    }

    override suspend fun recordElement(element: QuantumElement) {
        context.collectionDataStore.edit { prefs ->
            val current = prefs[key]?.let { encoded ->
                runCatching { json.decodeFromString<CollectionSnapshot>(encoded).state }.getOrNull()
            } ?: CollectionState()
            prefs[key] = json.encodeToString(CollectionSnapshot(state = current.recordElement(element)))
        }
    }

    override suspend fun unrecord(compoundSymbol: String) {
        context.collectionDataStore.edit { prefs ->
            val current = prefs[key]?.let { encoded ->
                runCatching { json.decodeFromString<CollectionSnapshot>(encoded).state }.getOrNull()
            } ?: CollectionState()
            prefs[key] = json.encodeToString(CollectionSnapshot(state = current.unrecord(compoundSymbol)))
        }
    }

    override suspend fun clear() {
        context.collectionDataStore.edit { prefs -> prefs.remove(key) }
    }
}
