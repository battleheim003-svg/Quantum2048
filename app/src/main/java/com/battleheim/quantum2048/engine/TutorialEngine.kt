package com.battleheim.quantum2048.engine

data class TutorialLessonState(
    val step: TutorialStep = TutorialStep.SUPERPOSITION,
    val board: GameState = TutorialEngine.sampleBoard(TutorialStep.SUPERPOSITION),
    val selectedTileId: Long? = null,
    val collapseChoiceIndex: Int? = null,
    val mergeDirection: Direction? = null,
    val completedSteps: Set<TutorialStep> = emptySet(),
) {
    val isCurrentStepComplete: Boolean get() = step in completedSteps
}

enum class TutorialStep {
    SUPERPOSITION,
    MANUAL_COLLAPSE,
    ENERGY_COST,
    MERGE_ENERGY,
}

object TutorialEngine {
    fun start(): TutorialLessonState = TutorialLessonState()

    fun selectTile(state: TutorialLessonState, tileId: Long): TutorialLessonState {
        val tile = state.board.cells.firstOrNull { it?.id == tileId } ?: return state
        if (tile.superpositionValues.isEmpty()) return state
        return state.copy(
            selectedTileId = tileId,
            completedSteps = if (state.step == TutorialStep.SUPERPOSITION) {
                state.completedSteps + TutorialStep.SUPERPOSITION
            } else {
                state.completedSteps
            },
        )
    }

    fun collapseSelected(state: TutorialLessonState, choiceIndex: Int): TutorialLessonState {
        if (state.step != TutorialStep.MANUAL_COLLAPSE && state.step != TutorialStep.ENERGY_COST) return state
        val tileId = state.selectedTileId ?: return state
        val result = GameEngine(SeededRandomProvider(2048)).collapseSuperposition(state.board, tileId, choiceIndex)
        if (result !is SuperpositionResult.Success) return state
        return state.copy(
            board = result.state,
            collapseChoiceIndex = choiceIndex,
            completedSteps = state.completedSteps + state.step,
        )
    }

    fun merge(state: TutorialLessonState, direction: Direction): TutorialLessonState {
        if (state.step != TutorialStep.MERGE_ENERGY) return state
        val result = GameEngine(SeededRandomProvider(2048)).move(state.board, direction)
        if (!result.changed || result.mergeCount == 0) return state
        return state.copy(
            board = result.state,
            mergeDirection = direction,
            completedSteps = state.completedSteps + TutorialStep.MERGE_ENERGY,
        )
    }

    fun next(state: TutorialLessonState): TutorialLessonState {
        val nextStep = TutorialStep.entries.getOrNull(state.step.ordinal + 1) ?: return state
        return state.copy(
            step = nextStep,
            board = sampleBoard(nextStep),
            selectedTileId = null,
            collapseChoiceIndex = null,
            mergeDirection = null,
        )
    }

    fun sampleBoard(step: TutorialStep): GameState {
        val cells = MutableList<Tile?>(16) { null }
        return when (step) {
            TutorialStep.SUPERPOSITION,
            TutorialStep.MANUAL_COLLAPSE -> {
                cells[5] = Tile(1, 1, TileKind.ELECTRON, superpositionValues = FusionRules.superpositionValuesFor(1))
                GameState(
                    cells = cells,
                    mode = GameMode.QUANTUM,
                    difficulty = Difficulty.QUANTUM,
                    nextTileId = 2,
                    energy = FusionRules.superpositionCollapseEnergyCosts.max() + 10,
                    score = FusionRules.superpositionScoreThreshold,
                )
            }
            TutorialStep.ENERGY_COST -> {
                cells[5] = Tile(1, 1, TileKind.ELECTRON, superpositionValues = FusionRules.superpositionValuesFor(1))
                GameState(
                    cells = cells,
                    mode = GameMode.QUANTUM,
                    difficulty = Difficulty.QUANTUM,
                    nextTileId = 2,
                    energy = FusionRules.superpositionCollapseEnergyCosts.max() + 10,
                    score = FusionRules.superpositionScoreThreshold,
                )
            }
            TutorialStep.MERGE_ENERGY -> {
                cells[0] = Tile(1, 1, TileKind.ELECTRON)
                cells[1] = Tile(2, 1, TileKind.ELECTRON)
                GameState(
                    cells = cells,
                    mode = GameMode.QUANTUM,
                    difficulty = Difficulty.QUANTUM,
                    nextTileId = 3,
                    energy = 0,
                )
            }
        }
    }
}
