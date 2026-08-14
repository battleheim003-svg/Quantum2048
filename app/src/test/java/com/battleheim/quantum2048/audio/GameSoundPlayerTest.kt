package com.battleheim.quantum2048.audio

import com.battleheim.quantum2048.engine.Difficulty
import com.battleheim.quantum2048.engine.GameMode
import com.battleheim.quantum2048.engine.GameState
import com.battleheim.quantum2048.engine.MoveResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GameSoundPlayerTest {
    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun soundPlayerDoesNotPlayWhenSoundIsDisabled() = runTest {
        val settings = MutableStateFlow(SoundHapticSettings(soundEnabled = false, hapticsEnabled = true))
        val playback = FakePlayback()
        val player = GameSoundPlayer(FakeSoundSettingsProvider(settings), playback, TestScope(StandardTestDispatcher(testScheduler)))
        advanceUntilIdle()

        player.onSoundEvent(SoundEvent.Merge)

        assertEquals(emptyList<SoundEvent>(), playback.played)
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun hapticControllerDoesNotPerformWhenHapticsAreDisabled() = runTest {
        val settings = MutableStateFlow(SoundHapticSettings(soundEnabled = true, hapticsEnabled = false))
        val performer = FakeHapticPerformer()
        val controller = HapticFeedbackController(FakeSoundSettingsProvider(settings), performer, TestScope(StandardTestDispatcher(testScheduler)))
        advanceUntilIdle()

        controller.onHapticEvent(HapticEvent.Merge)

        assertEquals(emptyList<HapticEvent>(), performer.performed)
    }

    @Test
    fun chainMergeMapsToChainMergeEvent() {
        val before = GameState(mode = GameMode.QUANTUM, difficulty = Difficulty.QUANTUM, energy = 0)
        val result = MoveResult(
            state = before.copy(energy = 15),
            changed = true,
            mergeCount = 2,
        )

        assertEquals(listOf(SoundEvent.ChainMerge), soundEventsForMove(before, result))
        assertEquals(listOf(HapticEvent.ChainMerge), hapticEventsForMove(before, result))
    }

    private class FakeSoundSettingsProvider(
        private val settings: MutableStateFlow<SoundHapticSettings>,
    ) : SoundSettingsProvider {
        override fun observeSoundSettings(): Flow<SoundHapticSettings> = settings
    }

    private class FakePlayback : SoundPlaybackEngine {
        val played = mutableListOf<SoundEvent>()
        override fun play(event: SoundEvent) {
            played += event
        }
    }

    private class FakeHapticPerformer : HapticPerformer {
        val performed = mutableListOf<HapticEvent>()
        override fun perform(event: HapticEvent) {
            performed += event
        }
    }
}
