package com.battleheim.quantum2048.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.battleheim.quantum2048.domain.StatisticsRepository
import com.battleheim.quantum2048.domain.StatsSnapshot
import com.battleheim.quantum2048.engine.GameMode
import com.battleheim.quantum2048.engine.GameState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val Context.statisticsDataStore by preferencesDataStore("statistics_state_v1")
private const val STATISTICS_SNAPSHOT_SCHEMA_VERSION = 1

class DataStoreStatisticsRepository(private val context: Context) : StatisticsRepository {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private fun key(mode: GameMode) = stringPreferencesKey("statistics_${mode.name.lowercase()}_v1")

    override fun observeStatistics(mode: GameMode): Flow<StatsSnapshot> =
        context.statisticsDataStore.data.map { prefs ->
            prefs[key(mode)]?.let { encoded ->
                runCatching { json.decodeFromString<StatisticsSnapshot>(encoded).stats }.getOrNull()
            } ?: StatsSnapshot(mode)
        }

    override suspend fun recordMerge(mode: GameMode, count: Int, state: GameState) {
        update(mode) { it.recordMerge(count, state) }
    }

    override suspend fun recordCollapse(mode: GameMode, lowValue: Boolean, manual: Boolean) {
        update(mode) { it.recordCollapse(lowValue = lowValue, manual = manual) }
    }

    override suspend fun recordGameEnded(mode: GameMode, state: GameState) {
        update(mode) { it.recordGameEnded(state) }
    }

    override suspend fun clear() {
        context.statisticsDataStore.edit { prefs ->
            GameMode.entries.forEach { prefs.remove(key(it)) }
        }
    }

    private suspend fun update(mode: GameMode, transform: (StatsSnapshot) -> StatsSnapshot) {
        context.statisticsDataStore.edit { prefs ->
            val current = prefs[key(mode)]?.let { encoded ->
                runCatching { json.decodeFromString<StatisticsSnapshot>(encoded).stats }.getOrNull()
            } ?: StatsSnapshot(mode)
            prefs[key(mode)] = json.encodeToString(StatisticsSnapshot(stats = transform(current).copy(mode = mode)))
        }
    }
}

@Serializable
private data class StatisticsSnapshot(
    val schemaVersion: Int = STATISTICS_SNAPSHOT_SCHEMA_VERSION,
    val stats: StatsSnapshot,
)
