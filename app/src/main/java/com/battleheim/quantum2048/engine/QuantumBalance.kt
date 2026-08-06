package com.battleheim.quantum2048.engine

/** Every tunable Quantum value lives here so balancing never leaks into rules or UI. */
data class QuantumBalance(
    val maxEnergy: Int = 100,
    val startingEnergy: Int = 30,
    val energyPerMerge: Int = 6,
    val chainEnergyBonus: Int = 3,
    val lowCollapseCost: Int = 18,
    val highCollapseCost: Int = 30,
    val quantumSpawnChance: Double = 0.18,
    val autoCollapseChance: Double = 0.08,
    val autoCollapseLowWeight: Double = 0.65,
) {
    init {
        require(maxEnergy > 0 && startingEnergy in 0..maxEnergy)
        require(energyPerMerge >= 0 && chainEnergyBonus >= 0)
        require(lowCollapseCost >= 0 && highCollapseCost >= lowCollapseCost)
        require(quantumSpawnChance in 0.0..1.0 && autoCollapseChance in 0.0..1.0)
        require(autoCollapseLowWeight in 0.0..1.0)
    }

    fun energyFor(mergeCount: Int): Int = if (mergeCount <= 0) 0 else
        energyPerMerge * mergeCount + chainEnergyBonus * (mergeCount - 1)
}
