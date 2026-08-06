package com.battleheim.quantum2048.engine

import kotlinx.serialization.Serializable

@Serializable enum class GameMode { CLASSIC, QUANTUM }
@Serializable enum class Direction { UP, DOWN, LEFT, RIGHT }
@Serializable enum class GameStatus { PLAYING, WON, LOST }

@Serializable
enum class QuantumSpecies(
    val symbol: String,
    val title: String,
    val massNumber: Int,
    val scoreValue: Int,
) {
    ELECTRON("e-", "Electron", 0, 1),
    PROTON("p+", "Proton", 1, 2),
    HYDROGEN("H", "Hydrogen", 1, 4),
    HELIUM("He", "Helium", 4, 8),
    LITHIUM("Li", "Lithium", 7, 16),
    BERYLLIUM("Be", "Beryllium", 9, 32),
    BORON("B", "Boron", 11, 64),
    CARBON("C", "Carbon", 12, 128),
    NITROGEN("N", "Nitrogen", 14, 256),
    OXYGEN("O", "Oxygen", 16, 512),
    NEON("Ne", "Neon", 20, 1024),
    SILICON("Si", "Silicon", 28, 2048),
    IRON("Fe", "Iron", 56, 4096),
    GOLD("Au", "Gold", 197, 8192);

    fun nextFusion(): QuantumSpecies? {
        val chain = fusionChain
        val index = chain.indexOf(this)
        return if (index >= 0 && index + 1 < chain.size) chain[index + 1] else null
    }

    companion object {
        val fusionChain = listOf(HYDROGEN, HELIUM, LITHIUM, BERYLLIUM, BORON, CARBON, NITROGEN, OXYGEN, NEON, SILICON, IRON, GOLD)
    }
}

@Serializable
data class Tile(
    val id: Long,
    val value: Int,
    val quantumAlternative: Int? = null,
    val species: QuantumSpecies? = null,
    val quantumAlternativeSpecies: QuantumSpecies? = null,
) {
    init { require(value > 0 && quantumAlternative?.let { it > value } != false) }
    val isQuantum: Boolean get() = species != null
    val isUnstable: Boolean get() = quantumAlternative != null || quantumAlternativeSpecies != null
    fun options(): List<Int> = quantumAlternative?.let { listOf(value, it) } ?: listOf(value)
    fun speciesOptions(): List<QuantumSpecies> = quantumAlternativeSpecies?.let { high ->
        listOfNotNull(species, high)
    } ?: listOfNotNull(species)
}

@Serializable
data class GameState(
    val size: Int = 4,
    val cells: List<Tile?> = List(16) { null },
    val score: Long = 0,
    val bestScore: Long = 0,
    val status: GameStatus = GameStatus.PLAYING,
    val mode: GameMode = GameMode.CLASSIC,
    val hasAcknowledgedWin: Boolean = false,
    val moveCount: Int = 0,
    val nextTileId: Long = 1,
    val quantumEnergy: Int = 0,
) {
    init { require(size >= 2 && cells.size == size * size) }
    operator fun get(row: Int, column: Int): Tile? = cells[row * size + column]
}

data class MoveResult(
    val state: GameState,
    val changed: Boolean,
    val gainedScore: Int = 0,
    val mergeCount: Int = 0,
    val energyGained: Int = 0,
    val autoCollapse: CollapseEvent? = null,
)

@Serializable data class CollapseEvent(val tileId: Long, val chosenValue: Int, val automatic: Boolean)
enum class CollapseFailure { TILE_NOT_FOUND, NOT_QUANTUM, INVALID_CHOICE, INSUFFICIENT_ENERGY, GAME_NOT_ACTIVE }
sealed interface CollapseResult {
    data class Success(val state: GameState, val event: CollapseEvent, val energySpent: Int) : CollapseResult
    data class Failure(val state: GameState, val reason: CollapseFailure) : CollapseResult
}

interface RandomProvider { fun nextInt(bound: Int): Int; fun nextDouble(): Double }
class KotlinRandomProvider(private val random: kotlin.random.Random = kotlin.random.Random.Default) : RandomProvider {
    override fun nextInt(bound: Int) = random.nextInt(bound)
    override fun nextDouble() = random.nextDouble()
}
class SeededRandomProvider(seed: Long) : RandomProvider {
    private val delegate = kotlin.random.Random(seed)
    override fun nextInt(bound: Int) = delegate.nextInt(bound)
    override fun nextDouble() = delegate.nextDouble()
}
