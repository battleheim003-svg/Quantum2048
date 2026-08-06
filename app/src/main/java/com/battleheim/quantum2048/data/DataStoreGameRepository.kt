package com.battleheim.quantum2048.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.battleheim.quantum2048.domain.GameRepository
import com.battleheim.quantum2048.engine.GameMode
import com.battleheim.quantum2048.engine.GameState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.gameDataStore by preferencesDataStore("game_state_v1")

class DataStoreGameRepository(private val context: Context) : GameRepository {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private fun key(mode: GameMode) = stringPreferencesKey("snapshot_${mode.name.lowercase()}")
    override fun observe(mode: GameMode): Flow<GameState?> = context.gameDataStore.data.map { prefs ->
        prefs[key(mode)]?.let { runCatching { json.decodeFromString<Snapshot>(it).state }.getOrNull() }
    }
    override suspend fun save(state: GameState) { context.gameDataStore.edit { it[key(state.mode)] = json.encodeToString(Snapshot(state = state)) } }
    override suspend fun clear(mode: GameMode) { context.gameDataStore.edit { it.remove(key(mode)) } }
}
