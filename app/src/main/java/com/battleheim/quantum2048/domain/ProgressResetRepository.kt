package com.battleheim.quantum2048.domain

interface ProgressResetRepository {
    suspend fun resetAllProgress()
}

class LocalProgressResetRepository(
    private val gameRepository: GameRepository,
    private val collectionRepository: CollectionRepository,
    private val profileRepository: ProfileRepository,
    private val socialRepository: SocialRepository,
    private val statisticsRepository: StatisticsRepository,
    private val levelProgressRepository: LevelProgressRepository,
    @Suppress("unused") private val dailyChallengeRepository: DailyChallengeRepository? = null,
    @Suppress("unused") private val achievementsRepository: AchievementsRepository? = null,
) : ProgressResetRepository {
    override suspend fun resetAllProgress() {
        gameRepository.clearAll()
        collectionRepository.clear()
        profileRepository.clear()
        socialRepository.clear()
        statisticsRepository.clear()
        levelProgressRepository.clear()
    }
}
