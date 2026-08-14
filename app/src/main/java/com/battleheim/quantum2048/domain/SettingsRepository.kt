package com.battleheim.quantum2048.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Serializable
data class AppSettings(
    val soundEnabled: Boolean = true,
    val musicEnabled: Boolean = true,
    val masterVolume: Float = 0.86f,
    val musicVolume: Float = 0.56f,
    val sfxVolume: Float = 0.92f,
    val hapticsEnabled: Boolean = true,
    val reducedMotion: Boolean = false,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val themeMode: AppThemeMode = AppThemeMode.DARK,
)

@Serializable enum class AppLanguage { ENGLISH, PERSIAN }
@Serializable enum class AppThemeMode { DARK, LIGHT }

interface SettingsRepository {
    fun observe(): Flow<AppSettings>
    suspend fun save(settings: AppSettings)
}
