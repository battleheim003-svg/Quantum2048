package com.battleheim.quantum2048.audio

import com.battleheim.quantum2048.engine.FusionRules
import com.battleheim.quantum2048.engine.GameStatus
import com.battleheim.quantum2048.engine.GameState
import com.battleheim.quantum2048.engine.MoveResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

enum class SoundEvent {
    Merge,
    ChainMerge,
    CollapseManual,
    CollapseAuto,
    EnergyFull,
    GameOver,
}

enum class HapticEvent {
    Merge,
    ChainMerge,
    CollapseManual,
    CollapseAuto,
    EnergyFull,
    GameOver,
}

interface SoundEventSink {
    fun onSoundEvent(event: SoundEvent)
}

interface HapticEventSink {
    fun onHapticEvent(event: HapticEvent)
}

object NoOpSoundEventSink : SoundEventSink {
    override fun onSoundEvent(event: SoundEvent) = Unit
}

object NoOpHapticEventSink : HapticEventSink {
    override fun onHapticEvent(event: HapticEvent) = Unit
}

interface SoundPlaybackEngine {
    fun play(event: SoundEvent)
}

class GameAudioSoundPlaybackEngine(
    private val audio: GameAudio,
) : SoundPlaybackEngine {
    override fun play(event: SoundEvent) {
        when (event) {
            SoundEvent.Merge -> audio.merge()
            SoundEvent.ChainMerge -> audio.reaction()
            SoundEvent.CollapseManual -> audio.collapseHigh()
            SoundEvent.CollapseAuto -> audio.collapseLow()
            SoundEvent.EnergyFull -> audio.unlock()
            SoundEvent.GameOver -> audio.gameOver()
        }
    }
}

class GameSoundPlayer(
    settingsProvider: SoundSettingsProvider,
    private val playback: SoundPlaybackEngine,
    scope: CoroutineScope,
) : SoundEventSink {
    private var soundEnabled = true

    init {
        settingsProvider.observeSoundSettings()
            .onEach { soundEnabled = it.soundEnabled }
            .launchIn(scope)
    }

    override fun onSoundEvent(event: SoundEvent) {
        if (soundEnabled) playback.play(event)
    }
}

fun soundEventsForMove(before: GameState, result: MoveResult): List<SoundEvent> = buildList {
    if (result.mergeCount > 1) {
        add(SoundEvent.ChainMerge)
    } else if (result.mergeCount == 1) {
        add(SoundEvent.Merge)
    }
    if (result.entanglementCollapseCount > 0) add(SoundEvent.CollapseAuto)
    val maxEnergy = FusionRules.maxEnergyFor(result.state.difficulty)
    if (before.energy < maxEnergy && result.state.energy >= maxEnergy) add(SoundEvent.EnergyFull)
    if (before.status != GameStatus.LOST && result.state.status == GameStatus.LOST) add(SoundEvent.GameOver)
}

fun hapticEventsForMove(before: GameState, result: MoveResult): List<HapticEvent> =
    soundEventsForMove(before, result).map {
        when (it) {
            SoundEvent.Merge -> HapticEvent.Merge
            SoundEvent.ChainMerge -> HapticEvent.ChainMerge
            SoundEvent.CollapseManual -> HapticEvent.CollapseManual
            SoundEvent.CollapseAuto -> HapticEvent.CollapseAuto
            SoundEvent.EnergyFull -> HapticEvent.EnergyFull
            SoundEvent.GameOver -> HapticEvent.GameOver
        }
    }
