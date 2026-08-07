package com.battleheim.quantum2048.engine

class GameEngine(
    private val random: RandomProvider,
) {
    fun newGame(mode: GameMode = GameMode.CLASSIC, size: Int = 4): GameState =
        newGame(Difficulty.fromMode(mode), size)

    fun newGame(difficulty: Difficulty, size: Int = 4): GameState {
        if (difficulty == Difficulty.DAILY) return newDailyChallenge(java.time.LocalDate.now(), size)
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

    fun newDailyChallenge(date: java.time.LocalDate, size: Int = 4): GameState {
        val dailyEngine = GameEngine(SeededRandomProvider(FusionRules.dailySeed(date)))
        var state = GameState(
            size = size,
            cells = List(size * size) { null },
            mode = GameMode.QUANTUM,
            difficulty = Difficulty.DAILY,
            dailyChallengeDate = date.toString(),
            energy = FusionRules.initialEnergyFor(Difficulty.DAILY),
        )
        repeat(FusionRules.spawnCount(size)) { state = dailyEngine.spawn(state) }
        return state
    }

    fun move(state: GameState, direction: Direction): MoveResult {
        if (state.status != GameStatus.PLAYING) return MoveResult(state, false)
        val output = MutableList<Tile?>(state.cells.size) { null }
        var gainedScore = 0
        var mergeCount = 0
        var reactionCount = 0
        var entanglementCollapseCount = 0
        var nextId = state.nextTileId
        val animations = mutableListOf<MoveAnimation>()
        val priorIds = state.cells.mapNotNull { it?.id }.toSet()
        val entanglementCollapses = mutableMapOf<Long, Tile>()

        for (line in 0 until state.size) {
            val tiles = (0 until state.size).mapNotNull { p ->
                val sourceIndex = index(state.size, direction, line, p)
                state.cells[sourceIndex]?.let { IndexedTile(it, sourceIndex) }
            }
            val merged = mutableListOf<IndexedTile>()
            var i = 0
            while (i < tiles.size && merged.size < state.size) {
                val first = tiles[i]
                val second = tiles.getOrNull(i + 1)
                val fusion = second?.let { mergeProduct(first.tile, it.tile, state.difficulty) }
                if (fusion != null && merged.size + fusion.tiles.size <= state.size) {
                    val animationKind = if (fusion.isReaction) MoveAnimationKind.REACTION else MoveAnimationKind.MERGE
                    val products = fusion.tiles.map { it.copy(id = nextId++, entanglementGroupId = null) }
                    products.forEach { merged += IndexedTile(it, first.sourceIndex, animationKind) }
                    val primaryProduct = products.first()
                    listOf(first.tile, second.tile)
                        .mapNotNull { it.entanglementGroupId }
                        .distinct()
                        .forEach { groupId -> entanglementCollapses[groupId] = primaryProduct }
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
                val to = index(state.size, direction, line, p)
                output[to] = indexed.tile
                val kind = when {
                    indexed.kind != null -> indexed.kind
                    indexed.tile.id !in priorIds -> MoveAnimationKind.MERGE
                    else -> MoveAnimationKind.SLIDE
                }
                animations += MoveAnimation(indexed.tile.id, indexed.sourceIndex, to, kind)
            }
        }

        if (entanglementCollapses.isNotEmpty()) {
            for (index in output.indices) {
                val tile = output[index] ?: continue
                val product = tile.entanglementGroupId?.let { entanglementCollapses[it] } ?: continue
                output[index] = product.copy(id = tile.id, entanglementGroupId = null)
                animations += MoveAnimation(tile.id, index, index, MoveAnimationKind.ENTANGLEMENT)
                entanglementCollapseCount++
            }
        }

        val changed = state.cells != output
        if (!changed) return MoveResult(state.copy(status = evaluate(state)), false)

        val gainedEnergy = if (state.mode == GameMode.QUANTUM) FusionRules.energyGainForMergeCount(mergeCount) else 0
        val overflowBonus = if (state.mode == GameMode.QUANTUM) FusionRules.overflowScoreBonus(state.energy, gainedEnergy, state.difficulty) else 0
        val nextEnergy = if (state.mode == GameMode.QUANTUM) minOf(FusionRules.maxEnergyFor(state.difficulty), state.energy + gainedEnergy) else state.energy
        val nextScore = state.score + gainedScore + overflowBonus
        var next = state.copy(
            cells = output,
            score = nextScore,
            bestScore = maxOf(state.bestScore, nextScore),
            dailyBestScore = if (state.difficulty == Difficulty.DAILY) maxOf(state.dailyBestScore, nextScore) else state.dailyBestScore,
            moveCount = state.moveCount + 1,
            nextTileId = nextId,
            energy = nextEnergy,
            totalChainMergeCount = state.totalChainMergeCount + maxOf(0, mergeCount - 1),
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
        next = finalizeState(next.copy(status = evaluate(next)), state.status)
        return MoveResult(next, true, gainedScore, mergeCount, reactionCount, entanglementCollapseCount, overflowBonus, animations)
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
        val cost = FusionRules.superpositionCollapseEnergyCosts[choiceIndex]
        if (state.energy < cost) return SuperpositionResult.Failure(state, SuperpositionFailure.INSUFFICIENT_SCORE)

        val resolved = tile.copy(value = tile.superpositionValues[choiceIndex], superpositionValues = emptyList())
        val cells = state.cells.toMutableList()
        cells[sourceIndex] = resolved
        val next = finalizeState(state.copy(
            cells = cells,
            energy = state.energy - cost,
            successfulCollapseCount = state.successfulCollapseCount + 1,
            lowCollapseCount = state.lowCollapseCount + if (choiceIndex == 0) 1 else 0,
            highCollapseCount = state.highCollapseCount + if (choiceIndex == 0) 0 else 1,
            status = evaluate(state.copy(cells = cells)),
        ), state.status)
        return SuperpositionResult.Success(
            state = next,
            animation = MoveAnimation(tile.id, sourceIndex, sourceIndex, if (choiceIndex == 0) MoveAnimationKind.COLLAPSE_LOW else MoveAnimationKind.COLLAPSE_HIGH),
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
            val kind = if (random.nextDouble() < 0.5) TileKind.ELECTRON else TileKind.PROTON
            val values = if (FusionRules.canSpawnSuperposition(state) && random.nextDouble() < FusionRules.superpositionSpawnChance) {
                FusionRules.superpositionValuesFor(1)
            } else {
                emptyList()
            }
            Tile(state.nextTileId, 1, kind, superpositionValues = values)
        } else {
            Tile(state.nextTileId, if (random.nextDouble() < 0.9) 2 else 4)
        }
        val pairedCells = if (state.mode == GameMode.QUANTUM) maybeEntangle(cells, state.size, at, state.nextTileId + 1) else cells
        return state.copy(cells = pairedCells, nextTileId = state.nextTileId + 1)
    }

    private fun maybeEntangle(cells: MutableList<Tile?>, size: Int, spawnedIndex: Int, groupId: Long): List<Tile?> {
        val spawned = cells[spawnedIndex] ?: return cells
        if (!FusionRules.canEntangle(spawned) || random.nextDouble() >= FusionRules.entanglementSpawnChance) return cells
        val candidates = adjacentIndices(size, spawnedIndex)
            .mapNotNull { index -> cells[index]?.takeIf(FusionRules::canEntangle)?.let { index to it } }
        if (candidates.isEmpty()) return cells
        val (partnerIndex, partner) = candidates[random.nextInt(candidates.size)]
        cells[spawnedIndex] = spawned.copy(entanglementGroupId = groupId)
        cells[partnerIndex] = partner.copy(entanglementGroupId = groupId)
        return cells
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
}
