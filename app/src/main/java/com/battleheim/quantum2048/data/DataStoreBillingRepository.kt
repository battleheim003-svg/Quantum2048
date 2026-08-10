package com.battleheim.quantum2048.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.battleheim.quantum2048.domain.BillingRepository
import com.battleheim.quantum2048.domain.EntitlementState
import com.battleheim.quantum2048.domain.RewardEntitlement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.billingDataStore by preferencesDataStore("billing_state_v1")

class DataStoreBillingRepository(private val context: Context) : BillingRepository {
    private val key = stringPreferencesKey("billing_snapshot_v1")

    override fun observe(): Flow<EntitlementState> = context.billingDataStore.data.map { prefs ->
        prefs[key]?.decodeEntitlements() ?: EntitlementState()
    }

    override suspend fun grant(productId: String) {
        update { it.grant(productId) }
    }

    override suspend fun grantReward(reward: RewardEntitlement) {
        update { it.grantReward(reward) }
    }

    override suspend fun consumeReward(reward: RewardEntitlement) {
        update { it.consumeReward(reward) }
    }

    override suspend fun clear() {
        context.billingDataStore.edit { prefs -> prefs.remove(key) }
    }

    private suspend fun update(transform: (EntitlementState) -> EntitlementState) {
        context.billingDataStore.edit { prefs ->
            val current = prefs[key]?.decodeEntitlements() ?: EntitlementState()
            prefs[key] = transform(current).encode()
        }
    }

    private fun EntitlementState.encode(): String =
        listOf(
            removeAds.toString(),
            ownedCosmeticPacks.joinToString(","),
            activeSeasonPassId.orEmpty(),
            rewardedExtraUndoCredits.toString(),
            rewardedReviveCredits.toString(),
            rewardedDailyAttemptCredits.toString(),
            lastUpdatedMillis.toString(),
        ).joinToString("|")

    private fun String.decodeEntitlements(): EntitlementState? =
        runCatching {
            val parts = split("|")
            EntitlementState(
                removeAds = parts.getOrNull(0).toBoolean(),
                ownedCosmeticPacks = parts.getOrNull(1)
                    ?.split(",")
                    ?.filter { it.isNotBlank() }
                    ?.toSet()
                    ?: emptySet(),
                activeSeasonPassId = parts.getOrNull(2)?.takeIf { it.isNotBlank() },
                rewardedExtraUndoCredits = parts.getOrNull(3)?.toIntOrNull() ?: 0,
                rewardedReviveCredits = parts.getOrNull(4)?.toIntOrNull() ?: 0,
                rewardedDailyAttemptCredits = parts.getOrNull(5)?.toIntOrNull() ?: 0,
                lastUpdatedMillis = parts.getOrNull(6)?.toLongOrNull() ?: 0,
            )
        }.getOrNull()
}
