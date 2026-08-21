package com.battleheim.quantum2048.engine

object QuantumBalance {
    const val entangledSpawnChance: Double = 0.04
    const val inverseEntanglementRelationChance: Double = 0.25
    val defaultEntanglementRelation: EntanglementRelation = EntanglementRelation.SAME_CHOICE
    val entanglementCollapseEnergyPolicy: EntanglementEnergyPolicy = EntanglementEnergyPolicy.SINGLE_COST
}
