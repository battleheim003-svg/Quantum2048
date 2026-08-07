package com.battleheim.quantum2048.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.battleheim.quantum2048.domain.CollectionRepository
import com.battleheim.quantum2048.domain.GameRepository
import com.battleheim.quantum2048.domain.UndoBuffer
import com.battleheim.quantum2048.engine.CompoundFailure
import com.battleheim.quantum2048.engine.CompoundResult
import com.battleheim.quantum2048.engine.Difficulty
import com.battleheim.quantum2048.engine.Direction
import com.battleheim.quantum2048.engine.BotDifficulty
import com.battleheim.quantum2048.engine.DuelConfig
import com.battleheim.quantum2048.engine.DuelEngine
import com.battleheim.quantum2048.engine.DuelOpponent
import com.battleheim.quantum2048.engine.DuelPlayer
import com.battleheim.quantum2048.engine.DuelState
import com.battleheim.quantum2048.engine.GameEngine
import com.battleheim.quantum2048.engine.GameMode
import com.battleheim.quantum2048.engine.GameState
import com.battleheim.quantum2048.engine.GameStatus
import com.battleheim.quantum2048.engine.FusionRules
import com.battleheim.quantum2048.engine.MoveAnimation
import com.battleheim.quantum2048.engine.SeededRandomProvider
import com.battleheim.quantum2048.engine.TileKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class GameUiState(
    val game: GameState = GameState(mode = GameMode.QUANTUM),
    val canUndo: Boolean = false,
    val loading: Boolean = true,
    val labTileIds: List<Long> = emptyList(),
    val message: String? = null,
    val feedback: GameFeedback? = null,
    val animations: List<MoveAnimation> = emptyList(),
    val duel: DuelState? = null,
)

enum class GameFeedback { MOVE, MERGE, REACTION, COMPOUND, GAME_OVER }

data class SavedGameKey(val difficulty: Difficulty, val size: Int)

class GameViewModel(
    private val repository: GameRepository,
    private val collectionRepository: CollectionRepository,
    private val engine: GameEngine,
) : ViewModel() {
    private val _ui = MutableStateFlow(GameUiState())
    val ui: StateFlow<GameUiState> = _ui.asStateFlow()
    private val undo = UndoBuffer()
    private val duelEngine = DuelEngine(SeededRandomProvider(404))
    private var inputLocked = false
    private var undoCompoundSymbol: String? = null
    private var requestedSize: Int = 4

    init { load(Difficulty.QUANTUM, requestedSize) }

    fun swipe(direction: Direction) {
        if (inputLocked || _ui.value.loading) return
        inputLocked = true
        val before = _ui.value.game
        val duel = _ui.value.duel
        val result = if (duel != null) {
            val (nextDuel, moveResult) = duelEngine.move(duel, direction)
            _ui.value = _ui.value.copy(duel = nextDuel)
            moveResult
        } else {
            engine.move(before, direction)
        }
        if (result.changed) {
            undo.remember(before)
            undoCompoundSymbol = null
            _ui.value = _ui.value.copy(
                game = _ui.value.duel?.activeBoard ?: result.state,
                canUndo = undo.canUndo,
                message = if (result.reactionCount > 0) "Particle reaction complete" else null,
                feedback = when {
                    result.state.status == GameStatus.LOST -> GameFeedback.GAME_OVER
                    result.reactionCount > 0 -> GameFeedback.REACTION
                    result.mergeCount > 0 -> GameFeedback.MERGE
                    else -> GameFeedback.MOVE
                },
                animations = result.animations,
            )
            persist()
            viewModelScope.launch {
                delay(MOVE_LOCK_MS)
                runBotTurnIfNeeded()
                inputLocked = false
            }
        } else {
            _ui.value = _ui.value.copy(game = result.state)
            inputLocked = false
        }
    }

    private fun runBotTurnIfNeeded() {
        val duel = _ui.value.duel ?: return
        val (nextDuel, result) = duelEngine.botMoveIfNeeded(duel)
        _ui.value = _ui.value.copy(
            duel = nextDuel,
            game = nextDuel.activeBoard,
            animations = result?.animations ?: emptyList(),
            message = nextDuel.winner?.let { "${it.label()} wins" },
            feedback = result?.let {
                when {
                    it.reactionCount > 0 -> GameFeedback.REACTION
                    it.mergeCount > 0 -> GameFeedback.MERGE
                    else -> GameFeedback.MOVE
                }
            },
        )
    }

    fun sendToCompoundLab(tileId: Long) {
        if (inputLocked || _ui.value.loading) return
        val state = _ui.value.game
        val tile = state.cells.firstOrNull { it?.id == tileId } ?: return
        if (tile.kind != TileKind.ELEMENT || tile.element == null) {
            _ui.value = _ui.value.copy(message = "Only elements can enter the lab")
            return
        }
        if (state.mode != GameMode.QUANTUM) {
            _ui.value = _ui.value.copy(message = "Compound Lab is locked on this level")
            return
        }

        val selected = (_ui.value.labTileIds + tileId).distinct()
        val maxInputs = FusionRules.compoundRecipes
            .maxOfOrNull { it.inputs.size } ?: 2
        _ui.value = _ui.value.copy(labTileIds = selected.takeLast(maxInputs), message = "Lab sample ${selected.size}/$maxInputs")
        tryCompleteLab()
    }

    fun clearCompoundLab() {
        _ui.value = _ui.value.copy(labTileIds = emptyList())
    }

    private fun tryCompleteLab() {
        val selected = _ui.value.labTileIds
        if (selected.size < 2) return
        val before = _ui.value.game
        when (val result = engine.combineCompound(before, selected)) {
            is CompoundResult.Success -> {
                undo.remember(before)
                undoCompoundSymbol = result.recipe.output.symbol
                _ui.value = _ui.value.copy(
                    game = result.state,
                    canUndo = true,
                    labTileIds = emptyList(),
                    message = "Discovered ${result.recipe.output.symbol}",
                    feedback = GameFeedback.COMPOUND,
                )
                viewModelScope.launch { collectionRepository.record(result.recipe.output, before.difficulty) }
                persist()
            }
            is CompoundResult.Failure -> {
                val reason = when (result.reason) {
                    CompoundFailure.NO_RECIPE -> "No compound recipe matched"
                    CompoundFailure.LAB_DISABLED -> "Compound Lab is disabled here"
                    else -> "This lab sample is invalid"
                }
                _ui.value = _ui.value.copy(message = reason)
            }
        }
    }

    fun switchMode(mode: GameMode) {
        if (mode == _ui.value.game.mode || inputLocked) return
        undo.clear()
        load(Difficulty.fromMode(mode), _ui.value.game.size)
    }

    fun switchDifficulty(difficulty: Difficulty) {
        if (difficulty == _ui.value.game.difficulty || inputLocked) return
        undo.clear()
        load(difficulty, _ui.value.game.size)
    }

    fun undo() {
        val prior = undo.consume() ?: return
        val compoundSymbol = undoCompoundSymbol
        undoCompoundSymbol = null
        if (compoundSymbol != null) {
            viewModelScope.launch { collectionRepository.unrecord(compoundSymbol) }
        }
        _ui.value = _ui.value.copy(game = prior, canUndo = false, labTileIds = emptyList(), message = "Move undone")
        persist()
    }

    fun newGame() {
        undo.clear()
        val previous = _ui.value.game
        _ui.value = _ui.value.copy(
            game = engine.newGame(previous.difficulty, previous.size).copy(bestScore = previous.bestScore),
            canUndo = false,
            labTileIds = emptyList(),
            animations = emptyList(),
        )
        persist()
    }

    fun newGame(difficulty: Difficulty, size: Int = requestedSize) {
        undo.clear()
        requestedSize = size
        _ui.value = _ui.value.copy(
            game = engine.newGame(difficulty, size),
            canUndo = false,
            labTileIds = emptyList(),
            animations = emptyList(),
            loading = false,
        )
        persist()
    }

    fun newDuel(difficulty: Difficulty, opponent: DuelOpponent, botDifficulty: BotDifficulty, turnSeconds: Int = 12) {
        undo.clear()
        val duel = duelEngine.newDuel(
            DuelConfig(
                difficulty = difficulty,
                opponent = opponent,
                botDifficulty = botDifficulty,
                boardSize = 4,
                turnSeconds = turnSeconds,
            ),
        )
        _ui.value = GameUiState(game = duel.activeBoard, loading = false, duel = duel)
    }

    fun passDuelTurn() {
        val duel = _ui.value.duel ?: return
        val next = duelEngine.passTimedOutTurn(duel)
        _ui.value = _ui.value.copy(
            duel = next,
            game = next.activeBoard,
            message = next.winner?.let { "${it.label()} wins" } ?: "Turn passed",
        )
        if (next.winner == null) runBotTurnIfNeeded()
    }

    fun loadDifficulty(difficulty: Difficulty, size: Int = requestedSize) {
        undo.clear()
        load(difficulty, size)
    }

    suspend fun hasSave(difficulty: Difficulty): Boolean = repository.observe(difficulty).first() != null
    suspend fun hasSave(difficulty: Difficulty, size: Int): Boolean = repository.observe(difficulty, size).first() != null

    suspend fun savedDifficulties(): Set<Difficulty> =
        Difficulty.entries.filter { hasSave(it) }.toSet()

    suspend fun savedGames(): Set<SavedGameKey> =
        Difficulty.entries.flatMap { difficulty ->
            FusionRules.supportedBoardSizes.mapNotNull { size ->
                if (hasSave(difficulty, size)) SavedGameKey(difficulty, size) else null
            }
        }.toSet()

    fun resetDifficulty(difficulty: Difficulty) {
        viewModelScope.launch {
            repository.clear(difficulty)
            if (_ui.value.game.difficulty == difficulty) {
                undo.clear()
                _ui.value = _ui.value.copy(
                    game = engine.newGame(difficulty, _ui.value.game.size),
                    canUndo = false,
                    labTileIds = emptyList(),
                    animations = emptyList(),
                    loading = false,
                )
                persist()
            }
        }
    }

    fun continueGame() {
        _ui.value = _ui.value.copy(game = engine.continueAfterWin(_ui.value.game))
        persist()
    }

    fun consumeMessage() {
        _ui.value = _ui.value.copy(message = null)
    }

    fun consumeFeedback() {
        _ui.value = _ui.value.copy(feedback = null)
    }

    private fun load(mode: GameMode) {
        load(Difficulty.fromMode(mode), requestedSize)
    }

    private fun load(difficulty: Difficulty, size: Int) {
        inputLocked = true
        requestedSize = size
        _ui.value = _ui.value.copy(loading = true)
        viewModelScope.launch {
            val restored = repository.observe(difficulty, size).first()
            val game = restored ?: engine.newGame(difficulty, size)
            _ui.value = GameUiState(game = game, loading = false, duel = null)
            repository.save(game)
            inputLocked = false
        }
    }

    private fun persist() = viewModelScope.launch { repository.save(_ui.value.game) }

    private companion object {
        const val MOVE_LOCK_MS = 190L
    }
}

private fun DuelPlayer.label(): String = when (this) {
    DuelPlayer.PLAYER_ONE -> "Player 1"
    DuelPlayer.PLAYER_TWO -> "Player 2"
}
