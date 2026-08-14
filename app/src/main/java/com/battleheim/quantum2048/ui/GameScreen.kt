package com.battleheim.quantum2048.ui

import android.content.Intent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.battleheim.quantum2048.audio.GameAudio
import com.battleheim.quantum2048.audio.SilentGameAudio
import com.battleheim.quantum2048.designsystem.Cyan
import com.battleheim.quantum2048.designsystem.BoardGlass
import com.battleheim.quantum2048.designsystem.Electric
import com.battleheim.quantum2048.designsystem.GlassPanel
import com.battleheim.quantum2048.designsystem.NeonPink
import com.battleheim.quantum2048.designsystem.PanelRaised
import com.battleheim.quantum2048.designsystem.TextMuted
import com.battleheim.quantum2048.designsystem.TextSecondary
import com.battleheim.quantum2048.designsystem.Void
import com.battleheim.quantum2048.designsystem.RadiantGold
import com.battleheim.quantum2048.designsystem.classicTileColor
import com.battleheim.quantum2048.designsystem.classicTileTextColor
import com.battleheim.quantum2048.designsystem.difficultyAccent
import com.battleheim.quantum2048.designsystem.difficultySurface
import com.battleheim.quantum2048.designsystem.elementColor
import com.battleheim.quantum2048.designsystem.elementFamily
import com.battleheim.quantum2048.designsystem.tileKindColor
import com.battleheim.quantum2048.domain.AppSettings
import com.battleheim.quantum2048.domain.LevelRunStatus
import com.battleheim.quantum2048.domain.LevelRunUiState
import com.battleheim.quantum2048.engine.Difficulty
import com.battleheim.quantum2048.engine.Direction
import com.battleheim.quantum2048.engine.DuelOpponent
import com.battleheim.quantum2048.engine.DuelPlayer
import com.battleheim.quantum2048.engine.FusionRules
import com.battleheim.quantum2048.engine.GameMode
import com.battleheim.quantum2048.engine.GameState
import com.battleheim.quantum2048.engine.GameStatus
import com.battleheim.quantum2048.engine.MoveAnimation
import com.battleheim.quantum2048.engine.MoveAnimationKind
import com.battleheim.quantum2048.engine.QuantumElement
import com.battleheim.quantum2048.engine.Tile
import com.battleheim.quantum2048.engine.TileKind
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import androidx.compose.ui.unit.IntOffset
import com.battleheim.quantum2048.R
import kotlinx.coroutines.launch
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

    LaunchedEffect(settings) {
        audio.applySettings(settings)
        audio.gameMusic()
        if (settings.musicEnabled) audio.ambientStart() else audio.ambientStop()
    }

    LaunchedEffect(ui.message) {
        ui.message?.let {
            snackbar.showSnackbar(it)
            vm.consumeMessage()
        }
    }
    LaunchedEffect(ui.feedback) {
        val feedback = ui.feedback
        when (feedback) {
            GameFeedback.MOVE -> {
                if (settings.soundEnabled) audio.move()
            }
            GameFeedback.MERGE -> {
                if (settings.soundEnabled) audio.merge()
            }
            GameFeedback.REACTION -> {
                if (settings.soundEnabled) audio.reaction()
            }
            GameFeedback.COMPOUND -> {
                if (settings.soundEnabled) audio.synthesis()
            }
            GameFeedback.TUNNEL -> {
                if (settings.soundEnabled) audio.tunnel()
            }
            GameFeedback.COLLAPSE_LOW -> {
                if (settings.soundEnabled) audio.collapseLow()
            }
            GameFeedback.COLLAPSE_HIGH -> {
                if (settings.soundEnabled) audio.collapseHigh()
            }
            GameFeedback.GAME_OVER -> {
                if (settings.soundEnabled) {
                    if (ui.game.status == GameStatus.WON) audio.win() else audio.gameOver()
                }
            }
            null -> Unit
        }
        if (feedback != null && settings.hapticsEnabled) {
            HapticFeedbackManager.perform(haptics, feedback.hapticPattern())
        }
        if (feedback != null) vm.consumeFeedback()
    }
    LaunchedEffect(ui.isBoardShaking) {
        if (ui.isBoardShaking) {
            delay(360)
            vm.consumeBoardShake()
        }
    }
    val duel = ui.duel
    var turnSecondsLeft by remember(duel?.turnNumber) { mutableIntStateOf(duel?.config?.turnSeconds ?: 0) }
    LaunchedEffect(duel?.turnNumber, duel?.winner) {
        val activeDuel = duel ?: return@LaunchedEffect
        turnSecondsLeft = activeDuel.config.turnSeconds
        while (turnSecondsLeft > 0 && activeDuel.winner == null) {
            delay(1000)
            turnSecondsLeft--
        }
        if (turnSecondsLeft == 0 && activeDuel.winner == null) vm.passDuelTurn()
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background, snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.background,
                        ),
                    ),
                ),
        ) {
            QuantumBackdrop(settings.reducedMotion)
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Header(ui.game, onPause)
                ui.level?.let { LevelGoalHud(it) }
                duel?.let { DuelHeader(it.currentPlayer, it.config.opponent, turnSecondsLeft, it.winner, vm::passDuelTurn) }
                if (ui.game.mode == GameMode.QUANTUM) {
                    FusionGuide(ui.game.difficulty)
                    CompoundLab(ui.game, ui.labTileIds, vm::clearCompoundLab)
                }
                Board(ui.game, ui.labTileIds, ui.tunnelingTileId, ui.observerPreview, ui.animations, settings.reducedMotion, ui.isBoardShaking, vm::swipe, vm::sendToCompoundLab, vm::tapBoardCell, vm::observeTile)
                duel?.let { OpponentBoardSummary(it.inactiveBoard, it.currentPlayer) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuantumActionButton(text = stringResource(R.string.new_game), onClick = vm::newGame, modifier = Modifier.weight(1f), accent = RadiantGold, filled = true, icon = "+")
                    QuantumActionButton(text = stringResource(R.string.undo), onClick = vm::undo, enabled = ui.canUndo, modifier = Modifier.weight(1f), accent = Cyan, icon = "↶")
                }
                if (ui.game.mode == GameMode.QUANTUM) {
                    QuantumActionButton(
                        text = stringResource(if (ui.tunnelingTileId == null) R.string.tunnel else R.string.cancel_tunnel),
                        onClick = vm::toggleTunneling,
                        modifier = Modifier.fillMaxWidth(),
                        accent = if (ui.tunnelingTileId == null) Electric else NeonPink,
                        filled = ui.tunnelingTileId != null,
                        icon = "⟐",
                    )
                }
                Text(
                    if (ui.game.mode == GameMode.QUANTUM) {
                        stringResource(R.string.quantum_help)
                    } else {
                        stringResource(R.string.classic_help)
                    },
                    color = TextSecondary,
                    fontSize = 13.sp,
                )
            }
        }

        if (ui.game.status != GameStatus.PLAYING) {
            EndDialog(ui.game, vm::continueGame, vm::newGame, audio, settings)
        }
        ui.superpositionTileId?.let { tileId ->
            val tile = ui.game.cells.firstOrNull { it?.id == tileId }
            if (tile != null) {
                SuperpositionDialog(tile, vm::dismissSuperposition, vm::collapseSuperposition)
            }
        }
        if (ui.quantumUnlockEventVisible) {
            LaunchedEffect(Unit) {
                if (settings.soundEnabled) audio.unlock()
            }
            QuantumUnlockEffect(onDismiss = vm::dismissQuantumUnlockEvent)
        }
    }
}

@Composable
private fun QuantumUnlockEffect(onDismiss: () -> Unit) {
    val flash = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        flash.snapTo(0.92f)
        flash.animateTo(0f, tween(700, easing = FastOutSlowInEasing))
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    listOf(
                        Color.White.copy(alpha = flash.value),
                        Electric.copy(alpha = flash.value * 0.62f),
                        NeonPink.copy(alpha = flash.value * 0.36f),
                        Color.Transparent,
                    ),
                ),
            ),
    )
    QuantumDialog(
        title = "Quantum Anomaly Detected!",
        onDismiss = onDismiss,
        accent = RadiantGold,
        confirmText = "Unlocked",
        onConfirm = onDismiss,
    ) {
        Text("New Mode Unlocked.", color = Color.White, fontWeight = FontWeight.Black)
        Text("Quantum modes are now available from New Game.", color = TextSecondary, fontSize = 13.sp)
    }
}

@Composable
private fun LevelGoalHud(level: LevelRunUiState) {
    val accent = when (level.status) {
        LevelRunStatus.ACTIVE -> Electric
        LevelRunStatus.COMPLETE -> RadiantGold
        LevelRunStatus.FAILED -> NeonPink
    }
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = GlassPanel,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, accent.copy(alpha = 0.55f), RoundedCornerShape(14.dp)),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(level.title.uppercase(), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 0.9.sp)
                    Text(level.zoneTitle, color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        level.movesRemaining?.let { stringResource(R.string.moves_left, formatNumber(it)) } ?: stringResource(R.string.moves_unlimited),
                        color = accent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                    )
                    if (level.status != LevelRunStatus.ACTIVE) {
                        Text(
                            if (level.status == LevelRunStatus.COMPLETE) stringResource(R.string.level_complete_stars, level.stars) else stringResource(R.string.level_failed),
                            color = accent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            level.goals.forEach { goal ->
                val progress = if (goal.target <= 0) 0f else (goal.current.toFloat() / goal.target.toFloat()).coerceIn(0f, 1f)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(goal.label, color = if (goal.complete) Cyan else TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("${formatNumber(goal.current)}/${formatNumber(goal.target)}", color = if (goal.complete) RadiantGold else accent, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .background(Color.Black.copy(alpha = 0.32f), RoundedCornerShape(8.dp))
                            .border(1.dp, accent.copy(alpha = 0.25f), RoundedCornerShape(8.dp)),
                    ) {
                        Box(
                            Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progress.coerceAtLeast(0.03f))
                                .background(Brush.horizontalGradient(listOf(Cyan, accent, RadiantGold.copy(alpha = 0.85f))), RoundedCornerShape(8.dp)),
                        )
                    }
                }
            }
            if (level.mercy.active) {
                Text(
                    stringResource(R.string.mercy_active, level.mercy.assistMoveBonus),
                    color = RadiantGold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }
    }
}

@Composable
private fun QuantumBackdrop(reducedMotion: Boolean) {
    val pulse = remember { Animatable(0f) }
    LaunchedEffect(reducedMotion) {
        if (reducedMotion) {
            pulse.snapTo(0f)
        } else {
            while (true) {
                pulse.animateTo(1f, tween(3200, easing = FastOutSlowInEasing))
                pulse.animateTo(0f, tween(3200, easing = FastOutSlowInEasing))
            }
        }
    }
    Canvas(Modifier.fillMaxSize()) {
        val orbitColor = Cyan.copy(alpha = 0.08f + pulse.value * 0.05f)
        val centerX = size.width * 0.5f
        val centerY = size.height * 0.28f
        repeat(3) { index ->
            rotate(degrees = index * 58f, pivot = androidx.compose.ui.geometry.Offset(centerX, centerY)) {
                drawOval(
                    color = orbitColor,
                    topLeft = androidx.compose.ui.geometry.Offset(centerX - size.width * 0.42f, centerY - 46f - index * 12f),
                    size = androidx.compose.ui.geometry.Size(size.width * 0.84f, 92f + index * 24f),
                    style = Stroke(width = 1.3f),
                )
            }
        }
        repeat(18) { index ->
            val angle = index * 0.72f
            val radius = 40f + (index % 6) * 34f + pulse.value * 12f
            val x = centerX + cos(angle) * radius
            val y = centerY + sin(angle * 1.35f) * radius
            drawCircle(
                color = if (index % 3 == 0) NeonPink.copy(alpha = 0.22f) else Electric.copy(alpha = 0.16f),
                radius = 1.8f + (index % 4),
                center = androidx.compose.ui.geometry.Offset(x, y),
            )
        }
    }
}

@Composable
private fun DuelHeader(current: DuelPlayer, opponent: DuelOpponent, secondsLeft: Int, winner: DuelPlayer?, pass: () -> Unit) {
    Surface(color = PanelRaised, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(winner?.let { stringResource(R.string.player_wins, it.label()) } ?: stringResource(R.string.player_turn, current.label()), fontWeight = FontWeight.Black)
                Text(if (opponent == DuelOpponent.BOT) stringResource(R.string.duel_vs_bot) else stringResource(R.string.pass_and_play), color = TextSecondary, fontSize = 12.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${secondsLeft}s", color = Cyan, fontWeight = FontWeight.Bold)
                OutlinedButton(onClick = pass, enabled = winner == null) { Text(stringResource(R.string.pass)) }
            }
        }
    }
}

@Composable
private fun OpponentBoardSummary(board: GameState, current: DuelPlayer) {
    Surface(color = PanelRaised, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(stringResource(R.string.opponent_board, current.opponent().label()), fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.score_moves, formatNumber(board.score), formatNumber(board.moveCount)), color = TextSecondary, fontSize = 12.sp)
            }
            Text(stringResource(R.string.empty_count, board.cells.count { it == null }), color = Cyan, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun Header(game: GameState, onPause: () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(9.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            stringResource(R.string.app_name).uppercase(),
            color = Color(0xFFD8FBFF),
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = GlassPanel,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Cyan.copy(alpha = 0.42f), RoundedCornerShape(12.dp)),
        ) {
            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    HudReadout(stringResource(R.string.hud_score), formatNumber(game.score), Cyan)
                    HudReadout(stringResource(R.string.hud_high), formatNumber(game.bestScore), Color.White)
                    OutlinedButton(onClick = onPause) { Text(stringResource(R.string.pause)) }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${game.difficulty.localizedLabel()} / ${if (game.mode == GameMode.QUANTUM) stringResource(R.string.synthesis_lab) else stringResource(R.string.classic_board)}",
                        color = difficultyAccent(game.difficulty),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    if (game.difficulty == Difficulty.DAILY) {
                        Text(stringResource(R.string.daily_best_line, formatNumber(game.dailyBestScore)), color = TextSecondary, fontSize = 11.sp)
                    }
                }
                if (game.mode == GameMode.QUANTUM) {
                    EnergyMeter(game.energy)
                }
            }
        }
    }
}

@Composable
private fun HudReadout(label: String, value: String, accent: Color) {
    val valueScale = remember { Animatable(1f) }
    LaunchedEffect(value) {
        valueScale.snapTo(1.16f)
        valueScale.animateTo(
            1f,
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow,
            ),
        )
    }
    Column(horizontalAlignment = Alignment.Start) {
        Text(label, color = accent, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        Text(
            value,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.graphicsLayer {
                scaleX = valueScale.value
                scaleY = valueScale.value
            },
        )
    }
}

@Composable
private fun EnergyMeter(energy: Int) {
    val capped = energy.coerceIn(0, 100)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.hud_energy, formatNumber(capped)), color = Cyan, fontSize = 12.sp, fontWeight = FontWeight.Black)
        Box(
            Modifier
                .weight(1f)
                .height(12.dp)
                .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                .border(1.dp, Cyan.copy(alpha = 0.45f), RoundedCornerShape(8.dp)),
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth((capped / 100f).coerceAtLeast(0.01f))
                    .background(Brush.horizontalGradient(listOf(Electric, Cyan, Color.White.copy(alpha = 0.92f))), RoundedCornerShape(8.dp)),
            )
        }
    }
}

@Composable
private fun FusionGuide(difficulty: Difficulty) {
    Surface(
        color = GlassPanel,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.border(1.dp, difficultyAccent(difficulty).copy(alpha = 0.55f), RoundedCornerShape(14.dp)),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(difficulty.localizedLabel(), color = TextSecondary, fontSize = 12.sp)
            Text(stringResource(R.string.fusion_guide), color = difficultyAccent(difficulty), fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

@Composable
private fun CompoundLab(game: GameState, labTileIds: List<Long>, clear: () -> Unit) {
    val labels = labTileIds.mapNotNull { id -> game.cells.firstOrNull { it?.id == id }?.element?.symbol }
    Surface(
        color = GlassPanel,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Cyan.copy(alpha = 0.45f), RoundedCornerShape(16.dp)),
    ) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("⟡ ${stringResource(R.string.compound_lab)}", fontWeight = FontWeight.Black, color = Color.White)
                Text(if (labels.isEmpty()) stringResource(R.string.lab_empty) else labels.joinToString(" + "), color = TextSecondary, fontSize = 12.sp)
            }
            QuantumChipButton(text = stringResource(R.string.clear), selected = labels.isNotEmpty(), onClick = clear, enabled = labels.isNotEmpty(), accent = NeonPink)
        }
    }
}

@Composable
private fun Board(
    game: GameState,
    labTileIds: List<Long>,
    tunnelingTileId: Long?,
    observerPreview: ObserverPreview?,
    animations: List<MoveAnimation>,
    reducedMotion: Boolean,
    isBoardShaking: Boolean,
    onSwipe: (Direction) -> Unit,
    onTileDragToLab: (Long) -> Unit,
    onCellTap: (Int) -> Unit,
    onObserveTile: (Long) -> Unit,
) {
    var dx by remember { mutableFloatStateOf(0f) }
    var dy by remember { mutableFloatStateOf(0f) }
    val gap = if (game.size >= 8) 4.dp else if (game.size >= 6) 5.dp else 7.dp
    val boardPadding = if (game.size >= 8) 5.dp else 8.dp
    val animationByTileId = animations.associateBy { it.tileId }
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .shake(isShaking = isBoardShaking, reducedMotion = reducedMotion)
            .background(
                Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.08f),
                        BoardGlass,
                        Color.Black.copy(alpha = 0.36f),
                    ),
                ),
                RoundedCornerShape(18.dp),
            )
            .border(1.4.dp, difficultyAccent(game.difficulty).copy(alpha = 0.72f), RoundedCornerShape(18.dp))
            .padding(boardPadding)
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
    ) {
        val density = LocalDensity.current
        val boardPx = constraints.maxWidth.toFloat()
        val paddingPx = with(density) { boardPadding.toPx() }
        val gapPx = with(density) { gap.toPx() }
        val cellPx = (boardPx - paddingPx * 2f - gapPx * (game.size - 1)) / game.size
        val stepPx = cellPx + gapPx
        val cellDp = with(density) { cellPx.toDp() }

        Canvas(Modifier.fillMaxSize()) {
            val accent = difficultyAccent(game.difficulty)
            drawRoundRect(
                color = accent.copy(alpha = 0.08f),
                style = Stroke(width = 2.2f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(22f, 22f),
            )
            repeat(9) { index ->
                val x = size.width * (index + 1) / 10f
                drawLine(
                    color = if (index % 2 == 0) Cyan.copy(alpha = 0.05f) else NeonPink.copy(alpha = 0.035f),
                    start = androidx.compose.ui.geometry.Offset(x, 0f),
                    end = androidx.compose.ui.geometry.Offset(x + 36f, size.height),
                    strokeWidth = 0.8f,
                )
            }
        }

        repeat(game.size) { row ->
            repeat(game.size) { column ->
                Box(
                    Modifier
                        .offset { IntOffset((column * stepPx).roundToInt(), (row * stepPx).roundToInt()) }
                        .size(cellDp)
                        .background(Color(0x6617203D), RoundedCornerShape(12.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.035f), RoundedCornerShape(12.dp))
                        .clickable(enabled = tunnelingTileId != null) { onCellTap(row * game.size + column) },
                )
            }
        }

        game.cells.forEachIndexed { index, tile ->
            if (tile != null) {
                val row = index / game.size
                val column = index % game.size
                val animation = animationByTileId[tile.id]
                TileCell(
                    tile = tile,
                    animation = animation,
                    selectedForLab = tile.id in labTileIds,
                    selectedForTunnel = tile.id == tunnelingTileId,
                    tunnelingActive = tunnelingTileId != null,
                    observerValue = observerPreview?.takeIf { it.tileId == tile.id }?.value,
                    reducedMotion = reducedMotion,
                    mode = game.mode,
                    boardSize = game.size,
                    cellSize = cellDp,
                    stepPx = stepPx,
                    modifier = Modifier.offset { IntOffset((column * stepPx).roundToInt(), (row * stepPx).roundToInt()) },
                    onDragToLab = onTileDragToLab,
                    onTap = { onCellTap(index) },
                    onLongPress = { onObserveTile(tile.id) },
                )
            }
        }
    }
}

@Composable
private fun TileCell(
    tile: Tile?,
    animation: MoveAnimation?,
    selectedForLab: Boolean,
    selectedForTunnel: Boolean,
    tunnelingActive: Boolean,
    observerValue: Int?,
    reducedMotion: Boolean,
    mode: GameMode,
    boardSize: Int,
    cellSize: androidx.compose.ui.unit.Dp,
    stepPx: Float,
    modifier: Modifier,
    onDragToLab: (Long) -> Unit,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
) {
    val scale = remember(tile?.id) { Animatable(1f) }
    val alpha = remember(tile?.id) { Animatable(1f) }
    val burst = remember(tile?.id) { Animatable(0f) }
    val translationX = remember(tile?.id) { Animatable(0f) }
    val translationY = remember(tile?.id) { Animatable(0f) }
    LaunchedEffect(tile?.id, animation, reducedMotion, stepPx) {
        if (tile == null || reducedMotion) {
            scale.snapTo(1f)
            alpha.snapTo(1f)
            burst.snapTo(0f)
            translationX.snapTo(0f)
            translationY.snapTo(0f)
            return@LaunchedEffect
        }
        val from = animation?.fromIndex
        val to = animation?.toIndex
        if (from != null && to != null) {
            val fromRow = from / boardSize
            val fromCol = from % boardSize
            val toRow = to / boardSize
            val toCol = to % boardSize
            translationX.snapTo((fromCol - toCol) * stepPx)
            translationY.snapTo((fromRow - toRow) * stepPx)
            scale.snapTo(0.985f)
            val x = launch { translationX.animateTo(0f, tween(MOVE_ANIMATION_MS, easing = FastOutSlowInEasing)) }
            val y = launch { translationY.animateTo(0f, tween(MOVE_ANIMATION_MS, easing = FastOutSlowInEasing)) }
            val s = launch {
                scale.animateTo(1.025f, tween(MOVE_ANIMATION_MS / 2, easing = FastOutSlowInEasing))
                scale.animateTo(1f, tween(MOVE_ANIMATION_MS / 2, easing = FastOutSlowInEasing))
            }
            x.join()
            y.join()
            s.join()
        }
        when (animation?.kind) {
            MoveAnimationKind.MERGE -> {
                scale.snapTo(0.88f)
                burst.snapTo(0f)
                launch { burst.animateTo(1f, tween(280, easing = FastOutSlowInEasing)) }
                scale.animateTo(1.1f, tween(90, easing = FastOutSlowInEasing))
                scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
            }
            MoveAnimationKind.REACTION -> {
                scale.snapTo(0.82f)
                alpha.snapTo(0.72f)
                burst.snapTo(0f)
                launch { burst.animateTo(1f, tween(360, easing = FastOutSlowInEasing)) }
                scale.animateTo(1.16f, tween(110, easing = FastOutSlowInEasing))
                alpha.animateTo(1f, tween(120, easing = FastOutSlowInEasing))
                scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
            }
            MoveAnimationKind.ENTANGLEMENT -> {
                scale.snapTo(0.76f)
                alpha.snapTo(0.66f)
                burst.snapTo(0f)
                launch { burst.animateTo(1f, tween(360, easing = FastOutSlowInEasing)) }
                scale.animateTo(1.18f, tween(120, easing = FastOutSlowInEasing))
                alpha.animateTo(1f, tween(130, easing = FastOutSlowInEasing))
                scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
            }
            MoveAnimationKind.TUNNEL -> {
                scale.snapTo(0.7f)
                alpha.snapTo(0.5f)
                burst.snapTo(0f)
                launch { burst.animateTo(1f, tween(300, easing = FastOutSlowInEasing)) }
                alpha.animateTo(1f, tween(130, easing = FastOutSlowInEasing))
                scale.animateTo(1.08f, tween(120, easing = FastOutSlowInEasing))
                scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
            }
            MoveAnimationKind.COLLAPSE_LOW -> {
                scale.snapTo(0.86f)
                alpha.snapTo(0.76f)
                burst.snapTo(0f)
                launch { burst.animateTo(1f, tween(260, easing = FastOutSlowInEasing)) }
                alpha.animateTo(1f, tween(90, easing = FastOutSlowInEasing))
                scale.animateTo(1.06f, tween(90, easing = FastOutSlowInEasing))
                scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioLowBouncy))
            }
            MoveAnimationKind.COLLAPSE_HIGH -> {
                scale.snapTo(0.72f)
                alpha.snapTo(0.55f)
                burst.snapTo(0f)
                launch { burst.animateTo(1f, tween(380, easing = FastOutSlowInEasing)) }
                alpha.animateTo(1f, tween(150, easing = FastOutSlowInEasing))
                scale.animateTo(1.2f, tween(130, easing = FastOutSlowInEasing))
                scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
            }
            MoveAnimationKind.SPAWN -> {
                scale.snapTo(0.55f)
                alpha.snapTo(0f)
                alpha.animateTo(1f, tween(140, easing = FastOutSlowInEasing))
                scale.animateTo(1f, tween(160, easing = FastOutSlowInEasing))
            }
            MoveAnimationKind.SLIDE, null -> {
                scale.animateTo(1f, tween(40))
                alpha.animateTo(1f, tween(40))
            }
        }
    }

    val color = when {
        tile == null -> Color(0xFF171D38)
        mode == GameMode.QUANTUM && tile.kind == TileKind.ELEMENT -> elementColor(tile.element)
        mode == GameMode.QUANTUM -> tileKindColor(tile.kind)
        else -> classicTileColor(tile.value)
    }

    Box(
        modifier
            .size(cellSize)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                this.alpha = alpha.value
                this.translationX = translationX.value
                this.translationY = translationY.value
                shadowElevation = if (animation?.kind != null) 24f else 8f
            }
            .background(
                Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = if (tile?.kind == TileKind.ELEMENT) 0.26f else 0.14f),
                        color.copy(alpha = 0.92f),
                        Color.Black.copy(alpha = 0.18f),
                    ),
                ),
                RoundedCornerShape(14.dp),
            )
            .border(1.5.dp, color.copy(alpha = 0.92f), RoundedCornerShape(14.dp))
            .then(
                when {
                    animation?.kind == MoveAnimationKind.COLLAPSE_LOW -> Modifier.border(2.dp, Color(0xFF56E0B5), RoundedCornerShape(14.dp))
                    animation?.kind == MoveAnimationKind.COLLAPSE_HIGH -> Modifier.border(3.dp, RadiantGold, RoundedCornerShape(14.dp))
                    tile?.isHighlightedForSynthesis == true -> Modifier.border(3.dp, Electric, RoundedCornerShape(14.dp))
                    selectedForTunnel -> Modifier.border(3.dp, RadiantGold, RoundedCornerShape(14.dp))
                    tile?.entanglementGroupId != null -> Modifier.border(2.dp, NeonPink, RoundedCornerShape(14.dp))
                    tile?.kind == TileKind.ELEMENT -> Modifier.border(if (selectedForLab) 2.dp else 1.dp, if (selectedForLab) Cyan else Color.White.copy(alpha = 0.22f), RoundedCornerShape(14.dp))
                    else -> Modifier
                },
            )
            .then(
                when {
                    tile?.kind == TileKind.ELEMENT -> Modifier
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
                        .pointerInput(tile.id, tunnelingActive) {
                            detectTapGestures(
                                onTap = { if (tunnelingActive || mode == GameMode.CLASSIC) onTap() else onDragToLab(tile.id) },
                                onLongPress = { onLongPress() },
                            )
                        }
                    tile != null -> Modifier.pointerInput(tile.id) {
                        detectTapGestures(
                            onTap = { onTap() },
                            onLongPress = { onLongPress() },
                        )
                    }
                    else -> Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (burst.value > 0f && tile != null) {
            ParticleBurst(
                progress = burst.value,
                color = if (tile.kind == TileKind.ELEMENT) elementColor(tile.element) else color,
                intense = animation?.kind == MoveAnimationKind.REACTION || animation?.kind == MoveAnimationKind.COLLAPSE_HIGH,
            )
        }
        if (tile?.kind == TileKind.ELEMENT) {
            Canvas(Modifier.fillMaxSize()) {
                val glow = elementColor(tile.element).copy(alpha = 0.22f)
                drawCircle(glow, radius = size.minDimension * 0.54f)
                drawLine(
                    color = Color.White.copy(alpha = 0.26f),
                    start = androidx.compose.ui.geometry.Offset(size.width * 0.16f, size.height * 0.18f),
                    end = androidx.compose.ui.geometry.Offset(size.width * 0.72f, size.height * 0.12f),
                    strokeWidth = 1.4f,
                    cap = StrokeCap.Round,
                )
                if (tile.element == QuantumElement.GOLD) {
                    repeat(6) { index ->
                        val angle = index * 1.04f
                        val x = size.width * 0.5f + cos(angle) * size.width * 0.28f
                        val y = size.height * 0.5f + sin(angle) * size.height * 0.24f
                        drawCircle(Color.White.copy(alpha = 0.72f), radius = 1.7f, center = androidx.compose.ui.geometry.Offset(x, y))
                    }
                }
            }
        }
        when {
            tile == null -> Unit
            mode == GameMode.QUANTUM -> QuantumTileLabel(tile, boardSize, observerValue)
            else -> Text(formatNumber(tile.value), fontSize = if (tile.value < 1000) 26.sp else 20.sp, fontWeight = FontWeight.Black, color = classicTileTextColor(tile.value))
        }
    }
}

@Composable
private fun ParticleBurst(progress: Float, color: Color, intense: Boolean) {
    Canvas(Modifier.fillMaxSize()) {
        val center = androidx.compose.ui.geometry.Offset(size.width * 0.5f, size.height * 0.5f)
        val rayCount = if (intense) 18 else 12
        val maxRadius = size.minDimension * if (intense) 0.72f else 0.54f
        val alpha = (1f - progress).coerceIn(0f, 1f)
        repeat(rayCount) { index ->
            val angle = (index.toFloat() / rayCount.toFloat()) * 6.28318f
            val startRadius = maxRadius * progress * 0.22f
            val endRadius = maxRadius * progress
            val start = androidx.compose.ui.geometry.Offset(
                center.x + cos(angle) * startRadius,
                center.y + sin(angle) * startRadius,
            )
            val end = androidx.compose.ui.geometry.Offset(
                center.x + cos(angle) * endRadius,
                center.y + sin(angle) * endRadius,
            )
            drawLine(
                color = color.copy(alpha = alpha * 0.8f),
                start = start,
                end = end,
                strokeWidth = if (intense) 2.2f else 1.4f,
                cap = StrokeCap.Round,
            )
            drawCircle(
                color = Color.White.copy(alpha = alpha * 0.72f),
                radius = if (intense) 2.3f else 1.5f,
                center = end,
            )
        }
        drawCircle(
            color = color.copy(alpha = alpha * 0.22f),
            radius = maxRadius * progress,
            center = center,
            style = Stroke(width = if (intense) 4f else 2.4f),
        )
    }
}

private const val MOVE_ANIMATION_MS = 270

@Composable
private fun QuantumTileLabel(tile: Tile, boardSize: Int, observerValue: Int?) {
    val symbolSize = if (tile.superpositionValues.isNotEmpty()) {
        if (boardSize >= 8) 8.sp else if (boardSize >= 6) 10.sp else 12.sp
    } else if (boardSize >= 8) 13.sp else if (boardSize >= 6) 17.sp else 21.sp
    val valueSize = if (boardSize >= 8) 10.sp else if (boardSize >= 6) 12.sp else 15.sp
    val rankSize = if (boardSize >= 8) 7.sp else 9.sp
    val familySize = if (boardSize >= 8) 0.sp else 7.sp
    Column(Modifier.fillMaxWidth().padding(if (boardSize >= 8) 2.dp else 5.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(formatNumber(FusionRules.rankOf(tile)), modifier = Modifier.align(Alignment.Start), color = TextSecondary, fontSize = rankSize, fontWeight = FontWeight.Bold)
        Text(observerValue?.let { formatNumber(it) } ?: FusionRules.displaySymbol(tile), fontSize = symbolSize, fontWeight = FontWeight.Black, color = Color.White, textAlign = TextAlign.Center)
        Text(formatNumber(FusionRules.gameValueOf(tile)), color = Cyan, fontSize = valueSize, fontWeight = FontWeight.Black)
        if (boardSize < 8) tile.element?.let { Text(elementFamily(it), color = TextSecondary, fontSize = familySize, textAlign = TextAlign.Center) }
    }
}

@Composable
private fun SuperpositionDialog(tile: Tile, onDismiss: () -> Unit, onCollapse: (Int) -> Unit) {
    QuantumDialog(
        title = stringResource(R.string.superposition_title),
        onDismiss = onDismiss,
        accent = Electric,
        dismissText = stringResource(R.string.cancel),
    ) {
        Text(stringResource(R.string.superposition_body), color = TextSecondary, fontSize = 13.sp)
        tile.superpositionValues.forEachIndexed { index, value ->
            QuantumActionButton(
                text = stringResource(R.string.superposition_choice, formatNumber(value), formatNumber(FusionRules.superpositionCollapseEnergyCosts[index])),
                onClick = { onCollapse(index) },
                modifier = Modifier.fillMaxWidth(),
                accent = actionAccent(index),
            )
        }
    }
}

@Composable
private fun DuelPlayer.label(): String = when (this) {
    DuelPlayer.PLAYER_ONE -> stringResource(R.string.player_one)
    DuelPlayer.PLAYER_TWO -> stringResource(R.string.player_two)
}

private fun DuelPlayer.opponent(): DuelPlayer = when (this) {
    DuelPlayer.PLAYER_ONE -> DuelPlayer.PLAYER_TWO
    DuelPlayer.PLAYER_TWO -> DuelPlayer.PLAYER_ONE
}

@Composable
private fun EndDialog(game: GameState, continueGame: () -> Unit, newGame: () -> Unit, audio: GameAudio, settings: AppSettings) {
    val status = game.status
    val context = LocalContext.current
    val bestElement = game.cells.mapNotNull { it?.element }.maxByOrNull { it.rank }
    QuantumDialog(
        title = if (status == GameStatus.WON) stringResource(R.string.target_reached) else stringResource(R.string.game_over),
        onDismiss = {},
        accent = if (status == GameStatus.WON) RadiantGold else NeonPink,
        confirmText = if (status == GameStatus.WON) stringResource(R.string.continue_game) else stringResource(R.string.new_game),
        onConfirm = if (status == GameStatus.WON) continueGame else newGame,
    ) {
        Text(if (status == GameStatus.WON) stringResource(R.string.win_body) else stringResource(R.string.lose_body), color = Color.White)
        Text(stringResource(R.string.difficulty_line, game.difficulty.localizedLabel()), color = TextSecondary)
        Text(stringResource(R.string.score_line, formatNumber(game.score)), color = TextSecondary)
        if (game.difficulty == Difficulty.DAILY) Text(stringResource(R.string.daily_best_line, formatNumber(game.dailyBestScore)), color = TextSecondary)
        Text(stringResource(R.string.moves_line, formatNumber(game.moveCount)), color = TextSecondary)
        Text(stringResource(R.string.best_element_line, bestElement?.symbol ?: stringResource(R.string.none)), color = TextSecondary)
        QuantumActionButton(
            text = stringResource(R.string.share_result),
            onClick = {
                if (settings.soundEnabled) audio.share()
                shareGameResult(context, game)
            },
            modifier = Modifier.fillMaxWidth(),
            accent = Electric,
        )
    }
}

private fun shareGameResult(context: android.content.Context, game: GameState) {
    val imageUri = createShareImageUri(context, game)
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, imageUri)
        putExtra(Intent.EXTRA_TEXT, sharePromptFor(game))
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(
        Intent.createChooser(sendIntent, context.getString(R.string.share_result)),
    )
}
