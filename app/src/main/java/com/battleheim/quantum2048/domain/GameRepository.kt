package com.battleheim.quantum2048.domain

import com.battleheim.quantum2048.engine.GameMode
import com.battleheim.quantum2048.engine.GameState
import kotlinx.coroutines.flow.Flow

interface GameRepository {
    fun observe(mode: GameMode): Flow<GameState?>
    suspend fun save(state: GameState)
    suspend fun clear(mode: GameMode)
}
