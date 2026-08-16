package com.battleheim.quantum2048.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TutorialEngineTest {
    @Test
    fun swipeStepUsesRealMoveAndCompletesFirstStep() {
        val start = TutorialEngine.start()

        val moved = TutorialEngine.merge(start, Direction.LEFT)

        assertEquals(2, moved.board[0, 0]?.value)
        assertTrue(moved.isCurrentStepComplete)
    }

    @Test
    fun manualCollapseUsesRealCollapseCostAndResolvesTile() {
        val start = TutorialEngine.next(TutorialEngine.next(TutorialEngine.start()))
        val tileId = start.board.cells.filterNotNull().single().id
        val selected = TutorialEngine.selectTile(start, tileId)
        val beforeEnergy = selected.board.energy

        val collapsed = TutorialEngine.collapseSelected(selected, 2)

        assertEquals(beforeEnergy - FusionRules.superpositionCollapseEnergyCosts[2], collapsed.board.energy)
        assertEquals(emptyList<Int>(), collapsed.board.cells.filterNotNull().single().superpositionValues)
        assertTrue(collapsed.isCurrentStepComplete)
    }

    @Test
    fun mergeStepUsesRealMoveAndAwardsEnergy() {
        val mergeStep = TutorialLessonState(step = TutorialStep.FUSION_MERGE, board = TutorialEngine.sampleBoard(TutorialStep.FUSION_MERGE))

        val merged = TutorialEngine.merge(mergeStep, Direction.LEFT)

        assertTrue(merged.board.energy > mergeStep.board.energy)
        assertTrue(merged.isCurrentStepComplete)
    }
}
