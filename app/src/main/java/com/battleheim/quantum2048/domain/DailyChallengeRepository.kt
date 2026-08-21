package com.battleheim.quantum2048.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.roundToLong

interface DailyDateProvider {
    fun todayUtc(): String
}

class SystemDailyDateProvider(private val clock: Clock = Clock.systemUTC()) : DailyDateProvider {
    override fun todayUtc(): String = LocalDate.now(clock.withZone(ZoneOffset.UTC)).toString()
}

enum class DailyChallengeStatus { AVAILABLE, IN_PROGRESS, COMPLETED }

@Serializable
data class DailyChallengeResult(
    val date: String,
    val score: Long,
)

@Serializable
data class DailyChallengeState(
    val activeDate: String? = null,
    val hasActiveRun: Boolean = false,
    val results: List<DailyChallengeResult> = emptyList(),
) {
    fun statusFor(date: String): DailyChallengeStatus = when {
        resultFor(date) != null -> DailyChallengeStatus.COMPLETED
        activeDate == date && hasActiveRun -> DailyChallengeStatus.IN_PROGRESS
        else -> DailyChallengeStatus.AVAILABLE
    }

    fun canStart(date: String): Boolean = statusFor(date) == DailyChallengeStatus.AVAILABLE

    fun markStarted(date: String): DailyChallengeState =
        if (canStart(date)) copy(activeDate = date, hasActiveRun = true) else this

    fun recordResult(date: String, score: Long): DailyChallengeState {
        if (resultFor(date) != null) return copy(activeDate = date, hasActiveRun = false)
        val entry = DailyChallengeResult(date = date, score = score)
        return copy(
            activeDate = date,
            hasActiveRun = false,
            results = (results + entry).distinctBy { it.date }.sortedByDescending { it.date },
        )
    }

    fun resultFor(date: String): DailyChallengeResult? = results.firstOrNull { it.date == date }

    val recentResults: List<DailyChallengeResult>
        get() = results.sortedByDescending { it.date }.take(7)

    val bestScore: Long
        get() = results.maxOfOrNull { it.score } ?: 0L

    val averageScore: Long
        get() = if (results.isEmpty()) 0L else results.map { it.score }.average().roundToLong()

    val participationStreak: Int
        get() {
            val dates = results.mapNotNull { runCatching { LocalDate.parse(it.date) }.getOrNull() }.toSet()
            val latest = dates.maxOrNull() ?: return 0
            var streak = 0
            var cursor = latest
            while (cursor in dates) {
                streak++
                cursor = cursor.minusDays(1)
            }
            return streak
        }
}

interface DailyChallengeRepository {
    fun observe(): Flow<DailyChallengeState>
    suspend fun markStarted(date: String)
    suspend fun recordResult(date: String, score: Long)
    suspend fun clearActiveRun(date: String)
    suspend fun clearHistory()
}
