package com.battleheim.quantum2048.domain

import com.battleheim.quantum2048.engine.CompoundRecipe
import com.battleheim.quantum2048.engine.Compound
import com.battleheim.quantum2048.engine.Difficulty
import com.battleheim.quantum2048.engine.FusionRules
import com.battleheim.quantum2048.engine.QuantumElement
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Serializable
data class CollectionEntry(
    val compoundSymbol: String,
    val englishName: String,
    val persianName: String,
    val firstDiscoveredAtMillis: Long,
    val lastDiscoveredAtMillis: Long,
    val firstDifficulty: Difficulty,
    val lastDifficulty: Difficulty,
    val discoveryCount: Int = 1,
) {
    init {
        require(compoundSymbol.isNotBlank())
        require(englishName.isNotBlank())
        require(persianName.isNotBlank())
        require(firstDiscoveredAtMillis > 0)
        require(lastDiscoveredAtMillis >= firstDiscoveredAtMillis)
        require(discoveryCount > 0)
    }
}

@Serializable
data class CollectionState(
    val entries: List<CollectionEntry> = emptyList(),
    val unlockedElements: Set<QuantumElement> = emptySet(),
    val elementEntries: List<ElementDiscoveryEntry> = emptyList(),
) {
    fun record(compound: Compound, difficulty: Difficulty, discoveredAtMillis: Long): CollectionState {
        require(discoveredAtMillis > 0)
        val existing = entries.firstOrNull { it.compoundSymbol == compound.symbol }
        val updated = if (existing == null) {
            CollectionEntry(
                compoundSymbol = compound.symbol,
                englishName = compound.englishName,
                persianName = compound.persianName,
                firstDiscoveredAtMillis = discoveredAtMillis,
                lastDiscoveredAtMillis = discoveredAtMillis,
                firstDifficulty = difficulty,
                lastDifficulty = difficulty,
            )
        } else {
            existing.copy(
                englishName = compound.englishName,
                persianName = compound.persianName,
                lastDiscoveredAtMillis = discoveredAtMillis,
                lastDifficulty = difficulty,
                discoveryCount = existing.discoveryCount + 1,
            )
        }
        return copy(entries = (entries.filterNot { it.compoundSymbol == compound.symbol } + updated).sortedBy { it.compoundSymbol })
    }

    fun unrecord(compoundSymbol: String): CollectionState {
        val existing = entries.firstOrNull { it.compoundSymbol == compoundSymbol } ?: return this
        return if (existing.discoveryCount <= 1) {
            copy(entries = entries.filterNot { it.compoundSymbol == compoundSymbol })
        } else {
            copy(
                entries = (entries.filterNot { it.compoundSymbol == compoundSymbol } + existing.copy(
                    discoveryCount = existing.discoveryCount - 1,
                )).sortedBy { it.compoundSymbol },
            )
        }
    }

    fun recordElement(element: QuantumElement, discoveredAtMillis: Long = System.currentTimeMillis()): CollectionState {
        val normalizedEntries = normalizedElementEntries()
        if (normalizedEntries.any { it.element == element }) {
            return copy(unlockedElements = unlockedElements + element, elementEntries = normalizedEntries)
        }
        return copy(
            unlockedElements = unlockedElements + element,
            elementEntries = normalizedEntries + ElementDiscoveryEntry(
                element = element,
                firstDiscoveredAtMillis = discoveredAtMillis,
                discoveryOrder = normalizedEntries.size + 1,
            ),
        )
    }

    fun codex(recipes: List<CompoundRecipe>): List<CodexEntry> {
        val discovered = entries.associateBy { it.compoundSymbol }
        return recipes
            .distinctBy { it.output.symbol }
            .sortedBy { it.output.symbol }
            .map { recipe ->
                val entry = discovered[recipe.output.symbol]
                CodexEntry(
                    symbol = recipe.output.symbol,
                    englishName = entry?.englishName,
                    persianName = entry?.persianName,
                    discovered = entry != null,
                    discoveryCount = entry?.discoveryCount ?: 0,
                    firstDifficulty = entry?.firstDifficulty,
                )
            }
    }
}

@Serializable
data class ElementDiscoveryEntry(
    val element: QuantumElement,
    val firstDiscoveredAtMillis: Long,
    val discoveryOrder: Int,
)

data class ElementCodexEntry(
    val element: QuantumElement,
    val discovered: Boolean,
    val discoveryOrder: Int? = null,
    val firstDiscoveredAtMillis: Long? = null,
)

val LabCodexElementChain: List<QuantumElement> = FusionRules.elementChain

fun CollectionState.elementCodex(): List<ElementCodexEntry> {
    val discoveries = normalizedElementEntries().associateBy { it.element }
    return LabCodexElementChain.map { element ->
        val discovery = discoveries[element]
        ElementCodexEntry(
            element = element,
            discovered = discovery != null || element in unlockedElements,
            discoveryOrder = discovery?.discoveryOrder,
            firstDiscoveredAtMillis = discovery?.firstDiscoveredAtMillis,
        )
    }
}

private fun CollectionState.normalizedElementEntries(): List<ElementDiscoveryEntry> {
    if (elementEntries.isNotEmpty()) return elementEntries.sortedBy { it.discoveryOrder }
    return unlockedElements
        .sortedBy { LabCodexElementChain.indexOf(it).let { index -> if (index < 0) Int.MAX_VALUE else index } }
        .mapIndexed { index, element -> ElementDiscoveryEntry(element, 1L, index + 1) }
}

data class CodexEntry(
    val symbol: String,
    val englishName: String?,
    val persianName: String?,
    val discovered: Boolean,
    val discoveryCount: Int,
    val firstDifficulty: Difficulty?,
)

interface CollectionRepository {
    fun observe(): Flow<CollectionState>
    suspend fun record(compound: Compound, difficulty: Difficulty, discoveredAtMillis: Long = System.currentTimeMillis())
    suspend fun recordElement(element: QuantumElement)
    suspend fun unrecord(compoundSymbol: String)
    suspend fun clear()
}
