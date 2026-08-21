package com.battleheim.quantum2048.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.battleheim.quantum2048.domain.AppLanguage
import com.battleheim.quantum2048.domain.AppSettings
import com.battleheim.quantum2048.domain.AppThemeMode
import com.battleheim.quantum2048.domain.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private val Context.settingsDataStore by preferencesDataStore("settings_state_v1")

class DataStoreSettingsRepository(private val context: Context) : SettingsRepository {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val key = stringPreferencesKey("settings_snapshot_v1")

    override fun observe(): Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        prefs[key]?.let { encoded ->
            runCatching { json.decodeFromString<SettingsSnapshot>(encoded).settings }.getOrNull()
                ?: decodeLegacySettings(encoded)
        } ?: context.defaultResolvedSettings()
    }

    override suspend fun save(settings: AppSettings) {
        context.settingsDataStore.edit { prefs ->
            prefs[key] = json.encodeToString(SettingsSnapshot(settings = settings))
        }
    }

    private fun decodeLegacySettings(encoded: String): AppSettings? =
        runCatching {
            val element = Json.parseToJsonElement(encoded)
                .jsonObject["settings"]
                ?.jsonObject
            val language = when (element?.get("language")?.jsonPrimitive?.contentOrNull) {
                "PERSIAN" -> AppLanguage.PERSIAN
                "ENGLISH" -> AppLanguage.ENGLISH
                else -> null
            }
            val theme = when (element?.get("themeMode")?.jsonPrimitive?.contentOrNull) {
                "SYSTEM" -> AppThemeMode.DARK
                "LIGHT" -> AppThemeMode.LIGHT
                "DARK" -> AppThemeMode.DARK
                else -> null
            }
            AppSettings(
                soundEnabled = element?.get("soundEnabled")?.jsonPrimitive?.booleanOrNull ?: true,
                musicEnabled = element?.get("musicEnabled")?.jsonPrimitive?.booleanOrNull ?: true,
                masterVolume = element?.get("masterVolume")?.jsonPrimitive?.floatOrNull ?: 0.86f,
                musicVolume = element?.get("musicVolume")?.jsonPrimitive?.floatOrNull ?: 0.56f,
                sfxVolume = element?.get("sfxVolume")?.jsonPrimitive?.floatOrNull ?: 0.92f,
                hapticsEnabled = element?.get("hapticsEnabled")?.jsonPrimitive?.booleanOrNull ?: true,
                reducedMotion = element?.get("reducedMotion")?.jsonPrimitive?.booleanOrNull ?: false,
                language = language ?: context.defaultResolvedSettings().language,
                themeMode = theme ?: context.defaultResolvedSettings().themeMode,
                tutorialCompleted = element?.get("tutorialCompleted")?.jsonPrimitive?.booleanOrNull ?: false,
                entanglementIntroSeen = element?.get("entanglementIntroSeen")?.jsonPrimitive?.booleanOrNull ?: false,
            )
        }.getOrNull()
}

@Serializable
private data class SettingsSnapshot(
    val schemaVersion: Int = 3,
    val settings: AppSettings = AppSettings(),
)

private fun Context.defaultResolvedSettings(): AppSettings {
    val localeTag = resources.configuration.locales[0]?.language.orEmpty()
    return AppSettings(
        language = if (localeTag == "fa") AppLanguage.PERSIAN else AppLanguage.ENGLISH,
        themeMode = AppThemeMode.DARK,
    )
}
