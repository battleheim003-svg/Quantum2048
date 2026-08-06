package com.battleheim.quantum2048.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.battleheim.quantum2048.designsystem.Cyan
import com.battleheim.quantum2048.designsystem.Panel
import com.battleheim.quantum2048.designsystem.Void
import com.battleheim.quantum2048.engine.Direction
import com.battleheim.quantum2048.engine.GameMode
import com.battleheim.quantum2048.engine.GameState
import com.battleheim.quantum2048.engine.GameStatus
import com.battleheim.quantum2048.engine.QuantumSpecies
import com.battleheim.quantum2048.engine.Tile
import kotlin.math.abs
import kotlinx.coroutines.delay

@Composable
fun GameScreen(vm: GameViewModel) {
    val ui by vm.ui.collectAsState()
    val snackbar = remember { SnackbarHostState() }

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

    Scaffold(containerColor = Void, snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Header(ui.game)
            ModeSelector(ui.game.mode, vm::switchMode)
            if (ui.game.mode == GameMode.QUANTUM) {
                EnergyBar(ui.game.quantumEnergy, vm.balance.maxEnergy)
                FusionGuide()
            }
            Board(ui.game, ui.collapsePulseId, vm::swipe)
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
                color = Color(0xFFABB4D6),
                fontSize = 13.sp,
            )
        }

        if (ui.game.status != GameStatus.PLAYING) {
            EndDialog(ui.game.status, vm::continueGame, vm::newGame)
        }
    }
}

@Composable
private fun Header(game: GameState) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text("Quantum 2048", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            Text(if (game.mode == GameMode.QUANTUM) "SYNTHESIS LAB" else "CLASSIC BOARD", color = Cyan, fontSize = 12.sp)
        }
        Surface(shape = RoundedCornerShape(12.dp), color = Panel) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 9.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(game.score.toString(), fontWeight = FontWeight.Black)
                Text("Best ${game.bestScore}", color = Color(0xFFABB4D6), fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun ModeSelector(selected: GameMode, onSelect: (GameMode) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        GameMode.entries.forEach { mode ->
            FilterChip(
                selected = selected == mode,
                onClick = { onSelect(mode) },
                label = { Text(if (mode == GameMode.QUANTUM) "Quantum" else "Classic") },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun EnergyBar(energy: Int, max: Int) {
    Column(Modifier.fillMaxWidth().background(Color(0xFF101933), RoundedCornerShape(12.dp)).padding(12.dp)) {
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
private fun FusionGuide() {
    Surface(color = Color(0xFF111A2E), shape = RoundedCornerShape(12.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Phase 1", color = Color(0xFFABB4D6), fontSize = 12.sp)
            Text("e- + p+ -> H -> He -> Li", color = Cyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

@Composable
private fun Board(game: GameState, pulseId: Long?, onSwipe: (Direction) -> Unit) {
    var dx by remember { mutableFloatStateOf(0f) }
    var dy by remember { mutableFloatStateOf(0f) }
    Column(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(Panel, RoundedCornerShape(18.dp))
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
                    TileCell(tile, tile?.id == pulseId, game.mode, Modifier.weight(1f).fillMaxHeight())
                }
            }
        }
    }
}

@Composable
private fun TileCell(tile: Tile?, pulsing: Boolean, mode: GameMode, modifier: Modifier) {
    val scale = remember(tile?.id) { Animatable(1f) }
    LaunchedEffect(pulsing) {
        if (pulsing) {
            scale.animateTo(0.78f, tween(160, easing = FastOutSlowInEasing))
            scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
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
            .background(color, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        when {
            tile == null -> Unit
            mode == GameMode.QUANTUM && tile.species != null -> ElementTileLabel(tile.species)
            else -> Text(tile.value.toString(), fontSize = if (tile.value < 1000) 26.sp else 20.sp, fontWeight = FontWeight.Black, color = Color.White)
        }
    }
}

private fun elementColor(species: QuantumSpecies?): Color = when (species) {
    QuantumSpecies.ELECTRON -> Color(0xFF315A7C)
    QuantumSpecies.PROTON -> Color(0xFF6A3C55)
    QuantumSpecies.HYDROGEN -> Color(0xFF28665C)
    QuantumSpecies.HELIUM -> Color(0xFF5A5278)
    QuantumSpecies.LITHIUM, QuantumSpecies.BERYLLIUM, QuantumSpecies.BORON -> Color(0xFF676A37)
    QuantumSpecies.CARBON, QuantumSpecies.NITROGEN, QuantumSpecies.OXYGEN -> Color(0xFF3E646D)
    QuantumSpecies.NEON, QuantumSpecies.SILICON -> Color(0xFF6D5140)
    QuantumSpecies.IRON, QuantumSpecies.GOLD -> Color(0xFF725F30)
    null -> Color(0xFF245064)
}

@Composable
private fun ElementTileLabel(species: QuantumSpecies) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(species.symbol, fontSize = 26.sp, fontWeight = FontWeight.Black, color = Color.White)
        Text(species.title, color = Color(0xFFD7DEF8), fontSize = 8.sp, textAlign = TextAlign.Center)
        if (species.massNumber > 0) {
            Text("A ${species.massNumber}", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun EndDialog(status: GameStatus, continueGame: () -> Unit, newGame: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text(if (status == GameStatus.WON) "Target reached" else "Game over") },
        text = { Text(if (status == GameStatus.WON) "Keep the lab running and synthesize heavier elements." else "No more moves are available.") },
        confirmButton = {
            Button(onClick = if (status == GameStatus.WON) continueGame else newGame) {
                Text(if (status == GameStatus.WON) "Continue" else "New game")
            }
        },
    )
}
