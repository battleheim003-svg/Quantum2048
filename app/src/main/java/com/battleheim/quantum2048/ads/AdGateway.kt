package com.battleheim.quantum2048.ads
interface AdGateway { val isRewardedReady: Boolean; fun showGameOver(); fun showRewarded(onReward: () -> Unit) }
object NoOpAdGateway : AdGateway { override val isRewardedReady = false; override fun showGameOver() = Unit; override fun showRewarded(onReward: () -> Unit) = Unit }
