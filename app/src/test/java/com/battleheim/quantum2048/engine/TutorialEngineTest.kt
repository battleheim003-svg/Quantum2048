package com.battleheim.quantum2048.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TutorialEngineTest {
    @Test
    fun tappingSuperpositionTileCompletesFirstStep() {
        val start = TutorialEngine.start()
        val tileId = start.board.cells.filterNotNull().single().id

        val selected = TutorialEngine.selectTile(start, tileId)

        assertEquals(tileId, selected.selectedTileId)
        assertTrue(selected.isCurrentStepComplete)
    }

    @Test
    fun manualCollapseUsesRealCollapseCostAndResolvesTile() {
        val start = TutorialEngine.next(TutorialEngine.start())
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
        val mergeStep = TutorialLessonState(step = TutorialStep.MERGE_ENERGY, board = TutorialEngine.sampleBoard(TutorialStep.MERGE_ENERGY))

        val merged = TutorialEngine.merge(mergeStep, Direction.LEFT)

        assertTrue(merged.board.energy > mergeStep.board.energy)
        assertTrue(merged.isCurrentStepComplete)
    }
}
