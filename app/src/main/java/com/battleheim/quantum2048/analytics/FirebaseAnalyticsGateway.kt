package com.battleheim.quantum2048.analytics

import android.util.Log
import com.battleheim.quantum2048.domain.AppLanguage
import com.battleheim.quantum2048.domain.AppThemeMode
import com.battleheim.quantum2048.engine.BotDifficulty
import com.battleheim.quantum2048.engine.Difficulty
import com.battleheim.quantum2048.engine.DuelOpponent
import com.battleheim.quantum2048.engine.DuelPlayer
import com.battleheim.quantum2048.engine.FusionRules
import com.battleheim.quantum2048.engine.GameState
import com.battleheim.quantum2048.engine.QuantumElement

/**
 * Offline-safe analytics implementation for regions where Firebase/Google Maven
 * dependencies cannot be resolved. It preserves the analytics contract and writes
 * debug-friendly event lines to Logcat without adding network SDKs.
 */
class OfflineAnalyticsGateway : AnalyticsGateway {

    override fun logSessionStart() {
        event("session_start")
    }

    override fun logSessionEnd() {
        event("session_end")
    }

    override fun logLevelStart(game: GameState) {
        event("level_start", game.levelParams())
    }

    override fun logLevelComplete(game: GameState) {
        event("level_complete", game.levelParams())
    }

    override fun logLevelFail(game: GameState) {
        event("level_fail", game.levelParams())
    }

    override fun logFusionPerformed(element: QuantumElement) {
        event(
            "fusion_performed",
            mapOf(
                "element_symbol" to element.symbol,
                "element_name" to element.title,
                "element_rank" to element.rank,
                "atomic_number" to element.atomicNumber,
            ),
        )
    }

    override fun logDuelStarted(difficulty: Difficulty, opponent: DuelOpponent, botDifficulty: BotDifficulty) {
        event(
            "duel_started",
            mapOf(
                "difficulty" to difficulty.name.lowercase(),
                "opponent" to opponent.name.lowercase(),
                "bot_difficulty" to botDifficulty.name.lowercase(),
            ),
        )
    }

    override fun logDuelResult(difficulty: Difficulty, botDifficulty: BotDifficulty, winner: DuelPlayer?) {
        event(
            "duel_result",
            mapOf(
                "difficulty" to difficulty.name.lowercase(),
                "bot_difficulty" to botDifficulty.name.lowercase(),
                "winner" to (winner?.name?.lowercase() ?: "none"),
            ),
        )
    }

    override fun logSettingsChanged(theme: AppThemeMode, language: AppLanguage) {
        event(
            "settings_changed",
            mapOf(
                "theme" to theme.name.lowercase(),
                "language" to language.name.lowercase(),
            ),
        )
    }

    private fun GameState.levelParams(): Map<String, Any> =
        mapOf(
            "difficulty" to difficulty.name.lowercase(),
            "game_mode" to modeName(),
            "board_size" to size,
            "score" to score,
            "moves" to moveCount,
            "energy" to energy,
            "best_element" to (cells.mapNotNull { it?.element }.maxByOrNull { it.rank }?.symbol ?: "none"),
            "best_value" to (cells.filterNotNull().maxOfOrNull { FusionRules.gameValueOf(it) } ?: 0),
        )

    private fun event(name: String, params: Map<String, Any> = emptyMap()) {
        Log.d(TAG, "$name $params")
    }

    private companion object {
        const val TAG = "QuantumAnalytics"
    }
}
