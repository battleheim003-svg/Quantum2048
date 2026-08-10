package com.battleheim.quantum2048.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.time.LocalDate

class DailyChallengeEngineTest {
    private class FixedRandom : RandomProvider {
        override fun nextInt(bound: Int): Int = (bound - 1).coerceAtLeast(0)
        override fun nextDouble(): Double = 0.99
    }

    @Test
    fun dailySeedIsStableForSameDateAndDifferentForDifferentDates() {
        val date = LocalDate.of(2026, 8, 7)

        assertEquals(FusionRules.dailySeed(date), FusionRules.dailySeed(date))
        assertNotEquals(FusionRules.dailySeed(date), FusionRules.dailySeed(date.plusDays(1)))
    }

    @Test
    fun dailyChallengeBoardIsDeterministicForDate() {
        val date = LocalDate.of(2026, 8, 7)
        val first = GameEngine(FixedRandom()).newDailyChallenge(date)
        val second = GameEngine(SeededRandomProvider(999)).newDailyChallenge(date)

        assertEquals(Difficulty.DAILY, first.difficulty)
        assertEquals(date.toString(), first.dailyChallengeDate)
        assertEquals(first.cells, second.cells)
    }

    @Test
    fun dailyChallengeBoardChangesAcrossDates() {
        val first = GameEngine(FixedRandom()).newDailyChallenge(LocalDate.of(2026, 8, 7))
        val second = GameEngine(FixedRandom()).newDailyChallenge(LocalDate.of(2026, 8, 8))

        assertNotEquals(first.cells, second.cells)
    }

    @Test
    fun dailyBestScoreTracksBestScoreWithinDailyState() {
        val cells = MutableList<Tile?>(16) { null }.apply {
            this[0] = Tile(1, 1, TileKind.ELECTRON)
            this[1] = Tile(2, 1, TileKind.ELECTRON)
        }
        val state = GameState(
            cells = cells,
            score = 5,
            dailyBestScore = 6,
            mode = GameMode.QUANTUM,
            difficulty = Difficulty.DAILY,
            dailyChallengeDate = "2026-08-07",
            nextTileId = 10,
        )

        val result = GameEngine(FixedRandom()).move(state, Direction.LEFT)

        assertEquals(result.state.score, result.state.dailyBestScore)
    }
}
