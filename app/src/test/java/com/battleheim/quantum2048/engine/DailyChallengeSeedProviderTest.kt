package com.battleheim.quantum2048.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class DailyChallengeSeedProviderTest {
    @Test
    fun sameUtcDateAlwaysProducesSameSeed() {
        val date = "2026-08-15"

        assertEquals(
            DailyChallengeSeedProvider.seedForDate(date),
            DailyChallengeSeedProvider.seedForDate(date),
        )
    }

    @Test
    fun differentUtcDatesProduceDifferentSeeds() {
        val seeds = listOf("2026-08-15", "2026-08-16", "2027-01-01")
            .map(DailyChallengeSeedProvider::seedForDate)

        assertNotEquals(seeds[0], seeds[1])
        assertNotEquals(seeds[1], seeds[2])
        assertNotEquals(seeds[0], seeds[2])
    }
}
