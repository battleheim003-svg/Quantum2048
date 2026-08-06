package com.battleheim.quantum2048.engine

import kotlinx.serialization.Serializable

@Serializable
data class ElementTile(
    val species: QuantumSpecies,
    val sourceTileId: Long? = null,
) {
    val symbol: String get() = species.symbol
}

@Serializable
data class Compound(
    val symbol: String,
    val englishName: String,
    val persianName: String,
    val scoreValue: Int,
    val energyReward: Int,
)

@Serializable
data class CompoundRecipe(
    val id: String,
    val inputs: List<QuantumSpecies>,
    val output: Compound,
    val unlockLevel: CompoundRecipeLevel,
) {
    init {
        require(id.isNotBlank())
        require(inputs.size >= 2)
        require(output.symbol.isNotBlank())
        require(output.englishName.isNotBlank())
        require(output.persianName.isNotBlank())
        require(output.scoreValue > 0)
        require(output.energyReward >= 0)
    }

    fun matches(elements: List<ElementTile>): Boolean {
        if (elements.size != inputs.size) return false
        return elements.map { it.species }.sortedBy { it.name } == inputs.sortedBy { it.name }
    }
}

@Serializable
enum class CompoundRecipeLevel {
    MEDIUM,
    HARD,
    QUANTUM,
}

object Chemistry {
    fun findRecipe(
        elements: List<ElementTile>,
        recipes: List<CompoundRecipe>,
        level: CompoundRecipeLevel? = null,
    ): CompoundRecipe? {
        val allowed = level?.let { requested ->
            recipes.filter { it.unlockLevel.ordinal <= requested.ordinal }
        } ?: recipes
        return allowed.firstOrNull { it.matches(elements) }
    }
}
