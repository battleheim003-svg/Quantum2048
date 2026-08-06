package com.battleheim.quantum2048.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.battleheim.quantum2048.domain.GameRepository
import com.battleheim.quantum2048.domain.UndoBuffer
import com.battleheim.quantum2048.engine.CollapseFailure
import com.battleheim.quantum2048.engine.CollapseResult
import com.battleheim.quantum2048.engine.Direction
import com.battleheim.quantum2048.engine.GameEngine
import com.battleheim.quantum2048.engine.GameMode
import com.battleheim.quantum2048.engine.GameState
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
    val collapsePulseId: Long? = null,
    val message: String? = null,
)

class GameViewModel(private val repository: GameRepository, private val engine: GameEngine) : ViewModel() {
    private val _ui = MutableStateFlow(GameUiState())
    val ui: StateFlow<GameUiState> = _ui.asStateFlow()
    val balance: QuantumBalance get() = engine.balance
    private val undo = UndoBuffer()
    private var inputLocked = false

    init { load(GameMode.QUANTUM) }

    fun swipe(direction: Direction) {
        if (inputLocked || _ui.value.loading || _ui.value.selectedTileId != null) return
        inputLocked = true
        val before = _ui.value.game
        val result = engine.move(before, direction)
        if (result.changed) {
            undo.remember(before)
            _ui.value = _ui.value.copy(
                game = result.state,
                canUndo = undo.canUndo,
                message = result.autoCollapse?.let { "فروپاشی خودکار: ${it.chosenValue}" },
            )
            persist()
        } else {
            _ui.value = _ui.value.copy(game = result.state)
        }
        inputLocked = false
    }

    @Suppress("UNUSED_PARAMETER")
    fun selectTile(tileId: Long) {
        // Quantum tiles are now stable particles/elements; tapping the board is reserved for future details.
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
                    message = "فروپاشی انجام شد - ${result.energySpent} انرژی مصرف شد",
                )
                persist()
            }
            is CollapseResult.Failure -> _ui.value = _ui.value.copy(
                selectedTileId = null,
                message = when (result.reason) {
                    CollapseFailure.INSUFFICIENT_ENERGY -> "انرژی کوانتومی کافی نیست"
                    CollapseFailure.GAME_NOT_ACTIVE -> "بازی در حال اجرا نیست"
                    else -> "این کاشی دیگر قابل فروپاشی نیست"
                },
            )
        }
        inputLocked = false
    }

    fun switchMode(mode: GameMode) {
        if (mode == _ui.value.game.mode || inputLocked) return
        undo.clear()
        load(mode)
    }

    fun undo() {
        val prior = undo.consume() ?: return
        _ui.value = _ui.value.copy(game = prior, canUndo = false, selectedTileId = null, message = "حرکت بازگردانده شد")
        persist()
    }

    fun newGame() {
        undo.clear()
        val previous = _ui.value.game
        _ui.value = _ui.value.copy(
            game = engine.newGame(previous.mode).copy(bestScore = previous.bestScore),
            canUndo = false,
            selectedTileId = null,
            collapsePulseId = null,
        )
        persist()
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

    private fun load(mode: GameMode) {
        inputLocked = true
        _ui.value = _ui.value.copy(loading = true, selectedTileId = null)
        viewModelScope.launch {
            val restored = repository.observe(mode).first()
            val game = restored?.takeUnless { it.requiresQuantumReset() } ?: engine.newGame(mode)
            _ui.value = GameUiState(game = game, loading = false)
            repository.save(game)
            inputLocked = false
        }
    }

    private fun persist() = viewModelScope.launch { repository.save(_ui.value.game) }

    private fun GameState.requiresQuantumReset(): Boolean {
        if (mode != GameMode.QUANTUM) return false
        return cells.any { tile -> tile != null && (tile.species == null || tile.isUnstable) }
    }
}
