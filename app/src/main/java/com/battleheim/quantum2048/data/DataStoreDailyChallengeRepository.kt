package com.battleheim.quantum2048.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.battleheim.quantum2048.domain.DailyChallengeRepository
import com.battleheim.quantum2048.domain.DailyChallengeState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val Context.dailyChallengeDataStore by preferencesDataStore("daily_challenge_state_v1")
private const val DAILY_CHALLENGE_SCHEMA_VERSION = 1

class DataStoreDailyChallengeRepository(private val context: Context) : DailyChallengeRepository {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val key = stringPreferencesKey("daily_challenge_snapshot_v1")

    override fun observe(): Flow<DailyChallengeState> =
        context.dailyChallengeDataStore.data.map { prefs ->
            prefs[key]?.let { encoded ->
                runCatching { json.decodeFromString<DailyChallengeSnapshot>(encoded).state }.getOrNull()
            } ?: DailyChallengeState()
        }

    override suspend fun markStarted(date: String) {
        update { it.markStarted(date) }
    }

    override suspend fun recordResult(date: String, score: Long) {
        update { it.recordResult(date, score) }
    }

    override suspend fun clearActiveRun(date: String) {
        update { state ->
            if (state.activeDate == date) state.copy(hasActiveRun = false) else state
        }
    }

    override suspend fun clearHistory() {
        context.dailyChallengeDataStore.edit { it.remove(key) }
    }

    private suspend fun update(transform: (DailyChallengeState) -> DailyChallengeState) {
        context.dailyChallengeDataStore.edit { prefs ->
            val current = prefs[key]?.let { encoded ->
                runCatching { json.decodeFromString<DailyChallengeSnapshot>(encoded).state }.getOrNull()
            } ?: DailyChallengeState()
            prefs[key] = json.encodeToString(DailyChallengeSnapshot(transform(current)))
        }
    }
}

@Serializable
private data class DailyChallengeSnapshot(
    val state: DailyChallengeState,
    val schemaVersion: Int = DAILY_CHALLENGE_SCHEMA_VERSION,
)
