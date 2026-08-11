package com.battleheim.quantum2048.engine

import kotlinx.serialization.Serializable

@Serializable
data class ElementTile(
    val element: QuantumElement,
    val sourceTileId: Long? = null,
) {
    val symbol: String get() = element.symbol
}

@Serializable
data class Compound(
    val symbol: String,
    val englishName: String,
    val persianName: String,
    val scoreValue: Int,
)

@Serializable
data class CompoundRecipe(
    val id: String,
    val inputs: List<QuantumElement>,
    val output: Compound,
    val unlockLevel: CompoundRecipeLevel,
    val atomicNumbers: List<Int> = inputs.map { it.atomicNumber },
) {
    init {
        require(id.isNotBlank())
        require(atomicNumbers.size >= 2)
        require(output.symbol.isNotBlank())
        require(output.englishName.isNotBlank())
        require(output.persianName.isNotBlank())
        require(output.scoreValue > 0)
    }

    fun matches(elements: List<ElementTile>): Boolean {
        if (elements.size != atomicNumbers.size) return false
        return elements.map { it.element.atomicNumber }.sorted() == atomicNumbers.sorted()
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

object ChemistryDictionary {
    private val entries: Map<List<Int>, Compound> = linkedMapOf(
        listOf(1, 1, 8) to Compound("H2O", "Water", "Water", 240),
        listOf(1, 8, 8) to Compound("H2O2", "Hydrogen peroxide", "Hydrogen peroxide", 300),
        listOf(6, 8, 8) to Compound("CO2", "Carbon dioxide", "Carbon dioxide", 360),
        listOf(6, 8) to Compound("CO", "Carbon monoxide", "Carbon monoxide", 320),
        listOf(6, 1, 1, 1, 1) to Compound("CH4", "Methane", "Methane", 420),
        listOf(6, 6, 1, 1, 1, 1, 1, 1) to Compound("C2H6", "Ethane", "Ethane", 520),
        listOf(6, 6, 1, 1, 1, 1) to Compound("C2H4", "Ethylene", "Ethylene", 500),
        listOf(6, 6, 1, 1) to Compound("C2H2", "Acetylene", "Acetylene", 540),
        listOf(7, 1, 1, 1) to Compound("NH3", "Ammonia", "Ammonia", 330),
        listOf(7, 8, 8) to Compound("NO2", "Nitrogen dioxide", "Nitrogen dioxide", 420),
        listOf(7, 8) to Compound("NO", "Nitric oxide", "Nitric oxide", 360),
        listOf(7, 1, 8, 8, 8) to Compound("HNO3", "Nitric acid", "Nitric acid", 620),
        listOf(16, 8, 8) to Compound("SO2", "Sulfur dioxide", "Sulfur dioxide", 420),
        listOf(16, 8, 8, 8) to Compound("SO3", "Sulfur trioxide", "Sulfur trioxide", 520),
        listOf(1, 1, 16, 8, 8, 8, 8) to Compound("H2SO4", "Sulfuric acid", "Sulfuric acid", 820),
        listOf(26, 8) to Compound("FeO", "Iron(II) oxide", "Iron(II) oxide", 760),
        listOf(26, 26, 8, 8, 8) to Compound("Fe2O3", "Iron(III) oxide", "Iron(III) oxide", 940),
        listOf(11, 17) to Compound("NaCl", "Sodium chloride", "Sodium chloride", 620),
        listOf(11, 8, 1) to Compound("NaOH", "Sodium hydroxide", "Sodium hydroxide", 580),
        listOf(20, 6, 8, 8, 8) to Compound("CaCO3", "Calcium carbonate", "Calcium carbonate", 780),
        listOf(20, 8) to Compound("CaO", "Calcium oxide", "Calcium oxide", 620),
        listOf(14, 8, 8) to Compound("SiO2", "Silicon dioxide", "Silicon dioxide", 720),
        listOf(15, 8, 8, 8, 8) to Compound("PO4", "Phosphate", "Phosphate", 680),
        listOf(1, 1, 15, 8, 8, 8, 8) to Compound("H3PO4", "Phosphoric acid", "Phosphoric acid", 840),
        listOf(6, 20) to Compound("CaC2", "Calcium carbide", "Calcium carbide", 700),
        listOf(29, 8) to Compound("CuO", "Copper(II) oxide", "Copper(II) oxide", 720),
        listOf(29, 17, 17) to Compound("CuCl2", "Copper chloride", "Copper chloride", 760),
        listOf(1, 17) to Compound("HCl", "Hydrochloric acid", "Hydrochloric acid", 420),
        listOf(11, 11, 6, 8, 8, 8) to Compound("Na2CO3", "Sodium carbonate", "Sodium carbonate", 740),
        listOf(11, 1, 6, 8, 8, 8) to Compound("NaHCO3", "Sodium bicarbonate", "Sodium bicarbonate", 760),
        listOf(6, 6, 8, 1, 1, 1, 1, 1, 1) to Compound("C2H6O", "Ethanol", "Ethanol", 780),
        listOf(6, 6, 8, 8, 1, 1, 1, 1) to Compound("C2H4O2", "Acetic acid", "Acetic acid", 820),
    ).mapKeys { it.key.sorted() }

    val compoundsByAtomicNumbers: Map<List<Int>, Compound> = entries

    fun recipes(levelFor: (Compound) -> CompoundRecipeLevel = ::defaultLevel): List<CompoundRecipe> =
        entries.map { (atomicNumbers, compound) ->
            CompoundRecipe(
                id = compound.symbol.lowercase().replace(Regex("[^a-z0-9]+"), "_"),
                inputs = atomicNumbers.mapNotNull { atomicNumber -> QuantumElement.entries.firstOrNull { it.atomicNumber == atomicNumber } },
                atomicNumbers = atomicNumbers,
                output = compound,
                unlockLevel = levelFor(compound),
            )
        }

    fun findCompound(atomicNumbers: List<Int>): Compound? =
        entries[atomicNumbers.sorted()]

    private fun defaultLevel(compound: Compound): CompoundRecipeLevel =
        if (compound.scoreValue <= 520) CompoundRecipeLevel.MEDIUM else if (compound.scoreValue <= 760) CompoundRecipeLevel.HARD else CompoundRecipeLevel.QUANTUM
}
