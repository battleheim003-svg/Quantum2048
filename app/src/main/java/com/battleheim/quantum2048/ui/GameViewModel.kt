package com.battleheim.quantum2048.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.battleheim.quantum2048.analytics.AnalyticsGateway
import com.battleheim.quantum2048.analytics.NoOpAnalyticsGateway
import com.battleheim.quantum2048.domain.CollectionRepository
import com.battleheim.quantum2048.domain.GameRepository
import com.battleheim.quantum2048.domain.ProfileRepository
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
import com.battleheim.quantum2048.engine.TunnelFailure
import com.battleheim.quantum2048.engine.TunnelResult
import com.battleheim.quantum2048.engine.SuperpositionFailure
import com.battleheim.quantum2048.engine.SuperpositionResult
import com.battleheim.quantum2048.engine.ObserverFailure
import com.battleheim.quantum2048.engine.ObserverResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate

data class GameUiState(
    val game: GameState = GameState(mode = GameMode.QUANTUM),
    val canUndo: Boolean = false,
    val loading: Boolean = true,
    val labTileIds: List<Long> = emptyList(),
    val message: String? = null,
    val feedback: GameFeedback? = null,
    val animations: List<MoveAnimation> = emptyList(),
    val duel: DuelState? = null,
    val tunnelingTileId: Long? = null,
    val superpositionTileId: Long? = null,
    val observerPreview: ObserverPreview? = null,
)

enum class GameFeedback { MOVE, MERGE, REACTION, COMPOUND, TUNNEL, COLLAPSE_LOW, COLLAPSE_HIGH, GAME_OVER }
data class ObserverPreview(val tileId: Long, val value: Int)

data class SavedGameKey(val difficulty: Difficulty, val size: Int)

class GameViewModel(
    private val repository: GameRepository,
    private val collectionRepository: CollectionRepository,
    private val profileRepository: ProfileRepository,
    private val engine: GameEngine,
    private val analytics: AnalyticsGateway = NoOpAnalyticsGateway,
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
            rememberUndoIfAllowed(before)
            undoCompoundSymbol = null
            logTerminalStateIfNeeded(result.state)
            result.state.cells.mapNotNull { it?.element }.maxByOrNull { it.rank }?.let { element ->
                val beforeBestRank = before.cells.mapNotNull { it?.element }.maxOfOrNull { it.rank } ?: 0
                if (element.rank > beforeBestRank) analytics.logFusionPerformed(element)
            }
            _ui.value = _ui.value.copy(
                game = _ui.value.duel?.activeBoard ?: result.state,
                canUndo = undo.canUndo && FusionRules.isUndoEnabled(before.difficulty),
                message = when {
                    result.energyOverflowBonus > 0 -> "Energy overflow +${result.energyOverflowBonus}"
                    result.reactionCount > 0 -> "Particle reaction complete"
                    else -> null
                },
                feedback = when {
                    result.state.status == GameStatus.LOST -> GameFeedback.GAME_OVER
                    result.entanglementCollapseCount > 0 -> GameFeedback.REACTION
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
        if (duel.winner == null && nextDuel.winner != null) {
            analytics.logDuelResult(nextDuel.config.difficulty, nextDuel.config.botDifficulty, nextDuel.winner)
        }
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

    fun toggleTunneling() {
        if (inputLocked || _ui.value.loading) return
        val selected = _ui.value.tunnelingTileId
        _ui.value = _ui.value.copy(
            tunnelingTileId = if (selected == null) -1L else null,
            labTileIds = emptyList(),
            message = if (selected == null) "Select a tile, then an empty cell" else "Tunneling cancelled",
        )
    }

    fun tapBoardCell(index: Int) {
        if (inputLocked || _ui.value.loading || _ui.value.duel != null) return
        val state = _ui.value.game
        val tile = state.cells.getOrNull(index)
        val tunneling = _ui.value.tunnelingTileId
        if (tunneling == null) {
            if (tile?.superpositionValues?.isNotEmpty() == true) {
                _ui.value = _ui.value.copy(superpositionTileId = tile.id, labTileIds = emptyList())
            }
            return
        }
        if (tunneling < 0L) {
            _ui.value = if (tile == null) {
                _ui.value.copy(message = "Select a tile first")
            } else {
                _ui.value.copy(tunnelingTileId = tile.id, message = "Select an empty destination")
            }
            return
        }
        if (tile != null) {
            _ui.value = _ui.value.copy(tunnelingTileId = tile.id, message = "Tunnel source changed")
            return
        }
        val before = state
        when (val result = engine.tunnel(before, tunneling, index)) {
            is TunnelResult.Success -> {
                rememberUndoIfAllowed(before)
                undoCompoundSymbol = null
                _ui.value = _ui.value.copy(
                    game = result.state,
                    canUndo = undo.canUndo,
                    tunnelingTileId = null,
                    message = "Tile tunneled",
                    feedback = GameFeedback.TUNNEL,
                    animations = listOf(result.animation),
                )
                persist()
            }
            is TunnelResult.Failure -> {
                val message = when (result.reason) {
                    TunnelFailure.INSUFFICIENT_SCORE -> "Not enough energy for tunneling"
                    TunnelFailure.DESTINATION_OCCUPIED -> "Choose an empty destination"
                    TunnelFailure.LAB_DISABLED -> "Tunneling is quantum-only"
                    TunnelFailure.GAME_NOT_ACTIVE -> "The game is not active"
                    TunnelFailure.TILE_NOT_FOUND -> "Tunnel source is gone"
                }
                _ui.value = _ui.value.copy(message = message)
            }
        }
    }

    fun collapseSuperposition(choiceIndex: Int) {
        if (inputLocked || _ui.value.loading) return
        val tileId = _ui.value.superpositionTileId ?: return
        val before = _ui.value.game
        when (val result = engine.collapseSuperposition(before, tileId, choiceIndex)) {
            is SuperpositionResult.Success -> {
                rememberUndoIfAllowed(before)
                undoCompoundSymbol = null
                _ui.value = _ui.value.copy(
                    game = result.state,
                    canUndo = undo.canUndo,
                    superpositionTileId = null,
                    message = if (choiceIndex == 0) "Low collapse stabilized" else "High collapse stabilized",
                    feedback = if (choiceIndex == 0) GameFeedback.COLLAPSE_LOW else GameFeedback.COLLAPSE_HIGH,
                    animations = listOf(result.animation),
                )
                persist()
            }
            is SuperpositionResult.Failure -> {
                val message = when (result.reason) {
                    SuperpositionFailure.INSUFFICIENT_SCORE -> "Not enough energy to collapse"
                    SuperpositionFailure.INVALID_CHOICE -> "Collapse choice is invalid"
                    SuperpositionFailure.NOT_SUPERPOSITION -> "Tile is already stable"
                    SuperpositionFailure.LAB_DISABLED -> "Superposition is quantum-only"
                    SuperpositionFailure.GAME_NOT_ACTIVE -> "The game is not active"
                    SuperpositionFailure.TILE_NOT_FOUND -> "Superposition tile is gone"
                }
                _ui.value = _ui.value.copy(message = message)
            }
        }
    }

    fun dismissSuperposition() {
        _ui.value = _ui.value.copy(superpositionTileId = null)
    }

    fun observeTile(tileId: Long) {
        if (inputLocked || _ui.value.loading || _ui.value.duel != null) return
        val before = _ui.value.game
        when (val result = engine.observeSuperposition(before, tileId)) {
            is ObserverResult.Success -> {
                _ui.value = _ui.value.copy(
                    game = result.state,
                    observerPreview = ObserverPreview(tileId, result.previewValue),
                    message = "Observer preview: ${result.previewValue}",
                )
                persist()
                viewModelScope.launch {
                    delay(OBSERVER_PREVIEW_MS)
                    if (_ui.value.observerPreview?.tileId == tileId) {
                        _ui.value = _ui.value.copy(observerPreview = null)
                    }
                }
            }
            is ObserverResult.Failure -> {
                val message = when (result.reason) {
                    ObserverFailure.INSUFFICIENT_SCORE -> "Not enough energy to observe"
                    ObserverFailure.NOT_SUPERPOSITION -> "Only superposition tiles can be observed"
                    ObserverFailure.LAB_DISABLED -> "Observer effect is quantum-only"
                    ObserverFailure.GAME_NOT_ACTIVE -> "The game is not active"
                    ObserverFailure.TILE_NOT_FOUND -> "Observer target is gone"
                }
                _ui.value = _ui.value.copy(message = message)
            }
        }
    }

    private fun tryCompleteLab() {
        val selected = _ui.value.labTileIds
        if (selected.size < 2) return
        val before = _ui.value.game
        when (val result = engine.combineCompound(before, selected)) {
            is CompoundResult.Success -> {
                rememberUndoIfAllowed(before)
                undoCompoundSymbol = result.recipe.output.symbol
                result.state.cells.mapNotNull { it?.element }.maxByOrNull { it.rank }?.let(analytics::logFusionPerformed)
                _ui.value = _ui.value.copy(
                    game = result.state,
                    canUndo = undo.canUndo,
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
        _ui.value = _ui.value.copy(game = prior.copy(usedUndo = true), canUndo = false, labTileIds = emptyList(), tunnelingTileId = null, superpositionTileId = null, observerPreview = null, message = "Move undone")
        persist()
    }

    fun newGame() {
        undo.clear()
        val previous = _ui.value.game
        val nextGame = engine.newGame(previous.difficulty, previous.size).copy(bestScore = previous.bestScore)
        analytics.logLevelStart(nextGame)
        _ui.value = _ui.value.copy(
            game = nextGame,
            canUndo = false,
            labTileIds = emptyList(),
            tunnelingTileId = null,
            superpositionTileId = null,
            observerPreview = null,
            animations = emptyList(),
        )
        persist()
    }

    fun newGame(difficulty: Difficulty, size: Int = requestedSize) {
        undo.clear()
        requestedSize = size
        val nextGame = engine.newGame(difficulty, size)
        analytics.logLevelStart(nextGame)
        _ui.value = _ui.value.copy(
            game = nextGame,
            canUndo = false,
            labTileIds = emptyList(),
            tunnelingTileId = null,
            superpositionTileId = null,
            observerPreview = null,
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
        analytics.logDuelStarted(difficulty, opponent, botDifficulty)
        _ui.value = GameUiState(game = duel.activeBoard, loading = false, duel = duel)
    }

    fun passDuelTurn() {
        val duel = _ui.value.duel ?: return
        val next = duelEngine.passTimedOutTurn(duel)
        if (duel.winner == null && next.winner != null) {
            analytics.logDuelResult(next.config.difficulty, next.config.botDifficulty, next.winner)
        }
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
                val nextGame = engine.newGame(difficulty, _ui.value.game.size)
                analytics.logLevelStart(nextGame)
                _ui.value = _ui.value.copy(
                    game = nextGame,
                    canUndo = false,
                    labTileIds = emptyList(),
                    tunnelingTileId = null,
                    superpositionTileId = null,
                    observerPreview = null,
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

    fun completeTutorial() {
        _ui.value = _ui.value.copy(game = _ui.value.game.copy(tutorialCompleted = true))
        persist()
    }

    private fun load(mode: GameMode) {
        load(Difficulty.fromMode(mode), requestedSize)
    }

    private fun load(difficulty: Difficulty, size: Int) {
        inputLocked = true
        requestedSize = size
        _ui.value = _ui.value.copy(loading = true)
        viewModelScope.launch {
            val today = LocalDate.now().toString()
            val restored = repository.observe(difficulty, size).first()
            val game = if (difficulty == Difficulty.DAILY && restored?.dailyChallengeDate != today) {
                engine.newGame(difficulty, size)
            } else {
                restored ?: engine.newGame(difficulty, size)
            }
            val loadedGame = if (difficulty == Difficulty.DAILY) {
                val profile = profileRepository.observe().first()
                val dailyBest = maxOf(game.dailyBestScore, profile.dailyBestScore(game.dailyChallengeDate))
                game.copy(dailyBestScore = dailyBest)
            } else {
                game
            }
            _ui.value = GameUiState(game = loadedGame, loading = false, duel = null)
            analytics.logLevelStart(loadedGame)
            repository.save(loadedGame)
            inputLocked = false
        }
    }

    private fun persist() = viewModelScope.launch {
        repository.save(_ui.value.game)
        profileRepository.record(_ui.value.game)
    }

    private fun rememberUndoIfAllowed(state: GameState) {
        if (FusionRules.isUndoEnabled(state.difficulty)) {
            undo.remember(state)
        } else {
            undo.clear()
        }
    }

    private fun logTerminalStateIfNeeded(state: GameState) {
        when (state.status) {
            GameStatus.WON -> analytics.logLevelComplete(state)
            GameStatus.LOST -> analytics.logLevelFail(state)
            GameStatus.PLAYING -> Unit
        }
    }

    private companion object {
        const val MOVE_LOCK_MS = 390L
        const val OBSERVER_PREVIEW_MS = 2400L
    }
}

private fun DuelPlayer.label(): String = when (this) {
    DuelPlayer.PLAYER_ONE -> "Player 1"
    DuelPlayer.PLAYER_TWO -> "Player 2"
}
