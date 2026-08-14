package com.battleheim.quantum2048.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.battleheim.quantum2048.domain.AchievementEvaluator
import com.battleheim.quantum2048.domain.AchievementProgress
import com.battleheim.quantum2048.domain.AchievementsRepository
import com.battleheim.quantum2048.domain.AchievementsState
import com.battleheim.quantum2048.domain.DailyChallengeRepository
import com.battleheim.quantum2048.domain.StatisticsRepository
import com.battleheim.quantum2048.domain.observeEvaluatedAchievements
import com.battleheim.quantum2048.engine.GameMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val Context.achievementsDataStore by preferencesDataStore("achievements_state_v1")
private const val ACHIEVEMENTS_SCHEMA_VERSION = 1

class DataStoreAchievementsRepository(
    private val context: Context,
    private val statisticsRepository: StatisticsRepository,
    private val dailyChallengeRepository: DailyChallengeRepository,
) : AchievementsRepository {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val key = stringPreferencesKey("achievements_snapshot_v1")

    private val stored: Flow<AchievementsState> = context.achievementsDataStore.data.map { prefs ->
        prefs[key]?.let { encoded ->
            runCatching { json.decodeFromString<AchievementsSnapshot>(encoded).state }.getOrNull()
        } ?: AchievementsState()
    }

    override fun observeAchievements(): Flow<List<AchievementProgress>> =
        observeEvaluatedAchievements(statisticsRepository, dailyChallengeRepository, stored)

    override suspend fun refresh(nowMillis: Long): List<AchievementProgress> {
        val classic = statisticsRepository.observeStatistics(GameMode.CLASSIC).first()
        val quantum = statisticsRepository.observeStatistics(GameMode.QUANTUM).first()
        val daily = dailyChallengeRepository.observe().first()
        val current = stored.first()
        val evaluated = AchievementEvaluator.evaluate(classic, quantum, daily, current, nowMillis)
        if (evaluated.newlyCompleted.isNotEmpty()) {
            context.achievementsDataStore.edit { prefs ->
                prefs[key] = json.encodeToString(AchievementsSnapshot(evaluated.nextState))
            }
        }
        return evaluated.newlyCompleted
    }

    override suspend fun clear() {
        context.achievementsDataStore.edit { it.remove(key) }
    }
}

@Serializable
private data class AchievementsSnapshot(
    val state: AchievementsState,
    val schemaVersion: Int = ACHIEVEMENTS_SCHEMA_VERSION,
)
