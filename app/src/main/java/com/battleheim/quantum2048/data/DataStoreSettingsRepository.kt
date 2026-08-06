package com.battleheim.quantum2048.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.battleheim.quantum2048.domain.AppSettings
import com.battleheim.quantum2048.domain.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val Context.settingsDataStore by preferencesDataStore("settings_state_v1")

class DataStoreSettingsRepository(private val context: Context) : SettingsRepository {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val key = stringPreferencesKey("settings_snapshot_v1")

    override fun observe(): Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        prefs[key]?.let { encoded ->
            runCatching { json.decodeFromString<SettingsSnapshot>(encoded).settings }.getOrNull()
        } ?: AppSettings()
    }

    override suspend fun save(settings: AppSettings) {
        context.settingsDataStore.edit { prefs ->
            prefs[key] = json.encodeToString(SettingsSnapshot(settings = settings))
        }
    }
}

@Serializable
private data class SettingsSnapshot(
    val schemaVersion: Int = 1,
    val settings: AppSettings = AppSettings(),
)
