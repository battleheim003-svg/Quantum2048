package com.battleheim.quantum2048.engine

import kotlinx.serialization.Serializable

@Serializable enum class GameMode { CLASSIC, QUANTUM }
@Serializable
enum class Difficulty(val mode: GameMode) {
    EASY(GameMode.CLASSIC),
    MEDIUM(GameMode.QUANTUM),
    HARD(GameMode.QUANTUM),
    QUANTUM(GameMode.QUANTUM);

    companion object {
        fun fromMode(mode: GameMode): Difficulty = when (mode) {
            GameMode.CLASSIC -> EASY
            GameMode.QUANTUM -> QUANTUM
        }
    }
}
@Serializable enum class Direction { UP, DOWN, LEFT, RIGHT }
@Serializable enum class GameStatus { PLAYING, WON, LOST }
@Serializable enum class DuelOpponent { PASS_AND_PLAY, BOT }
@Serializable enum class BotDifficulty { EASY, NORMAL, QUANTUM_HARD }
@Serializable enum class DuelPlayer { PLAYER_ONE, PLAYER_TWO }

@Serializable
enum class TileKind { CLASSIC, ELECTRON, PROTON, ELEMENT }

@Serializable
enum class QuantumElement(
    val symbol: String,
    val title: String,
    val atomicNumber: Int,
    val rank: Int,
) {
    HYDROGEN("H", "Hydrogen", 1, 1),
    HELIUM("He", "Helium", 2, 2),
    BERYLLIUM("Be", "Beryllium", 4, 3),
    OXYGEN("O", "Oxygen", 8, 4),
    NEON("Ne", "Neon", 10, 5),
    SILICON("Si", "Silicon", 14, 6),
    IRON("Fe", "Iron", 26, 7),
    GOLD("Au", "Gold", 79, 8);
}

@Serializable
data class Tile(
    val id: Long,
    val value: Int,
    val kind: TileKind = TileKind.CLASSIC,
    val element: QuantumElement? = null,
) {
    init {
        require(value > 0)
        require(kind != TileKind.ELEMENT || element != null)
    }
}

@Serializable
data class GameState(
    val size: Int = 4,
    val cells: List<Tile?> = List(16) { null },
    val score: Long = 0,
    val bestScore: Long = 0,
    val status: GameStatus = GameStatus.PLAYING,
    val mode: GameMode = GameMode.CLASSIC,
    val difficulty: Difficulty = Difficulty.fromMode(mode),
    val hasAcknowledgedWin: Boolean = false,
    val moveCount: Int = 0,
    val nextTileId: Long = 1,
) {
    init { require(size >= 2 && cells.size == size * size) }
    operator fun get(row: Int, column: Int): Tile? = cells[row * size + column]
}

data class MoveResult(
    val state: GameState,
    val changed: Boolean,
    val gainedScore: Int = 0,
    val mergeCount: Int = 0,
    val reactionCount: Int = 0,
    val animations: List<MoveAnimation> = emptyList(),
)

data class MoveAnimation(
    val tileId: Long,
    val fromIndex: Int,
    val toIndex: Int,
    val kind: MoveAnimationKind,
)

enum class MoveAnimationKind { SLIDE, MERGE, REACTION, SPAWN }

enum class CompoundFailure { LAB_DISABLED, GAME_NOT_ACTIVE, TILE_NOT_FOUND, INVALID_TILE, NO_RECIPE }
sealed interface CompoundResult {
    data class Success(val state: GameState, val recipe: CompoundRecipe) : CompoundResult
    data class Failure(val state: GameState, val reason: CompoundFailure) : CompoundResult
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

data class DuelConfig(
    val difficulty: Difficulty = Difficulty.QUANTUM,
    val opponent: DuelOpponent = DuelOpponent.BOT,
    val botDifficulty: BotDifficulty = BotDifficulty.NORMAL,
    val boardSize: Int = 4,
    val turnSeconds: Int = 12,
)

data class DuelState(
    val playerOne: GameState,
    val playerTwo: GameState,
    val currentPlayer: DuelPlayer = DuelPlayer.PLAYER_ONE,
    val config: DuelConfig = DuelConfig(),
    val winner: DuelPlayer? = null,
    val turnNumber: Int = 1,
) {
    val activeBoard: GameState get() = if (currentPlayer == DuelPlayer.PLAYER_ONE) playerOne else playerTwo
    val inactiveBoard: GameState get() = if (currentPlayer == DuelPlayer.PLAYER_ONE) playerTwo else playerOne
}
