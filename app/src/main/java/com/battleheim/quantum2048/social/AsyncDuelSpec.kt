package com.battleheim.quantum2048.social

import com.battleheim.quantum2048.engine.BotDifficulty
import com.battleheim.quantum2048.engine.Difficulty
import kotlinx.serialization.Serializable

@Serializable
data class RemoteDuelOpponent(
    val playerId: String,
    val displayName: String,
)

@Serializable
data class AsyncDuelConfig(
    val difficulty: Difficulty = Difficulty.QUANTUM,
    val boardSize: Int = 4,
    val turnTimeoutHours: Int = 24,
    val fallbackBotDifficulty: BotDifficulty = BotDifficulty.NORMAL,
)

@Serializable
data class AsyncDuelTurnEnvelope(
    val duelId: String,
    val turnNumber: Int,
    val actingPlayerId: String,
    val compressedGameState: String,
    val createdAtMillis: Long,
)

/**
 * Phase 4 spec-only contract for future Play Games / backend turn-based duels.
 * The current app keeps local bot and pass-and-play duel logic untouched.
 */
interface AsyncDuelGateway {
    suspend fun create(config: AsyncDuelConfig, opponent: RemoteDuelOpponent): String
    suspend fun submitTurn(turn: AsyncDuelTurnEnvelope)
    suspend fun pendingTurns(): List<AsyncDuelTurnEnvelope>
}

class OfflineAsyncDuelGateway : AsyncDuelGateway {
    override suspend fun create(config: AsyncDuelConfig, opponent: RemoteDuelOpponent): String =
        "offline-${opponent.playerId}-${System.currentTimeMillis()}"

    override suspend fun submitTurn(turn: AsyncDuelTurnEnvelope) = Unit

    override suspend fun pendingTurns(): List<AsyncDuelTurnEnvelope> = emptyList()
}
