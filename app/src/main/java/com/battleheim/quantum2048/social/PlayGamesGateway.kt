package com.battleheim.quantum2048.social

import com.battleheim.quantum2048.domain.LeaderboardEntry
import com.battleheim.quantum2048.engine.Difficulty
import com.battleheim.quantum2048.engine.QuantumElement

interface PlayGamesGateway {
    val isSignedIn: Boolean
    fun submitLeaderboard(entry: LeaderboardEntry)
    fun unlockElementAchievement(element: QuantumElement)
    fun submitDuelWinStreak(streak: Int)
    fun requestCloudSaveSync()
}

class OfflinePlayGamesGateway : PlayGamesGateway {
    override val isSignedIn: Boolean = false
    override fun submitLeaderboard(entry: LeaderboardEntry) = Unit
    override fun unlockElementAchievement(element: QuantumElement) = Unit
    override fun submitDuelWinStreak(streak: Int) = Unit
    override fun requestCloudSaveSync() = Unit
}

fun leaderboardIdFor(difficulty: Difficulty): String =
    "leaderboard_high_score_${difficulty.name.lowercase()}"
