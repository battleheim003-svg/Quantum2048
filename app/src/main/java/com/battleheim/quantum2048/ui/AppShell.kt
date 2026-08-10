package com.battleheim.quantum2048.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
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
import com.battleheim.quantum2048.ads.AdGateway
import com.battleheim.quantum2048.ads.NoOpAdGateway
import com.battleheim.quantum2048.ads.RewardPlacement
import com.battleheim.quantum2048.analytics.AnalyticsGateway
import com.battleheim.quantum2048.analytics.NoOpAnalyticsGateway
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
import com.battleheim.quantum2048.domain.BillingRepository
import com.battleheim.quantum2048.domain.EntitlementState
import com.battleheim.quantum2048.domain.GameRepository
import com.battleheim.quantum2048.domain.AppLanguage
import com.battleheim.quantum2048.domain.AppSettings
import com.battleheim.quantum2048.domain.AppThemeMode
import com.battleheim.quantum2048.domain.ProfileRepository
import com.battleheim.quantum2048.domain.ProductIds
import com.battleheim.quantum2048.domain.RewardEntitlement
import com.battleheim.quantum2048.domain.SettingsRepository
import com.battleheim.quantum2048.engine.Difficulty
import com.battleheim.quantum2048.engine.FusionRules
import com.battleheim.quantum2048.engine.GameEngine
import com.battleheim.quantum2048.engine.BotDifficulty
import com.battleheim.quantum2048.engine.DuelOpponent
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.time.LocalDate

private object Routes {
    const val MainMenu = "main_menu"
    const val LevelSelect = "level_select"
    const val Collection = "collection"
    const val Statistics = "statistics"
    const val Tutorial = "tutorial"
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
    profileRepository: ProfileRepository,
    settingsRepository: SettingsRepository,
    billingRepository: BillingRepository,
    adGateway: AdGateway = NoOpAdGateway,
    analytics: AnalyticsGateway = NoOpAnalyticsGateway,
    engine: GameEngine,
) {
    val nav = rememberNavController()
    val gameViewModel: GameViewModel = viewModel { GameViewModel(gameRepository, collectionRepository, profileRepository, engine, analytics) }
    val ui by gameViewModel.ui.collectAsState()
    val settings by settingsRepository.observe().collectAsState(initial = com.battleheim.quantum2048.domain.AppSettings())
    val audio = remember { ToneGameAudio() }
    var tutorialPrompted by remember { mutableStateOf(false) }
    var showSplash by remember { mutableStateOf(true) }
    DisposableEffect(Unit) { onDispose { audio.release() } }
    LaunchedEffect(Unit) {
        delay(SPLASH_MS)
        showSplash = false
    }
    LaunchedEffect(ui.loading, ui.game.tutorialCompleted) {
        if (!showSplash && !ui.loading && !ui.game.tutorialCompleted && !tutorialPrompted) {
            tutorialPrompted = true
            nav.navigate(Routes.Tutorial)
        }
    }

    if (showSplash) {
        SplashScreen()
        return
    }

    NavHost(navController = nav, startDestination = Routes.MainMenu) {
        composable(Routes.MainMenu) {
            MainMenuScreen(
                vm = gameViewModel,
                settings = settings,
                settingsRepository = settingsRepository,
                analytics = analytics,
                audio = audio,
                onContinue = { saved -> playSelect(audio, settings); nav.navigate(Routes.game(saved.difficulty, saved.size)) },
                onNewGame = { playSelect(audio, settings); nav.navigate(Routes.LevelSelect) },
                onCollection = { playMenu(audio, settings); nav.navigate(Routes.Collection) },
                onStatistics = { playMenu(audio, settings); nav.navigate(Routes.Statistics) },
                onTutorial = { playMenu(audio, settings); nav.navigate(Routes.Tutorial) },
                onSettings = { playMenu(audio, settings); nav.navigate(Routes.Settings) },
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
            CollectionScreen(collectionRepository, profileRepository, onBack = { nav.popBackStack() })
        }
        composable(Routes.Statistics) {
            StatisticsScreen(profileRepository, onBack = { nav.popBackStack() })
        }
        composable(Routes.Tutorial) {
            TutorialScreen(
                vm = gameViewModel,
                onDone = {
                    gameViewModel.completeTutorial()
                    nav.popBackStack()
                },
            )
        }
        composable(Routes.Settings) {
            SettingsScreen(
                settingsRepository = settingsRepository,
                collectionRepository = collectionRepository,
                profileRepository = profileRepository,
                billingRepository = billingRepository,
                settings = settings,
                entitlements = billingRepository.observe().collectAsState(initial = EntitlementState()).value,
                adGateway = adGateway,
                audio = audio,
                vm = gameViewModel,
                analytics = analytics,
                onBack = { nav.popBackStack() },
            )
        }
    }
}

@Composable
private fun MainMenuScreen(
    vm: GameViewModel,
    settings: AppSettings,
    settingsRepository: SettingsRepository,
    analytics: AnalyticsGateway,
    audio: ToneGameAudio,
    onContinue: (SavedGameKey) -> Unit,
    onNewGame: () -> Unit,
    onCollection: () -> Unit,
    onStatistics: () -> Unit,
    onTutorial: () -> Unit,
    onSettings: () -> Unit,
) {
    var saves by remember { mutableStateOf(emptySet<SavedGameKey>()) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { saves = vm.savedGames() }
    val continueSave = saves.lastOrNull() ?: SavedGameKey(Difficulty.QUANTUM, 4)

    MenuScaffold {
        QuickSettingsRow(
            settings = settings,
            onLanguage = {
                playMenu(audio, settings)
                val next = settings.copy(language = settings.language.next())
                analytics.logSettingsChanged(next.themeMode, next.language)
                scope.launch { settingsRepository.save(next) }
            },
            onTheme = {
                playMenu(audio, settings)
                val next = settings.copy(themeMode = settings.themeMode.next())
                analytics.logSettingsChanged(next.themeMode, next.language)
                scope.launch { settingsRepository.save(next) }
            },
        )
        SectionTitle(stringResource(R.string.app_title), stringResource(R.string.fusion_lab))
        Button(
            onClick = { onContinue(continueSave) },
            enabled = saves.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().testTag("continue_button"),
        ) { Text(stringResource(R.string.continue_game)) }
        Button(onClick = onNewGame, modifier = Modifier.fillMaxWidth().testTag("new_game_button")) { Text(stringResource(R.string.new_game)) }
        OutlinedButton(onClick = onCollection, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.collection)) }
        OutlinedButton(onClick = onStatistics, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.statistics)) }
        OutlinedButton(onClick = onTutorial, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.tutorial)) }
        OutlinedButton(onClick = onSettings, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.settings)) }
        TextButton(onClick = { playMenu(audio, settings); scope.launch { saves = vm.savedGames() } }) { Text(stringResource(R.string.refresh_saves)) }
    }
}

@Composable
private fun SplashScreen() {
    Box(Modifier.fillMaxSize().background(Color(0xFFFF9800)), contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(R.drawable.splash_logo),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun QuickSettingsRow(settings: AppSettings, onLanguage: () -> Unit, onTheme: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(onClick = onLanguage, modifier = Modifier.testTag("quick_language")) {
            Text(settings.language.shortLabel(), fontSize = 12.sp)
        }
        OutlinedButton(onClick = onTheme, modifier = Modifier.padding(start = 8.dp).testTag("quick_theme")) {
            Text(settings.themeMode.iconLabel(), fontSize = 14.sp)
        }
    }
}

@Composable
private fun TutorialScreen(vm: GameViewModel, onDone: () -> Unit) {
    var step by remember { mutableStateOf(0) }
    var completedAction by remember(step) { mutableStateOf(false) }
    val titles = listOf(
        R.string.tutorial_step_move,
        R.string.tutorial_step_quantum,
        R.string.tutorial_step_collapse,
        R.string.tutorial_step_energy,
        R.string.tutorial_step_auto,
        R.string.tutorial_step_undo,
        R.string.tutorial_step_new_features,
    )
    val boards = listOf(
        listOf("2", "2", "", "", "", "", "", "", "", "", "", "", "", "", "", ""),
        listOf("e-", "p+", "", "", "", "", "", "", "", "", "", "", "", "", "", ""),
        listOf("1 | 2 | 4", "", "", "", "", "", "", "", "", "", "", "", "", "", "", ""),
        listOf("2e-", "2e-", "p+", "p+", "", "", "", "", "", "", "", "", "", "", "", ""),
        listOf("?", "", "", "", "", "", "", "", "", "", "", "", "", "", "", ""),
        listOf("4", "", "", "", "", "", "", "", "", "", "", "", "", "", "", ""),
        listOf("E", "T", "D", "", "", "", "", "", "", "", "", "", "", "", "", ""),
    )
    MenuScaffold {
        SectionTitle(stringResource(R.string.tutorial), stringResource(titles[step]))
        TutorialBoard(boards[step])
        Text(stringResource(R.string.tutorial_body), color = TextSecondary, fontSize = 13.sp)
        Button(onClick = { completedAction = true }, modifier = Modifier.fillMaxWidth().testTag("tutorial_action")) {
            Text(stringResource(R.string.tutorial_action))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onDone, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.skip)) }
            Button(
                onClick = {
                    if (step == titles.lastIndex) onDone() else {
                        step++
                        completedAction = false
                    }
                },
                enabled = completedAction,
                modifier = Modifier.weight(1f).testTag("tutorial_next"),
            ) { Text(stringResource(if (step == titles.lastIndex) R.string.finish else R.string.next)) }
        }
    }
}

@Composable
private fun TutorialBoard(cells: List<String>) {
    Column(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(8.dp)).padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(4) { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(4) { column ->
                    val label = cells[row * 4 + column]
                    Box(
                        Modifier.weight(1f).size(56.dp).background(if (label.isEmpty()) Color(0xFF171D38) else PanelRaised, RoundedCornerShape(8.dp)).border(1.dp, Cyan.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                    ) {
                        if (label.isNotEmpty()) Text(label, modifier = Modifier.padding(8.dp), color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatisticsScreen(profileRepository: ProfileRepository, onBack: () -> Unit) {
    val profile by profileRepository.observe().collectAsState(initial = com.battleheim.quantum2048.domain.ProfileState())
    MenuScaffold {
        SectionTitle(stringResource(R.string.statistics), stringResource(R.string.profile))
        StatRow(stringResource(R.string.stat_daily_best_today), profile.dailyBestScore(LocalDate.now().toString()).toString())
        StatRow(stringResource(R.string.stat_best_daily_score), profile.bestDailyScore.toString())
        StatRow(stringResource(R.string.stat_daily_challenge_count), profile.dailyChallengeCount.toString())
        StatRow(stringResource(R.string.stat_collapse_ratio), "${(profile.collapseLowRatio * 100).toInt()}%")
        StatRow(stringResource(R.string.stat_average_win_energy), "%.1f".format(profile.averageWinEnergy))
        StatRow(stringResource(R.string.stat_chain_merges), profile.totalChainMergeCount.toString())
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.back)) }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)).padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = TextSecondary, fontSize = 13.sp)
        Text(value, color = Cyan, fontWeight = FontWeight.Black)
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
private fun CollectionScreen(repository: CollectionRepository, profileRepository: ProfileRepository, onBack: () -> Unit) {
    val state by repository.observe().collectAsState(initial = com.battleheim.quantum2048.domain.CollectionState())
    val profile by profileRepository.observe().collectAsState(initial = com.battleheim.quantum2048.domain.ProfileState())
    val codex = state.codex(FusionRules.compoundRecipes)
    MenuScaffold {
        SectionTitle(stringResource(R.string.collection), stringResource(R.string.fusion_lab))
        AchievementList(profile.unlockedAchievements)
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
private fun AchievementList(unlocked: Set<String>) {
    Card(colors = CardDefaults.cardColors(containerColor = PanelRaised), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.achievements), fontWeight = FontWeight.Black)
            AchievementRow(stringResource(R.string.achievement_collapse_century), FusionRules.achievementCollapseCentury in unlocked)
            AchievementRow(stringResource(R.string.achievement_resolved_2048), FusionRules.achievementResolved2048 in unlocked)
            AchievementRow(stringResource(R.string.achievement_no_undo_win), FusionRules.achievementNoUndoWin in unlocked)
            Text(stringResource(R.string.achievements_local_note), color = TextMuted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun AchievementRow(label: String, unlocked: Boolean) {
    Text(
        "${if (unlocked) "[x]" else "[ ]"} $label",
        color = if (unlocked) Cyan else TextSecondary,
        fontSize = 12.sp,
    )
}

@Composable
private fun SettingsScreen(
    settingsRepository: SettingsRepository,
    collectionRepository: CollectionRepository,
    profileRepository: ProfileRepository,
    billingRepository: BillingRepository,
    settings: AppSettings,
    entitlements: EntitlementState,
    adGateway: AdGateway,
    audio: ToneGameAudio,
    vm: GameViewModel,
    analytics: AnalyticsGateway,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var confirmResetCollection by remember { mutableStateOf(false) }
    var confirmResetProfile by remember { mutableStateOf(false) }
    var confirmResetDifficulty by remember { mutableStateOf<Difficulty?>(null) }

    MenuScaffold {
        SectionTitle(stringResource(R.string.settings), stringResource(R.string.fusion_lab))
        SettingsToggle(stringResource(R.string.sound), settings.soundEnabled) { playMenu(audio, settings); saveSettings(settings.copy(soundEnabled = it), analytics, settingsRepository, scope) }
        SettingsToggle(stringResource(R.string.music), settings.musicEnabled) { playMenu(audio, settings); saveSettings(settings.copy(musicEnabled = it), analytics, settingsRepository, scope) }
        SettingsToggle(stringResource(R.string.haptics), settings.hapticsEnabled) { playMenu(audio, settings); saveSettings(settings.copy(hapticsEnabled = it), analytics, settingsRepository, scope) }
        SettingsToggle(stringResource(R.string.reduced_motion), settings.reducedMotion) { playMenu(audio, settings); saveSettings(settings.copy(reducedMotion = it), analytics, settingsRepository, scope) }
        OutlinedButton(
            onClick = {
                playMenu(audio, settings)
                saveSettings(settings.copy(language = settings.language.next()), analytics, settingsRepository, scope)
            },
            modifier = Modifier.fillMaxWidth().testTag("settings_language"),
        ) { Text(stringResource(R.string.language_button, settings.language.displayLabel())) }
        OutlinedButton(
            onClick = {
                playMenu(audio, settings)
                saveSettings(settings.copy(themeMode = settings.themeMode.next()), analytics, settingsRepository, scope)
            },
            modifier = Modifier.fillMaxWidth().testTag("settings_theme"),
        ) { Text(stringResource(R.string.theme_button, settings.themeMode.displayLabel())) }
        Text(stringResource(R.string.language_note), color = TextSecondary, fontSize = 12.sp)
        MonetizationSection(
            entitlements = entitlements,
            adGateway = adGateway,
            onRemoveAds = { scope.launch { billingRepository.grant(ProductIds.REMOVE_ADS) } },
            onRewardUndo = {
                adGateway.showRewarded(RewardPlacement.EXTRA_UNDO) {
                    scope.launch { billingRepository.grantReward(RewardEntitlement.EXTRA_UNDO) }
                }
            },
            onRewardRevive = {
                adGateway.showRewarded(RewardPlacement.REVIVE_AFTER_GAME_OVER) {
                    scope.launch { billingRepository.grantReward(RewardEntitlement.REVIVE) }
                }
            },
            onRewardDaily = {
                adGateway.showRewarded(RewardPlacement.DAILY_BONUS_ATTEMPT) {
                    scope.launch { billingRepository.grantReward(RewardEntitlement.DAILY_ATTEMPT) }
                }
            },
            onReset = { scope.launch { billingRepository.clear() } },
        )
        OutlinedButton(onClick = { playMenu(audio, settings); confirmResetCollection = true }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.reset_collection)) }
        OutlinedButton(onClick = { playMenu(audio, settings); confirmResetProfile = true }, modifier = Modifier.fillMaxWidth().testTag("reset_profile")) { Text(stringResource(R.string.reset_profile)) }
        Difficulty.entries.forEach { difficulty ->
            OutlinedButton(onClick = { playMenu(audio, settings); confirmResetDifficulty = difficulty }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.reset_progress, difficulty.name.lowercase())) }
        }
        OutlinedButton(onClick = { playMenu(audio, settings); onBack() }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.back)) }
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
    if (confirmResetProfile) {
        ConfirmDialog(
            title = stringResource(R.string.reset_profile_title),
            body = stringResource(R.string.reset_profile_body),
            onDismiss = { confirmResetProfile = false },
            onConfirm = {
                scope.launch { profileRepository.clear() }
                confirmResetProfile = false
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
private fun MonetizationSection(
    entitlements: EntitlementState,
    adGateway: AdGateway,
    onRemoveAds: () -> Unit,
    onRewardUndo: () -> Unit,
    onRewardRevive: () -> Unit,
    onRewardDaily: () -> Unit,
    onReset: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = PanelRaised), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.monetization), fontWeight = FontWeight.Black)
            Text(
                stringResource(if (entitlements.removeAds) R.string.remove_ads_owned else R.string.remove_ads_not_owned),
                color = if (entitlements.removeAds) Cyan else TextSecondary,
                fontSize = 12.sp,
            )
            StatRow(stringResource(R.string.reward_extra_undo), entitlements.rewardedExtraUndoCredits.toString())
            StatRow(stringResource(R.string.reward_revive), entitlements.rewardedReviveCredits.toString())
            StatRow(stringResource(R.string.reward_daily_attempt), entitlements.rewardedDailyAttemptCredits.toString())
            Button(onClick = onRemoveAds, enabled = !entitlements.removeAds, modifier = Modifier.fillMaxWidth().testTag("grant_remove_ads")) {
                Text(stringResource(R.string.test_grant_remove_ads))
            }
            OutlinedButton(onClick = onRewardUndo, enabled = adGateway.isRewardedReady, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.watch_for_extra_undo))
            }
            OutlinedButton(onClick = onRewardRevive, enabled = adGateway.isRewardedReady, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.watch_for_revive))
            }
            OutlinedButton(onClick = onRewardDaily, enabled = adGateway.isRewardedReady, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.watch_for_daily_attempt))
            }
            Text(stringResource(R.string.monetization_offline_note), color = TextMuted, fontSize = 11.sp)
            TextButton(onClick = onReset, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.reset_entitlements)) }
        }
    }
}

private fun playMenu(audio: ToneGameAudio, settings: AppSettings) {
    if (settings.soundEnabled) audio.menu()
}

private fun playSelect(audio: ToneGameAudio, settings: AppSettings) {
    if (settings.soundEnabled) audio.select()
}

private fun saveSettings(
    settings: AppSettings,
    analytics: AnalyticsGateway,
    settingsRepository: SettingsRepository,
    scope: kotlinx.coroutines.CoroutineScope,
) {
    analytics.logSettingsChanged(settings.themeMode, settings.language)
    scope.launch { settingsRepository.save(settings) }
}

private fun AppLanguage.next(): AppLanguage = when (this) {
    AppLanguage.SYSTEM -> AppLanguage.ENGLISH
    AppLanguage.ENGLISH -> AppLanguage.PERSIAN
    AppLanguage.PERSIAN -> AppLanguage.SYSTEM
}

private fun AppThemeMode.next(): AppThemeMode = when (this) {
    AppThemeMode.SYSTEM -> AppThemeMode.DARK
    AppThemeMode.DARK -> AppThemeMode.LIGHT
    AppThemeMode.LIGHT -> AppThemeMode.SYSTEM
}

private fun AppLanguage.shortLabel(): String = when (this) {
    AppLanguage.SYSTEM -> "Auto"
    AppLanguage.ENGLISH -> "EN"
    AppLanguage.PERSIAN -> "FA"
}

private fun AppLanguage.displayLabel(): String = when (this) {
    AppLanguage.SYSTEM -> "System"
    AppLanguage.ENGLISH -> "English"
    AppLanguage.PERSIAN -> "فارسی"
}

private fun AppThemeMode.iconLabel(): String = when (this) {
    AppThemeMode.SYSTEM -> "☾☀"
    AppThemeMode.DARK -> "☾"
    AppThemeMode.LIGHT -> "☀"
}

private fun AppThemeMode.displayLabel(): String = when (this) {
    AppThemeMode.SYSTEM -> "System"
    AppThemeMode.DARK -> "Dark"
    AppThemeMode.LIGHT -> "Light"
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
    Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)).padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
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
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
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
    Difficulty.ZEN -> stringResource(R.string.zen_description)
    Difficulty.HARDCORE -> stringResource(R.string.hardcore_description)
    Difficulty.PUZZLE -> stringResource(R.string.puzzle_description)
    Difficulty.DAILY -> stringResource(R.string.daily_description)
}

private const val SPLASH_MS = 2600L
