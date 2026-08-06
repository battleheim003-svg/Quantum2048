package com.battleheim.quantum2048.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChemistryTest {
    private val recipes = QuantumBalance.defaultCompoundRecipes

    @Test
    fun matchingRecipeReturnsCompoundRegardlessOfInputOrder() {
        val inputs = listOf(
            ElementTile(QuantumSpecies.OXYGEN),
            ElementTile(QuantumSpecies.HYDROGEN),
            ElementTile(QuantumSpecies.HYDROGEN),
        )

        val recipe = Chemistry.findRecipe(inputs, recipes)

        assertEquals("H2O", recipe?.output?.symbol)
        assertEquals("Water", recipe?.output?.englishName)
    }

    @Test
    fun unmatchedElementsReturnNull() {
        val inputs = listOf(
            ElementTile(QuantumSpecies.GOLD),
            ElementTile(QuantumSpecies.NEON),
        )

        assertNull(Chemistry.findRecipe(inputs, recipes))
    }

    @Test
    fun levelFilterHidesRecipesLockedForLaterLevels() {
        val inputs = listOf(
            ElementTile(QuantumSpecies.SILICON),
            ElementTile(QuantumSpecies.OXYGEN),
            ElementTile(QuantumSpecies.OXYGEN),
        )

        assertNull(Chemistry.findRecipe(inputs, recipes, CompoundRecipeLevel.HARD))
        assertEquals("SiO2", Chemistry.findRecipe(inputs, recipes, CompoundRecipeLevel.QUANTUM)?.output?.symbol)
    }

    @Test
    fun defaultRecipeTableContainsAtLeastTenRealCompounds() {
        assertTrue(recipes.size >= 10)
        assertTrue(recipes.all { it.output.symbol.isNotBlank() && it.inputs.size >= 2 })
    }
}
