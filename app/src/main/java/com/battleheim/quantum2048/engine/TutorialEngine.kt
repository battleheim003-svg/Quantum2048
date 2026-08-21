package com.battleheim.quantum2048.engine

data class TutorialLessonState(
    val step: TutorialStep = TutorialStep.SWIPE,
    val board: GameState = TutorialEngine.sampleBoard(TutorialStep.SWIPE),
    val selectedTileId: Long? = null,
    val collapseChoiceIndex: Int? = null,
    val mergeDirection: Direction? = null,
    val completedSteps: Set<TutorialStep> = emptySet(),
) {
    val isCurrentStepComplete: Boolean get() = step in completedSteps
}

enum class TutorialStep {
    SWIPE,
    FUSION_MERGE,
    COLLAPSE,
}

object TutorialEngine {
    fun start(): TutorialLessonState = TutorialLessonState()

    fun selectTile(state: TutorialLessonState, tileId: Long): TutorialLessonState {
        val tile = state.board.cells.firstOrNull { it?.id == tileId } ?: return state
        if (state.step != TutorialStep.COLLAPSE || tile.superpositionValues.isEmpty()) return state
        return state.copy(
            selectedTileId = tileId,
        )
    }

    fun collapseSelected(state: TutorialLessonState, choiceIndex: Int): TutorialLessonState {
        if (state.step != TutorialStep.COLLAPSE) return state
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
        if (state.step != TutorialStep.SWIPE && state.step != TutorialStep.FUSION_MERGE) return state
        val result = GameEngine(SeededRandomProvider(2048)).move(state.board, direction)
        if (!result.changed) return state
        if (state.step == TutorialStep.FUSION_MERGE && result.mergeCount == 0) return state
        return state.copy(
            board = result.state,
            mergeDirection = direction,
            completedSteps = state.completedSteps + state.step,
        )
    }

    fun skip(state: TutorialLessonState): TutorialLessonState =
        state.copy(completedSteps = state.completedSteps + state.step)

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
            TutorialStep.SWIPE -> {
                cells[1] = Tile(1, 2, TileKind.CLASSIC)
                GameState(
                    cells = cells,
                    mode = GameMode.CLASSIC,
                    difficulty = Difficulty.EASY,
                    nextTileId = 2,
                )
            }
            TutorialStep.FUSION_MERGE -> {
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
            TutorialStep.COLLAPSE -> {
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
        }
    }
}
