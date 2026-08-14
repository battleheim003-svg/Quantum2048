package com.battleheim.quantum2048.domain

import com.battleheim.quantum2048.engine.GameMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AchievementEvaluatorTest {
    @Test
    fun completesBooleanAndCumulativeAchievementsFromSnapshots() {
        val classic = StatsSnapshot(
            mode = GameMode.CLASSIC,
            highestTile = 4096,
            gamesPlayed = 1,
            totalMerges = 600,
            longestWinStreak = 5,
        )
        val quantum = StatsSnapshot(
            mode = GameMode.QUANTUM,
            highestTile = 2048,
            gamesPlayed = 1,
            totalMerges = 400,
            manualCollapseLow = 100,
            manualCollapseHigh = 100,
            longestWinStreak = 3,
        )
        val daily = DailyChallengeState(
            results = (1..30).map { day ->
                DailyChallengeResult("2026-08-${day.toString().padStart(2, '0')}", day * 10L)
            },
        )

        val evaluation = AchievementEvaluator.evaluate(classic, quantum, daily, AchievementsState(), nowMillis = 123L)

        assertEquals(AchievementCatalog.all.size, evaluation.newlyCompleted.size)
        assertTrue(evaluation.achievements.all { it.isCompleted })
        assertEquals(AchievementCatalog.all.map { it.id }.toSet(), evaluation.nextState.completedAtMillis.keys)
    }

    @Test
    fun partialProgressDoesNotCompleteUntilTargetIsReached() {
        val classic = StatsSnapshot(mode = GameMode.CLASSIC, gamesPlayed = 1, totalMerges = 25)
        val quantum = StatsSnapshot(mode = GameMode.QUANTUM, manualCollapseLow = 99, manualCollapseHigh = 1)
        val daily = DailyChallengeState(
            results = (1..4).map { day ->
                DailyChallengeResult("2026-08-${day.toString().padStart(2, '0')}", 10)
            },
        )

        val evaluation = AchievementEvaluator.evaluate(classic, quantum, daily, AchievementsState(), nowMillis = 123L)
        val lowCollapse = evaluation.achievements.single { it.achievement.id == AchievementCatalog.LOW_COLLAPSE_100 }
        val dailyFive = evaluation.achievements.single { it.achievement.id == AchievementCatalog.DAILY_STREAK_5 }

        assertEquals(99, lowCollapse.current)
        assertEquals(false, lowCollapse.isCompleted)
        assertEquals(4, dailyFive.current)
        assertEquals(false, dailyFive.isCompleted)
    }

    @Test
    fun completedAchievementIsIdempotentAndKeepsOriginalTimestamp() {
        val classic = StatsSnapshot(mode = GameMode.CLASSIC, gamesPlayed = 1)
        val quantum = StatsSnapshot(mode = GameMode.QUANTUM)
        val stored = AchievementsState(
            completedAtMillis = mapOf(AchievementCatalog.CLASSIC_FIRST_GAME to 111L),
        )

        val evaluation = AchievementEvaluator.evaluate(classic, quantum, DailyChallengeState(), stored, nowMillis = 999L)
        val classicFirst = evaluation.achievements.single { it.achievement.id == AchievementCatalog.CLASSIC_FIRST_GAME }

        assertEquals(111L, classicFirst.completedAtMillis)
        assertTrue(evaluation.newlyCompleted.isEmpty())
        assertEquals(111L, evaluation.nextState.completedAtMillis.getValue(AchievementCatalog.CLASSIC_FIRST_GAME))
    }
}
