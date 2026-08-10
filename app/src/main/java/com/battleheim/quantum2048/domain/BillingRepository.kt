package com.battleheim.quantum2048.domain

import kotlinx.coroutines.flow.Flow

object ProductIds {
    const val REMOVE_ADS: String = "remove_ads"
    const val COSMETIC_NEON_PACK: String = "cosmetic_neon_pack"
    const val SEASON_PASS: String = "season_pass"
}

data class EntitlementState(
    val removeAds: Boolean = false,
    val ownedCosmeticPacks: Set<String> = emptySet(),
    val activeSeasonPassId: String? = null,
    val rewardedExtraUndoCredits: Int = 0,
    val rewardedReviveCredits: Int = 0,
    val rewardedDailyAttemptCredits: Int = 0,
    val lastUpdatedMillis: Long = 0,
) {
    val adsAllowed: Boolean get() = !removeAds

    fun grant(productId: String, nowMillis: Long = System.currentTimeMillis()): EntitlementState =
        when (productId) {
            ProductIds.REMOVE_ADS -> copy(removeAds = true, lastUpdatedMillis = nowMillis)
            ProductIds.COSMETIC_NEON_PACK -> copy(ownedCosmeticPacks = ownedCosmeticPacks + productId, lastUpdatedMillis = nowMillis)
            ProductIds.SEASON_PASS -> copy(activeSeasonPassId = productId, lastUpdatedMillis = nowMillis)
            else -> this
        }

    fun grantReward(reward: RewardEntitlement, nowMillis: Long = System.currentTimeMillis()): EntitlementState =
        when (reward) {
            RewardEntitlement.EXTRA_UNDO -> copy(rewardedExtraUndoCredits = rewardedExtraUndoCredits + 1, lastUpdatedMillis = nowMillis)
            RewardEntitlement.REVIVE -> copy(rewardedReviveCredits = rewardedReviveCredits + 1, lastUpdatedMillis = nowMillis)
            RewardEntitlement.DAILY_ATTEMPT -> copy(rewardedDailyAttemptCredits = rewardedDailyAttemptCredits + 1, lastUpdatedMillis = nowMillis)
        }

    fun consumeReward(reward: RewardEntitlement, nowMillis: Long = System.currentTimeMillis()): EntitlementState =
        when (reward) {
            RewardEntitlement.EXTRA_UNDO -> copy(rewardedExtraUndoCredits = maxOf(0, rewardedExtraUndoCredits - 1), lastUpdatedMillis = nowMillis)
            RewardEntitlement.REVIVE -> copy(rewardedReviveCredits = maxOf(0, rewardedReviveCredits - 1), lastUpdatedMillis = nowMillis)
            RewardEntitlement.DAILY_ATTEMPT -> copy(rewardedDailyAttemptCredits = maxOf(0, rewardedDailyAttemptCredits - 1), lastUpdatedMillis = nowMillis)
        }
}

enum class RewardEntitlement {
    EXTRA_UNDO,
    REVIVE,
    DAILY_ATTEMPT,
}

interface BillingRepository {
    fun observe(): Flow<EntitlementState>
    suspend fun grant(productId: String)
    suspend fun grantReward(reward: RewardEntitlement)
    suspend fun consumeReward(reward: RewardEntitlement)
    suspend fun clear()
}
