package com.battleheim.quantum2048.domain

import com.battleheim.quantum2048.engine.Difficulty
import com.battleheim.quantum2048.engine.DuelPlayer
import com.battleheim.quantum2048.engine.GameState
import org.junit.Assert.assertEquals
import org.junit.Test

class SocialStateTest {
    @Test
    fun recordGameUpdatesLeaderboardAndDailyStreak() {
        val first = GameState(
            difficulty = Difficulty.DAILY,
            score = 120,
            bestScore = 120,
            dailyBestScore = 120,
            dailyChallengeDate = "2026-08-10",
        )
        val second = first.copy(score = 180, bestScore = 180, dailyBestScore = 180, dailyChallengeDate = "2026-08-11")

        val recorded = SocialState().recordGame(first, nowMillis = 1).recordGame(second, nowMillis = 2)

        assertEquals(1, recorded.leaderboards.size)
        assertEquals(180L, recorded.leaderboards.first().score)
        assertEquals(2, recorded.dailyStreak.currentStreak)
        assertEquals(2, recorded.dailyStreak.bestStreak)
    }

    @Test
    fun recordDuelResultTracksBestWinStreak() {
        val recorded = SocialState()
            .recordDuelResult(DuelPlayer.PLAYER_ONE, nowMillis = 1)
            .recordDuelResult(DuelPlayer.PLAYER_ONE, nowMillis = 2)
            .recordDuelResult(DuelPlayer.PLAYER_TWO, nowMillis = 3)

        assertEquals(2, recorded.duelRecord.bestWinStreak)
        assertEquals(0, recorded.duelRecord.currentWinStreak)
        assertEquals(2, recorded.duelRecord.totalWins)
        assertEquals(1, recorded.duelRecord.totalLosses)
    }
}
