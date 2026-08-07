package com.battleheim.quantum2048.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.battleheim.quantum2048.domain.GameRepository
import com.battleheim.quantum2048.engine.Difficulty
import com.battleheim.quantum2048.engine.GameMode
import com.battleheim.quantum2048.engine.GameState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.gameDataStore by preferencesDataStore("game_state_v1")

class DataStoreGameRepository(private val context: Context) : GameRepository {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private fun key(mode: GameMode) = stringPreferencesKey("snapshot_${mode.name.lowercase()}")
    private fun key(difficulty: Difficulty) = stringPreferencesKey("snapshot_v3_${difficulty.name.lowercase()}")
    private fun key(difficulty: Difficulty, size: Int) = stringPreferencesKey("snapshot_v3_${difficulty.name.lowercase()}_${size}x$size")

    override fun observe(mode: GameMode): Flow<GameState?> = context.gameDataStore.data.map { prefs ->
        val difficulty = Difficulty.fromMode(mode)
        prefs[key(difficulty)]?.decodeState() ?: prefs[key(mode)]?.decodeState()?.copy(difficulty = difficulty, mode = difficulty.mode)
    }

    override fun observe(difficulty: Difficulty): Flow<GameState?> = context.gameDataStore.data.map { prefs ->
        prefs[key(difficulty, DEFAULT_SIZE)]?.decodeState()
            ?: prefs[key(difficulty)]?.decodeState()
            ?: legacyModeFor(difficulty)?.let { mode ->
                prefs[key(mode)]?.decodeState()?.copy(difficulty = difficulty, mode = difficulty.mode)
            }
    }

    override fun observe(difficulty: Difficulty, size: Int): Flow<GameState?> = context.gameDataStore.data.map { prefs ->
        prefs[key(difficulty, size)]?.decodeState()
            ?: if (size == DEFAULT_SIZE) {
                prefs[key(difficulty)]?.decodeState()
                    ?: legacyModeFor(difficulty)?.let { mode ->
                        prefs[key(mode)]?.decodeState()?.copy(difficulty = difficulty, mode = difficulty.mode)
                    }
            } else {
                null
            }
    }

    override suspend fun save(state: GameState) {
        context.gameDataStore.edit {
            it[key(state.difficulty, state.size)] = json.encodeToString(Snapshot(state = state.copy(mode = state.difficulty.mode)))
        }
    }

    override suspend fun clear(mode: GameMode) {
        clear(Difficulty.fromMode(mode))
    }

    override suspend fun clear(difficulty: Difficulty) {
        context.gameDataStore.edit {
            it.remove(key(difficulty))
            BOARD_SIZES.forEach { size -> it.remove(key(difficulty, size)) }
        }
    }

    override suspend fun clear(difficulty: Difficulty, size: Int) {
        context.gameDataStore.edit { it.remove(key(difficulty, size)) }
    }

    private fun String.decodeState(): GameState? =
        runCatching { json.decodeFromString<Snapshot>(this).state }.getOrNull()

    private fun legacyModeFor(difficulty: Difficulty): GameMode? = when (difficulty) {
        Difficulty.EASY -> GameMode.CLASSIC
        Difficulty.QUANTUM -> GameMode.QUANTUM
        Difficulty.MEDIUM, Difficulty.HARD, Difficulty.ZEN, Difficulty.HARDCORE, Difficulty.PUZZLE, Difficulty.DAILY -> null
    }

    private companion object {
        const val DEFAULT_SIZE = 4
        val BOARD_SIZES = listOf(4, 6, 8)
    }
}
