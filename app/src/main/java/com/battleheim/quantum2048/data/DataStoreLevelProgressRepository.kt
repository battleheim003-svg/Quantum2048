package com.battleheim.quantum2048.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.battleheim.quantum2048.domain.LevelProgressRepository
import com.battleheim.quantum2048.domain.PlayerProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val Context.levelProgressDataStore by preferencesDataStore("periodic_path_progress_v1")
private const val LEVEL_PROGRESS_SCHEMA_VERSION = 1

class DataStoreLevelProgressRepository(private val context: Context) : LevelProgressRepository {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val key = stringPreferencesKey("periodic_path_progress_snapshot_v1")

    override fun observe(): Flow<PlayerProgress> = context.levelProgressDataStore.data.map { prefs ->
        prefs[key]?.let { encoded ->
            runCatching { json.decodeFromString<LevelProgressSnapshot>(encoded).progress }.getOrNull()
        } ?: PlayerProgress()
    }

    override suspend fun save(progress: PlayerProgress) {
        context.levelProgressDataStore.edit { prefs ->
            prefs[key] = json.encodeToString(LevelProgressSnapshot(progress = progress))
        }
    }

    override suspend fun clear() {
        context.levelProgressDataStore.edit { prefs -> prefs.remove(key) }
    }
}

@Serializable
private data class LevelProgressSnapshot(
    val schemaVersion: Int = LEVEL_PROGRESS_SCHEMA_VERSION,
    val progress: PlayerProgress = PlayerProgress(),
)
