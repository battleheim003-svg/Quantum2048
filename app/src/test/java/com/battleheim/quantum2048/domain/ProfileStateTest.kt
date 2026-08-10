package com.battleheim.quantum2048.domain

import com.battleheim.quantum2048.engine.FusionRules
import com.battleheim.quantum2048.engine.GameState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileStateTest {
    @Test
    fun recordMergesAchievementsAndKeepsLargestCounters() {
        val profile = ProfileState(
            successfulCollapseCount = 3,
            lowCollapseCount = 2,
            highCollapseCount = 1,
            totalWinEnergy = 20,
            winEnergySamples = 1,
            totalChainMergeCount = 4,
        )
        val state = GameState(
            successfulCollapseCount = 5,
            lowCollapseCount = 4,
            highCollapseCount = 4,
            totalWinEnergy = 70,
            winEnergySamples = 2,
            totalChainMergeCount = 6,
            unlockedAchievements = setOf(FusionRules.achievementNoUndoWin),
        )

        val recorded = profile.record(state)

        assertEquals(5, recorded.successfulCollapseCount)
        assertEquals(4, recorded.lowCollapseCount)
        assertEquals(4, recorded.highCollapseCount)
        assertEquals(70, recorded.totalWinEnergy)
        assertEquals(2, recorded.winEnergySamples)
        assertEquals(6, recorded.totalChainMergeCount)
        assertTrue(FusionRules.achievementNoUndoWin in recorded.unlockedAchievements)
    }

    @Test
    fun derivedStatisticsUseProfileCounters() {
        val profile = ProfileState(lowCollapseCount = 3, highCollapseCount = 1, totalWinEnergy = 90, winEnergySamples = 3)

        assertEquals(0.75, profile.collapseLowRatio, 0.001)
        assertEquals(30.0, profile.averageWinEnergy, 0.001)
    }

    @Test
    fun recordKeepsDailyBestScoreByDate() {
        val profile = ProfileState(dailyBestScores = mapOf("2026-08-07" to 80L))
        val lower = GameState(
            score = 70,
            dailyBestScore = 75,
            dailyChallengeDate = "2026-08-07",
        )
        val higher = GameState(
            score = 120,
            dailyBestScore = 100,
            dailyChallengeDate = "2026-08-08",
        )

        val recorded = profile.record(lower).record(higher)

        assertEquals(80L, recorded.dailyBestScore("2026-08-07"))
        assertEquals(120L, recorded.dailyBestScore("2026-08-08"))
        assertEquals(0L, recorded.dailyBestScore("2026-08-09"))
    }

    @Test
    fun dailyHistorySummariesIgnoreEmptyScores() {
        val profile = ProfileState(
            dailyBestScores = mapOf(
                "2026-08-07" to 80L,
                "2026-08-08" to 0L,
                "2026-08-09" to 140L,
            ),
        )

        assertEquals(2, profile.dailyChallengeCount)
        assertEquals(140L, profile.bestDailyScore)
    }
}
