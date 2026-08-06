package com.battleheim.quantum2048.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.battleheim.quantum2048.MainActivity
import org.junit.Rule
import org.junit.Test

class AppShellNavigationTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun mainMenuLevelSelectGamePauseMainMenu() {
        compose.onNodeWithTag("new_game_button").performClick()
        compose.onNodeWithText("Select level").assertIsDisplayed()
        compose.onNodeWithTag("level_easy").performClick()
        compose.onNodeWithText("Pause").performClick()
        compose.onNodeWithTag("pause_screen").assertIsDisplayed()
        compose.onNodeWithTag("pause_main_menu").performClick()
        compose.onNodeWithText("Quantum 2048").assertIsDisplayed()
    }
}
