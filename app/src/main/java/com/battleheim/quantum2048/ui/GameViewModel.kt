package com.battleheim.quantum2048.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.battleheim.quantum2048.domain.CollectionRepository
import com.battleheim.quantum2048.domain.GameRepository
import com.battleheim.quantum2048.domain.UndoBuffer
import com.battleheim.quantum2048.engine.CollapseFailure
import com.battleheim.quantum2048.engine.CollapseResult
import com.battleheim.quantum2048.engine.CompoundFailure
import com.battleheim.quantum2048.engine.CompoundResult
import com.battleheim.quantum2048.engine.Difficulty
import com.battleheim.quantum2048.engine.Direction
import com.battleheim.quantum2048.engine.GameEngine
import com.battleheim.quantum2048.engine.GameMode
import com.battleheim.quantum2048.engine.GameState
import com.battleheim.quantum2048.engine.GameStatus
import com.battleheim.quantum2048.engine.QuantumBalance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class GameUiState(
    val game: GameState = GameState(mode = GameMode.QUANTUM),
    val canUndo: Boolean = false,
    val loading: Boolean = true,
    val selectedTileId: Long? = null,
    val labTileIds: List<Long> = emptyList(),
    val collapsePulseId: Long? = null,
    val message: String? = null,
    val feedback: GameFeedback? = null,
)

enum class GameFeedback { MOVE, MERGE, COLLAPSE, COMPOUND, GAME_OVER }

class GameViewModel(
    private val repository: GameRepository,
    private val collectionRepository: CollectionRepository,
    private val engine: GameEngine,
) : ViewModel() {
    private val _ui = MutableStateFlow(GameUiState())
    val ui: StateFlow<GameUiState> = _ui.asStateFlow()
    val balance: QuantumBalance get() = engine.balance
    private val undo = UndoBuffer()
    private var inputLocked = false
    private var undoCompoundSymbol: String? = null

    init { load(Difficulty.QUANTUM) }

    fun swipe(direction: Direction) {
        if (inputLocked || _ui.value.loading || _ui.value.selectedTileId != null) return
        inputLocked = true
        val before = _ui.value.game
        val result = engine.move(before, direction)
        if (result.changed) {
            undo.remember(before)
            undoCompoundSymbol = null
            _ui.value = _ui.value.copy(
                game = result.state,
                canUndo = undo.canUndo,
                message = result.autoCollapse?.let { "Auto collapse: ${it.chosenValue}" },
                feedback = when {
                    result.state.status == GameStatus.LOST -> GameFeedback.GAME_OVER
                    result.mergeCount > 0 -> GameFeedback.MERGE
                    else -> GameFeedback.MOVE
                },
            )
            persist()
        } else {
            _ui.value = _ui.value.copy(game = result.state)
        }
        inputLocked = false
    }

    fun selectTile(tileId: Long) {
        if (_ui.value.game.cells.any { it?.id == tileId && it.isUnstable }) {
            _ui.value = _ui.value.copy(selectedTileId = tileId)
        }
    }

    fun dismissCollapse() {
        _ui.value = _ui.value.copy(selectedTileId = null)
    }

    fun collapse(chosenValue: Int) {
        if (inputLocked) return
        val tileId = _ui.value.selectedTileId ?: return
        inputLocked = true
        val before = _ui.value.game
        when (val result = engine.collapse(before, tileId, chosenValue)) {
            is CollapseResult.Success -> {
                undo.remember(before)
                _ui.value = _ui.value.copy(
                    game = result.state,
                    selectedTileId = null,
                    collapsePulseId = result.event.tileId,
                    canUndo = true,
                    message = "Collapse complete - ${result.energySpent} energy spent",
                feedback = GameFeedback.COLLAPSE,
                )
                persist()
            }
            is CollapseResult.Failure -> _ui.value = _ui.value.copy(
                selectedTileId = null,
                message = when (result.reason) {
                    CollapseFailure.INSUFFICIENT_ENERGY -> "Not enough quantum energy"
                    CollapseFailure.GAME_NOT_ACTIVE -> "The game is not active"
                    else -> "This tile cannot collapse"
                },
            )
        }
        inputLocked = false
    }

    fun sendToCompoundLab(tileId: Long) {
        if (inputLocked || _ui.value.loading) return
        val state = _ui.value.game
        val tile = state.cells.firstOrNull { it?.id == tileId } ?: return
        if (tile.species == null || tile.isUnstable) {
            _ui.value = _ui.value.copy(message = "Only stable elements can enter the lab")
            return
        }
        if (!engine.balance.rulesFor(state.difficulty).compoundLabEnabled) {
            _ui.value = _ui.value.copy(message = "Compound Lab is locked on this level")
            return
        }

        val selected = (_ui.value.labTileIds + tileId).distinct()
        val maxInputs = engine.balance.compoundRecipes
            .filter { recipe ->
                val level = engine.balance.rulesFor(state.difficulty).allowedRecipeLevel
                level == null || recipe.unlockLevel.ordinal <= level.ordinal
            }
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
                    CompoundFailure.INSUFFICIENT_ENERGY -> "Not enough energy for lab synthesis"
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
        load(Difficulty.fromMode(mode))
    }

    fun switchDifficulty(difficulty: Difficulty) {
        if (difficulty == _ui.value.game.difficulty || inputLocked) return
        undo.clear()
        load(difficulty)
    }

    fun undo() {
        val prior = undo.consume() ?: return
        val compoundSymbol = undoCompoundSymbol
        undoCompoundSymbol = null
        if (compoundSymbol != null) {
            viewModelScope.launch { collectionRepository.unrecord(compoundSymbol) }
        }
        _ui.value = _ui.value.copy(game = prior, canUndo = false, selectedTileId = null, labTileIds = emptyList(), message = "Move undone")
        persist()
    }

    fun newGame() {
        undo.clear()
        val previous = _ui.value.game
        _ui.value = _ui.value.copy(
            game = engine.newGame(previous.difficulty).copy(bestScore = previous.bestScore),
            canUndo = false,
            selectedTileId = null,
            labTileIds = emptyList(),
            collapsePulseId = null,
        )
        persist()
    }

    fun newGame(difficulty: Difficulty) {
        undo.clear()
        _ui.value = _ui.value.copy(
            game = engine.newGame(difficulty),
            canUndo = false,
            selectedTileId = null,
            labTileIds = emptyList(),
            collapsePulseId = null,
            loading = false,
        )
        persist()
    }

    fun loadDifficulty(difficulty: Difficulty) {
        undo.clear()
        load(difficulty)
    }

    suspend fun hasSave(difficulty: Difficulty): Boolean = repository.observe(difficulty).first() != null

    suspend fun savedDifficulties(): Set<Difficulty> =
        Difficulty.entries.filter { hasSave(it) }.toSet()

    fun resetDifficulty(difficulty: Difficulty) {
        viewModelScope.launch {
            repository.clear(difficulty)
            if (_ui.value.game.difficulty == difficulty) {
                undo.clear()
                _ui.value = _ui.value.copy(
                    game = engine.newGame(difficulty),
                    canUndo = false,
                    selectedTileId = null,
                    labTileIds = emptyList(),
                    collapsePulseId = null,
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

    fun consumeCollapsePulse() {
        _ui.value = _ui.value.copy(collapsePulseId = null)
    }

    fun consumeFeedback() {
        _ui.value = _ui.value.copy(feedback = null)
    }

    private fun load(mode: GameMode) {
        load(Difficulty.fromMode(mode))
    }

    private fun load(difficulty: Difficulty) {
        inputLocked = true
        _ui.value = _ui.value.copy(loading = true, selectedTileId = null)
        viewModelScope.launch {
            val restored = repository.observe(difficulty).first()
            val game = restored ?: engine.newGame(difficulty)
            _ui.value = GameUiState(game = game, loading = false)
            repository.save(game)
            inputLocked = false
        }
    }

    private fun persist() = viewModelScope.launch { repository.save(_ui.value.game) }
}
