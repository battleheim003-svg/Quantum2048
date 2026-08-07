package com.battleheim.quantum2048.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.battleheim.quantum2048.R
import com.battleheim.quantum2048.audio.ToneGameAudio
import com.battleheim.quantum2048.designsystem.Cyan
import com.battleheim.quantum2048.designsystem.PanelRaised
import com.battleheim.quantum2048.designsystem.Panel
import com.battleheim.quantum2048.designsystem.TextMuted
import com.battleheim.quantum2048.designsystem.TextSecondary
import com.battleheim.quantum2048.designsystem.Void
import com.battleheim.quantum2048.designsystem.difficultyAccent
import com.battleheim.quantum2048.designsystem.difficultySurface
import com.battleheim.quantum2048.domain.CollectionRepository
import com.battleheim.quantum2048.domain.GameRepository
import com.battleheim.quantum2048.domain.SettingsRepository
import com.battleheim.quantum2048.engine.Difficulty
import com.battleheim.quantum2048.engine.FusionRules
import com.battleheim.quantum2048.engine.GameEngine
import com.battleheim.quantum2048.engine.BotDifficulty
import com.battleheim.quantum2048.engine.DuelOpponent
import kotlinx.coroutines.launch

private object Routes {
    const val MainMenu = "main_menu"
    const val LevelSelect = "level_select"
    const val Collection = "collection"
    const val Settings = "settings"
    const val Game = "game/{difficulty}/{size}"
    const val DuelGame = "duel/{difficulty}"
    const val Pause = "pause"

    fun game(difficulty: Difficulty, size: Int) = "game/${difficulty.name}/$size"
    fun duel(difficulty: Difficulty) = "duel/${difficulty.name}"
}

@Composable
fun QuantumAppShell(
    gameRepository: GameRepository,
    collectionRepository: CollectionRepository,
    settingsRepository: SettingsRepository,
    engine: GameEngine,
) {
    val nav = rememberNavController()
    val gameViewModel: GameViewModel = viewModel { GameViewModel(gameRepository, collectionRepository, engine) }
    val settings by settingsRepository.observe().collectAsState(initial = com.battleheim.quantum2048.domain.AppSettings())
    val audio = remember { ToneGameAudio() }
    DisposableEffect(Unit) { onDispose { audio.release() } }

    NavHost(navController = nav, startDestination = Routes.MainMenu) {
        composable(Routes.MainMenu) {
            MainMenuScreen(
                vm = gameViewModel,
                onContinue = { saved -> nav.navigate(Routes.game(saved.difficulty, saved.size)) },
                onNewGame = { nav.navigate(Routes.LevelSelect) },
                onCollection = { nav.navigate(Routes.Collection) },
                onSettings = { nav.navigate(Routes.Settings) },
            )
        }
        composable(Routes.LevelSelect) {
            LevelSelectScreen(
                vm = gameViewModel,
                onBack = { nav.popBackStack() },
                onSelect = { difficulty, size, duel, opponent, botDifficulty ->
                    if (duel) {
                        gameViewModel.newDuel(difficulty, opponent, botDifficulty)
                        nav.navigate(Routes.duel(difficulty))
                    } else {
                        gameViewModel.newGame(difficulty, size)
                        nav.navigate(Routes.game(difficulty, size))
                    }
                },
            )
        }
        composable(
            route = Routes.DuelGame,
            arguments = listOf(navArgument("difficulty") { type = NavType.StringType }),
        ) {
            GameScreen(vm = gameViewModel, settings = settings, audio = audio, onPause = { nav.navigate(Routes.Pause) })
        }
        composable(
            route = Routes.Game,
            arguments = listOf(
                navArgument("difficulty") { type = NavType.StringType },
                navArgument("size") { type = NavType.IntType },
            ),
        ) { backStack ->
            val difficulty = Difficulty.valueOf(backStack.arguments?.getString("difficulty") ?: Difficulty.QUANTUM.name)
            val size = backStack.arguments?.getInt("size") ?: 4
            LaunchedEffect(difficulty, size) { gameViewModel.loadDifficulty(difficulty, size) }
            GameScreen(vm = gameViewModel, settings = settings, audio = audio, onPause = { nav.navigate(Routes.Pause) })
        }
        composable(Routes.Pause) {
            PauseScreen(
                vm = gameViewModel,
                onResume = { nav.popBackStack() },
                onMainMenu = {
                    nav.navigate(Routes.MainMenu) { popUpTo(Routes.MainMenu) { inclusive = true } }
                },
            )
        }
        composable(Routes.Collection) {
            CollectionScreen(collectionRepository, onBack = { nav.popBackStack() })
        }
        composable(Routes.Settings) {
            SettingsScreen(
                settingsRepository = settingsRepository,
                collectionRepository = collectionRepository,
                vm = gameViewModel,
                onBack = { nav.popBackStack() },
            )
        }
    }
}

@Composable
private fun MainMenuScreen(
    vm: GameViewModel,
    onContinue: (SavedGameKey) -> Unit,
    onNewGame: () -> Unit,
    onCollection: () -> Unit,
    onSettings: () -> Unit,
) {
    var saves by remember { mutableStateOf(emptySet<SavedGameKey>()) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { saves = vm.savedGames() }
    val continueSave = saves.lastOrNull() ?: SavedGameKey(Difficulty.QUANTUM, 4)

    MenuScaffold {
        SectionTitle(stringResource(R.string.app_title), stringResource(R.string.fusion_lab))
        Button(
            onClick = { onContinue(continueSave) },
            enabled = saves.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().testTag("continue_button"),
        ) { Text(stringResource(R.string.continue_game)) }
        Button(onClick = onNewGame, modifier = Modifier.fillMaxWidth().testTag("new_game_button")) { Text(stringResource(R.string.new_game)) }
        OutlinedButton(onClick = onCollection, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.collection)) }
        OutlinedButton(onClick = onSettings, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.settings)) }
        TextButton(onClick = { scope.launch { saves = vm.savedGames() } }) { Text(stringResource(R.string.refresh_saves)) }
    }
}

@Composable
private fun LevelSelectScreen(vm: GameViewModel, onBack: () -> Unit, onSelect: (Difficulty, Int, Boolean, DuelOpponent, BotDifficulty) -> Unit) {
    var saves by remember { mutableStateOf(emptySet<SavedGameKey>()) }
    var selectedSize by remember { mutableStateOf(4) }
    var duel by remember { mutableStateOf(false) }
    var opponent by remember { mutableStateOf(DuelOpponent.BOT) }
    var botDifficulty by remember { mutableStateOf(BotDifficulty.NORMAL) }
    LaunchedEffect(Unit) { saves = vm.savedGames() }
    MenuScaffold {
        SectionTitle(stringResource(R.string.new_game), stringResource(R.string.choose_rules_size))
        ModeSelector(duel, onSelect = { duel = it; if (it) selectedSize = 4 })
        BoardSizeSelector(selectedSize, enabled = !duel, onSelect = { selectedSize = it })
        if (duel) {
            DuelSelector(opponent, botDifficulty, onOpponent = { opponent = it }, onBot = { botDifficulty = it })
        }
        Difficulty.entries.forEach { difficulty ->
            DifficultyCard(
                difficulty = difficulty,
                size = selectedSize,
                hasSave = SavedGameKey(difficulty, selectedSize) in saves,
                description = difficultyDescription(difficulty),
                onClick = { onSelect(difficulty, selectedSize, duel, opponent, botDifficulty) },
            )
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.back)) }
    }
}

@Composable
private fun ModeSelector(duel: Boolean, onSelect: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { onSelect(false) }, modifier = Modifier.weight(1f)) { Text(stringResource(if (!duel) R.string.solo_selected else R.string.solo)) }
        Button(onClick = { onSelect(true) }, modifier = Modifier.weight(1f).testTag("mode_duel")) { Text(stringResource(if (duel) R.string.duel_selected else R.string.duel)) }
    }
}

@Composable
private fun BoardSizeSelector(selected: Int, enabled: Boolean, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FusionRules.supportedBoardSizes.forEach { size ->
            val active = size == selected
            Button(
                onClick = { onSelect(size) },
                enabled = enabled || active,
                modifier = Modifier.weight(1f).testTag("board_size_${size}x$size"),
            ) {
                Text(stringResource(if (active) R.string.board_size_selected else R.string.board_size, size), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun DuelSelector(opponent: DuelOpponent, botDifficulty: BotDifficulty, onOpponent: (DuelOpponent) -> Unit, onBot: (BotDifficulty) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onOpponent(DuelOpponent.BOT) }, modifier = Modifier.weight(1f).testTag("duel_bot")) { Text(stringResource(if (opponent == DuelOpponent.BOT) R.string.bot_selected else R.string.bot)) }
            Button(onClick = { onOpponent(DuelOpponent.PASS_AND_PLAY) }, modifier = Modifier.weight(1f).testTag("duel_pass")) { Text(stringResource(if (opponent == DuelOpponent.PASS_AND_PLAY) R.string.pass_play_selected else R.string.pass_play)) }
        }
        if (opponent == DuelOpponent.BOT) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BotDifficulty.entries.forEach { level ->
                    Button(onClick = { onBot(level) }, modifier = Modifier.weight(1f).testTag("bot_${level.name.lowercase()}")) {
                        Text(level.label(), fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun BotDifficulty.label(): String = when (this) {
    BotDifficulty.EASY -> stringResource(R.string.easy)
    BotDifficulty.NORMAL -> stringResource(R.string.normal)
    BotDifficulty.QUANTUM_HARD -> stringResource(R.string.quantum)
}

@Composable
private fun DifficultyCard(difficulty: Difficulty, size: Int, hasSave: Boolean, description: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = difficultySurface(difficulty)),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, difficultyAccent(difficulty), RoundedCornerShape(8.dp))
            .testTag("level_${difficulty.name.lowercase()}"),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(14.dp).background(difficultyAccent(difficulty), RoundedCornerShape(3.dp)))
                    Text(difficulty.name.lowercase().replaceFirstChar { it.uppercase() }, fontWeight = FontWeight.Black)
                }
                Text(if (hasSave) stringResource(R.string.saved_size, size) else stringResource(R.string.board_size, size), color = difficultyAccent(difficulty), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Text(description, color = TextSecondary, fontSize = 13.sp)
        }
    }
}

@Composable
private fun CollectionScreen(repository: CollectionRepository, onBack: () -> Unit) {
    val state by repository.observe().collectAsState(initial = com.battleheim.quantum2048.domain.CollectionState())
    val codex = state.codex(FusionRules.compoundRecipes)
    MenuScaffold {
        SectionTitle(stringResource(R.string.collection), stringResource(R.string.fusion_lab))
        codex.forEach { entry ->
            Card(colors = CardDefaults.cardColors(containerColor = if (entry.discovered) PanelRaised else Panel), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(entry.symbol, fontWeight = FontWeight.Black, color = if (entry.discovered) Color.White else TextMuted)
                    Text(
                        if (entry.discovered) "${entry.englishName} / ${entry.persianName} x${entry.discoveryCount}" else stringResource(R.string.none),
                        color = if (entry.discovered) TextSecondary else TextMuted,
                        fontSize = 12.sp,
                    )
                }
            }
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.back)) }
    }
}

@Composable
private fun SettingsScreen(
    settingsRepository: SettingsRepository,
    collectionRepository: CollectionRepository,
    vm: GameViewModel,
    onBack: () -> Unit,
) {
    val settings by settingsRepository.observe().collectAsState(initial = com.battleheim.quantum2048.domain.AppSettings())
    val scope = rememberCoroutineScope()
    var confirmResetCollection by remember { mutableStateOf(false) }
    var confirmResetDifficulty by remember { mutableStateOf<Difficulty?>(null) }

    MenuScaffold {
        SectionTitle(stringResource(R.string.settings), stringResource(R.string.fusion_lab))
        SettingsToggle(stringResource(R.string.sound), settings.soundEnabled) { scope.launch { settingsRepository.save(settings.copy(soundEnabled = it)) } }
        SettingsToggle(stringResource(R.string.music), settings.musicEnabled) { scope.launch { settingsRepository.save(settings.copy(musicEnabled = it)) } }
        SettingsToggle(stringResource(R.string.haptics), settings.hapticsEnabled) { scope.launch { settingsRepository.save(settings.copy(hapticsEnabled = it)) } }
        SettingsToggle(stringResource(R.string.reduced_motion), settings.reducedMotion) { scope.launch { settingsRepository.save(settings.copy(reducedMotion = it)) } }
        Text(stringResource(R.string.language_note), color = TextSecondary, fontSize = 12.sp)
        OutlinedButton(onClick = { confirmResetCollection = true }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.reset_collection)) }
        Difficulty.entries.forEach { difficulty ->
            OutlinedButton(onClick = { confirmResetDifficulty = difficulty }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.reset_progress, difficulty.name.lowercase())) }
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.back)) }
    }

    if (confirmResetCollection) {
        ConfirmDialog(
            title = "Reset collection?",
            body = "Discovered compounds will be cleared.",
            onDismiss = { confirmResetCollection = false },
            onConfirm = {
                scope.launch { collectionRepository.clear() }
                confirmResetCollection = false
            },
        )
    }
    confirmResetDifficulty?.let { difficulty ->
        ConfirmDialog(
            title = "Reset ${difficulty.name.lowercase()}?",
            body = "The active save for this level will be deleted.",
            onDismiss = { confirmResetDifficulty = null },
            onConfirm = {
                vm.resetDifficulty(difficulty)
                confirmResetDifficulty = null
            },
        )
    }
}

@Composable
private fun PauseScreen(vm: GameViewModel, onResume: () -> Unit, onMainMenu: () -> Unit) {
    val ui by vm.ui.collectAsState()
    var confirmRestart by remember { mutableStateOf(false) }
    MenuScaffold(modifier = Modifier.testTag("pause_screen")) {
        SectionTitle(stringResource(R.string.pause), ui.game.difficulty.name.lowercase().replaceFirstChar { it.uppercase() })
        Button(onClick = onResume, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.resume)) }
        OutlinedButton(onClick = { confirmRestart = true }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.new_game)) }
        OutlinedButton(onClick = onMainMenu, modifier = Modifier.fillMaxWidth().testTag("pause_main_menu")) { Text(stringResource(R.string.main_menu)) }
    }
    if (confirmRestart) {
        ConfirmDialog(
            title = "Restart level?",
            body = "Current progress for this run will be lost.",
            onDismiss = { confirmRestart = false },
            onConfirm = {
                vm.newGame(ui.game.difficulty)
                confirmRestart = false
                onResume()
            },
        )
    }
}

@Composable
private fun SettingsToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().background(PanelRaised, RoundedCornerShape(8.dp)).padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontWeight = FontWeight.Bold)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ConfirmDialog(title: String, body: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = { Button(onClick = onConfirm) { Text(stringResource(R.string.confirm)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun MenuScaffold(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Scaffold(containerColor = Void) { padding ->
        Column(
            modifier
                .fillMaxSize()
                .background(Void)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, fontSize = 30.sp, fontWeight = FontWeight.Black)
        Text(subtitle, color = Cyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
private fun difficultyDescription(difficulty: Difficulty): String = when (difficulty) {
    Difficulty.EASY -> stringResource(R.string.classic_description)
    Difficulty.MEDIUM -> stringResource(R.string.medium_description)
    Difficulty.HARD -> stringResource(R.string.hard_description)
    Difficulty.QUANTUM -> stringResource(R.string.quantum_description)
}
