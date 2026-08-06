package com.battleheim.quantum2048.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.battleheim.quantum2048.audio.GameAudio
import com.battleheim.quantum2048.audio.SilentGameAudio
import com.battleheim.quantum2048.designsystem.Cyan
import com.battleheim.quantum2048.designsystem.PanelRaised
import com.battleheim.quantum2048.designsystem.Panel
import com.battleheim.quantum2048.designsystem.TextMuted
import com.battleheim.quantum2048.designsystem.TextSecondary
import com.battleheim.quantum2048.designsystem.Void
import com.battleheim.quantum2048.designsystem.difficultyAccent
import com.battleheim.quantum2048.designsystem.difficultySurface
import com.battleheim.quantum2048.designsystem.elementColor
import com.battleheim.quantum2048.designsystem.elementFamily
import com.battleheim.quantum2048.domain.AppSettings
import com.battleheim.quantum2048.engine.Difficulty
import com.battleheim.quantum2048.engine.Direction
import com.battleheim.quantum2048.engine.GameMode
import com.battleheim.quantum2048.engine.GameState
import com.battleheim.quantum2048.engine.GameStatus
import com.battleheim.quantum2048.engine.QuantumBalance
import com.battleheim.quantum2048.engine.QuantumSpecies
import com.battleheim.quantum2048.engine.Tile
import kotlin.math.abs
import kotlinx.coroutines.delay

@Composable
fun GameScreen(
    vm: GameViewModel,
    settings: AppSettings = AppSettings(),
    audio: GameAudio = SilentGameAudio,
    onPause: () -> Unit = {},
) {
    val ui by vm.ui.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(ui.message) {
        ui.message?.let {
            snackbar.showSnackbar(it)
            vm.consumeMessage()
        }
    }
    LaunchedEffect(ui.collapsePulseId) {
        if (ui.collapsePulseId != null) {
            delay(650)
            vm.consumeCollapsePulse()
        }
    }
    LaunchedEffect(ui.feedback) {
        when (ui.feedback) {
            GameFeedback.MOVE -> {
                if (settings.soundEnabled) audio.move()
            }
            GameFeedback.MERGE -> {
                if (settings.soundEnabled) audio.merge()
                if (settings.hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            GameFeedback.COLLAPSE -> {
                if (settings.soundEnabled) audio.collapse()
                if (settings.hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
            GameFeedback.COMPOUND -> {
                if (settings.soundEnabled) audio.merge()
                if (settings.hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            GameFeedback.GAME_OVER -> {
                if (settings.soundEnabled) audio.gameOver()
                if (settings.hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            null -> Unit
        }
        if (ui.feedback != null) vm.consumeFeedback()
    }

    Scaffold(containerColor = Void, snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Header(ui.game, onPause)
            if (ui.game.mode == GameMode.QUANTUM) {
                EnergyBar(ui.game.quantumEnergy, vm.balance.maxEnergy)
                FusionGuide(ui.game.difficulty)
                if (vm.balance.rulesFor(ui.game.difficulty).compoundLabEnabled) {
                    CompoundLab(ui.game, ui.labTileIds, vm::clearCompoundLab)
                }
            }
            Board(ui.game, ui.collapsePulseId, ui.labTileIds, settings.reducedMotion, vm::swipe, vm::selectTile, vm::sendToCompoundLab)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = vm::newGame, modifier = Modifier.weight(1f)) { Text("New game") }
                OutlinedButton(onClick = vm::undo, enabled = ui.canUndo, modifier = Modifier.weight(1f)) { Text("Undo") }
            }
            Text(
                if (ui.game.mode == GameMode.QUANTUM) {
                    "Quantum mode is now a clean synthesis board: e- + p+ makes H, then matching elements fuse upward."
                } else {
                    "Classic mode keeps the normal 2048 number rules."
                },
                color = TextSecondary,
                fontSize = 13.sp,
            )
        }

        if (ui.game.status != GameStatus.PLAYING) {
            EndDialog(ui.game, vm::continueGame, vm::newGame)
        }
        val selected = ui.selectedTileId?.let { id -> ui.game.cells.firstOrNull { it?.id == id } }
        if (selected?.isUnstable == true) {
            CollapseDialog(selected, ui.game.quantumEnergy, vm.balance, vm::dismissCollapse, vm::collapse)
        }
    }
}

@Composable
private fun Header(game: GameState, onPause: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text("Quantum 2048", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            Text("${game.difficulty.label()} - ${if (game.mode == GameMode.QUANTUM) "SYNTHESIS LAB" else "CLASSIC BOARD"}", color = difficultyAccent(game.difficulty), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onPause) { Text("Pause") }
            Surface(shape = RoundedCornerShape(12.dp), color = difficultySurface(game.difficulty)) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 9.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(game.score.toString(), fontWeight = FontWeight.Black)
                    Text("Best ${game.bestScore}", color = TextSecondary, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun EnergyBar(energy: Int, max: Int) {
    Column(Modifier.fillMaxWidth().background(PanelRaised, RoundedCornerShape(8.dp)).padding(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Fusion energy", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text("$energy / $max", color = Cyan, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(7.dp))
        LinearProgressIndicator(
            progress = { energy.toFloat() / max },
            modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(10.dp)),
            color = Cyan,
            trackColor = Color(0xFF242B4A),
        )
    }
}

@Composable
private fun FusionGuide(difficulty: Difficulty) {
    Surface(color = difficultySurface(difficulty), shape = RoundedCornerShape(8.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(difficulty.label(), color = TextSecondary, fontSize = 12.sp)
            Text("e- + p+ -> H -> He -> Li", color = difficultyAccent(difficulty), fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

@Composable
private fun CompoundLab(game: GameState, labTileIds: List<Long>, clear: () -> Unit) {
    val labels = labTileIds.mapNotNull { id -> game.cells.firstOrNull { it?.id == id }?.species?.symbol }
    Surface(color = PanelRaised, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Compound Lab", fontWeight = FontWeight.Black)
                Text(if (labels.isEmpty()) "Drag or tap stable elements here" else labels.joinToString(" + "), color = TextSecondary, fontSize = 12.sp)
            }
            TextButton(onClick = clear, enabled = labels.isNotEmpty()) { Text("Clear") }
        }
    }
}

@Composable
private fun Board(
    game: GameState,
    pulseId: Long?,
    labTileIds: List<Long>,
    reducedMotion: Boolean,
    onSwipe: (Direction) -> Unit,
    onTileClick: (Long) -> Unit,
    onTileDragToLab: (Long) -> Unit,
) {
    var dx by remember { mutableFloatStateOf(0f) }
    var dy by remember { mutableFloatStateOf(0f) }
    Column(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(Panel, RoundedCornerShape(12.dp))
            .border(1.dp, difficultyAccent(game.difficulty).copy(alpha = 0.42f), RoundedCornerShape(12.dp))
            .padding(8.dp)
            .pointerInput(game.mode) {
                detectDragGestures(
                    onDragStart = { dx = 0f; dy = 0f },
                    onDrag = { change, drag ->
                        change.consume()
                        dx += drag.x
                        dy += drag.y
                    },
                    onDragEnd = {
                        if (maxOf(abs(dx), abs(dy)) > 36f) {
                            onSwipe(
                                if (abs(dx) > abs(dy)) {
                                    if (dx > 0) Direction.RIGHT else Direction.LEFT
                                } else {
                                    if (dy > 0) Direction.DOWN else Direction.UP
                                },
                            )
                        }
                    },
                )
            },
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        repeat(game.size) { row ->
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                repeat(game.size) { column ->
                    val tile = game[row, column]
                    TileCell(
                        tile = tile,
                        pulsing = tile?.id == pulseId,
                        selectedForLab = tile?.id in labTileIds,
                        reducedMotion = reducedMotion,
                        mode = game.mode,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        onClick = onTileClick,
                        onDragToLab = onTileDragToLab,
                    )
                }
            }
        }
    }
}

@Composable
private fun TileCell(
    tile: Tile?,
    pulsing: Boolean,
    selectedForLab: Boolean,
    reducedMotion: Boolean,
    mode: GameMode,
    modifier: Modifier,
    onClick: (Long) -> Unit,
    onDragToLab: (Long) -> Unit,
) {
    val scale = remember(tile?.id) { Animatable(1f) }
    LaunchedEffect(pulsing, reducedMotion) {
        if (pulsing && !reducedMotion) {
            scale.animateTo(0.78f, tween(160, easing = FastOutSlowInEasing))
            scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        } else {
            scale.snapTo(1f)
        }
    }

    val color = when {
        tile == null -> Color(0xFF171D38)
        mode == GameMode.QUANTUM -> elementColor(tile.species)
        tile.value < 16 -> Color(0xFF193A55)
        tile.value < 128 -> Color(0xFF214C75)
        tile.value < 1024 -> Color(0xFF4F5178)
        else -> Color(0xFF71643F)
    }

    Box(
        modifier
            .animateContentSize()
            .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
            .background(color, RoundedCornerShape(8.dp))
            .then(if (tile?.species != null) Modifier.border(if (selectedForLab) 2.dp else 1.dp, if (selectedForLab) Cyan else Color.White.copy(alpha = 0.18f), RoundedCornerShape(8.dp)) else Modifier)
            .then(
                when {
                    tile?.species != null && !tile.isUnstable -> Modifier
                        .clickable { onDragToLab(tile.id) }
                        .pointerInput(tile.id) {
                            var dragY = 0f
                            detectDragGestures(
                                onDragStart = { dragY = 0f },
                                onDrag = { change, drag ->
                                    change.consume()
                                    dragY += drag.y
                                },
                                onDragEnd = { if (dragY > 18f) onDragToLab(tile.id) },
                            )
                        }
                    tile?.isUnstable == true -> Modifier.clickable { onClick(tile.id) }
                    else -> Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        when {
            tile == null -> Unit
            tile.isUnstable -> UnstableTileLabel(tile)
            mode == GameMode.QUANTUM && tile.species != null -> ElementTileLabel(tile.species)
            else -> Text(tile.value.toString(), fontSize = if (tile.value < 1000) 26.sp else 20.sp, fontWeight = FontWeight.Black, color = Color.White)
        }
    }
}

@Composable
private fun UnstableTileLabel(tile: Tile) {
    val low = tile.species?.symbol ?: tile.value.toString()
    val high = tile.quantumAlternativeSpecies?.symbol ?: tile.quantumAlternative.toString()
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$low | $high", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
        Text("Unresolved", color = Cyan, fontSize = 8.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun CollapseDialog(tile: Tile, energy: Int, balance: QuantumBalance, dismiss: () -> Unit, collapse: (Int) -> Unit) {
    val high = tile.quantumAlternative ?: return
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text("Collapse") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Choose one state. Collapse is atomic and does not spawn a new tile.")
                CollapseChoice(tile.value, tile.species?.symbol ?: tile.value.toString(), balance.lowCollapseCost, energy, collapse)
                CollapseChoice(high, tile.quantumAlternativeSpecies?.symbol ?: high.toString(), balance.highCollapseCost, energy, collapse)
                Text("Energy: $energy", color = Cyan, fontWeight = FontWeight.Bold)
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = dismiss) { Text("Cancel") } },
    )
}

@Composable
private fun CollapseChoice(value: Int, label: String, cost: Int, energy: Int, choose: (Int) -> Unit) {
    OutlinedButton(onClick = { choose(value) }, enabled = energy >= cost, modifier = Modifier.fillMaxWidth()) {
        Text("Choose $label - cost $cost", textAlign = TextAlign.Center)
    }
}

private fun Difficulty.label(): String = name.lowercase().replaceFirstChar { it.uppercase() }

@Composable
private fun ElementTileLabel(species: QuantumSpecies) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(species.symbol, fontSize = 26.sp, fontWeight = FontWeight.Black, color = Color.White)
        Text(elementFamily(species), color = TextSecondary, fontSize = 7.sp, textAlign = TextAlign.Center)
        if (species.massNumber > 0) {
            Text("A ${species.massNumber}", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun EndDialog(game: GameState, continueGame: () -> Unit, newGame: () -> Unit) {
    val status = game.status
    val bestElement = game.cells.mapNotNull { it?.species }.maxByOrNull { it.scoreValue }
    AlertDialog(
        onDismissRequest = {},
        title = { Text(if (status == GameStatus.WON) "Target reached" else "Game over") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (status == GameStatus.WON) "Keep the lab running and synthesize heavier elements." else "No more moves are available.")
                Text("Difficulty: ${game.difficulty.label()}", color = TextSecondary)
                Text("Score: ${game.score}", color = TextSecondary)
                Text("Moves: ${game.moveCount}", color = TextSecondary)
                Text("Best element: ${bestElement?.symbol ?: "none"}", color = TextSecondary)
            }
        },
        confirmButton = {
            Button(onClick = if (status == GameStatus.WON) continueGame else newGame) {
                Text(if (status == GameStatus.WON) "Continue" else "New game")
            }
        },
    )
}
