package com.battleheim.quantum2048.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.battleheim.quantum2048.domain.SocialRepository
import com.battleheim.quantum2048.domain.SocialState
import com.battleheim.quantum2048.engine.BotDifficulty
import com.battleheim.quantum2048.engine.Difficulty
import com.battleheim.quantum2048.engine.DuelOpponent
import com.battleheim.quantum2048.engine.DuelPlayer
import com.battleheim.quantum2048.engine.GameState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val Context.socialDataStore by preferencesDataStore("social_state_v1")
private const val SOCIAL_SNAPSHOT_SCHEMA_VERSION = 1

class DataStoreSocialRepository(private val context: Context) : SocialRepository {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val key = stringPreferencesKey("social_snapshot_v1")

    override fun observe(): Flow<SocialState> = context.socialDataStore.data.map { prefs ->
        prefs[key]?.let { encoded ->
            runCatching { json.decodeFromString<SocialSnapshot>(encoded).state }.getOrNull()
        } ?: SocialState()
    }

    override suspend fun recordGame(game: GameState) {
        update { it.recordGame(game) }
    }

    override suspend fun recordDuelResult(
        difficulty: Difficulty,
        opponent: DuelOpponent,
        botDifficulty: BotDifficulty,
        winner: DuelPlayer?,
    ) {
        update { it.recordDuelResult(winner) }
    }

    override suspend fun syncAchievements(achievementIds: Set<String>) {
        update { it.syncAchievements(achievementIds) }
    }

    override suspend fun clear() {
        context.socialDataStore.edit { prefs -> prefs.remove(key) }
    }

    private suspend fun update(transform: (SocialState) -> SocialState) {
        context.socialDataStore.edit { prefs ->
            val current = prefs[key]?.let { encoded ->
                runCatching { json.decodeFromString<SocialSnapshot>(encoded).state }.getOrNull()
            } ?: SocialState()
            prefs[key] = json.encodeToString(SocialSnapshot(state = transform(current)))
        }
    }
}

@Serializable
private data class SocialSnapshot(
    val schemaVersion: Int = SOCIAL_SNAPSHOT_SCHEMA_VERSION,
    val state: SocialState = SocialState(),
)
