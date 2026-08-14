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
