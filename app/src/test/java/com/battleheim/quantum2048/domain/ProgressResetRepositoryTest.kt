package com.battleheim.quantum2048.domain

import com.battleheim.quantum2048.engine.Compound
import com.battleheim.quantum2048.engine.Difficulty
import com.battleheim.quantum2048.engine.GameMode
import com.battleheim.quantum2048.engine.GameState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressResetRepositoryTest {
    @Test
    fun resetAllProgressClearsProgressRepositoriesOnly() = runTest {
        val game = FakeGameRepository()
        val collection = FakeCollectionRepository()
        val profile = FakeProfileRepository()
        val social = FakeSocialRepository()
        val statistics = FakeStatisticsRepository()
        val levels = FakeLevelProgressRepository()
        val daily = FakeDailyChallengeRepository()
        val achievements = FakeAchievementsRepository()
        val settings = AppSettings(language = AppLanguage.PERSIAN, soundEnabled = false, hapticsEnabled = false)

        LocalProgressResetRepository(game, collection, profile, social, statistics, levels, daily, achievements).resetAllProgress()

        assertTrue(game.clearAllCalled)
        assertTrue(collection.clearCalled)
        assertTrue(profile.clearCalled)
        assertTrue(social.clearCalled)
        assertTrue(statistics.clearCalled)
        assertTrue(levels.clearCalled)
        assertEquals(false, daily.clearHistoryCalled)
        assertEquals(false, achievements.clearCalled)
        assertEquals(AppLanguage.PERSIAN, settings.language)
        assertEquals(false, settings.soundEnabled)
        assertEquals(false, settings.hapticsEnabled)
    }

    private class FakeGameRepository : GameRepository {
        var clearAllCalled = false
        override fun observe(mode: GameMode): Flow<GameState?> = MutableStateFlow(null)
        override fun observe(difficulty: Difficulty): Flow<GameState?> = MutableStateFlow(null)
        override fun observe(difficulty: Difficulty, size: Int): Flow<GameState?> = MutableStateFlow(null)
        override suspend fun save(state: GameState) = Unit
        override suspend fun clear(mode: GameMode) = Unit
        override suspend fun clear(difficulty: Difficulty) = Unit
        override suspend fun clear(difficulty: Difficulty, size: Int) = Unit
        override suspend fun clearAll() {
            clearAllCalled = true
        }
    }

    private class FakeCollectionRepository : CollectionRepository {
        var clearCalled = false
        override fun observe(): Flow<CollectionState> = MutableStateFlow(CollectionState())
        override suspend fun record(compound: Compound, difficulty: Difficulty, discoveredAtMillis: Long) = Unit
        override suspend fun unrecord(compoundSymbol: String) = Unit
        override suspend fun clear() {
            clearCalled = true
        }
    }

    private class FakeProfileRepository : ProfileRepository {
        var clearCalled = false
        override fun observe(): Flow<ProfileState> = MutableStateFlow(ProfileState())
        override suspend fun record(game: GameState) = Unit
        override suspend fun unlockQuantumModes() = Unit
        override suspend fun clear() {
            clearCalled = true
        }
    }

    private class FakeSocialRepository : SocialRepository {
        var clearCalled = false
        override fun observe(): Flow<SocialState> = MutableStateFlow(SocialState())
        override suspend fun recordGame(game: GameState) = Unit
        override suspend fun recordDuelResult(
            difficulty: Difficulty,
            opponent: com.battleheim.quantum2048.engine.DuelOpponent,
            botDifficulty: com.battleheim.quantum2048.engine.BotDifficulty,
            winner: com.battleheim.quantum2048.engine.DuelPlayer?,
        ) = Unit
        override suspend fun syncAchievements(achievementIds: Set<String>) = Unit
        override suspend fun clear() {
            clearCalled = true
        }
    }

    private class FakeLevelProgressRepository : LevelProgressRepository {
        var clearCalled = false
        override fun observe(): Flow<PlayerProgress> = MutableStateFlow(PlayerProgress())
        override suspend fun save(progress: PlayerProgress) = Unit
        override suspend fun clear() {
            clearCalled = true
        }
    }

    private class FakeStatisticsRepository : StatisticsRepository {
        var clearCalled = false
        override fun observeStatistics(mode: com.battleheim.quantum2048.engine.GameMode): Flow<StatsSnapshot> =
            MutableStateFlow(StatsSnapshot(mode))

        override suspend fun recordMerge(mode: com.battleheim.quantum2048.engine.GameMode, count: Int, state: GameState) = Unit
        override suspend fun recordCollapse(mode: com.battleheim.quantum2048.engine.GameMode, lowValue: Boolean, manual: Boolean) = Unit
        override suspend fun recordEntangledCollapse(mode: com.battleheim.quantum2048.engine.GameMode, count: Int) = Unit
        override suspend fun recordGameEnded(mode: com.battleheim.quantum2048.engine.GameMode, state: GameState) = Unit
        override suspend fun clear() {
            clearCalled = true
        }
    }

    private class FakeDailyChallengeRepository : DailyChallengeRepository {
        var clearHistoryCalled = false
        override fun observe(): Flow<DailyChallengeState> = MutableStateFlow(DailyChallengeState())
        override suspend fun markStarted(date: String) = Unit
        override suspend fun recordResult(date: String, score: Long) = Unit
        override suspend fun clearActiveRun(date: String) = Unit
        override suspend fun clearHistory() {
            clearHistoryCalled = true
        }
    }

    private class FakeAchievementsRepository : AchievementsRepository {
        var clearCalled = false
        override fun observeAchievements(): Flow<List<AchievementProgress>> = MutableStateFlow(emptyList())
        override suspend fun refresh(nowMillis: Long): List<AchievementProgress> = emptyList()
        override suspend fun clear() {
            clearCalled = true
        }
    }
}
