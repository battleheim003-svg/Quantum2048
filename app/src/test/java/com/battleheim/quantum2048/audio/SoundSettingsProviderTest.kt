package com.battleheim.quantum2048.audio

import com.battleheim.quantum2048.domain.AppLanguage
import com.battleheim.quantum2048.domain.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SoundSettingsProviderTest {
    @Test
    fun providerProjectsSoundAndHapticToggles() = runTest {
        val source = MutableStateFlow(AppSettings(soundEnabled = false, hapticsEnabled = true))
        val provider = AppSoundSettingsProvider(source)

        assertEquals(SoundHapticSettings(soundEnabled = false, hapticsEnabled = true), provider.observeSoundSettings().first())

        source.value = source.value.copy(soundEnabled = true, hapticsEnabled = false)

        assertEquals(SoundHapticSettings(soundEnabled = true, hapticsEnabled = false), provider.observeSoundSettings().first())
    }

    @Test
    fun settingsCopyPersistsLanguageSoundHapticsAndTutorialFlags() {
        val settings = AppSettings()
            .copy(language = AppLanguage.PERSIAN)
            .copy(soundEnabled = false)
            .copy(hapticsEnabled = false)
            .copy(tutorialCompleted = true)

        assertEquals(AppLanguage.PERSIAN, settings.language)
        assertEquals(false, settings.soundEnabled)
        assertEquals(false, settings.hapticsEnabled)
        assertEquals(true, settings.tutorialCompleted)
    }
}
