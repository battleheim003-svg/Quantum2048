package com.battleheim.quantum2048.domain

import com.battleheim.quantum2048.engine.GameMode
import com.battleheim.quantum2048.engine.GameState
import com.battleheim.quantum2048.engine.Difficulty
import kotlinx.coroutines.flow.Flow

interface GameRepository {
    fun observe(mode: GameMode): Flow<GameState?>
    fun observe(difficulty: Difficulty): Flow<GameState?>
    fun observe(difficulty: Difficulty, size: Int): Flow<GameState?>
    suspend fun save(state: GameState)
    suspend fun clear(mode: GameMode)
    suspend fun clear(difficulty: Difficulty)
    suspend fun clear(difficulty: Difficulty, size: Int)
    suspend fun clearAll()
}
