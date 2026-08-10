package com.battleheim.quantum2048.ads

enum class RewardPlacement {
    EXTRA_UNDO,
    REVIVE_AFTER_GAME_OVER,
    DAILY_BONUS_ATTEMPT,
}

enum class InterstitialPlacement {
    RETURN_TO_MAIN_MENU_AFTER_DUEL,
    RETURN_TO_MAIN_MENU_AFTER_GAME_OVER,
}

interface AdGateway {
    val isRewardedReady: Boolean
    val isInterstitialReady: Boolean
    fun showInterstitial(placement: InterstitialPlacement)
    fun showRewarded(placement: RewardPlacement, onReward: () -> Unit)
}

object NoOpAdGateway : AdGateway {
    override val isRewardedReady: Boolean = false
    override val isInterstitialReady: Boolean = false
    override fun showInterstitial(placement: InterstitialPlacement) = Unit
    override fun showRewarded(placement: RewardPlacement, onReward: () -> Unit) = Unit
}

class OfflineAdGateway(
    private val rewardsEnabled: Boolean = false,
) : AdGateway {
    override val isRewardedReady: Boolean get() = rewardsEnabled
    override val isInterstitialReady: Boolean = false

    override fun showInterstitial(placement: InterstitialPlacement) = Unit

    override fun showRewarded(placement: RewardPlacement, onReward: () -> Unit) {
        if (rewardsEnabled) onReward()
    }
}
