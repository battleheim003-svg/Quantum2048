package com.battleheim.quantum2048.domain

import com.battleheim.quantum2048.engine.Difficulty

data class MainMenuState(
    val savedGames: Set<SavedGameRef> = emptySet(),
) {
    val canContinue: Boolean get() = savedGames.isNotEmpty()
    val preferredContinue: SavedGameRef? get() = savedGames.lastOrNull()
}

data class SavedGameRef(
    val difficulty: Difficulty,
    val size: Int,
)

enum class MainGameModeRoute(val routeValue: String, val difficulty: Difficulty) {
    CLASSIC("classic", Difficulty.EASY),
    QUANTUM("quantum", Difficulty.QUANTUM);

    companion object {
        fun fromRoute(value: String?): MainGameModeRoute =
            entries.firstOrNull { it.routeValue == value } ?: QUANTUM
    }
}
