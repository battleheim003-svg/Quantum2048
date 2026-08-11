package com.battleheim.quantum2048.engine

interface DuelBot {
    fun chooseMove(state: GameState): Direction?
}

class EasyBot(private val random: RandomProvider) : DuelBot {
    override fun chooseMove(state: GameState): Direction? {
        val valid = BotScoring.validMoves(state)
        if (valid.isEmpty()) return null
        return valid.firstOrNull { BotScoring.scoreAfter(state, it).status != GameStatus.LOST }
            ?: valid[random.nextInt(valid.size)]
    }
}

class NormalBot : DuelBot {
    override fun chooseMove(state: GameState): Direction? =
        BotScoring.validMoves(state).maxByOrNull { direction ->
            BotScoring.heuristic(BotScoring.scoreAfter(state, direction), chemistryAware = false)
        }
}

class QuantumHardBot : DuelBot {
    override fun chooseMove(state: GameState): Direction? =
        BotScoring.validMoves(state).maxByOrNull { direction ->
            val next = BotScoring.scoreAfter(state, direction)
            val lookahead = BotScoring.validMoves(next).maxOfOrNull {
                BotScoring.heuristic(BotScoring.scoreAfter(next, it), chemistryAware = true)
            } ?: BotScoring.heuristic(next, chemistryAware = true)
            BotScoring.heuristic(next, chemistryAware = true) + lookahead / 2
        }
}

object BotScoring {
    fun botFor(difficulty: BotDifficulty, random: RandomProvider): DuelBot = when (difficulty) {
        BotDifficulty.EASY -> EasyBot(random)
        BotDifficulty.NORMAL -> NormalBot()
        BotDifficulty.QUANTUM_HARD -> QuantumHardBot()
    }

    fun validMoves(state: GameState): List<Direction> =
        Direction.entries.filter { GameEngine(SeededRandomProvider(17)).move(state, it).changed }

    fun scoreAfter(state: GameState, direction: Direction): GameState =
        GameEngine(SeededRandomProvider(direction.ordinal.toLong() + state.moveCount * 31L)).move(state, direction).state

    fun heuristic(state: GameState, chemistryAware: Boolean): Int {
        val empty = state.cells.count { it == null }
        val maxValue = state.cells.filterNotNull().maxOfOrNull { FusionRules.gameValueOf(it) } ?: 0
        val cornerBonus = listOf(0, state.size - 1, state.cells.size - state.size, state.cells.size - 1)
            .maxOf { index -> state.cells[index]?.let { FusionRules.gameValueOf(it) } ?: 0 }
        val reactionPotential = if (chemistryAware) adjacentReactionPotential(state) else 0
        val entanglementPotential = if (chemistryAware) entangledPairPotential(state) else 0
        val statusPenalty = if (state.status == GameStatus.LOST) -100_000 else 0
        return statusPenalty + empty * 120 + maxValue * 2 + cornerBonus + reactionPotential * 60 + entanglementPotential * 30 + state.score.toInt()
    }

    private fun adjacentReactionPotential(state: GameState): Int {
        var potential = 0
        for (row in 0 until state.size) for (column in 0 until state.size) {
            val tile = state[row, column] ?: continue
            if (column + 1 < state.size && isParticlePair(tile, state[row, column + 1])) potential++
            if (row + 1 < state.size && isParticlePair(tile, state[row + 1, column])) potential++
        }
        return potential
    }

    private fun isParticlePair(a: Tile, b: Tile?): Boolean =
        b != null && ((a.kind == TileKind.ELECTRON && b.kind == TileKind.PROTON) || (a.kind == TileKind.PROTON && b.kind == TileKind.ELECTRON))

    private fun entangledPairPotential(state: GameState): Int =
        state.cells
            .filterNotNull()
            .mapNotNull { it.entanglementGroupId }
            .groupingBy { it }
            .eachCount()
            .values
            .count { it >= 2 }
}

class DuelEngine(
    private val random: RandomProvider,
    private val gameEngine: GameEngine = GameEngine(random),
) {
    fun newDuel(config: DuelConfig): DuelState {
        val size = 4
        val normalized = config.copy(boardSize = size, sandboxUnlocksQuantum = true)
        return DuelState(
            playerOne = gameEngine.newGame(normalized.difficulty, size),
            playerTwo = gameEngine.newGame(normalized.difficulty, size),
            config = normalized,
        )
    }

    fun move(state: DuelState, direction: Direction): Pair<DuelState, MoveResult> {
        if (state.winner != null) return state to MoveResult(state.activeBoard, false)
        val board = state.activeBoard
        val result = gameEngine.move(board, direction)
        if (!result.changed) return state to result
        val updated = if (state.currentPlayer == DuelPlayer.PLAYER_ONE) {
            state.copy(playerOne = result.state)
        } else {
            state.copy(playerTwo = result.state)
        }
        return advanceTurn(updated) to result
    }

    fun botMoveIfNeeded(state: DuelState): Pair<DuelState, MoveResult?> {
        if (state.config.opponent != DuelOpponent.BOT || state.currentPlayer != DuelPlayer.PLAYER_TWO || state.winner != null) {
            return state to null
        }
        val bot = BotScoring.botFor(state.config.botDifficulty, random)
        val direction = bot.chooseMove(state.playerTwo) ?: return state.copy(winner = DuelPlayer.PLAYER_ONE) to null
        val moved = move(state, direction)
        return moved
    }

    fun passTimedOutTurn(state: DuelState): DuelState {
        val valid = BotScoring.validMoves(state.activeBoard)
        if (valid.isEmpty()) {
            return state.copy(winner = opponentOf(state.currentPlayer))
        }
        return advanceTurn(state)
    }

    private fun advanceTurn(state: DuelState): DuelState {
        val activeLost = state.activeBoard.status == GameStatus.LOST || BotScoring.validMoves(state.activeBoard).isEmpty()
        if (activeLost) return state.copy(winner = opponentOf(state.currentPlayer))
        return state.copy(
            currentPlayer = opponentOf(state.currentPlayer),
            turnNumber = state.turnNumber + 1,
        )
    }

    private fun opponentOf(player: DuelPlayer): DuelPlayer = when (player) {
        DuelPlayer.PLAYER_ONE -> DuelPlayer.PLAYER_TWO
        DuelPlayer.PLAYER_TWO -> DuelPlayer.PLAYER_ONE
    }
}
