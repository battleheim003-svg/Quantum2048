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
    val compoundRecipes: List<CompoundRecipe> = defaultCompoundRecipes,
    val difficultyRules: Map<Difficulty, DifficultyRules> = defaultDifficultyRules,
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

    companion object {
        val defaultDifficultyRules = mapOf(
            Difficulty.EASY to DifficultyRules(
                difficulty = Difficulty.EASY,
                particleMode = false,
                energyEnabled = false,
                collapseEnabled = false,
                compoundLabEnabled = false,
                maxFusionSpecies = null,
                allowedRecipeLevel = null,
                startingEnergy = 0,
                quantumSpawnChance = 0.0,
                autoCollapseChance = 0.0,
            ),
            Difficulty.MEDIUM to DifficultyRules(
                difficulty = Difficulty.MEDIUM,
                particleMode = true,
                energyEnabled = false,
                collapseEnabled = false,
                compoundLabEnabled = true,
                maxFusionSpecies = QuantumSpecies.NEON,
                allowedRecipeLevel = CompoundRecipeLevel.MEDIUM,
                startingEnergy = 0,
                quantumSpawnChance = 0.0,
                autoCollapseChance = 0.0,
            ),
            Difficulty.HARD to DifficultyRules(
                difficulty = Difficulty.HARD,
                particleMode = true,
                energyEnabled = true,
                collapseEnabled = false,
                compoundLabEnabled = true,
                maxFusionSpecies = QuantumSpecies.GOLD,
                allowedRecipeLevel = CompoundRecipeLevel.HARD,
                startingEnergy = 20,
                quantumSpawnChance = 0.0,
                autoCollapseChance = 0.0,
                compoundEnergyCost = 12,
            ),
            Difficulty.QUANTUM to DifficultyRules(
                difficulty = Difficulty.QUANTUM,
                particleMode = true,
                energyEnabled = true,
                collapseEnabled = true,
                compoundLabEnabled = true,
                maxFusionSpecies = QuantumSpecies.GOLD,
                allowedRecipeLevel = CompoundRecipeLevel.QUANTUM,
                startingEnergy = 30,
                quantumSpawnChance = 0.18,
                autoCollapseChance = 0.08,
                compoundEnergyCost = 18,
            ),
        )

        val defaultCompoundRecipes = listOf(
            CompoundRecipe(
                id = "water",
                inputs = listOf(QuantumSpecies.HYDROGEN, QuantumSpecies.HYDROGEN, QuantumSpecies.OXYGEN),
                output = Compound("H2O", "Water", "آب", scoreValue = 240, energyReward = 8),
                unlockLevel = CompoundRecipeLevel.MEDIUM,
            ),
            CompoundRecipe(
                id = "salt",
                inputs = listOf(QuantumSpecies.SODIUM, QuantumSpecies.CHLORINE),
                output = Compound("NaCl", "Sodium chloride", "نمک خوراکی", scoreValue = 320, energyReward = 10),
                unlockLevel = CompoundRecipeLevel.MEDIUM,
            ),
            CompoundRecipe(
                id = "carbon_dioxide",
                inputs = listOf(QuantumSpecies.CARBON, QuantumSpecies.OXYGEN, QuantumSpecies.OXYGEN),
                output = Compound("CO2", "Carbon dioxide", "کربن دی‌اکسید", scoreValue = 360, energyReward = 10),
                unlockLevel = CompoundRecipeLevel.HARD,
            ),
            CompoundRecipe(
                id = "ammonia",
                inputs = listOf(QuantumSpecies.NITROGEN, QuantumSpecies.HYDROGEN, QuantumSpecies.HYDROGEN, QuantumSpecies.HYDROGEN),
                output = Compound("NH3", "Ammonia", "آمونیاک", scoreValue = 380, energyReward = 12),
                unlockLevel = CompoundRecipeLevel.HARD,
            ),
            CompoundRecipe(
                id = "methane",
                inputs = listOf(QuantumSpecies.CARBON, QuantumSpecies.HYDROGEN, QuantumSpecies.HYDROGEN, QuantumSpecies.HYDROGEN, QuantumSpecies.HYDROGEN),
                output = Compound("CH4", "Methane", "متان", scoreValue = 420, energyReward = 12),
                unlockLevel = CompoundRecipeLevel.HARD,
            ),
            CompoundRecipe(
                id = "calcium_carbonate",
                inputs = listOf(QuantumSpecies.CALCIUM, QuantumSpecies.CARBON, QuantumSpecies.OXYGEN, QuantumSpecies.OXYGEN, QuantumSpecies.OXYGEN),
                output = Compound("CaCO3", "Calcium carbonate", "کلسیم کربنات", scoreValue = 640, energyReward = 16),
                unlockLevel = CompoundRecipeLevel.HARD,
            ),
            CompoundRecipe(
                id = "magnesium_oxide",
                inputs = listOf(QuantumSpecies.MAGNESIUM, QuantumSpecies.OXYGEN),
                output = Compound("MgO", "Magnesium oxide", "منیزیم اکسید", scoreValue = 460, energyReward = 12),
                unlockLevel = CompoundRecipeLevel.HARD,
            ),
            CompoundRecipe(
                id = "silicon_dioxide",
                inputs = listOf(QuantumSpecies.SILICON, QuantumSpecies.OXYGEN, QuantumSpecies.OXYGEN),
                output = Compound("SiO2", "Silicon dioxide", "سیلیسیم دی‌اکسید", scoreValue = 720, energyReward = 18),
                unlockLevel = CompoundRecipeLevel.QUANTUM,
            ),
            CompoundRecipe(
                id = "iron_oxide",
                inputs = listOf(QuantumSpecies.IRON, QuantumSpecies.OXYGEN, QuantumSpecies.OXYGEN, QuantumSpecies.OXYGEN),
                output = Compound("Fe2O3", "Iron oxide", "اکسید آهن", scoreValue = 860, energyReward = 20),
                unlockLevel = CompoundRecipeLevel.QUANTUM,
            ),
            CompoundRecipe(
                id = "lithium_chloride",
                inputs = listOf(QuantumSpecies.LITHIUM, QuantumSpecies.CHLORINE),
                output = Compound("LiCl", "Lithium chloride", "لیتیم کلرید", scoreValue = 520, energyReward = 14),
                unlockLevel = CompoundRecipeLevel.QUANTUM,
            ),
        )
    }

    fun rulesFor(difficulty: Difficulty): DifficultyRules =
        difficultyRules[difficulty] ?: error("Missing rules for $difficulty")
}

data class DifficultyRules(
    val difficulty: Difficulty,
    val particleMode: Boolean,
    val energyEnabled: Boolean,
    val collapseEnabled: Boolean,
    val compoundLabEnabled: Boolean,
    val maxFusionSpecies: QuantumSpecies?,
    val allowedRecipeLevel: CompoundRecipeLevel?,
    val startingEnergy: Int,
    val quantumSpawnChance: Double,
    val autoCollapseChance: Double,
    val compoundEnergyCost: Int = 0,
) {
    init {
        require(startingEnergy >= 0)
        require(quantumSpawnChance in 0.0..1.0)
        require(autoCollapseChance in 0.0..1.0)
        require(compoundEnergyCost >= 0)
    }
}
