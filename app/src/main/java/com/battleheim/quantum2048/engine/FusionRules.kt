package com.battleheim.quantum2048.engine

import kotlin.math.min

/** Central rule table for all numeric, particle, element, spawn, and reaction behavior. */
object FusionRules {
    val supportedBoardSizes: List<Int> = listOf(4, 6, 8)
    const val achievementCollapseCentury: String = "collapse_century"
    const val achievementResolved2048: String = "resolved_2048"
    const val achievementNoUndoWin: String = "no_undo_win"

    // Current-architecture Entanglement policy: a freshly spawned quantum tile can bond
    // with one adjacent unpaired quantum tile. When either member fuses, the partner
    // collapses into the primary fusion output and the bond is consumed.
    const val entanglementSpawnChance: Double = 0.12
    const val protonInjectionSpawnChance: Double = 0.18
    const val elementSpawnScoreThreshold: Long = 650L

    const val tunnelingEnergyCost: Int = 42

    // Three-state superposition starts only once the run has real momentum.
    const val superpositionScoreThreshold: Long = 512L
    const val superpositionSpawnChance: Double = 0.10
    val superpositionCollapseEnergyCosts: List<Int> = listOf(18, 28, 40)
    const val observerPreviewEnergyCost: Int = 8

    const val initialEnergy: Int = 30
    const val maxEnergy: Int = 100
    const val energyPerMerge: Int = 6
    const val energyPerChainMerge: Int = 3
    const val overflowScorePerEnergy: Int = 4
    const val zenEnergy: Int = 1_000
    const val hardcoreInitialEnergy: Int = 0
    const val puzzleMoveLimit: Int = 3
    const val puzzleTargetValue: Int = 128
    val puzzleDefinitions: List<PuzzleDefinition> = listOf(
        PuzzleDefinition(
            id = "pair_reactor",
            tiles = listOf(
                PuzzleTile(0, 32, TileKind.ELECTRON),
                PuzzleTile(1, 32, TileKind.ELECTRON),
                PuzzleTile(4, 32, TileKind.PROTON),
                PuzzleTile(5, 32, TileKind.PROTON),
            ),
        ),
        PuzzleDefinition(
            id = "proton_bridge",
            tiles = listOf(
                PuzzleTile(0, 16, TileKind.PROTON),
                PuzzleTile(1, 16, TileKind.PROTON),
                PuzzleTile(2, 32, TileKind.PROTON),
                PuzzleTile(3, 32, TileKind.PROTON),
                PuzzleTile(8, 64, TileKind.ELECTRON),
                PuzzleTile(12, 64, TileKind.ELECTRON),
            ),
        ),
        PuzzleDefinition(
            id = "electron_corner",
            tiles = listOf(
                PuzzleTile(3, 64, TileKind.ELECTRON),
                PuzzleTile(7, 64, TileKind.ELECTRON),
                PuzzleTile(12, 32, TileKind.PROTON),
                PuzzleTile(13, 32, TileKind.PROTON),
            ),
        ),
    )

    fun energyGainForMergeCount(mergeCount: Int): Int =
        if (mergeCount <= 0) 0 else mergeCount * energyPerMerge + (mergeCount - 1) * energyPerChainMerge

    fun overflowScoreBonus(currentEnergy: Int, gainedEnergy: Int, difficulty: Difficulty = Difficulty.QUANTUM): Int =
        maxOf(0, currentEnergy + gainedEnergy - maxEnergyFor(difficulty)) * overflowScorePerEnergy

    fun initialEnergyFor(difficulty: Difficulty): Int = when (difficulty) {
        Difficulty.EASY -> 0
        Difficulty.ZEN -> zenEnergy
        Difficulty.HARDCORE -> hardcoreInitialEnergy
        else -> initialEnergy
    }

    fun maxEnergyFor(difficulty: Difficulty): Int = when (difficulty) {
        Difficulty.ZEN -> zenEnergy
        else -> maxEnergy
    }

    fun isUndoEnabled(difficulty: Difficulty): Boolean = difficulty != Difficulty.HARDCORE

    fun isPuzzleSolved(state: GameState): Boolean =
        state.difficulty == Difficulty.PUZZLE && state.cells.filterNotNull().any { gameValueOf(it) >= puzzleTargetValue }

    fun isPuzzleFailed(state: GameState): Boolean =
        state.difficulty == Difficulty.PUZZLE && state.moveCount >= puzzleMoveLimit && !isPuzzleSolved(state)

    fun dailySeed(date: java.time.LocalDate): Long =
        DailyChallengeSeedProvider.seedForDate(date.toString())

    fun unlockedAchievementsFor(state: GameState): Set<String> {
        val unlocked = state.unlockedAchievements.toMutableSet()
        if (state.successfulCollapseCount >= 100 && state.status != GameStatus.LOST) {
            unlocked += achievementCollapseCentury
        }
        if (
            state.status == GameStatus.WON &&
            state.cells.filterNotNull().any { gameValueOf(it) >= 2048 } &&
            state.cells.none { it?.superpositionValues?.isNotEmpty() == true }
        ) {
            unlocked += achievementResolved2048
        }
        if (state.status == GameStatus.WON && !state.usedUndo) {
            unlocked += achievementNoUndoWin
        }
        return unlocked
    }

    fun collapseLowRatio(state: GameState): Double {
        val total = state.lowCollapseCount + state.highCollapseCount
        return if (total == 0) 0.0 else state.lowCollapseCount.toDouble() / total
    }

    fun averageWinEnergy(state: GameState): Double =
        if (state.winEnergySamples == 0) 0.0 else state.totalWinEnergy.toDouble() / state.winEnergySamples

    val elementsByAtomicNumber: Map<Int, QuantumElement> =
        QuantumElement.entries.associateBy { it.atomicNumber }

    val elementChain: List<QuantumElement> = listOf(
        QuantumElement.HYDROGEN,
        QuantumElement.HELIUM,
        QuantumElement.BERYLLIUM,
        QuantumElement.CARBON,
        QuantumElement.NITROGEN,
        QuantumElement.OXYGEN,
        QuantumElement.FLUORINE,
        QuantumElement.NEON,
        QuantumElement.SODIUM,
        QuantumElement.SILICON,
        QuantumElement.PHOSPHORUS,
        QuantumElement.SULFUR,
        QuantumElement.CHLORINE,
        QuantumElement.CALCIUM,
        QuantumElement.IRON,
        QuantumElement.COPPER,
        QuantumElement.GOLD,
    )

    val compoundRecipes: List<CompoundRecipe> = ChemistryDictionary.recipes()

    fun gameValueOf(tile: Tile): Int = when (tile.kind) {
        TileKind.CLASSIC -> tile.value
        TileKind.ELECTRON, TileKind.PROTON -> tile.value * 2
        TileKind.ELEMENT -> 1 shl (tile.element?.rank ?: 1)
    }

    fun displaySymbol(tile: Tile): String =
        if (tile.superpositionValues.isNotEmpty()) tile.superpositionValues.joinToString(" | ") else stableDisplaySymbol(tile)

    fun stableDisplaySymbol(tile: Tile): String = when (tile.kind) {
        TileKind.CLASSIC -> tile.value.toString()
        TileKind.ELECTRON -> if (tile.value == 1) "e-" else "${tile.value}e-"
        TileKind.PROTON -> if (tile.value == 1) "p+" else "${tile.value}p+"
        TileKind.ELEMENT -> tile.element?.symbol ?: "?"
    }

    fun rankOf(tile: Tile): Int = when (tile.kind) {
        TileKind.CLASSIC -> Integer.numberOfTrailingZeros(tile.value)
        TileKind.ELECTRON, TileKind.PROTON -> tile.value
        TileKind.ELEMENT -> tile.element?.atomicNumber ?: 0
    }

    fun mergeProduct(a: Tile, b: Tile): FusionProduct? {
        if (a.superpositionValues.isNotEmpty() || b.superpositionValues.isNotEmpty()) return null
        if (a.kind == TileKind.CLASSIC && b.kind == TileKind.CLASSIC && a.value == b.value) {
            return FusionProduct(listOf(Tile(0, a.value * 2)), a.value * 2, false)
        }
        if (a.kind == b.kind && (a.kind == TileKind.ELECTRON || a.kind == TileKind.PROTON) && a.value == b.value) {
            val product = Tile(0, a.value * 2, a.kind)
            return FusionProduct(listOf(product), gameValueOf(product), false)
        }
        if (a.kind == TileKind.ELEMENT && b.kind == TileKind.ELEMENT && a.element == b.element) {
            val next = nextElement(a.element ?: return null) ?: return null
            val product = Tile(0, next.atomicNumber, TileKind.ELEMENT, next)
            return FusionProduct(listOf(product), gameValueOf(product), false)
        }
        protonInjectionProduct(a, b)?.let { return it }
        if ((a.kind == TileKind.ELECTRON && b.kind == TileKind.PROTON) || (a.kind == TileKind.PROTON && b.kind == TileKind.ELECTRON)) {
            return particleReaction(a, b)
        }
        return null
    }

    private fun protonInjectionProduct(a: Tile, b: Tile): FusionProduct? {
        val proton = listOf(a, b).singleOrNull { it.kind == TileKind.PROTON && it.value == 1 } ?: return null
        val target = if (proton == a) b else a
        if (target.kind != TileKind.ELEMENT) return null
        val targetElement = target.element ?: return null
        val injectedAtomicNumber = targetElement.atomicNumber + 1
        val injectedElement = elementsByAtomicNumber[injectedAtomicNumber] ?: return null
        val product = Tile(0, injectedAtomicNumber, TileKind.ELEMENT, injectedElement)
        return FusionProduct(listOf(product), gameValueOf(product), true)
    }

    private fun particleReaction(a: Tile, b: Tile): FusionProduct? {
        val electrons = if (a.kind == TileKind.ELECTRON) a.value else b.value
        val protons = if (a.kind == TileKind.PROTON) a.value else b.value
        val paired = min(electrons, protons)
        val element = nearestDefinedElementAtOrBelow(paired) ?: return null
        val outputs = mutableListOf(Tile(0, element.atomicNumber, TileKind.ELEMENT, element))
        val remainder = protons - electrons
        if (remainder > 0) outputs += Tile(0, remainder, TileKind.PROTON)
        if (remainder < 0) outputs += Tile(0, -remainder, TileKind.ELECTRON)
        return FusionProduct(outputs, outputs.sumOf { gameValueOf(it) }, true)
    }

    // Reaction table policy: undefined Z values round down to the nearest defined element.
    fun nearestDefinedElementAtOrBelow(atomicNumber: Int): QuantumElement? =
        elementChain.filter { it.atomicNumber <= atomicNumber }.maxByOrNull { it.atomicNumber }

    fun nextElement(element: QuantumElement): QuantumElement? =
        elementChain.getOrNull(elementChain.indexOf(element) + 1)

    fun spawnCount(size: Int): Int = when (size) {
        4 -> 2
        6 -> 3
        else -> 4
    }

    fun quantumSpawnTile(state: GameState, roll: Double): Tile {
        val cells = state.cells.filterNotNull()
        val particleValues = cells
            .filter { it.kind == TileKind.ELECTRON || it.kind == TileKind.PROTON }
            .groupBy { it.kind }
            .mapValues { (_, tiles) -> tiles.sumOf { it.value } }
        val electrons = particleValues[TileKind.ELECTRON] ?: 0
        val protons = particleValues[TileKind.PROTON] ?: 0
        val emptyRatio = state.cells.count { it == null }.toDouble() / state.cells.size.toDouble()
        val elementSpawnChance = elementSpawnChanceFor(state, emptyRatio, cells)
        val protonChance = protonSpawnChanceFor(state, electrons, protons, emptyRatio)
        return when {
            roll < elementSpawnChance -> {
                val element = spawnElementFor(state, cells)
                Tile(state.nextTileId, element.atomicNumber, TileKind.ELEMENT, element)
            }
            roll < elementSpawnChance + protonChance -> Tile(state.nextTileId, 1, TileKind.PROTON)
            else -> Tile(state.nextTileId, 1, TileKind.ELECTRON)
        }
    }

    private fun protonSpawnChanceFor(state: GameState, electrons: Int, protons: Int, emptyRatio: Double): Double {
        val balanceBias = ((electrons - protons).coerceIn(-8, 8)) * 0.035
        val pressureAssist = if (emptyRatio < 0.25) 0.06 else 0.0
        val difficultyBias = when (state.difficulty) {
            Difficulty.MEDIUM -> 0.06
            Difficulty.HARD, Difficulty.QUANTUM, Difficulty.DAILY -> 0.10
            Difficulty.ZEN -> 0.08
            Difficulty.HARDCORE -> 0.12
            Difficulty.PUZZLE, Difficulty.EASY -> 0.0
        }
        val boardBias = when {
            state.size >= 8 -> -0.04
            state.size >= 6 -> -0.02
            else -> 0.0
        }
        return (0.34 + balanceBias + pressureAssist + difficultyBias + boardBias).coerceIn(0.24, 0.58)
    }

    private fun elementSpawnChanceFor(state: GameState, emptyRatio: Double, cells: List<Tile>): Double {
        if (state.score < elementSpawnScoreThreshold) return 0.0
        if (cells.none { it.kind == TileKind.ELEMENT }) return 0.0
        val pressure = when {
            emptyRatio < 0.16 -> 0.10
            emptyRatio < 0.28 -> 0.07
            else -> 0.035
        }
        val difficulty = when (state.difficulty) {
            Difficulty.MEDIUM -> 0.015
            Difficulty.HARD, Difficulty.QUANTUM, Difficulty.DAILY -> 0.03
            Difficulty.ZEN -> 0.05
            Difficulty.HARDCORE -> 0.025
            Difficulty.PUZZLE, Difficulty.EASY -> 0.0
        }
        val board = if (state.size >= 6) 0.015 else 0.0
        return (pressure + difficulty + board).coerceIn(0.0, 0.16)
    }

    private fun spawnElementFor(state: GameState, cells: List<Tile>): QuantumElement {
        val bestRank = cells.mapNotNull { it.element?.rank }.maxOrNull() ?: 1
        val rankLimit = when (state.difficulty) {
            Difficulty.MEDIUM -> 2
            Difficulty.HARD -> 3
            Difficulty.QUANTUM, Difficulty.DAILY -> 4
            Difficulty.ZEN -> 5
            Difficulty.HARDCORE -> 4
            Difficulty.PUZZLE, Difficulty.EASY -> 1
        }
        val maxRank = min(bestRank, rankLimit)
        return elementChain.lastOrNull { it.rank <= maxRank } ?: QuantumElement.HYDROGEN
    }

    fun canEntangle(tile: Tile): Boolean =
        tile.kind != TileKind.CLASSIC && tile.entanglementGroupId == null && tile.superpositionValues.isEmpty()

    fun canSpawnSuperposition(state: GameState): Boolean =
        state.mode == GameMode.QUANTUM && state.score >= superpositionScoreThreshold

    fun superpositionValuesFor(baseValue: Int): List<Int> =
        listOf(baseValue, baseValue * 2, baseValue * 4)
}

data class FusionProduct(
    val tiles: List<Tile>,
    val score: Int,
    val isReaction: Boolean,
)

data class PuzzleDefinition(
    val id: String,
    val tiles: List<PuzzleTile>,
)

data class PuzzleTile(
    val index: Int,
    val value: Int,
    val kind: TileKind,
)
