package com.battleheim.quantum2048.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyChallengeRepositoryTest {
    @Test
    fun oneAttemptPerUtcDayBlocksSecondStartAndAllowsNextDay() {
        val provider = FakeDailyDateProvider("2026-08-15")
        var state = DailyChallengeState()

        val today = provider.todayUtc()
        assertTrue(state.canStart(today))

        state = state.markStarted(today)
        assertEquals(DailyChallengeStatus.IN_PROGRESS, state.statusFor(today))
        assertFalse(state.canStart(today))

        state = state.recordResult(today, 2_048)
        assertEquals(DailyChallengeStatus.COMPLETED, state.statusFor(today))
        assertFalse(state.canStart(today))

        provider.date = "2026-08-16"
        assertTrue(state.canStart(provider.todayUtc()))
        assertEquals(DailyChallengeStatus.AVAILABLE, state.statusFor(provider.todayUtc()))
    }

    @Test
    fun recordsRecentHistoryBestAverageAndStreak() {
        val state = DailyChallengeState()
            .recordResult("2026-08-13", 100)
            .recordResult("2026-08-14", 300)
            .recordResult("2026-08-15", 200)

        assertEquals(3, state.results.size)
        assertEquals(300, state.bestScore)
        assertEquals(200, state.averageScore)
        assertEquals(3, state.participationStreak)
        assertEquals("2026-08-15", state.recentResults.first().date)
    }

    private class FakeDailyDateProvider(var date: String) : DailyDateProvider {
        override fun todayUtc(): String = date
    }
}
