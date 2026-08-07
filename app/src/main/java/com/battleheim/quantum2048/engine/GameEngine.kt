package com.battleheim.quantum2048.engine

class GameEngine(
    private val random: RandomProvider,
) {
    fun newGame(mode: GameMode = GameMode.CLASSIC, size: Int = 4): GameState =
        newGame(Difficulty.fromMode(mode), size)

    fun newGame(difficulty: Difficulty, size: Int = 4): GameState {
        var state = GameState(
            size = size,
            cells = List(size * size) { null },
            mode = difficulty.mode,
            difficulty = difficulty,
        )
        repeat(FusionRules.spawnCount(size)) { state = spawn(state) }
        return state
    }

    fun move(state: GameState, direction: Direction): MoveResult {
        if (state.status != GameStatus.PLAYING) return MoveResult(state, false)
        val output = MutableList<Tile?>(state.cells.size) { null }
        var gainedScore = 0
        var mergeCount = 0
        var reactionCount = 0
        var nextId = state.nextTileId
        val animations = mutableListOf<MoveAnimation>()
        val priorIds = state.cells.mapNotNull { it?.id }.toSet()

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
                    fusion.tiles.forEach { merged += IndexedTile(it.copy(id = nextId++), first.sourceIndex, animationKind) }
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

        val changed = state.cells != output
        if (!changed) return MoveResult(state.copy(status = evaluate(state)), false)

        var next = state.copy(
            cells = output,
            score = state.score + gainedScore,
            bestScore = maxOf(state.bestScore, state.score + gainedScore),
            moveCount = state.moveCount + 1,
            nextTileId = nextId,
        )
        val beforeSpawn = next
        next = spawn(next)
        val spawnedIndex = next.cells.indices.firstOrNull { beforeSpawn.cells[it] == null && next.cells[it] != null }
        val spawnedTile = spawnedIndex?.let { next.cells[it] }
        if (spawnedIndex != null && spawnedTile != null) {
            animations += MoveAnimation(spawnedTile.id, spawnedIndex, spawnedIndex, MoveAnimationKind.SPAWN)
        }
        next = next.copy(status = evaluate(next))
        return MoveResult(next, true, gainedScore, mergeCount, reactionCount, animations)
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
        return CompoundResult.Success(
            state = state.copy(
                cells = cells,
                score = nextScore,
                bestScore = maxOf(state.bestScore, nextScore),
                status = evaluate(state.copy(cells = cells, score = nextScore, bestScore = maxOf(state.bestScore, nextScore))),
            ),
            recipe = recipe,
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
            Tile(state.nextTileId, 1, kind)
        } else {
            Tile(state.nextTileId, if (random.nextDouble() < 0.9) 2 else 4)
        }
        return state.copy(cells = cells, nextTileId = state.nextTileId + 1)
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
        if (!state.hasAcknowledgedWin && state.cells.any { it != null && FusionRules.gameValueOf(it) >= 2048 }) return GameStatus.WON
        if (state.cells.any { it == null }) return GameStatus.PLAYING
        for (r in 0 until state.size) for (c in 0 until state.size) {
            val tile = state.cells[r * state.size + c] ?: continue
            if (c + 1 < state.size && mergeProduct(tile, state.cells[r * state.size + c + 1]!!, state.difficulty) != null) return GameStatus.PLAYING
            if (r + 1 < state.size && mergeProduct(tile, state.cells[(r + 1) * state.size + c]!!, state.difficulty) != null) return GameStatus.PLAYING
        }
        return GameStatus.LOST
    }

    private fun index(size: Int, d: Direction, line: Int, p: Int) = when (d) {
        Direction.LEFT -> line * size + p
        Direction.RIGHT -> line * size + (size - 1 - p)
        Direction.UP -> p * size + line
        Direction.DOWN -> (size - 1 - p) * size + line
    }

    private data class IndexedTile(val tile: Tile, val sourceIndex: Int, val kind: MoveAnimationKind? = null)
}
