package com.battleheim.quantum2048.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Serializable
data class AppSettings(
    val soundEnabled: Boolean = true,
    val musicEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val reducedMotion: Boolean = false,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
)

@Serializable enum class AppLanguage { SYSTEM, ENGLISH, PERSIAN }
@Serializable enum class AppThemeMode { SYSTEM, DARK, LIGHT }

interface SettingsRepository {
    fun observe(): Flow<AppSettings>
    suspend fun save(settings: AppSettings)
}
