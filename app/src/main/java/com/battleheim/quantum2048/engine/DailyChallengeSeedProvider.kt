package com.battleheim.quantum2048.engine

import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset

object DailyChallengeSeedProvider {
    fun todayUtc(clock: Clock = Clock.systemUTC()): String =
        LocalDate.now(clock.withZone(ZoneOffset.UTC)).toString()

    fun seedForDate(date: String): Long =
        date.fold(1125899906842597L) { acc, char -> acc * 31L + char.code }
}
