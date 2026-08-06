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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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
import com.battleheim.quantum2048.engine.GameEngine
import com.battleheim.quantum2048.engine.QuantumBalance
import kotlinx.coroutines.launch

private object Routes {
    const val MainMenu = "main_menu"
    const val LevelSelect = "level_select"
    const val Collection = "collection"
    const val Settings = "settings"
    const val Game = "game/{difficulty}"
    const val Pause = "pause"

    fun game(difficulty: Difficulty) = "game/${difficulty.name}"
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
                onContinue = { difficulty -> nav.navigate(Routes.game(difficulty)) },
                onNewGame = { nav.navigate(Routes.LevelSelect) },
                onCollection = { nav.navigate(Routes.Collection) },
                onSettings = { nav.navigate(Routes.Settings) },
            )
        }
        composable(Routes.LevelSelect) {
            LevelSelectScreen(
                vm = gameViewModel,
                balance = engine.balance,
                onBack = { nav.popBackStack() },
                onSelect = { difficulty ->
                    gameViewModel.newGame(difficulty)
                    nav.navigate(Routes.game(difficulty))
                },
            )
        }
        composable(
            route = Routes.Game,
            arguments = listOf(navArgument("difficulty") { type = NavType.StringType }),
        ) { backStack ->
            val difficulty = Difficulty.valueOf(backStack.arguments?.getString("difficulty") ?: Difficulty.QUANTUM.name)
            LaunchedEffect(difficulty) { gameViewModel.loadDifficulty(difficulty) }
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
            CollectionScreen(collectionRepository, engine.balance, onBack = { nav.popBackStack() })
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
    onContinue: (Difficulty) -> Unit,
    onNewGame: () -> Unit,
    onCollection: () -> Unit,
    onSettings: () -> Unit,
) {
    var saves by remember { mutableStateOf(emptySet<Difficulty>()) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { saves = vm.savedDifficulties() }
    val continueDifficulty = saves.lastOrNull() ?: Difficulty.QUANTUM

    MenuScaffold {
        SectionTitle("Quantum 2048", "Collapse Lab")
        Button(
            onClick = { onContinue(continueDifficulty) },
            enabled = saves.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().testTag("continue_button"),
        ) { Text("Continue") }
        Button(onClick = onNewGame, modifier = Modifier.fillMaxWidth().testTag("new_game_button")) { Text("New game") }
        OutlinedButton(onClick = onCollection, modifier = Modifier.fillMaxWidth()) { Text("Collection") }
        OutlinedButton(onClick = onSettings, modifier = Modifier.fillMaxWidth()) { Text("Settings") }
        TextButton(onClick = { scope.launch { saves = vm.savedDifficulties() } }) { Text("Refresh saves") }
    }
}

@Composable
private fun LevelSelectScreen(vm: GameViewModel, balance: QuantumBalance, onBack: () -> Unit, onSelect: (Difficulty) -> Unit) {
    var saves by remember { mutableStateOf(emptySet<Difficulty>()) }
    LaunchedEffect(Unit) { saves = vm.savedDifficulties() }
    MenuScaffold {
        SectionTitle("Select level", "Choose the lab rules for this run")
        Difficulty.entries.forEach { difficulty ->
            DifficultyCard(
                difficulty = difficulty,
                hasSave = difficulty in saves,
                description = difficultyDescription(difficulty, balance),
                onClick = { onSelect(difficulty) },
            )
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
    }
}

@Composable
private fun DifficultyCard(difficulty: Difficulty, hasSave: Boolean, description: String, onClick: () -> Unit) {
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
                Text(if (hasSave) "Saved" else "New", color = difficultyAccent(difficulty), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Text(description, color = TextSecondary, fontSize = 13.sp)
        }
    }
}

@Composable
private fun CollectionScreen(repository: CollectionRepository, balance: QuantumBalance, onBack: () -> Unit) {
    val state by repository.observe().collectAsState(initial = com.battleheim.quantum2048.domain.CollectionState())
    val codex = state.codex(balance.compoundRecipes)
    MenuScaffold {
        SectionTitle("Collection", "Discovered compounds and locked silhouettes")
        codex.forEach { entry ->
            Card(colors = CardDefaults.cardColors(containerColor = if (entry.discovered) PanelRaised else Panel), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(entry.symbol, fontWeight = FontWeight.Black, color = if (entry.discovered) Color.White else TextMuted)
                    Text(
                        if (entry.discovered) "${entry.englishName} / ${entry.persianName} x${entry.discoveryCount}" else "Locked compound",
                        color = if (entry.discovered) TextSecondary else TextMuted,
                        fontSize = 12.sp,
                    )
                }
            }
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
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
        SectionTitle("Settings", "Audio, feedback, and progress controls")
        SettingsToggle("Sound", settings.soundEnabled) { scope.launch { settingsRepository.save(settings.copy(soundEnabled = it)) } }
        SettingsToggle("Music", settings.musicEnabled) { scope.launch { settingsRepository.save(settings.copy(musicEnabled = it)) } }
        SettingsToggle("Haptics", settings.hapticsEnabled) { scope.launch { settingsRepository.save(settings.copy(hapticsEnabled = it)) } }
        SettingsToggle("Reduced motion", settings.reducedMotion) { scope.launch { settingsRepository.save(settings.copy(reducedMotion = it)) } }
        Text("Language switching is deferred because localized string resources need repair.", color = TextSecondary, fontSize = 12.sp)
        OutlinedButton(onClick = { confirmResetCollection = true }, modifier = Modifier.fillMaxWidth()) { Text("Reset collection") }
        Difficulty.entries.forEach { difficulty ->
            OutlinedButton(onClick = { confirmResetDifficulty = difficulty }, modifier = Modifier.fillMaxWidth()) { Text("Reset ${difficulty.name.lowercase()} progress") }
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
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
        SectionTitle("Paused", ui.game.difficulty.name.lowercase().replaceFirstChar { it.uppercase() })
        Button(onClick = onResume, modifier = Modifier.fillMaxWidth()) { Text("Resume") }
        OutlinedButton(onClick = { confirmRestart = true }, modifier = Modifier.fillMaxWidth()) { Text("Restart ${ui.game.difficulty.name.lowercase()}") }
        OutlinedButton(onClick = onMainMenu, modifier = Modifier.fillMaxWidth().testTag("pause_main_menu")) { Text("Main menu") }
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
        confirmButton = { Button(onClick = onConfirm) { Text("Confirm") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
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

private fun difficultyDescription(difficulty: Difficulty, balance: QuantumBalance): String {
    val rules = balance.rulesFor(difficulty)
    return when (difficulty) {
        Difficulty.EASY -> "Classic 2048. No lab, energy, particles, or collapse."
        Difficulty.MEDIUM -> "Particles and light fusion to ${rules.maxFusionSpecies?.symbol}; simple compounds."
        Difficulty.HARD -> "Full element chain, full Compound Lab, energy cost ${rules.compoundEnergyCost}, no collapse."
        Difficulty.QUANTUM -> "Full synthesis with unresolved tiles, collapse, energy, and all compounds."
    }
}
