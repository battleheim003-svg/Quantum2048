package com.battleheim.quantum2048.audio

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

interface HapticPerformer {
    fun perform(event: HapticEvent)
}

class AndroidHapticPerformer(context: Context) : HapticPerformer {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Vibrator::class.java)
    }

    override fun perform(event: HapticEvent) {
        val duration = when (event) {
            HapticEvent.Merge -> 14L
            HapticEvent.ChainMerge -> 28L
            HapticEvent.CollapseManual -> 22L
            HapticEvent.CollapseAuto -> 12L
            HapticEvent.EnergyFull -> 34L
            HapticEvent.GameOver -> 48L
        }
        val amplitude = when (event) {
            HapticEvent.Merge -> 70
            HapticEvent.ChainMerge -> 130
            HapticEvent.CollapseManual -> 110
            HapticEvent.CollapseAuto -> 60
            HapticEvent.EnergyFull -> 160
            HapticEvent.GameOver -> 190
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(duration, amplitude))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(duration)
        }
    }
}

class HapticFeedbackController(
    settingsProvider: SoundSettingsProvider,
    private val performer: HapticPerformer,
    scope: CoroutineScope,
) : HapticEventSink {
    private var hapticsEnabled = true

    init {
        settingsProvider.observeSoundSettings()
            .onEach { hapticsEnabled = it.hapticsEnabled }
            .launchIn(scope)
    }

    override fun onHapticEvent(event: HapticEvent) {
        if (hapticsEnabled) performer.perform(event)
    }
}
