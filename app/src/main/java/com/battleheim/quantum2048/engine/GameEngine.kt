package com.battleheim.quantum2048.engine

class GameEngine(
    private val random: RandomProvider,
    private val entanglementEnabled: Boolean = FeatureFlags.ENTANGLEMENT_ENABLED,
) {
    fun newGame(mode: GameMode = GameMode.CLASSIC, size: Int = 4): GameState =
        newGame(Difficulty.fromMode(mode), size)

    fun newGame(difficulty: Difficulty, size: Int = 4): GameState {
        if (difficulty == Difficulty.DAILY) return newDailyChallenge(DailyChallengeSeedProvider.todayUtc(), size)
        if (difficulty == Difficulty.PUZZLE) return puzzleGame(size)
        var state = GameState(
            size = size,
            cells = List(size * size) { null },
            mode = difficulty.mode,
            difficulty = difficulty,
            energy = FusionRules.initialEnergyFor(difficulty),
        )
        repeat(FusionRules.spawnCount(size)) { state = spawn(state) }
        return state
    }

    fun newDailyChallenge(date: java.time.LocalDate, size: Int = 4): GameState =
        newDailyChallenge(date.toString(), size)

    fun newDailyChallenge(date: String, size: Int = 4): GameState {
        val dailyEngine = GameEngine(SeededRandomProvider(DailyChallengeSeedProvider.seedForDate(date)))
        var state = GameState(
            size = size,
            cells = List(size * size) { null },
            mode = GameMode.QUANTUM,
            difficulty = Difficulty.DAILY,
            dailyChallengeDate = date,
            energy = FusionRules.initialEnergyFor(Difficulty.DAILY),
        )
        repeat(FusionRules.spawnCount(size)) { state = dailyEngine.spawn(state) }
        return state
    }

    fun move(state: GameState, direction: Direction): MoveResult {
        if (state.status != GameStatus.PLAYING) return MoveResult(state, false)
        val normalizedState = clearSynthesisHighlights(state)
        val output = MutableList<Tile?>(normalizedState.cells.size) { null }
        var gainedScore = 0
        var mergeCount = 0
        var reactionCount = 0
        var entanglementCollapseCount = 0
        var nextId = state.nextTileId
        val animations = mutableListOf<MoveAnimation>()
        val priorIds = normalizedState.cells.mapNotNull { it?.id }.toSet()
        val consumedEntangledTileIds = mutableSetOf<Long>()

        for (line in 0 until normalizedState.size) {
            val tiles = (0 until normalizedState.size).mapNotNull { p ->
                val sourceIndex = index(normalizedState.size, direction, line, p)
                normalizedState.cells[sourceIndex]?.let { IndexedTile(it, sourceIndex) }
            }
            val merged = mutableListOf<IndexedTile>()
            var i = 0
            while (i < tiles.size && merged.size < normalizedState.size) {
                val first = tiles[i]
                val second = tiles.getOrNull(i + 1)
                val fusion = second?.let { mergeProduct(first.tile, it.tile, normalizedState.difficulty) }
                if (fusion != null && merged.size + fusion.tiles.size <= normalizedState.size) {
                    val animationKind = if (fusion.isReaction) MoveAnimationKind.REACTION else MoveAnimationKind.MERGE
                    val products = fusion.tiles.map { it.copy(id = nextId++, entanglementGroupId = null) }
                    products.forEach { merged += IndexedTile(it, first.sourceIndex, animationKind) }
                    if (entanglementEnabled) {
                        consumedEntangledTileIds += listOf(first.tile, second.tile)
                            .filter { it.entanglementGroupId != null }
                            .map { it.id }
                    }
                    gainedScore += fusion.score
                    mergeCount++
                    if (fusion.isReaction) reactionCount++
                    i += 2
                } else {
                    merged += first
                    i++
                }
            }
            merged.forEachIndexed { p, indexed ->
                val to = index(normalizedState.size, direction, line, p)
                output[to] = indexed.tile
                val kind = when {
                    indexed.kind != null -> indexed.kind
                    indexed.tile.id !in priorIds -> MoveAnimationKind.MERGE
                    else -> MoveAnimationKind.SLIDE
                }
                animations += MoveAnimation(indexed.tile.id, indexed.sourceIndex, to, kind)
            }
        }

        val changed = normalizedState.cells != output
        if (!changed) return MoveResult(normalizedState.copy(status = evaluate(normalizedState)), false)
        val invalidatedPairs = if (entanglementEnabled) {
            normalizedState.entangledPairs.filter { pair ->
                pair.firstTileId in consumedEntangledTileIds || pair.secondTileId in consumedEntangledTileIds
            }
        } else {
            emptyList()
        }
        val nextPairs = if (entanglementEnabled) {
            normalizedState.entangledPairs - invalidatedPairs.toSet()
        } else {
            normalizedState.entangledPairs
        }
        val clearedEntanglementTileIds = invalidatedPairs.flatMap { listOf(it.firstTileId, it.secondTileId) }.toSet()
        val nextOutput = if (entanglementEnabled && clearedEntanglementTileIds.isNotEmpty()) {
            output.map { tile ->
                if (tile?.id in clearedEntanglementTileIds) tile?.copy(entanglementGroupId = null) else tile
            }
        } else {
            output
        }

        val gainedEnergy = if (normalizedState.mode == GameMode.QUANTUM) FusionRules.energyGainForMergeCount(mergeCount) else 0
        val overflowBonus = if (normalizedState.mode == GameMode.QUANTUM) FusionRules.overflowScoreBonus(normalizedState.energy, gainedEnergy, normalizedState.difficulty) else 0
        val nextEnergy = if (normalizedState.mode == GameMode.QUANTUM) minOf(FusionRules.maxEnergyFor(normalizedState.difficulty), normalizedState.energy + gainedEnergy) else normalizedState.energy
        val nextScore = normalizedState.score + gainedScore + overflowBonus
        var next = normalizedState.copy(
            cells = nextOutput,
            score = nextScore,
            bestScore = maxOf(normalizedState.bestScore, nextScore),
            dailyBestScore = if (normalizedState.difficulty == Difficulty.DAILY) maxOf(normalizedState.dailyBestScore, nextScore) else normalizedState.dailyBestScore,
            moveCount = normalizedState.moveCount + 1,
            nextTileId = nextId,
            energy = nextEnergy,
            totalChainMergeCount = normalizedState.totalChainMergeCount + maxOf(0, mergeCount - 1),
            entangledPairs = nextPairs,
        )
        val beforeSpawn = next
        if (next.difficulty != Difficulty.PUZZLE) {
            next = spawn(next)
        }
        val spawnedIndex = next.cells.indices.firstOrNull { beforeSpawn.cells[it] == null && next.cells[it] != null }
        val spawnedTile = spawnedIndex?.let { next.cells[it] }
        if (spawnedIndex != null && spawnedTile != null) {
            animations += MoveAnimation(spawnedTile.id, spawnedIndex, spawnedIndex, MoveAnimationKind.SPAWN)
        }
        val scan = scanBoard(next)
        val scoredState = scan.state
        val totalGainedScore = gainedScore + (scan.synthesizedCompound?.scoreValue ?: 0)
        next = finalizeState(scoredState.copy(status = evaluate(scoredState)), normalizedState.status)
        return MoveResult(next, true, totalGainedScore, mergeCount, reactionCount, entanglementCollapseCount, overflowBonus, scan.synthesizedCompound, animations)
    }

    fun scanBoard(state: GameState): BoardScanResult {
        if (state.mode != GameMode.QUANTUM || state.status != GameStatus.PLAYING) {
            return BoardScanResult(clearSynthesisHighlights(state))
        }
        val match = findSynthesisMatch(state) ?: return BoardScanResult(clearSynthesisHighlights(state))
        return when (state.difficulty) {
            Difficulty.MEDIUM -> {
                val consumed = match.tileIndices.toSet()
                val cells = state.cells.mapIndexed { index, tile ->
                    if (index in consumed) null else tile?.copy(isHighlightedForSynthesis = false)
                }
                val score = state.score + match.recipe.output.scoreValue
                BoardScanResult(
                    state = state.copy(
                        cells = cells,
                        score = score,
                        bestScore = maxOf(state.bestScore, score),
                        dailyBestScore = if (state.difficulty == Difficulty.DAILY) maxOf(state.dailyBestScore, score) else state.dailyBestScore,
                    ),
                    highlightedTileIds = match.tileIds,
                    synthesizedCompound = match.recipe.output,
                )
            }
            Difficulty.HARD -> {
                val highlighted = match.tileIds.toSet()
                BoardScanResult(
                    state = state.copy(
                        cells = state.cells.map { tile ->
                            tile?.copy(isHighlightedForSynthesis = tile.id in highlighted)
                        },
                    ),
                    highlightedTileIds = match.tileIds,
                )
            }
            else -> BoardScanResult(clearSynthesisHighlights(state))
        }
    }

    fun combineCompound(state: GameState, tileIds: List<Long>): CompoundResult {
        if (state.status != GameStatus.PLAYING) return CompoundResult.Failure(state, CompoundFailure.GAME_NOT_ACTIVE)
        if (state.difficulty.mode != GameMode.QUANTUM) return CompoundResult.Failure(state, CompoundFailure.LAB_DISABLED)
        if (tileIds.distinct().size != tileIds.size || tileIds.size < 2) return CompoundResult.Failure(state, CompoundFailure.INVALID_TILE)

        val indexedTiles = tileIds.map { id ->
            val index = state.cells.indexOfFirst { it?.id == id }
            if (index < 0) return CompoundResult.Failure(state, CompoundFailure.TILE_NOT_FOUND)
            index to state.cells[index]!!
        }
        val elements = indexedTiles.map { (_, tile) ->
            val element = tile.element ?: return CompoundResult.Failure(state, CompoundFailure.INVALID_TILE)
            ElementTile(element = element, sourceTileId = tile.id)
        }
        val recipe = Chemistry.findRecipe(elements, FusionRules.compoundRecipes, CompoundRecipeLevel.QUANTUM)
            ?: return CompoundResult.Failure(state, CompoundFailure.NO_RECIPE)
        val cells = state.cells.toMutableList()
        indexedTiles.forEach { (index, _) -> cells[index] = null }
        val nextScore = state.score + recipe.output.scoreValue
        val next = finalizeState(
            state.copy(
                cells = cells,
                score = nextScore,
                bestScore = maxOf(state.bestScore, nextScore),
                status = evaluate(state.copy(cells = cells, score = nextScore, bestScore = maxOf(state.bestScore, nextScore))),
            ), state.status,
        )
        return CompoundResult.Success(
            state = next,
            recipe = recipe,
        )
    }

    fun tunnel(state: GameState, tileId: Long, destinationIndex: Int): TunnelResult {
        if (state.status != GameStatus.PLAYING) return TunnelResult.Failure(state, TunnelFailure.GAME_NOT_ACTIVE)
        if (state.difficulty.mode != GameMode.QUANTUM) return TunnelResult.Failure(state, TunnelFailure.LAB_DISABLED)
        if (destinationIndex !in state.cells.indices || state.cells[destinationIndex] != null) {
            return TunnelResult.Failure(state, TunnelFailure.DESTINATION_OCCUPIED)
        }
        if (state.energy < FusionRules.tunnelingEnergyCost) return TunnelResult.Failure(state, TunnelFailure.INSUFFICIENT_SCORE)
        val sourceIndex = state.cells.indexOfFirst { it?.id == tileId }
        if (sourceIndex < 0) return TunnelResult.Failure(state, TunnelFailure.TILE_NOT_FOUND)
        if (sourceIndex == destinationIndex) return TunnelResult.Failure(state, TunnelFailure.DESTINATION_OCCUPIED)

        val cells = state.cells.toMutableList()
        val tile = cells[sourceIndex]!!
        cells[sourceIndex] = null
        cells[destinationIndex] = tile
        val next = finalizeState(state.copy(
            cells = cells,
            energy = state.energy - FusionRules.tunnelingEnergyCost,
            status = evaluate(state.copy(cells = cells)),
        ), state.status)
        return TunnelResult.Success(
            state = next,
            animation = MoveAnimation(tile.id, sourceIndex, destinationIndex, MoveAnimationKind.TUNNEL),
        )
    }

    fun collapseSuperposition(state: GameState, tileId: Long, choiceIndex: Int): SuperpositionResult {
        if (state.status != GameStatus.PLAYING) return SuperpositionResult.Failure(state, SuperpositionFailure.GAME_NOT_ACTIVE)
        if (state.difficulty.mode != GameMode.QUANTUM) return SuperpositionResult.Failure(state, SuperpositionFailure.LAB_DISABLED)
        val sourceIndex = state.cells.indexOfFirst { it?.id == tileId }
        if (sourceIndex < 0) return SuperpositionResult.Failure(state, SuperpositionFailure.TILE_NOT_FOUND)
        val tile = state.cells[sourceIndex] ?: return SuperpositionResult.Failure(state, SuperpositionFailure.TILE_NOT_FOUND)
        if (tile.superpositionValues.isEmpty()) return SuperpositionResult.Failure(state, SuperpositionFailure.NOT_SUPERPOSITION)
        if (choiceIndex !in tile.superpositionValues.indices || choiceIndex !in FusionRules.superpositionCollapseEnergyCosts.indices) {
            return SuperpositionResult.Failure(state, SuperpositionFailure.INVALID_CHOICE)
        }
        val cost = FusionRules.superpositionCollapseEnergyCosts[choiceIndex] + entangledPartnerCost(state, tile, choiceIndex)
        if (state.energy < cost) return SuperpositionResult.Failure(state, SuperpositionFailure.INSUFFICIENT_SCORE)

        val resolved = tile.copy(value = tile.superpositionValues[choiceIndex], superpositionValues = emptyList())
        val cells = state.cells.toMutableList()
        cells[sourceIndex] = resolved
        val collapse = if (entanglementEnabled) {
            collapseEntangledPartner(state, cells, tile, choiceIndex)
        } else {
            EntangledCollapseUpdate(cells, state.entangledPairs, 0, null)
        }
        val next = finalizeState(state.copy(
            cells = collapse.cells,
            energy = state.energy - cost,
            successfulCollapseCount = state.successfulCollapseCount + 1 + collapse.extraCollapseCount,
            lowCollapseCount = state.lowCollapseCount + if (choiceIndex == 0) 1 else 0,
            highCollapseCount = state.highCollapseCount + if (choiceIndex == 0) 0 else 1,
            status = evaluate(state.copy(cells = collapse.cells)),
            entangledPairs = collapse.pairs,
        ), state.status)
        return SuperpositionResult.Success(
            state = next,
            animation = MoveAnimation(tile.id, sourceIndex, sourceIndex, if (choiceIndex == 0) MoveAnimationKind.COLLAPSE_LOW else MoveAnimationKind.COLLAPSE_HIGH),
            entanglementCollapseCount = collapse.extraCollapseCount,
        )
    }

    fun observeSuperposition(state: GameState, tileId: Long): ObserverResult {
        if (state.status != GameStatus.PLAYING) return ObserverResult.Failure(state, ObserverFailure.GAME_NOT_ACTIVE)
        if (state.difficulty.mode != GameMode.QUANTUM) return ObserverResult.Failure(state, ObserverFailure.LAB_DISABLED)
        val sourceIndex = state.cells.indexOfFirst { it?.id == tileId }
        if (sourceIndex < 0) return ObserverResult.Failure(state, ObserverFailure.TILE_NOT_FOUND)
        val tile = state.cells[sourceIndex] ?: return ObserverResult.Failure(state, ObserverFailure.TILE_NOT_FOUND)
        if (tile.superpositionValues.isEmpty()) return ObserverResult.Failure(state, ObserverFailure.NOT_SUPERPOSITION)
        if (state.energy < FusionRules.observerPreviewEnergyCost) return ObserverResult.Failure(state, ObserverFailure.INSUFFICIENT_SCORE)

        return ObserverResult.Success(
            state = state.copy(energy = state.energy - FusionRules.observerPreviewEnergyCost),
            previewValue = tile.superpositionValues[tile.superpositionValues.size / 2],
        )
    }

    fun continueAfterWin(state: GameState) = state.copy(status = GameStatus.PLAYING, hasAcknowledgedWin = true)

    fun spawn(state: GameState): GameState {
        val empty = state.cells.indices.filter { state.cells[it] == null }
        if (empty.isEmpty()) return state
        val at = empty[random.nextInt(empty.size)]
        val cells = state.cells.toMutableList()
        cells[at] = if (state.mode == GameMode.QUANTUM) {
            val baseTile = FusionRules.quantumSpawnTile(state, random.nextDouble())
            val values = if (FusionRules.canSpawnSuperposition(state) && random.nextDouble() < FusionRules.superpositionSpawnChance) {
                FusionRules.superpositionValuesFor(1)
            } else {
                emptyList()
            }
            if (baseTile.kind == TileKind.ELEMENT) baseTile else baseTile.copy(superpositionValues = values)
        } else {
            Tile(state.nextTileId, if (random.nextDouble() < 0.9) 2 else 4)
        }
        val entangled = if (entanglementEnabled && state.mode == GameMode.QUANTUM) {
            maybeEntangle(cells, state, at, state.nextTileId + 1)
        } else {
            EntanglementSpawnUpdate(cells, state.entangledPairs)
        }
        return state.copy(cells = entangled.cells, nextTileId = state.nextTileId + 1, entangledPairs = entangled.pairs)
    }

    private fun maybeEntangle(cells: MutableList<Tile?>, state: GameState, spawnedIndex: Int, groupId: Long): EntanglementSpawnUpdate {
        val spawned = cells[spawnedIndex] ?: return EntanglementSpawnUpdate(cells, state.entangledPairs)
        if (!FusionRules.canEntangle(spawned) || random.nextDouble() >= QuantumBalance.entangledSpawnChance) {
            return EntanglementSpawnUpdate(cells, state.entangledPairs)
        }
        val candidates = adjacentIndices(state.size, spawnedIndex)
            .mapNotNull { index -> cells[index]?.takeIf(FusionRules::canEntangle)?.let { index to it } }
        if (candidates.isEmpty()) return EntanglementSpawnUpdate(cells, state.entangledPairs)
        val (partnerIndex, partner) = candidates[random.nextInt(candidates.size)]
        cells[spawnedIndex] = spawned.copy(entanglementGroupId = groupId)
        cells[partnerIndex] = partner.copy(entanglementGroupId = groupId)
        val pair = EntangledPair(
            id = groupId,
            firstTileId = spawned.id,
            secondTileId = partner.id,
            relation = nextEntanglementRelation(),
        )
        return EntanglementSpawnUpdate(cells, state.entangledPairs + pair)
    }

    private fun nextEntanglementRelation(): EntanglementRelation =
        if (random.nextDouble() < QuantumBalance.inverseEntanglementRelationChance) {
            EntanglementRelation.INVERSE_CHOICE
        } else {
            QuantumBalance.defaultEntanglementRelation
        }

    private fun entangledPartnerCost(state: GameState, tile: Tile, choiceIndex: Int): Int {
        if (!entanglementEnabled || QuantumBalance.entanglementCollapseEnergyPolicy == EntanglementEnergyPolicy.SINGLE_COST) return 0
        val pair = state.entangledPairs.firstOrNull { it.firstTileId == tile.id || it.secondTileId == tile.id } ?: return 0
        val partner = partnerTile(state, pair, tile.id) ?: return 0
        if (partner.superpositionValues.isEmpty()) return 0
        val partnerChoice = pair.partnerChoiceIndex(choiceIndex, partner.superpositionValues.lastIndex)
        return FusionRules.superpositionCollapseEnergyCosts.getOrNull(partnerChoice) ?: 0
    }

    private fun collapseEntangledPartner(
        state: GameState,
        cells: MutableList<Tile?>,
        tile: Tile,
        choiceIndex: Int,
    ): EntangledCollapseUpdate {
        val pair = state.entangledPairs.firstOrNull { it.firstTileId == tile.id || it.secondTileId == tile.id }
            ?: return EntangledCollapseUpdate(cells, state.entangledPairs, 0, null)
        val partnerIndex = cells.indexOfFirst { it?.id == pair.partnerId(tile.id) }
        val partner = cells.getOrNull(partnerIndex) ?: return EntangledCollapseUpdate(cells, state.entangledPairs - pair, 0, null)
        if (partner.superpositionValues.isEmpty()) return EntangledCollapseUpdate(cells, state.entangledPairs - pair, 0, null)
        val partnerChoice = pair.partnerChoiceIndex(choiceIndex, partner.superpositionValues.lastIndex)
        cells[partnerIndex] = partner.copy(
            value = partner.superpositionValues[partnerChoice],
            superpositionValues = emptyList(),
            entanglementGroupId = null,
        )
        val sourceIndex = cells.indexOfFirst { it?.id == tile.id }
        if (sourceIndex >= 0) {
            cells[sourceIndex] = cells[sourceIndex]?.copy(entanglementGroupId = null)
        }
        return EntangledCollapseUpdate(cells, state.entangledPairs - pair, 1, partner.id)
    }

    private fun partnerTile(state: GameState, pair: EntangledPair, sourceTileId: Long): Tile? =
        state.cells.firstOrNull { it?.id == pair.partnerId(sourceTileId) }

    private fun EntangledPair.partnerId(sourceTileId: Long): Long =
        if (sourceTileId == firstTileId) secondTileId else firstTileId

    private fun EntangledPair.partnerChoiceIndex(sourceChoiceIndex: Int, partnerLastIndex: Int): Int =
        when (relation) {
            EntanglementRelation.SAME_CHOICE -> sourceChoiceIndex.coerceIn(0, partnerLastIndex)
            EntanglementRelation.INVERSE_CHOICE -> (partnerLastIndex - sourceChoiceIndex).coerceIn(0, partnerLastIndex)
        }

    private fun adjacentIndices(size: Int, index: Int): List<Int> {
        val row = index / size
        val column = index % size
        return buildList {
            if (row > 0) add((row - 1) * size + column)
            if (row + 1 < size) add((row + 1) * size + column)
            if (column > 0) add(row * size + column - 1)
            if (column + 1 < size) add(row * size + column + 1)
        }
    }

    private fun mergeProduct(a: Tile, b: Tile, difficulty: Difficulty): FusionProduct? {
        if (difficulty.mode == GameMode.CLASSIC) {
            return if (a.kind == TileKind.CLASSIC && b.kind == TileKind.CLASSIC && a.value == b.value) {
                FusionProduct(listOf(Tile(0, a.value * 2)), a.value * 2, false)
            } else null
        }
        return FusionRules.mergeProduct(a, b)
    }

    private fun clearSynthesisHighlights(state: GameState): GameState =
        if (state.cells.none { it?.isHighlightedForSynthesis == true }) {
            state
        } else {
            state.copy(cells = state.cells.map { tile -> tile?.copy(isHighlightedForSynthesis = false) })
        }

    private fun findSynthesisMatch(state: GameState): SynthesisMatch? {
        val indexedElements = state.cells.mapIndexedNotNull { index, tile ->
            val element = tile?.element ?: return@mapIndexedNotNull null
            IndexedElement(index, tile.id, element.atomicNumber)
        }
        if (indexedElements.size < 2) return null
        return FusionRules.compoundRecipes
            .filter { it.unlockLevel.ordinal <= CompoundRecipeLevel.HARD.ordinal }
            .asSequence()
            .mapNotNull { recipe -> matchRecipe(indexedElements, recipe) }
            .firstOrNull()
    }

    private fun matchRecipe(elements: List<IndexedElement>, recipe: CompoundRecipe): SynthesisMatch? {
        val available = elements.groupBy { it.atomicNumber }.mapValues { (_, value) -> value.toMutableList() }
        val indices = mutableListOf<Int>()
        val ids = mutableListOf<Long>()
        recipe.atomicNumbers.sorted().forEach { atomicNumber ->
            val tile = available[atomicNumber]?.removeFirstOrNull() ?: return null
            indices += tile.index
            ids += tile.tileId
        }
        return SynthesisMatch(recipe, indices, ids)
    }

    private fun evaluate(state: GameState): GameStatus {
        if (FusionRules.isPuzzleSolved(state)) return GameStatus.WON
        if (FusionRules.isPuzzleFailed(state)) return GameStatus.LOST
        if (!state.hasAcknowledgedWin && state.cells.any { it != null && FusionRules.gameValueOf(it) >= 2048 }) return GameStatus.WON
        if (state.cells.any { it == null }) return GameStatus.PLAYING
        for (r in 0 until state.size) for (c in 0 until state.size) {
            val tile = state.cells[r * state.size + c] ?: continue
            if (c + 1 < state.size && mergeProduct(tile, state.cells[r * state.size + c + 1]!!, state.difficulty) != null) return GameStatus.PLAYING
            if (r + 1 < state.size && mergeProduct(tile, state.cells[(r + 1) * state.size + c]!!, state.difficulty) != null) return GameStatus.PLAYING
        }
        return GameStatus.LOST
    }

    private fun finalizeState(state: GameState, previousStatus: GameStatus): GameState {
        val withWinEnergy = if (previousStatus != GameStatus.WON && state.status == GameStatus.WON) {
            state.copy(totalWinEnergy = state.totalWinEnergy + state.energy, winEnergySamples = state.winEnergySamples + 1)
        } else {
            state
        }
        return withWinEnergy.copy(unlockedAchievements = FusionRules.unlockedAchievementsFor(withWinEnergy))
    }

    private fun index(size: Int, d: Direction, line: Int, p: Int) = when (d) {
        Direction.LEFT -> line * size + p
        Direction.RIGHT -> line * size + (size - 1 - p)
        Direction.UP -> p * size + line
        Direction.DOWN -> (size - 1 - p) * size + line
    }

    fun puzzleGame(size: Int, puzzleIndex: Int = random.nextInt(FusionRules.puzzleDefinitions.size)): GameState {
        val cells = MutableList<Tile?>(size * size) { null }
        val puzzle = FusionRules.puzzleDefinitions[puzzleIndex.mod(FusionRules.puzzleDefinitions.size)]
        var nextId = 1L
        puzzle.tiles.forEach { tile ->
            if (tile.index in cells.indices) {
                cells[tile.index] = Tile(nextId++, tile.value, tile.kind)
            }
        }
        return GameState(
            size = size,
            cells = cells,
            mode = GameMode.QUANTUM,
            difficulty = Difficulty.PUZZLE,
            nextTileId = nextId,
            energy = FusionRules.initialEnergyFor(Difficulty.PUZZLE),
        )
    }

    private data class IndexedTile(val tile: Tile, val sourceIndex: Int, val kind: MoveAnimationKind? = null)
    private data class IndexedElement(val index: Int, val tileId: Long, val atomicNumber: Int)
    private data class SynthesisMatch(val recipe: CompoundRecipe, val tileIndices: List<Int>, val tileIds: List<Long>)
    private data class EntanglementSpawnUpdate(val cells: List<Tile?>, val pairs: List<EntangledPair>)
    private data class EntangledCollapseUpdate(
        val cells: List<Tile?>,
        val pairs: List<EntangledPair>,
        val extraCollapseCount: Int,
        val partnerTileId: Long?,
    )
}

data class BoardScanResult(
    val state: GameState,
    val highlightedTileIds: List<Long> = emptyList(),
    val synthesizedCompound: Compound? = null,
)
