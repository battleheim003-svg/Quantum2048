package com.battleheim.quantum2048.engine

class GameEngine(
    private val random: RandomProvider,
    val balance: QuantumBalance = QuantumBalance(),
) {
    fun newGame(mode: GameMode = GameMode.CLASSIC, size: Int = 4): GameState {
        return newGame(Difficulty.fromMode(mode), size)
    }

    fun newGame(difficulty: Difficulty, size: Int = 4): GameState {
        val rules = balance.rulesFor(difficulty)
        return spawn(
            spawn(
                GameState(
                    size = size,
                    cells = List(size * size) { null },
                    mode = difficulty.mode,
                    difficulty = difficulty,
                    quantumEnergy = rules.startingEnergy,
                ),
            ),
        )
    }

    fun move(state: GameState, direction: Direction): MoveResult {
        if (state.status != GameStatus.PLAYING) return MoveResult(state, false)
        val output = MutableList<Tile?>(state.cells.size) { null }
        var gainedScore = 0
        var mergeCount = 0
        var nextId = state.nextTileId
        for (line in 0 until state.size) {
            val tiles = (0 until state.size).mapNotNull { state.cells[index(state.size, direction, line, it)] }
            val merged = mutableListOf<Tile>()
            var i = 0
            while (i < tiles.size) {
                val first = tiles[i]
                val second = tiles.getOrNull(i + 1)
                val fusion = second?.let { mergeProduct(first, it, state.difficulty) }
                if (fusion != null) {
                    merged += fusion.copy(id = nextId++)
                    val value = fusion.value
                    gainedScore += value
                    mergeCount++
                    i += 2
                } else {
                    merged += first
                    i++
                }
            }
            merged.forEachIndexed { p, tile -> output[index(state.size, direction, line, p)] = tile }
        }

        val changed = state.cells != output
        if (!changed) return MoveResult(state.copy(status = evaluate(state)), false)

        val rules = balance.rulesFor(state.difficulty)
        val energyGain = if (rules.energyEnabled) balance.energyFor(mergeCount) else 0
        var next = state.copy(
            cells = output,
            score = state.score + gainedScore,
            bestScore = maxOf(state.bestScore, state.score + gainedScore),
            moveCount = state.moveCount + 1,
            nextTileId = nextId,
            quantumEnergy = minOf(balance.maxEnergy, state.quantumEnergy + energyGain),
        )
        next = spawn(next)
        val auto = maybeAutoCollapse(next)
        next = auto.first
        next = next.copy(status = evaluate(next))
        return MoveResult(next, true, gainedScore, mergeCount, energyGain, auto.second)
    }

    fun collapse(state: GameState, tileId: Long, chosenValue: Int): CollapseResult {
        if (state.status != GameStatus.PLAYING) return CollapseResult.Failure(state, CollapseFailure.GAME_NOT_ACTIVE)
        if (!balance.rulesFor(state.difficulty).collapseEnabled) return CollapseResult.Failure(state, CollapseFailure.NOT_QUANTUM)
        val index = state.cells.indexOfFirst { it?.id == tileId }
        if (index < 0) return CollapseResult.Failure(state, CollapseFailure.TILE_NOT_FOUND)
        val tile = state.cells[index] ?: return CollapseResult.Failure(state, CollapseFailure.TILE_NOT_FOUND)
        val high = tile.quantumAlternative ?: return CollapseResult.Failure(state, CollapseFailure.NOT_QUANTUM)
        if (chosenValue != tile.value && chosenValue != high) return CollapseResult.Failure(state, CollapseFailure.INVALID_CHOICE)
        val cost = if (chosenValue == tile.value) balance.lowCollapseCost else balance.highCollapseCost
        if (state.quantumEnergy < cost) return CollapseResult.Failure(state, CollapseFailure.INSUFFICIENT_ENERGY)
        val cells = state.cells.toMutableList()
        val chosenSpecies = tile.speciesOptions().firstOrNull { it.scoreValue == chosenValue }
        cells[index] = Tile(tile.id, chosenValue, species = chosenSpecies)
        val event = CollapseEvent(tile.id, chosenValue, automatic = false)
        return CollapseResult.Success(state.copy(cells = cells, quantumEnergy = state.quantumEnergy - cost), event, cost)
    }

    fun combineCompound(state: GameState, tileIds: List<Long>): CompoundResult {
        if (state.status != GameStatus.PLAYING) return CompoundResult.Failure(state, CompoundFailure.GAME_NOT_ACTIVE)
        val rules = balance.rulesFor(state.difficulty)
        if (!rules.compoundLabEnabled) return CompoundResult.Failure(state, CompoundFailure.LAB_DISABLED)
        if (tileIds.distinct().size != tileIds.size || tileIds.size < 2) {
            return CompoundResult.Failure(state, CompoundFailure.INVALID_TILE)
        }

        val indexedTiles = tileIds.map { id ->
            val index = state.cells.indexOfFirst { it?.id == id }
            if (index < 0) return CompoundResult.Failure(state, CompoundFailure.TILE_NOT_FOUND)
            index to state.cells[index]!!
        }
        val elements = indexedTiles.map { (_, tile) ->
            val species = tile.species ?: return CompoundResult.Failure(state, CompoundFailure.INVALID_TILE)
            if (tile.isUnstable) return CompoundResult.Failure(state, CompoundFailure.INVALID_TILE)
            ElementTile(species = species, sourceTileId = tile.id)
        }
        val recipe = Chemistry.findRecipe(elements, balance.compoundRecipes, rules.allowedRecipeLevel)
            ?: return CompoundResult.Failure(state, CompoundFailure.NO_RECIPE)
        val cost = if (rules.energyEnabled) rules.compoundEnergyCost else 0
        if (state.quantumEnergy < cost) return CompoundResult.Failure(state, CompoundFailure.INSUFFICIENT_ENERGY)

        val cells = state.cells.toMutableList()
        indexedTiles.forEach { (index, _) -> cells[index] = null }
        val nextEnergy = if (rules.energyEnabled) {
            minOf(balance.maxEnergy, state.quantumEnergy - cost + recipe.output.energyReward)
        } else {
            state.quantumEnergy
        }
        val nextScore = state.score + recipe.output.scoreValue
        return CompoundResult.Success(
            state = state.copy(
                cells = cells,
                score = nextScore,
                bestScore = maxOf(state.bestScore, nextScore),
                quantumEnergy = nextEnergy,
                status = evaluate(state.copy(cells = cells, score = nextScore, bestScore = maxOf(state.bestScore, nextScore), quantumEnergy = nextEnergy)),
            ),
            recipe = recipe,
            energySpent = cost,
        )
    }

    fun continueAfterWin(state: GameState) = state.copy(status = GameStatus.PLAYING, hasAcknowledgedWin = true)

    fun spawn(state: GameState): GameState {
        val empty = state.cells.indices.filter { state.cells[it] == null }
        if (empty.isEmpty()) return state
        val at = empty[random.nextInt(empty.size)]
        val rules = balance.rulesFor(state.difficulty)
        val base = if (rules.particleMode) {
            if (random.nextDouble() < 0.52) QuantumSpecies.ELECTRON.scoreValue else QuantumSpecies.PROTON.scoreValue
        } else if (random.nextDouble() < 0.9) 2 else 4
        val quantum = rules.collapseEnabled && random.nextDouble() < rules.quantumSpawnChance
        val cells = state.cells.toMutableList()
        cells[at] = if (rules.particleMode) {
            val species = if (base == QuantumSpecies.ELECTRON.scoreValue) QuantumSpecies.ELECTRON else QuantumSpecies.PROTON
            val alternative = if (species == QuantumSpecies.ELECTRON) QuantumSpecies.PROTON else QuantumSpecies.HYDROGEN
            Tile(
                id = state.nextTileId,
                value = species.scoreValue,
                quantumAlternative = if (quantum) alternative.scoreValue else null,
                species = species,
                quantumAlternativeSpecies = if (quantum) alternative else null,
            )
        } else {
            Tile(state.nextTileId, base)
        }
        return state.copy(cells = cells, nextTileId = state.nextTileId + 1)
    }

    private fun maybeAutoCollapse(state: GameState): Pair<GameState, CollapseEvent?> {
        val rules = balance.rulesFor(state.difficulty)
        if (!rules.collapseEnabled || random.nextDouble() >= rules.autoCollapseChance) return state to null
        val quantumIndices = state.cells.indices.filter { state.cells[it]?.isUnstable == true }
        if (quantumIndices.isEmpty()) return state to null
        val index = quantumIndices[random.nextInt(quantumIndices.size)]
        val tile = state.cells[index]!!
        val chosen = if (random.nextDouble() < balance.autoCollapseLowWeight) tile.value else tile.quantumAlternative!!
        val cells = state.cells.toMutableList()
        val chosenSpecies = tile.speciesOptions().firstOrNull { it.scoreValue == chosen }
        cells[index] = Tile(tile.id, chosen, species = chosenSpecies)
        return state.copy(cells = cells) to CollapseEvent(tile.id, chosen, automatic = true)
    }

    private fun canMerge(a: Tile, b: Tile): Boolean = mergeProduct(a, b, Difficulty.EASY) != null

    private fun mergeProduct(a: Tile, b: Tile, difficulty: Difficulty): Tile? {
        if (difficulty == Difficulty.EASY) return if (a.value == b.value) Tile(0, a.value * 2) else null
        if (a.isUnstable || b.isUnstable) return null

        val left = a.species
        val right = b.species
        if (left == QuantumSpecies.ELECTRON && right == QuantumSpecies.PROTON ||
            left == QuantumSpecies.PROTON && right == QuantumSpecies.ELECTRON
        ) return Tile(0, QuantumSpecies.HYDROGEN.scoreValue, species = QuantumSpecies.HYDROGEN)

        if (left != null && left == right) {
            val product = left.nextFusion() ?: return null
            if (!isFusionAllowed(product, balance.rulesFor(difficulty).maxFusionSpecies)) return null
            return Tile(0, product.scoreValue, species = product)
        }
        return if (a.value == b.value) Tile(0, a.value * 2) else null
    }

    private fun evaluate(state: GameState): GameStatus {
        if (!state.hasAcknowledgedWin && state.cells.any { it?.isQuantum == false && it.value >= 2048 }) return GameStatus.WON
        if (state.cells.any { it == null }) return GameStatus.PLAYING
        for (r in 0 until state.size) for (c in 0 until state.size) {
            val tile = state.cells[r * state.size + c] ?: continue
            if (c + 1 < state.size && mergeProduct(tile, state.cells[r * state.size + c + 1]!!, state.difficulty) != null) return GameStatus.PLAYING
            if (r + 1 < state.size && mergeProduct(tile, state.cells[(r + 1) * state.size + c]!!, state.difficulty) != null) return GameStatus.PLAYING
        }
        return GameStatus.LOST
    }

    private fun isFusionAllowed(product: QuantumSpecies, maxSpecies: QuantumSpecies?): Boolean {
        if (maxSpecies == null) return true
        val chain = QuantumSpecies.fusionChain
        val productIndex = chain.indexOf(product)
        val maxIndex = chain.indexOf(maxSpecies)
        return productIndex >= 0 && maxIndex >= 0 && productIndex <= maxIndex
    }

    private fun index(size: Int, d: Direction, line: Int, p: Int) = when (d) {
        Direction.LEFT -> line * size + p
        Direction.RIGHT -> line * size + (size - 1 - p)
        Direction.UP -> p * size + line
        Direction.DOWN -> (size - 1 - p) * size + line
    }
}
