package com.battleheim.quantum2048.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.battleheim.quantum2048.domain.ProfileRepository
import com.battleheim.quantum2048.domain.ProfileState
import com.battleheim.quantum2048.engine.GameState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val Context.profileDataStore by preferencesDataStore("profile_state_v1")
private const val PROFILE_SNAPSHOT_SCHEMA_VERSION = 1

class DataStoreProfileRepository(private val context: Context) : ProfileRepository {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val key = stringPreferencesKey("profile_snapshot_v1")

    override fun observe(): Flow<ProfileState> = context.profileDataStore.data.map { prefs ->
        prefs[key]?.let { encoded ->
            runCatching { json.decodeFromString<ProfileSnapshot>(encoded).state }.getOrNull()
        } ?: ProfileState()
    }

    override suspend fun record(game: GameState) {
        context.profileDataStore.edit { prefs ->
            val current = prefs[key]?.let { encoded ->
                runCatching { json.decodeFromString<ProfileSnapshot>(encoded).state }.getOrNull()
            } ?: ProfileState()
            prefs[key] = json.encodeToString(ProfileSnapshot(state = current.record(game)))
        }
    }

    override suspend fun clear() {
        context.profileDataStore.edit { prefs -> prefs.remove(key) }
    }
}

@Serializable
private data class ProfileSnapshot(
    val schemaVersion: Int = PROFILE_SNAPSHOT_SCHEMA_VERSION,
    val state: ProfileState = ProfileState(),
)
