package com.battleheim.quantum2048.analytics

import com.battleheim.quantum2048.domain.AppLanguage
import com.battleheim.quantum2048.domain.AppThemeMode
import com.battleheim.quantum2048.engine.BotDifficulty
import com.battleheim.quantum2048.engine.Difficulty
import com.battleheim.quantum2048.engine.DuelOpponent
import com.battleheim.quantum2048.engine.DuelPlayer
import com.battleheim.quantum2048.engine.GameMode
import com.battleheim.quantum2048.engine.GameState
import com.battleheim.quantum2048.engine.QuantumElement

interface AnalyticsGateway {
    fun logSessionStart()
    fun logSessionEnd()
    fun logLevelStart(game: GameState)
    fun logLevelComplete(game: GameState)
    fun logLevelFail(game: GameState)
    fun logFusionPerformed(element: QuantumElement)
    fun logDuelStarted(difficulty: Difficulty, opponent: DuelOpponent, botDifficulty: BotDifficulty)
    fun logDuelResult(difficulty: Difficulty, botDifficulty: BotDifficulty, winner: DuelPlayer?)
    fun logSettingsChanged(theme: AppThemeMode, language: AppLanguage)
}

object NoOpAnalyticsGateway : AnalyticsGateway {
    override fun logSessionStart() = Unit
    override fun logSessionEnd() = Unit
    override fun logLevelStart(game: GameState) = Unit
    override fun logLevelComplete(game: GameState) = Unit
    override fun logLevelFail(game: GameState) = Unit
    override fun logFusionPerformed(element: QuantumElement) = Unit
    override fun logDuelStarted(difficulty: Difficulty, opponent: DuelOpponent, botDifficulty: BotDifficulty) = Unit
    override fun logDuelResult(difficulty: Difficulty, botDifficulty: BotDifficulty, winner: DuelPlayer?) = Unit
    override fun logSettingsChanged(theme: AppThemeMode, language: AppLanguage) = Unit
}

internal fun GameState.modeName(): String = when (mode) {
    GameMode.CLASSIC -> "classic"
    GameMode.QUANTUM -> "quantum"
}
