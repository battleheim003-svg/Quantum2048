package com.battleheim.quantum2048.engine

object QuantumBalance {
    const val entangledSpawnChance: Double = 0.12
    val defaultEntanglementRelation: EntanglementRelation = EntanglementRelation.SAME_CHOICE
    val entanglementCollapseEnergyPolicy: EntanglementEnergyPolicy = EntanglementEnergyPolicy.SINGLE_COST
}
