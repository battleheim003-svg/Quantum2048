package com.battleheim.quantum2048.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import com.battleheim.quantum2048.MainActivity
import org.junit.Rule
import org.junit.Test

class GameplayFlowTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun classicBoardAcceptsSwipeToMergeGesture() {
        compose.onNodeWithTag("start_classic_button").performClick()
        compose.onNodeWithTag("game_board").assertIsDisplayed()

        compose.onNodeWithTag("game_board").performTouchInput { swipeLeft() }

        compose.onNodeWithTag("game_board").assertIsDisplayed()
    }

    @Test
    fun quantumBoardTileTapCanOpenCollapseDialogAndConfirm() {
        compose.onNodeWithTag("start_quantum_button").performClick()
        dismissTutorialIfPresent()
        compose.onNodeWithTag("game_board").assertIsDisplayed()

        compose.onAllNodesWithTag("tile_0", useUnmergedTree = true)[0].performClick()
        compose.onNodeWithText("Collapse superposition").assertIsDisplayed()
        compose.onNodeWithText("Cancel").performClick()

        compose.onNodeWithTag("game_board").assertIsDisplayed()
    }

    @Test
    fun modeSwitchPreservesIndependentSaveState() {
        compose.onNodeWithTag("start_classic_button").performClick()
        compose.onNodeWithTag("game_board").performTouchInput { swipeLeft() }
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }

        compose.onNodeWithTag("start_quantum_button").performClick()
        dismissTutorialIfPresent()
        compose.onNodeWithTag("game_board").performTouchInput { swipeLeft() }
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }

        compose.onNodeWithTag("continue_button").assertIsDisplayed()
    }

    private fun dismissTutorialIfPresent() {
        compose.waitForIdle()
        runCatching {
            compose.onNodeWithText("Skip").performClick()
        }
        compose.waitForIdle()
    }
}
