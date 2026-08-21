package com.battleheim.quantum2048.audio

import com.battleheim.quantum2048.domain.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class SoundHapticSettings(
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
)

interface SoundSettingsProvider {
    fun observeSoundSettings(): Flow<SoundHapticSettings>
}

class AppSoundSettingsProvider(
    private val settings: Flow<AppSettings>,
) : SoundSettingsProvider {
    override fun observeSoundSettings(): Flow<SoundHapticSettings> =
        settings.map { SoundHapticSettings(soundEnabled = it.soundEnabled, hapticsEnabled = it.hapticsEnabled) }
}
