package com.battleheim.quantum2048.domain

import com.battleheim.quantum2048.engine.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MainMenuStateTest {
    @Test
    fun continueIsDisabledWithoutSavedGames() {
        val state = MainMenuState()

        assertFalse(state.canContinue)
        assertEquals(null, state.preferredContinue)
    }

    @Test
    fun continueUsesAvailableSavedGame() {
        val save = SavedGameRef(Difficulty.QUANTUM, 4)
        val state = MainMenuState(savedGames = setOf(save))

        assertTrue(state.canContinue)
        assertEquals(save, state.preferredContinue)
    }

    @Test
    fun modeRouteMapsStableRouteValuesToDifficulties() {
        assertEquals(Difficulty.EASY, MainGameModeRoute.fromRoute("classic").difficulty)
        assertEquals(Difficulty.QUANTUM, MainGameModeRoute.fromRoute("quantum").difficulty)
        assertEquals(Difficulty.QUANTUM, MainGameModeRoute.fromRoute("unknown").difficulty)
    }
}
