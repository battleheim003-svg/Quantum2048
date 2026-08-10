package com.battleheim.quantum2048.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EntitlementStateTest {
    @Test
    fun grantRemoveAdsDisablesAds() {
        val state = EntitlementState().grant(ProductIds.REMOVE_ADS, nowMillis = 10)

        assertTrue(state.removeAds)
        assertFalse(state.adsAllowed)
        assertEquals(10, state.lastUpdatedMillis)
    }

    @Test
    fun rewardCreditsCanBeGrantedAndConsumedSafely() {
        val state = EntitlementState()
            .grantReward(RewardEntitlement.EXTRA_UNDO, nowMillis = 11)
            .grantReward(RewardEntitlement.EXTRA_UNDO, nowMillis = 12)
            .consumeReward(RewardEntitlement.EXTRA_UNDO, nowMillis = 13)
            .consumeReward(RewardEntitlement.REVIVE, nowMillis = 14)

        assertEquals(1, state.rewardedExtraUndoCredits)
        assertEquals(0, state.rewardedReviveCredits)
        assertEquals(14, state.lastUpdatedMillis)
    }
}
