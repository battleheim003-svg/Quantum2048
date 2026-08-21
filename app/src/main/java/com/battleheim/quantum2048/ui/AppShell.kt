package com.battleheim.quantum2048.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.battleheim.quantum2048.R
import com.battleheim.quantum2048.BuildConfig
import com.battleheim.quantum2048.ads.AdGateway
import com.battleheim.quantum2048.ads.NoOpAdGateway
import com.battleheim.quantum2048.ads.RewardPlacement
import com.battleheim.quantum2048.analytics.AnalyticsGateway
import com.battleheim.quantum2048.analytics.NoOpAnalyticsGateway
import com.battleheim.quantum2048.audio.AndroidHapticPerformer
import com.battleheim.quantum2048.audio.AppSoundSettingsProvider
import com.battleheim.quantum2048.audio.GameAudioSoundPlaybackEngine
import com.battleheim.quantum2048.audio.GameAudio
import com.battleheim.quantum2048.audio.GameSoundPlayer
import com.battleheim.quantum2048.audio.HapticFeedbackController
import com.battleheim.quantum2048.audio.ToneGameAudio
import com.battleheim.quantum2048.designsystem.BoardGlass
import com.battleheim.quantum2048.designsystem.Cyan
import com.battleheim.quantum2048.designsystem.Electric
import com.battleheim.quantum2048.designsystem.GlassPanel
import com.battleheim.quantum2048.designsystem.NeonPink
import com.battleheim.quantum2048.designsystem.PanelRaised
import com.battleheim.quantum2048.designsystem.Panel
import com.battleheim.quantum2048.designsystem.RadiantGold
import com.battleheim.quantum2048.designsystem.TextMuted
import com.battleheim.quantum2048.designsystem.TextSecondary
import com.battleheim.quantum2048.designsystem.Void
import com.battleheim.quantum2048.designsystem.difficultyAccent
import com.battleheim.quantum2048.domain.CollectionRepository
import com.battleheim.quantum2048.domain.elementCodex
import com.battleheim.quantum2048.domain.AchievementProgress
import com.battleheim.quantum2048.domain.AchievementsRepository
import com.battleheim.quantum2048.domain.BillingRepository
import com.battleheim.quantum2048.domain.DailyChallengeRepository
import com.battleheim.quantum2048.domain.DailyChallengeStatus
import com.battleheim.quantum2048.domain.EntitlementState
import com.battleheim.quantum2048.domain.GameRepository
import com.battleheim.quantum2048.domain.AppLanguage
import com.battleheim.quantum2048.domain.AppSettings
import com.battleheim.quantum2048.domain.AppThemeMode
import com.battleheim.quantum2048.domain.LevelCatalog
import com.battleheim.quantum2048.domain.LevelCatalogRepository
import com.battleheim.quantum2048.domain.LevelDefinition
import com.battleheim.quantum2048.domain.LevelProgressRepository
import com.battleheim.quantum2048.domain.MainGameModeRoute
import com.battleheim.quantum2048.domain.MainMenuState
import com.battleheim.quantum2048.domain.LocalProgressResetRepository
import com.battleheim.quantum2048.domain.PlayerProgress
import com.battleheim.quantum2048.domain.ProgressResetRepository
import com.battleheim.quantum2048.domain.ProfileRepository
import com.battleheim.quantum2048.domain.ProductIds
import com.battleheim.quantum2048.domain.RewardEntitlement
import com.battleheim.quantum2048.domain.SettingsRepository
import com.battleheim.quantum2048.domain.SocialRepository
import com.battleheim.quantum2048.domain.StatisticsRepository
import com.battleheim.quantum2048.engine.Difficulty
import com.battleheim.quantum2048.engine.DailyChallengeSeedProvider
import com.battleheim.quantum2048.engine.Direction
import com.battleheim.quantum2048.engine.FusionRules
import com.battleheim.quantum2048.engine.GameEngine
import com.battleheim.quantum2048.engine.GameMode
import com.battleheim.quantum2048.engine.BotDifficulty
import com.battleheim.quantum2048.engine.DuelOpponent
import com.battleheim.quantum2048.engine.TutorialEngine
import com.battleheim.quantum2048.engine.TutorialLessonState
import com.battleheim.quantum2048.engine.TutorialStep
import kotlinx.coroutines.launch

private object Routes {
    const val MainMenu = "menu"
    const val LevelSelect = "level_select"
    const val Collection = "collection"
    const val Achievements = "achievements"
    const val Statistics = "stats"
    const val DailyChallenge = "daily_challenge"
    const val About = "about"
    const val PrivacyPolicy = "privacy-policy"
    const val PeriodicPath = "periodic_path"
    const val PeriodicGame = "periodic_level/{levelId}"
    const val Tutorial = "tutorial"
    const val Settings = "settings"
    const val Game = "game/{mode}"
    const val SavedGame = "game_saved/{difficulty}/{size}"
    const val DuelGame = "duel/{difficulty}"
    const val Pause = "pause"

    fun game(mode: MainGameModeRoute) = "game/${mode.routeValue}"
    fun savedGame(difficulty: Difficulty, size: Int) = "game_saved/${difficulty.name}/$size"
    fun duel(difficulty: Difficulty) = "duel/${difficulty.name}"
    fun periodicGame(levelId: String) = "periodic_level/$levelId"
}

@Composable
fun QuantumAppShell(
    gameRepository: GameRepository,
    collectionRepository: CollectionRepository,
    profileRepository: ProfileRepository,
    settingsRepository: SettingsRepository,
    socialRepository: SocialRepository,
    statisticsRepository: StatisticsRepository,
    dailyChallengeRepository: DailyChallengeRepository,
    achievementsRepository: AchievementsRepository,
    billingRepository: BillingRepository,
    levelCatalogRepository: LevelCatalogRepository,
    levelProgressRepository: LevelProgressRepository,
    adGateway: AdGateway = NoOpAdGateway,
    analytics: AnalyticsGateway = NoOpAnalyticsGateway,
    engine: GameEngine,
) {
    val nav = rememberNavController()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val audio = remember(context) { ToneGameAudio(context) }
    val eventScope = rememberCoroutineScope()
    val soundSettingsProvider = remember(settingsRepository) { AppSoundSettingsProvider(settingsRepository.observe()) }
    val gameSoundPlayer = remember(audio, soundSettingsProvider, eventScope) {
        GameSoundPlayer(soundSettingsProvider, GameAudioSoundPlaybackEngine(audio), eventScope)
    }
    val hapticFeedbackController = remember(context, soundSettingsProvider, eventScope) {
        HapticFeedbackController(soundSettingsProvider, AndroidHapticPerformer(context), eventScope)
    }
    val gameViewModel: GameViewModel = viewModel {
        GameViewModel(
            repository = gameRepository,
            collectionRepository = collectionRepository,
            profileRepository = profileRepository,
            socialRepository = socialRepository,
            levelCatalogRepository = levelCatalogRepository,
            levelProgressRepository = levelProgressRepository,
            statisticsRepository = statisticsRepository,
            dailyChallengeRepository = dailyChallengeRepository,
            achievementsRepository = achievementsRepository,
            engine = engine,
            analytics = analytics,
            soundEvents = gameSoundPlayer,
            hapticEvents = hapticFeedbackController,
        )
    }
    val ui by gameViewModel.ui.collectAsState()
    val settings by settingsRepository.observe().collectAsState(initial = com.battleheim.quantum2048.domain.AppSettings())
    var tutorialPrompted by remember { mutableStateOf(false) }
    DisposableEffect(lifecycleOwner, audio) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> audio.ambientStop()
                Lifecycle.Event.ON_STOP -> audio.ambientStop()
                Lifecycle.Event.ON_DESTROY -> audio.release()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            audio.release()
        }
    }
    LaunchedEffect(settings) {
        audio.applySettings(settings)
    }
    LaunchedEffect(ui.game.mode, ui.duel != null) {
        if (settings.musicEnabled) {
            if (ui.duel == null && ui.game.moveCount == 0) audio.menuMusic() else audio.gameMusic()
        }
    }
    LaunchedEffect(ui.game.entangledPairs.size, settings.entanglementIntroSeen) {
        if (ui.game.entangledPairs.isNotEmpty() && !settings.entanglementIntroSeen) {
            gameViewModel.showEntanglementIntro()
            settingsRepository.save(settings.copy(entanglementIntroSeen = true))
        }
    }
    NavHost(
        navController = nav,
        startDestination = Routes.MainMenu,
        enterTransition = { quantumEnterTransition() },
        exitTransition = { quantumExitTransition() },
        popEnterTransition = { quantumPopEnterTransition() },
        popExitTransition = { quantumPopExitTransition() },
    ) {
        composable(Routes.MainMenu) {
            MainMenuScreen(
                vm = gameViewModel,
                settings = settings,
                settingsRepository = settingsRepository,
                analytics = analytics,
                audio = audio,
                onContinue = { saved -> playSelect(audio, settings); nav.navigate(Routes.savedGame(saved.difficulty, saved.size)) },
                onNewClassic = { playSelect(audio, settings); nav.navigate(Routes.game(MainGameModeRoute.CLASSIC)) },
                onNewQuantum = { playSelect(audio, settings); nav.navigate(Routes.game(MainGameModeRoute.QUANTUM)) },
                onNewGame = { playSelect(audio, settings); nav.navigate(Routes.LevelSelect) },
                onCollection = { playMenu(audio, settings); nav.navigate(Routes.Collection) },
                onAchievements = { playMenu(audio, settings); nav.navigate(Routes.Achievements) },
                onStatistics = { playMenu(audio, settings); nav.navigate(Routes.Statistics) },
                onDailyChallenge = { playSelect(audio, settings); nav.navigate(Routes.DailyChallenge) },
                onAbout = { playMenu(audio, settings); nav.navigate(Routes.About) },
                onPeriodicPath = { playSelect(audio, settings); nav.navigate(Routes.PeriodicPath) },
                onTutorial = { playMenu(audio, settings); nav.navigate(Routes.Tutorial) },
                onSettings = { playMenu(audio, settings); nav.navigate(Routes.Settings) },
            )
        }
        composable(Routes.PeriodicPath) {
            PeriodicPathScreen(
                catalogRepository = levelCatalogRepository,
                progressRepository = levelProgressRepository,
                onBack = { nav.popBackStack() },
                onPlay = { level ->
                    playSelect(audio, settings)
                    gameViewModel.startPeriodicLevel(level.id)
                    nav.navigate(Routes.periodicGame(level.id))
                },
            )
        }
        composable(
            route = Routes.PeriodicGame,
            arguments = listOf(navArgument("levelId") { type = NavType.StringType }),
        ) {
            GameScreen(vm = gameViewModel, settings = settings, audio = audio, adGateway = adGateway, onPause = { nav.navigate(Routes.Pause) })
        }
        composable(Routes.LevelSelect) {
            LevelSelectScreen(
                vm = gameViewModel,
                profileRepository = profileRepository,
                onBack = { nav.popBackStack() },
                onSelect = { difficulty, size, duel, opponent, botDifficulty ->
                    if (duel) {
                        gameViewModel.newDuel(difficulty, opponent, botDifficulty)
                        nav.navigate(Routes.duel(difficulty))
                    } else {
                        gameViewModel.newGame(difficulty, size)
                        nav.navigate(Routes.savedGame(difficulty, size))
                    }
                },
            )
        }
        composable(
            route = Routes.DuelGame,
            arguments = listOf(navArgument("difficulty") { type = NavType.StringType }),
        ) {
            GameScreen(vm = gameViewModel, settings = settings, audio = audio, adGateway = adGateway, onPause = { nav.navigate(Routes.Pause) })
        }
        composable(
            route = Routes.Game,
            arguments = listOf(
                navArgument("mode") { type = NavType.StringType },
            ),
        ) { backStack ->
            val mode = MainGameModeRoute.fromRoute(backStack.arguments?.getString("mode"))
            LaunchedEffect(mode) {
                gameViewModel.newGame(mode.difficulty, 4)
                if (mode == MainGameModeRoute.QUANTUM && !settings.tutorialCompleted && !tutorialPrompted) {
                    tutorialPrompted = true
                    nav.navigate(Routes.Tutorial)
                }
            }
            BackHandler { nav.popBackStack(Routes.MainMenu, inclusive = false) }
            GameScreen(vm = gameViewModel, settings = settings, audio = audio, adGateway = adGateway, onPause = { nav.navigate(Routes.Pause) })
        }
        composable(
            route = Routes.SavedGame,
            arguments = listOf(
                navArgument("difficulty") { type = NavType.StringType },
                navArgument("size") { type = NavType.IntType },
            ),
        ) { backStack ->
            val difficulty = Difficulty.valueOf(backStack.arguments?.getString("difficulty") ?: Difficulty.QUANTUM.name)
            val size = backStack.arguments?.getInt("size") ?: 4
            LaunchedEffect(difficulty, size) { gameViewModel.loadDifficulty(difficulty, size) }
            BackHandler { nav.popBackStack(Routes.MainMenu, inclusive = false) }
            GameScreen(vm = gameViewModel, settings = settings, audio = audio, adGateway = adGateway, onPause = { nav.navigate(Routes.Pause) })
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
        composable(Routes.Achievements) {
            AchievementsScreen(achievementsRepository, onBack = { nav.popBackStack() })
        }
        composable(Routes.DailyChallenge) {
            DailyChallengeScreen(
                dailyChallengeRepository = dailyChallengeRepository,
                onBack = { nav.popBackStack() },
                onStart = { date ->
                    playSelect(audio, settings)
                    eventScope.launch { dailyChallengeRepository.markStarted(date) }
                    gameViewModel.startDailyChallenge(date)
                    nav.navigate(Routes.savedGame(Difficulty.DAILY, 4))
                },
                onContinue = {
                    playSelect(audio, settings)
                    nav.navigate(Routes.savedGame(Difficulty.DAILY, 4))
                },
            )
        }
        composable(Routes.Statistics) {
            StatisticsScreen(statisticsRepository, dailyChallengeRepository, onBack = { nav.popBackStack() })
        }
        composable(Routes.About) {
            AboutScreen(
                onPrivacy = { nav.navigate(Routes.PrivacyPolicy) },
                onBack = { nav.popBackStack() },
            )
        }
        composable(Routes.PrivacyPolicy) {
            PrivacyPolicyScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.Tutorial) {
            val scope = rememberCoroutineScope()
            TutorialScreen(
                onDone = {
                    scope.launch { settingsRepository.save(settings.copy(tutorialCompleted = true)) }
                    nav.popBackStack()
                },
            )
        }
        composable(Routes.Settings) {
            SettingsScreen(
                settingsRepository = settingsRepository,
                collectionRepository = collectionRepository,
                profileRepository = profileRepository,
                socialRepository = socialRepository,
                billingRepository = billingRepository,
                progressResetRepository = LocalProgressResetRepository(
                    gameRepository = gameRepository,
                    collectionRepository = collectionRepository,
                    profileRepository = profileRepository,
                    socialRepository = socialRepository,
                    statisticsRepository = statisticsRepository,
                    levelProgressRepository = levelProgressRepository,
                    dailyChallengeRepository = dailyChallengeRepository,
                    achievementsRepository = achievementsRepository,
                ),
                settings = settings,
                entitlements = billingRepository.observe().collectAsState(initial = EntitlementState()).value,
                adGateway = adGateway,
                audio = audio,
                vm = gameViewModel,
                analytics = analytics,
                onTutorial = {
                    nav.navigate(Routes.Tutorial)
                },
                onBack = { nav.popBackStack() },
            )
        }
    }
}

@Composable
internal fun MainMenuScreen(
    vm: GameViewModel,
    settings: AppSettings,
    settingsRepository: SettingsRepository,
    analytics: AnalyticsGateway,
    audio: GameAudio,
    onContinue: (SavedGameKey) -> Unit,
    onNewClassic: () -> Unit,
    onNewQuantum: () -> Unit,
    onNewGame: () -> Unit,
    onCollection: () -> Unit,
    onAchievements: () -> Unit,
    onStatistics: () -> Unit,
    onDailyChallenge: () -> Unit,
    onAbout: () -> Unit,
    onPeriodicPath: () -> Unit,
    onTutorial: () -> Unit,
    onSettings: () -> Unit,
) {
    var saves by remember { mutableStateOf(emptySet<SavedGameKey>()) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { saves = vm.savedGames() }
    LaunchedEffect(settings.musicEnabled) {
        if (settings.musicEnabled) audio.menuMusic()
    }
    val menuState = MainMenuState(saves.map { com.battleheim.quantum2048.domain.SavedGameRef(it.difficulty, it.size) }.toSet())
    val continueSave = menuState.preferredContinue?.let { SavedGameKey(it.difficulty, it.size) } ?: SavedGameKey(Difficulty.QUANTUM, 4)

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
        MainMenuHero()
        if (menuState.canContinue) {
            NeonMenuButton(
                text = stringResource(R.string.continue_game),
                onClick = { onContinue(continueSave) },
                modifier = Modifier.fillMaxWidth().testTag("continue_button"),
                accent = Cyan,
                filled = true,
            )
        }
        NeonMenuButton(
            text = stringResource(R.string.new_game),
            onClick = onNewGame,
            modifier = Modifier.fillMaxWidth().testTag("new_game_button"),
            accent = RadiantGold,
            filled = !menuState.canContinue,
            icon = "+",
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NeonMenuButton(
                text = stringResource(R.string.start_classic),
                onClick = onNewClassic,
                modifier = Modifier.weight(1f).testTag("start_classic_button"),
                accent = Cyan,
            )
            NeonMenuButton(
                text = stringResource(R.string.start_quantum),
                onClick = onNewQuantum,
                modifier = Modifier.weight(1f).testTag("start_quantum_button"),
                accent = RadiantGold,
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryMenuButton(text = stringResource(R.string.daily_challenge), onClick = onDailyChallenge, modifier = Modifier.weight(1f).testTag("daily_challenge_button"), accent = RadiantGold, icon = "!")
            SecondaryMenuButton(text = stringResource(R.string.periodic_path), onClick = onPeriodicPath, modifier = Modifier.weight(1f).testTag("periodic_path_button"), accent = Electric, icon = "P")
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryMenuButton(text = stringResource(R.string.collection), onClick = onCollection, modifier = Modifier.weight(1f), accent = NeonPink, icon = "C")
            SecondaryMenuButton(text = stringResource(R.string.statistics), onClick = onStatistics, modifier = Modifier.weight(1f), accent = Electric, icon = "S")
            SecondaryMenuButton(text = stringResource(R.string.settings), onClick = onSettings, modifier = Modifier.weight(1f), accent = RadiantGold, icon = "*")
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryMenuButton(text = stringResource(R.string.achievements), onClick = onAchievements, modifier = Modifier.weight(1f), accent = RadiantGold, icon = "A")
            SecondaryMenuButton(text = stringResource(R.string.tutorial), onClick = onTutorial, modifier = Modifier.weight(1f), accent = Cyan, icon = "?")
            SecondaryMenuButton(text = stringResource(R.string.about_game), onClick = onAbout, modifier = Modifier.weight(1f).testTag("about_button"), accent = TextSecondary, icon = "i")
        }
    }
}

@Composable
private fun SecondaryMenuButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = Cyan,
    icon: String,
) {
    val shape = RoundedCornerShape(16.dp)
    Button(
        onClick = onClick,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
            contentColor = accent,
        ),
        modifier = modifier
            .height(50.dp)
            .border(1.dp, accent.copy(alpha = 0.46f), shape),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp),
    ) {
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 12.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(end = 5.dp))
            Text(text, fontSize = 11.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, lineHeight = 12.sp)
        }
    }
}

@Composable
private fun MainMenuHero() {
    val shape = RoundedCornerShape(24.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f),
                    ),
                ),
                shape,
            )
            .border(1.25.dp, Cyan.copy(alpha = 0.58f), shape)
            .padding(12.dp),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val center = androidx.compose.ui.geometry.Offset(size.width * 0.78f, size.height * 0.36f)
            repeat(3) { index ->
                rotate(-28f + index * 34f, pivot = center) {
                    drawOval(
                        color = Cyan.copy(alpha = 0.10f - index * 0.018f),
                        topLeft = androidx.compose.ui.geometry.Offset(center.x - size.width * 0.24f, center.y - size.height * (0.09f + index * 0.02f)),
                        size = androidx.compose.ui.geometry.Size(size.width * 0.48f, size.height * (0.18f + index * 0.04f)),
                        style = Stroke(width = 1.2f),
                    )
                }
            }
            drawCircle(RadiantGold.copy(alpha = 0.18f), radius = size.minDimension * 0.08f, center = center)
            drawCircle(Cyan.copy(alpha = 0.22f), radius = 2.2f, center = androidx.compose.ui.geometry.Offset(size.width * 0.18f, size.height * 0.26f))
            drawCircle(NeonPink.copy(alpha = 0.20f), radius = 1.8f, center = androidx.compose.ui.geometry.Offset(size.width * 0.92f, size.height * 0.72f))
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "QUANTUM 2048",
                color = Cyan,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.4.sp,
            )
            Text(
                stringResource(R.string.app_title),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.2.sp,
                lineHeight = 30.sp,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Box(
                    Modifier
                        .size(8.dp)
                        .background(RadiantGold, RoundedCornerShape(8.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.32f), RoundedCornerShape(8.dp)),
                )
                Text(
                    stringResource(R.string.fusion_lab),
                    color = RadiantGold,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.4.sp,
                )
            }
            Box(
                Modifier
                    .fillMaxWidth(0.70f)
                    .height(2.dp)
                    .background(Brush.horizontalGradient(listOf(Cyan, Electric, NeonPink.copy(alpha = 0.65f), Color.Transparent)), RoundedCornerShape(2.dp)),
            )
        }
    }
}

@Composable
private fun PeriodicPathScreen(
    catalogRepository: LevelCatalogRepository,
    progressRepository: LevelProgressRepository,
    onBack: () -> Unit,
    onPlay: (LevelDefinition) -> Unit,
) {
    var catalog by remember { mutableStateOf<LevelCatalog?>(null) }
    val progress by progressRepository.observe().collectAsState(initial = PlayerProgress())
    LaunchedEffect(Unit) { catalog = catalogRepository.catalog() }
    MenuScaffold {
        SectionTitle(stringResource(R.string.periodic_path), stringResource(R.string.periodic_path_subtitle))
        catalog?.let { loaded ->
            NeonPanel(title = stringResource(R.string.periodic_path_progress), accent = RadiantGold) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DataReadout(
                        label = stringResource(R.string.periodic_levels_done),
                        value = "${progress.completedLevelCount}/${loaded.levels.size}",
                        accent = Cyan,
                        modifier = Modifier.weight(1f),
                    )
                    DataReadout(
                        label = stringResource(R.string.periodic_total_stars),
                        value = progress.totalStars.toString(),
                        accent = RadiantGold,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            loaded.zones.forEach { zone ->
                PeriodicZoneCard(zone, progress, onPlay)
            }
        } ?: NeonPanel(title = stringResource(R.string.periodic_path), accent = Cyan) {
            Text(stringResource(R.string.loading), color = TextSecondary, fontSize = 13.sp)
        }
        NeonMenuButton(text = stringResource(R.string.back), onClick = onBack, modifier = Modifier.fillMaxWidth(), accent = Cyan)
    }
}

@Composable
private fun PeriodicZoneCard(zone: com.battleheim.quantum2048.domain.ZoneDefinition, progress: PlayerProgress, onPlay: (LevelDefinition) -> Unit) {
    val completed = zone.levels.count { progress.completion(it.id) != null }
    NeonPanel(title = zone.title, accent = if (completed == zone.levels.size) RadiantGold else Electric) {
        Text(zone.subtitle, color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text("${completed}/${zone.levels.size} ${stringResource(R.string.periodic_levels_done_short)}", color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.Black)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            zone.levels.chunked(3).forEach { rowLevels ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowLevels.forEach { level ->
                        PeriodicLevelChip(
                            level = level,
                            progress = progress,
                            onPlay = onPlay,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(3 - rowLevels.size) {
                        Box(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun PeriodicLevelChip(level: LevelDefinition, progress: PlayerProgress, onPlay: (LevelDefinition) -> Unit, modifier: Modifier = Modifier) {
    val unlocked = progress.isUnlocked(level.id)
    val completion = progress.completion(level.id)
    val mercy = progress.mercyFor(level.id)
    val accent = when {
        completion?.bestStars == 3 -> RadiantGold
        completion != null -> Cyan
        unlocked -> Electric
        else -> TextMuted
    }
    Button(
        onClick = { onPlay(level) },
        enabled = unlocked,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (unlocked) GlassPanel else Color.Black.copy(alpha = 0.18f),
            contentColor = accent,
            disabledContainerColor = Color.Black.copy(alpha = 0.14f),
            disabledContentColor = TextMuted,
        ),
        modifier = modifier
            .height(72.dp)
            .border(1.dp, accent.copy(alpha = if (unlocked) 0.55f else 0.18f), RoundedCornerShape(14.dp)),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(level.indexInZone.toString().padStart(2, '0'), fontWeight = FontWeight.Black, fontSize = 16.sp)
            Text(
                completion?.let { "★".repeat(it.bestStars) } ?: if (unlocked) stringResource(R.string.periodic_ready) else stringResource(R.string.periodic_locked),
                fontSize = 10.sp,
                color = accent,
                textAlign = TextAlign.Center,
            )
            if (mercy.active && completion == null) {
                Text("+${mercy.assistMoveBonus} ${stringResource(R.string.moves_short)}", color = RadiantGold, fontSize = 9.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun QuickSettingsRow(settings: AppSettings, onLanguage: () -> Unit, onTheme: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
        NeonPillButton(settings.language.shortLabel(), onClick = onLanguage, modifier = Modifier.testTag("quick_language"))
        NeonPillButton(settings.themeMode.iconLabel(), onClick = onTheme, modifier = Modifier.padding(start = 8.dp).testTag("quick_theme"), accent = RadiantGold)
    }
}

@Composable
internal fun TutorialScreen(onDone: () -> Unit, initialLesson: TutorialLessonState = TutorialEngine.start()) {
    var lesson by remember { mutableStateOf(initialLesson) }
    val title = when (lesson.step) {
        TutorialStep.SWIPE -> R.string.tutorial_step_swipe
        TutorialStep.FUSION_MERGE -> R.string.tutorial_step_fusion_merge
        TutorialStep.COLLAPSE -> R.string.tutorial_step_manual_collapse
    }
    val body = when (lesson.step) {
        TutorialStep.SWIPE -> R.string.tutorial_body_swipe
        TutorialStep.FUSION_MERGE -> R.string.tutorial_body_fusion_merge
        TutorialStep.COLLAPSE -> R.string.tutorial_body_manual_collapse
    }
    MenuScaffold {
        SectionTitle(stringResource(R.string.tutorial), stringResource(title))
        TutorialBoard(
            lesson = lesson,
            onTile = { lesson = TutorialEngine.selectTile(lesson, it) },
        )
        if (lesson.step == TutorialStep.COLLAPSE) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StatRow(stringResource(R.string.hud_energy, ""), formatNumber(lesson.board.energy), Modifier.weight(1f))
                FusionRules.superpositionCollapseEnergyCosts.take(2).forEachIndexed { index, cost ->
                    StatRow(stringResource(R.string.tutorial_collapse_choice_label, index + 1), formatNumber(cost), Modifier.weight(1f))
                }
            }
        } else {
            TutorialEnergyPanel(lesson)
        }
        Text(stringResource(body), color = TextSecondary, fontSize = 13.sp)
        TutorialInteractionControls(
            lesson = lesson,
            onCollapse = { lesson = TutorialEngine.collapseSelected(lesson, it) },
            onMerge = { lesson = TutorialEngine.merge(lesson, it) },
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { lesson = TutorialEngine.skip(lesson) }, enabled = !lesson.isCurrentStepComplete, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.skip)) }
            Button(
                onClick = {
                    if (lesson.step == TutorialStep.entries.last()) onDone() else lesson = TutorialEngine.next(lesson)
                },
                enabled = lesson.isCurrentStepComplete,
                modifier = Modifier.weight(1f).testTag("tutorial_next"),
            ) { Text(stringResource(if (lesson.step == TutorialStep.entries.last()) R.string.finish else R.string.next)) }
        }
    }
}

@Composable
private fun TutorialBoard(lesson: TutorialLessonState, onTile: (Long) -> Unit) {
    Column(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(8.dp)).padding(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(4) { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(4) { column ->
                    val tile = lesson.board.cells[row * 4 + column]
                    val selected = tile?.id == lesson.selectedTileId
                    val label = tile?.tutorialLabel().orEmpty()
                    Box(
                        Modifier
                            .weight(1f)
                            .size(48.dp)
                            .background(if (tile == null) Color(0xFF171D38) else PanelRaised, RoundedCornerShape(8.dp))
                            .border(1.dp, if (selected) RadiantGold else Cyan.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .clickable(enabled = tile?.superpositionValues?.isNotEmpty() == true) { onTile(tile!!.id) }
                            .testTag("tutorial_cell_${row}_${column}"),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (label.isNotEmpty()) Text(label, color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable
private fun TutorialEnergyPanel(lesson: TutorialLessonState) {
    NeonPanel(title = stringResource(R.string.tutorial_energy_panel), accent = Electric) {
        StatRow(stringResource(R.string.hud_energy, ""), formatNumber(lesson.board.energy))
        if (lesson.step == TutorialStep.COLLAPSE) {
            FusionRules.superpositionCollapseEnergyCosts.forEachIndexed { index, cost ->
                StatRow(stringResource(R.string.tutorial_collapse_choice_label, index + 1), formatNumber(cost))
            }
        }
    }
}

@Composable
private fun TutorialInteractionControls(
    lesson: TutorialLessonState,
    onCollapse: (Int) -> Unit,
    onMerge: (Direction) -> Unit,
) {
    when (lesson.step) {
        TutorialStep.SWIPE -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NeonMenuButton(stringResource(R.string.tutorial_swipe_left), onClick = { onMerge(Direction.LEFT) }, enabled = !lesson.isCurrentStepComplete, modifier = Modifier.weight(1f).testTag("tutorial_swipe_left"), accent = Cyan, filled = true)
            NeonMenuButton(stringResource(R.string.tutorial_swipe_right), onClick = { onMerge(Direction.RIGHT) }, enabled = !lesson.isCurrentStepComplete, modifier = Modifier.weight(1f), accent = RadiantGold)
        }
        TutorialStep.FUSION_MERGE -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NeonMenuButton(stringResource(R.string.tutorial_swipe_left), onClick = { onMerge(Direction.LEFT) }, enabled = !lesson.isCurrentStepComplete, modifier = Modifier.weight(1f).testTag("tutorial_merge_left"), accent = Cyan, filled = true)
            NeonMenuButton(stringResource(R.string.tutorial_swipe_right), onClick = { onMerge(Direction.RIGHT) }, enabled = !lesson.isCurrentStepComplete, modifier = Modifier.weight(1f), accent = RadiantGold)
        }
        TutorialStep.COLLAPSE -> {
            if (lesson.selectedTileId == null) Text(stringResource(R.string.tutorial_tap_tile), color = Cyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FusionRules.superpositionCollapseEnergyCosts.forEachIndexed { index, _ ->
                NeonMenuButton(
                    text = lesson.board.cells.firstNotNullOfOrNull { it?.superpositionValues?.getOrNull(index) }?.toString() ?: "?",
                    onClick = { onCollapse(index) },
                    enabled = lesson.selectedTileId != null && !lesson.isCurrentStepComplete,
                    modifier = Modifier.weight(1f).testTag("tutorial_collapse_$index"),
                    accent = if (index == 0) Cyan else RadiantGold,
                )
            }
        }
        }
    }
}

private fun com.battleheim.quantum2048.engine.Tile.tutorialLabel(): String =
    if (superpositionValues.isNotEmpty()) superpositionValues.joinToString(" | ") else when (kind) {
        com.battleheim.quantum2048.engine.TileKind.ELECTRON -> "${value}e-"
        com.battleheim.quantum2048.engine.TileKind.PROTON -> "${value}p+"
        com.battleheim.quantum2048.engine.TileKind.ELEMENT -> element?.symbol.orEmpty()
        com.battleheim.quantum2048.engine.TileKind.CLASSIC -> value.toString()
    }

@Composable
private fun AchievementsScreen(achievementsRepository: AchievementsRepository, onBack: () -> Unit) {
    val achievements by achievementsRepository.observeAchievements().collectAsState(initial = emptyList())
    MenuScaffold {
        SectionTitle(stringResource(R.string.achievements), stringResource(R.string.achievements_progress_subtitle))
        if (achievements.isEmpty()) {
            NeonPanel(title = stringResource(R.string.achievements), accent = RadiantGold) {
                Text(stringResource(R.string.loading), color = TextSecondary, fontSize = 13.sp)
            }
        } else {
            achievements.forEach { progress ->
                AchievementCard(progress)
            }
        }
        NeonMenuButton(text = stringResource(R.string.back), onClick = onBack, modifier = Modifier.fillMaxWidth(), accent = Cyan)
    }
}

@Composable
private fun AchievementCard(progress: AchievementProgress) {
    val done = progress.isCompleted
    NeonPanel(
        title = stringResource(progress.achievement.titleKey.stringRes()),
        accent = if (done) RadiantGold else Electric,
    ) {
        Text(stringResource(progress.achievement.descriptionKey.stringRes()), color = TextSecondary, fontSize = 13.sp)
        LinearProgressIndicator(
            progress = { progress.ratio },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = if (done) RadiantGold else Cyan,
            trackColor = Color.White.copy(alpha = 0.10f),
        )
        StatRow(
            stringResource(if (done) R.string.achievement_completed else R.string.achievement_progress),
            stringResource(R.string.achievement_progress_value, formatNumber(progress.current), formatNumber(progress.achievement.target)),
        )
    }
}

private fun String.stringRes(): Int = when (this) {
    "achievement_low_collapse_100_title" -> R.string.achievement_low_collapse_100_title
    "achievement_low_collapse_100_desc" -> R.string.achievement_low_collapse_100_desc
    "achievement_high_collapse_100_title" -> R.string.achievement_high_collapse_100_title
    "achievement_high_collapse_100_desc" -> R.string.achievement_high_collapse_100_desc
    "achievement_quantum_2048_title" -> R.string.achievement_quantum_2048_title
    "achievement_quantum_2048_desc" -> R.string.achievement_quantum_2048_desc
    "achievement_tile_4096_title" -> R.string.achievement_tile_4096_title
    "achievement_tile_4096_desc" -> R.string.achievement_tile_4096_desc
    "achievement_daily_streak_5_title" -> R.string.achievement_daily_streak_5_title
    "achievement_daily_streak_5_desc" -> R.string.achievement_daily_streak_5_desc
    "achievement_daily_streak_30_title" -> R.string.achievement_daily_streak_30_title
    "achievement_daily_streak_30_desc" -> R.string.achievement_daily_streak_30_desc
    "achievement_classic_first_game_title" -> R.string.achievement_classic_first_game_title
    "achievement_classic_first_game_desc" -> R.string.achievement_classic_first_game_desc
    "achievement_quantum_first_game_title" -> R.string.achievement_quantum_first_game_title
    "achievement_quantum_first_game_desc" -> R.string.achievement_quantum_first_game_desc
    "achievement_merges_1000_title" -> R.string.achievement_merges_1000_title
    "achievement_merges_1000_desc" -> R.string.achievement_merges_1000_desc
    "achievement_win_streak_5_title" -> R.string.achievement_win_streak_5_title
    "achievement_win_streak_5_desc" -> R.string.achievement_win_streak_5_desc
    "achievement_both_modes_title" -> R.string.achievement_both_modes_title
    "achievement_both_modes_desc" -> R.string.achievement_both_modes_desc
    "achievement_entangled_collapse_50_title" -> R.string.achievement_entangled_collapse_50_title
    "achievement_entangled_collapse_50_desc" -> R.string.achievement_entangled_collapse_50_desc
    else -> R.string.achievements
}

@Composable
private fun DailyChallengeScreen(
    dailyChallengeRepository: DailyChallengeRepository,
    onBack: () -> Unit,
    onStart: (String) -> Unit,
    onContinue: () -> Unit,
) {
    val today = remember { DailyChallengeSeedProvider.todayUtc() }
    val state by dailyChallengeRepository.observe().collectAsState(initial = com.battleheim.quantum2048.domain.DailyChallengeState())
    val status = state.statusFor(today)
    val todayResult = state.resultFor(today)
    MenuScaffold {
        SectionTitle(stringResource(R.string.daily_challenge), stringResource(R.string.daily_challenge_subtitle))
        NeonPanel(title = stringResource(R.string.daily_today), accent = RadiantGold) {
            StatRow(stringResource(R.string.stat_today_date), today)
            StatRow(
                stringResource(R.string.daily_status),
                stringResource(
                    when (status) {
                        DailyChallengeStatus.AVAILABLE -> R.string.daily_status_available
                        DailyChallengeStatus.IN_PROGRESS -> R.string.daily_status_in_progress
                        DailyChallengeStatus.COMPLETED -> R.string.daily_status_completed
                    },
                ),
            )
            if (todayResult != null) {
                StatRow(stringResource(R.string.daily_score_today), formatNumber(todayResult.score))
            }
            StatRow(stringResource(R.string.daily_personal_best), formatNumber(state.bestScore))
            StatRow(stringResource(R.string.daily_average_score), formatNumber(state.averageScore))
        }
        when (status) {
            DailyChallengeStatus.AVAILABLE -> NeonMenuButton(
                text = stringResource(R.string.daily_start),
                onClick = { onStart(today) },
                modifier = Modifier.fillMaxWidth(),
                accent = RadiantGold,
                filled = true,
            )
            DailyChallengeStatus.IN_PROGRESS -> NeonMenuButton(
                text = stringResource(R.string.daily_continue),
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth(),
                accent = Cyan,
                filled = true,
            )
            DailyChallengeStatus.COMPLETED -> NeonPanel(title = stringResource(R.string.daily_locked_title), accent = Electric) {
                Text(stringResource(R.string.daily_locked_body), color = TextSecondary, fontSize = 13.sp)
            }
        }
        DailyHistoryPanel(state)
        NeonMenuButton(text = stringResource(R.string.back), onClick = onBack, modifier = Modifier.fillMaxWidth(), accent = Cyan)
    }
}

@Composable
private fun DailyHistoryPanel(state: com.battleheim.quantum2048.domain.DailyChallengeState) {
    NeonPanel(title = stringResource(R.string.daily_recent_history), accent = Electric) {
        if (state.recentResults.isEmpty()) {
            Text(stringResource(R.string.daily_history_empty), color = TextSecondary, fontSize = 13.sp)
        } else {
            state.recentResults.forEach { result ->
                StatRow(result.date, formatNumber(result.score))
            }
        }
    }
}

@Composable
internal fun StatisticsScreen(
    statisticsRepository: StatisticsRepository,
    dailyChallengeRepository: DailyChallengeRepository,
    onBack: () -> Unit,
) {
    var selectedMode by remember { mutableStateOf(GameMode.CLASSIC) }
    val stats by statisticsRepository.observeStatistics(selectedMode).collectAsState(
        initial = com.battleheim.quantum2048.domain.StatsSnapshot(selectedMode),
    )
    val dailyState by dailyChallengeRepository.observe().collectAsState(initial = com.battleheim.quantum2048.domain.DailyChallengeState())
    MenuScaffold {
        SectionTitle(stringResource(R.string.statistics), stringResource(R.string.stats_by_mode))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NeonMenuButton(
                text = stringResource(R.string.classic),
                onClick = { selectedMode = GameMode.CLASSIC },
                modifier = Modifier.weight(1f).testTag("stats_classic"),
                accent = Cyan,
                filled = selectedMode == GameMode.CLASSIC,
            )
            NeonMenuButton(
                text = stringResource(R.string.quantum),
                onClick = { selectedMode = GameMode.QUANTUM },
                modifier = Modifier.weight(1f).testTag("stats_quantum"),
                accent = RadiantGold,
                filled = selectedMode == GameMode.QUANTUM,
            )
        }
        if (stats.isEmpty) {
            NeonPanel(title = stringResource(R.string.statistics), accent = Electric) {
                Text(stringResource(R.string.stats_empty), color = TextSecondary, fontSize = 13.sp)
            }
        } else {
            NeonPanel(title = stringResource(if (selectedMode == GameMode.CLASSIC) R.string.classic else R.string.quantum), accent = Electric) {
                listOf(
                    stringResource(R.string.stat_highest_tile) to formatNumber(stats.highestTile),
                    stringResource(R.string.stat_high_score) to formatNumber(stats.highScore),
                    stringResource(R.string.stat_games_played) to formatNumber(stats.gamesPlayed),
                    stringResource(R.string.stat_total_merges) to formatNumber(stats.totalMerges),
                    stringResource(R.string.stat_longest_merge_chain) to formatNumber(stats.longestMergeChain),
                    stringResource(R.string.stat_longest_win_streak) to formatNumber(stats.longestWinStreak),
                ).chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { (label, value) -> StatRow(label, value, Modifier.weight(1f)) }
                    }
                }
                if (selectedMode == GameMode.QUANTUM) {
                    StatRow(stringResource(R.string.stat_manual_collapse_low), formatNumber(stats.manualCollapseLow))
                    StatRow(stringResource(R.string.stat_manual_collapse_high), formatNumber(stats.manualCollapseHigh))
                    StatRow(stringResource(R.string.stat_auto_collapse), formatNumber(stats.autoCollapseCount))
                    StatRow(stringResource(R.string.stat_entangled_collapse_chain), formatNumber(stats.entangledCollapseChainCount))
                }
            }
        }
        NeonPanel(title = stringResource(R.string.daily_challenge), accent = RadiantGold) {
            if (dailyState.results.isEmpty()) {
                Text(stringResource(R.string.daily_history_empty), color = TextSecondary, fontSize = 13.sp)
            } else {
                StatRow(stringResource(R.string.stat_daily_challenge_count), formatNumber(dailyState.results.size))
                StatRow(stringResource(R.string.stat_best_daily_score), formatNumber(dailyState.bestScore))
                StatRow(stringResource(R.string.stat_daily_current_streak), formatNumber(dailyState.participationStreak))
            }
        }
        NeonMenuButton(text = stringResource(R.string.back), onClick = onBack, modifier = Modifier.fillMaxWidth(), accent = Cyan)
    }
}

@Composable
private fun AboutScreen(onPrivacy: () -> Unit, onBack: () -> Unit) {
    MenuScaffold(modifier = Modifier.testTag("about_screen")) {
        SectionTitle(stringResource(R.string.about_game), stringResource(R.string.app_title))
        NeonPanel(title = stringResource(R.string.about_version), accent = Cyan) {
            StatRow(stringResource(R.string.app_name), BuildConfig.VERSION_NAME)
            StatRow(stringResource(R.string.about_studio), stringResource(R.string.studio_name))
        }
        NeonPanel(title = stringResource(R.string.notice), accent = RadiantGold) {
            Text(stringResource(R.string.notice_body), color = TextSecondary, fontSize = 13.sp)
            Text(stringResource(R.string.notice), color = Cyan, fontSize = 16.sp, fontWeight = FontWeight.Black, modifier = Modifier.testTag("notice_link"))
        }
        NeonMenuButton(text = stringResource(R.string.privacy_policy), onClick = onPrivacy, modifier = Modifier.fillMaxWidth(), accent = Electric)
        NeonMenuButton(text = stringResource(R.string.back), onClick = onBack, modifier = Modifier.fillMaxWidth(), accent = Cyan)
    }
}

@Composable
private fun PrivacyPolicyScreen(onBack: () -> Unit) {
    MenuScaffold {
        SectionTitle(stringResource(R.string.privacy_policy), stringResource(R.string.privacy_policy_subtitle))
        NeonPanel(title = stringResource(R.string.privacy_policy_offline_title), accent = Cyan) {
            Text(stringResource(R.string.privacy_policy_offline_body), color = TextSecondary, fontSize = 13.sp)
        }
        NeonPanel(title = stringResource(R.string.privacy_policy_local_title), accent = RadiantGold) {
            Text(stringResource(R.string.privacy_policy_local_body), color = TextSecondary, fontSize = 13.sp)
        }
        NeonPanel(title = stringResource(R.string.privacy_policy_ads_title), accent = Electric) {
            Text(stringResource(R.string.privacy_policy_ads_body), color = TextSecondary, fontSize = 13.sp)
        }
        NeonMenuButton(text = stringResource(R.string.back), onClick = onBack, modifier = Modifier.fillMaxWidth(), accent = Cyan)
    }
}

@Composable
private fun StatRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f), RoundedCornerShape(10.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.48f), RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Text(value, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
    }
}

@Composable
private fun ProfileDataHeader(date: String, dailyBest: String, bestDaily: String) {
    NeonPanel(title = stringResource(R.string.profile), accent = Cyan) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DataReadout(label = stringResource(R.string.stat_today_date), value = date, accent = TextSecondary, modifier = Modifier.weight(1f))
            DataReadout(label = stringResource(R.string.stat_daily_best_today), value = dailyBest, accent = Cyan, modifier = Modifier.weight(1f))
        }
        DataReadout(label = stringResource(R.string.stat_best_daily_score), value = bestDaily, accent = RadiantGold, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun DataReadout(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Column(
        modifier
            .background(Color.Black.copy(alpha = 0.22f), RoundedCornerShape(10.dp))
            .border(1.dp, accent.copy(alpha = 0.42f), RoundedCornerShape(10.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label.uppercase(), color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp)
        Text(value, color = accent, fontSize = 19.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
    }
}

@Composable
private fun LevelSelectScreen(
    vm: GameViewModel,
    profileRepository: ProfileRepository,
    onBack: () -> Unit,
    onSelect: (Difficulty, Int, Boolean, DuelOpponent, BotDifficulty) -> Unit,
) {
    var saves by remember { mutableStateOf(emptySet<SavedGameKey>()) }
    val profile by profileRepository.observe().collectAsState(initial = com.battleheim.quantum2048.domain.ProfileState())
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
        Difficulty.entries.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { difficulty ->
                    val unlock = unlockRequirementFor(difficulty, profile)
                    val locked = !duel && !unlock.unlocked
                    DifficultyCard(
                        difficulty = difficulty,
                        size = selectedSize,
                        hasSave = SavedGameKey(difficulty, selectedSize) in saves,
                        description = difficultyDescription(difficulty),
                        mission = missionFor(difficulty),
                        unlockText = unlock.text,
                        locked = locked,
                        onClick = { onSelect(difficulty, selectedSize, duel, opponent, botDifficulty) },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(2 - row.size) { Box(Modifier.weight(1f)) }
            }
        }
        NeonMenuButton(text = stringResource(R.string.back), onClick = onBack, modifier = Modifier.fillMaxWidth(), accent = Cyan)
    }
}

@Composable
private fun ModeSelector(duel: Boolean, onSelect: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        QuantumChipButton(text = stringResource(R.string.solo), selected = !duel, onClick = { onSelect(false) }, modifier = Modifier.weight(1f), accent = Cyan)
        QuantumChipButton(text = stringResource(R.string.duel), selected = duel, onClick = { onSelect(true) }, modifier = Modifier.weight(1f).testTag("mode_duel"), accent = RadiantGold)
    }
}

@Composable
private fun BoardSizeSelector(selected: Int, enabled: Boolean, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FusionRules.supportedBoardSizes.forEach { size ->
            val active = size == selected
            QuantumChipButton(
                text = stringResource(R.string.board_size, size),
                selected = active,
                onClick = { onSelect(size) },
                enabled = enabled || active,
                modifier = Modifier.weight(1f).testTag("board_size_${size}x$size"),
                accent = Electric,
            )
        }
    }
}

@Composable
private fun DuelSelector(opponent: DuelOpponent, botDifficulty: BotDifficulty, onOpponent: (DuelOpponent) -> Unit, onBot: (BotDifficulty) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QuantumChipButton(text = stringResource(R.string.bot), selected = opponent == DuelOpponent.BOT, onClick = { onOpponent(DuelOpponent.BOT) }, modifier = Modifier.weight(1f).testTag("duel_bot"), accent = Electric)
            QuantumChipButton(text = stringResource(R.string.pass_play), selected = opponent == DuelOpponent.PASS_AND_PLAY, onClick = { onOpponent(DuelOpponent.PASS_AND_PLAY) }, modifier = Modifier.weight(1f).testTag("duel_pass"), accent = NeonPink)
        }
        if (opponent == DuelOpponent.BOT) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BotDifficulty.entries.forEach { level ->
                    QuantumChipButton(text = level.label(), selected = botDifficulty == level, onClick = { onBot(level) }, modifier = Modifier.weight(1f).testTag("bot_${level.name.lowercase()}"), accent = actionAccent(level.ordinal))
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
private fun DifficultyCard(
    difficulty: Difficulty,
    size: Int,
    hasSave: Boolean,
    description: String,
    mission: String,
    unlockText: String,
    locked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = difficultyAccent(difficulty)
    val titleColor = MaterialTheme.colorScheme.onSurface
    val bodyColor = if (locked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f) else MaterialTheme.colorScheme.onSurfaceVariant
    val cardBrush = if (locked) {
        Brush.linearGradient(
            listOf(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
            ),
        )
    } else {
        Brush.linearGradient(
            listOf(
                accent.copy(alpha = 0.16f),
                MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
            ),
        )
    }
    Card(
        onClick = { if (!locked) onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
            .graphicsLayer { alpha = if (locked) 0.52f else 1f }
            .background(cardBrush, RoundedCornerShape(8.dp))
            .border(1.dp, if (locked) TextMuted.copy(alpha = 0.55f) else accent, RoundedCornerShape(8.dp))
            .defaultMinSize(minHeight = 132.dp)
            .testTag("level_${difficulty.name.lowercase()}"),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(12.dp).background(accent, RoundedCornerShape(3.dp)))
                    Text(difficulty.localizedLabel(), color = titleColor, fontWeight = FontWeight.Black, fontSize = 13.sp)
                }
                Text(
                    if (locked) stringResource(R.string.periodic_locked) else if (hasSave) stringResource(R.string.saved_size, size) else stringResource(R.string.board_size, size),
                    color = if (locked) TextMuted else accent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                if (locked) unlockText else description,
                color = bodyColor,
                fontSize = 10.sp,
                lineHeight = 13.sp,
            )
            Text(
                mission,
                color = if (locked) TextMuted else accent,
                fontSize = 10.sp,
                lineHeight = 13.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

private data class DifficultyUnlock(val unlocked: Boolean, val text: String)

@Composable
private fun unlockRequirementFor(
    difficulty: Difficulty,
    profile: com.battleheim.quantum2048.domain.ProfileState,
): DifficultyUnlock = when (difficulty) {
    Difficulty.EASY, Difficulty.MEDIUM, Difficulty.HARD -> DifficultyUnlock(true, stringResource(R.string.unlock_available))
    Difficulty.QUANTUM, Difficulty.ZEN ->
        DifficultyUnlock(profile.isQuantumUnlocked, stringResource(R.string.unlock_quantum_lab))
    Difficulty.HARDCORE ->
        DifficultyUnlock(FusionRules.achievementCollapseCentury in profile.unlockedAchievements, stringResource(R.string.unlock_hardcore))
    Difficulty.PUZZLE ->
        DifficultyUnlock(FusionRules.achievementNoUndoWin in profile.unlockedAchievements, stringResource(R.string.unlock_puzzle))
    Difficulty.DAILY ->
        DifficultyUnlock(FusionRules.achievementResolved2048 in profile.unlockedAchievements, stringResource(R.string.unlock_daily))
}

@Composable
private fun missionFor(difficulty: Difficulty): String = when (difficulty) {
    Difficulty.EASY -> stringResource(R.string.mission_easy)
    Difficulty.MEDIUM -> stringResource(R.string.mission_medium)
    Difficulty.HARD -> stringResource(R.string.mission_hard)
    Difficulty.QUANTUM -> stringResource(R.string.mission_quantum)
    Difficulty.ZEN -> stringResource(R.string.mission_zen)
    Difficulty.HARDCORE -> stringResource(R.string.mission_hardcore)
    Difficulty.PUZZLE -> stringResource(R.string.mission_puzzle)
    Difficulty.DAILY -> stringResource(R.string.mission_daily)
}

@Composable
internal fun CollectionScreen(repository: CollectionRepository, profileRepository: ProfileRepository, onBack: () -> Unit) {
    val state by repository.observe().collectAsState(initial = com.battleheim.quantum2048.domain.CollectionState())
    val profile by profileRepository.observe().collectAsState(initial = com.battleheim.quantum2048.domain.ProfileState())
    val codex = state.codex(FusionRules.compoundRecipes)
    val elementCodex = state.elementCodex()
    var selectedElement by remember { mutableStateOf<com.battleheim.quantum2048.domain.ElementCodexEntry?>(null) }
    val discoveredCount = elementCodex.count { it.discovered }
    val totalCount = elementCodex.size.coerceAtLeast(1)
    MenuScaffold {
        SectionTitle(stringResource(R.string.collection), stringResource(R.string.fusion_lab))
        NeonPanel(title = stringResource(R.string.element_codex), accent = Cyan) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("$discoveredCount/$totalCount", color = Cyan, fontSize = 22.sp, fontWeight = FontWeight.Black)
                Text(stringResource(R.string.collection), color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            LinearProgressIndicator(
                progress = { discoveredCount.toFloat() / totalCount },
                color = Cyan,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth().height(8.dp),
            )
            ElementCodexGrid(elementCodex, onSelect = { selectedElement = it })
        }
        CompactAchievementList(profile.unlockedAchievements)
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.back)) }
    }
    selectedElement?.let { entry ->
        ElementDetailDialog(entry = entry, onDismiss = { selectedElement = null })
    }
}

@Composable
private fun CompactAchievementList(unlocked: Set<String>) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.achievements), fontWeight = FontWeight.Black, fontSize = 12.sp, modifier = Modifier.weight(1f))
            Text("${listOf(FusionRules.achievementCollapseCentury, FusionRules.achievementResolved2048, FusionRules.achievementNoUndoWin).count { it in unlocked }}/3", color = Cyan, fontWeight = FontWeight.Black, fontSize = 13.sp)
        }
    }
}

@Composable
private fun ElementCodexGrid(
    entries: List<com.battleheim.quantum2048.domain.ElementCodexEntry>,
    onSelect: (com.battleheim.quantum2048.domain.ElementCodexEntry) -> Unit,
) {
    entries.chunked(5).forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            row.forEach { entry ->
                ElementCodexCard(entry, onClick = { if (entry.discovered) onSelect(entry) }, modifier = Modifier.weight(1f))
            }
            repeat(5 - row.size) { Box(Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun ElementCodexCard(
    entry: com.battleheim.quantum2048.domain.ElementCodexEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = if (entry.discovered) Cyan else MaterialTheme.colorScheme.outlineVariant
    Card(
        onClick = onClick,
        enabled = entry.discovered,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (entry.discovered) 0.92f else 0.42f)),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.height(48.dp).border(1.dp, accent.copy(alpha = 0.58f), RoundedCornerShape(8.dp)),
    ) {
        Column(Modifier.fillMaxSize().padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(if (entry.discovered) entry.element.symbol else "?${entry.element.atomicNumber}", color = accent, fontSize = 15.sp, fontWeight = FontWeight.Black)
            Text(if (entry.discovered) entry.element.title else "${entry.element.atomicNumber}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, textAlign = TextAlign.Center, maxLines = 1)
        }
    }
}

@Composable
private fun ElementDetailDialog(entry: com.battleheim.quantum2048.domain.ElementCodexEntry, onDismiss: () -> Unit) {
    QuantumDialog(
        title = entry.element.title,
        onDismiss = onDismiss,
        accent = Cyan,
        confirmText = stringResource(R.string.confirm),
        onConfirm = onDismiss,
    ) {
        StatRow(entry.element.symbol, entry.element.atomicNumber.toString())
        StatRow(stringResource(R.string.collection), entry.discoveryOrder?.let { "#$it" } ?: stringResource(R.string.none))
        StatRow(stringResource(R.string.element_codex), entry.firstDiscoveredAtMillis?.takeIf { it > 1L }?.toString() ?: stringResource(R.string.none))
    }
}

@Composable
private fun AchievementList(unlocked: Set<String>) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
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
internal fun SettingsScreen(
    settingsRepository: SettingsRepository,
    collectionRepository: CollectionRepository,
    profileRepository: ProfileRepository,
    socialRepository: SocialRepository,
    billingRepository: BillingRepository,
    progressResetRepository: ProgressResetRepository,
    settings: AppSettings,
    entitlements: EntitlementState,
    adGateway: AdGateway,
    audio: GameAudio,
    vm: GameViewModel,
    analytics: AnalyticsGateway,
    onTutorial: () -> Unit,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val ui by vm.ui.collectAsState()
    var confirmResetCollection by remember { mutableStateOf(false) }
    var confirmResetProfile by remember { mutableStateOf(false) }
    var confirmResetDifficulty by remember { mutableStateOf<Difficulty?>(null) }
    var confirmResetProgress by remember { mutableStateOf(false) }
    var confirmResetProgressFinal by remember { mutableStateOf(false) }
    var irreversibleChecked by remember { mutableStateOf(false) }
    var advancedSettings by remember { mutableStateOf(false) }

    MenuScaffold {
        SectionTitle(stringResource(R.string.settings), stringResource(R.string.fusion_lab))
        if (!advancedSettings) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NeonMenuButton(text = stringResource(R.string.sound), onClick = { saveSettings(settings.copy(soundEnabled = !settings.soundEnabled), analytics, settingsRepository, scope) }, modifier = Modifier.weight(1f), accent = Cyan, filled = settings.soundEnabled)
                NeonMenuButton(text = stringResource(R.string.music), onClick = { saveSettings(settings.copy(musicEnabled = !settings.musicEnabled), analytics, settingsRepository, scope) }, modifier = Modifier.weight(1f), accent = RadiantGold, filled = settings.musicEnabled)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NeonMenuButton(text = stringResource(R.string.haptics), onClick = { saveSettings(settings.copy(hapticsEnabled = !settings.hapticsEnabled), analytics, settingsRepository, scope) }, modifier = Modifier.weight(1f), accent = Electric, filled = settings.hapticsEnabled)
                NeonMenuButton(text = stringResource(R.string.reduced_motion), onClick = { saveSettings(settings.copy(reducedMotion = !settings.reducedMotion), analytics, settingsRepository, scope) }, modifier = Modifier.weight(1f), accent = NeonPink, filled = settings.reducedMotion)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NeonMenuButton(text = stringResource(R.string.language_button, stringResource(settings.language.labelRes())), onClick = { playMenu(audio, settings); saveSettings(settings.copy(language = settings.language.next()), analytics, settingsRepository, scope) }, modifier = Modifier.weight(1f).testTag("settings_language"), accent = Electric)
                NeonMenuButton(text = stringResource(R.string.theme_button, stringResource(settings.themeMode.labelRes())), onClick = { playMenu(audio, settings); saveSettings(settings.copy(themeMode = settings.themeMode.next()), analytics, settingsRepository, scope) }, modifier = Modifier.weight(1f).testTag("settings_theme"), accent = RadiantGold)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NeonMenuButton(text = stringResource(R.string.show_tutorial_again), onClick = { playMenu(audio, settings); saveSettings(settings.copy(tutorialCompleted = false), analytics, settingsRepository, scope); onTutorial() }, modifier = Modifier.weight(1f).testTag("reset_tutorial"), accent = Cyan)
                NeonMenuButton(text = stringResource(R.string.reset_profile), onClick = { advancedSettings = true }, modifier = Modifier.weight(1f), accent = NeonPink)
            }
            NeonMenuButton(text = stringResource(R.string.back), onClick = { playMenu(audio, settings); onBack() }, modifier = Modifier.fillMaxWidth(), accent = Cyan)
        } else {
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
        SocialRetentionSection(socialRepository)
        NeonPanel(title = stringResource(R.string.reset_profile), accent = NeonPink) {
            NeonMenuButton(text = stringResource(R.string.reset_collection), onClick = { playMenu(audio, settings); confirmResetCollection = true }, modifier = Modifier.fillMaxWidth(), accent = NeonPink)
            NeonMenuButton(text = stringResource(R.string.reset_profile), onClick = { playMenu(audio, settings); confirmResetProfile = true }, modifier = Modifier.fillMaxWidth().testTag("reset_profile"), accent = NeonPink)
            Difficulty.entries.forEach { difficulty ->
                NeonMenuButton(text = stringResource(R.string.reset_progress, difficulty.name.lowercase()), onClick = { playMenu(audio, settings); confirmResetDifficulty = difficulty }, modifier = Modifier.fillMaxWidth(), accent = TextMuted)
            }
            NeonMenuButton(
                text = stringResource(R.string.reset_all_progress),
                onClick = { playMenu(audio, settings); confirmResetProgress = true },
                modifier = Modifier.fillMaxWidth().testTag("reset_all_progress"),
                accent = NeonPink,
                filled = true,
            )
        }
        NeonMenuButton(text = stringResource(R.string.back), onClick = { advancedSettings = false }, modifier = Modifier.fillMaxWidth(), accent = Cyan)
        }
    }

    if (confirmResetCollection) {
        ConfirmDialog(
            title = stringResource(R.string.reset_collection_title),
            body = stringResource(R.string.reset_collection_body),
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
            title = stringResource(R.string.reset_difficulty_title, difficulty.localizedLabel()),
            body = stringResource(R.string.reset_difficulty_body),
            onDismiss = { confirmResetDifficulty = null },
            onConfirm = {
                vm.resetDifficulty(difficulty)
                confirmResetDifficulty = null
            },
        )
    }
    if (confirmResetProgress) {
        ConfirmDialog(
            title = stringResource(R.string.reset_all_progress_title),
            body = stringResource(R.string.reset_all_progress_body),
            onDismiss = { confirmResetProgress = false },
            onConfirm = {
                irreversibleChecked = false
                confirmResetProgress = false
                confirmResetProgressFinal = true
            },
        )
    }
    if (confirmResetProgressFinal) {
        QuantumDialog(
            title = stringResource(R.string.reset_all_progress_final_title),
            onDismiss = {
                irreversibleChecked = false
                confirmResetProgressFinal = false
            },
            accent = NeonPink,
            confirmText = stringResource(R.string.reset_all_progress_confirm),
            onConfirm = {
                if (irreversibleChecked) {
                    scope.launch {
                        progressResetRepository.resetAllProgress()
                        vm.newGame(ui.game.difficulty, ui.game.size)
                    }
                    irreversibleChecked = false
                    confirmResetProgressFinal = false
                }
            },
            dismissText = stringResource(R.string.cancel),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = irreversibleChecked,
                    onCheckedChange = { irreversibleChecked = it },
                    modifier = Modifier.testTag("reset_all_progress_irreversible"),
                )
                Text(stringResource(R.string.reset_all_progress_irreversible), color = TextSecondary, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun SocialRetentionSection(socialRepository: SocialRepository) {
    val social by socialRepository.observe().collectAsState(initial = com.battleheim.quantum2048.domain.SocialState())
    val scope = rememberCoroutineScope()
    NeonPanel(title = stringResource(R.string.social_retention), accent = Electric) {
        StatRow(stringResource(R.string.stat_best_duel_streak), formatNumber(social.duelRecord.bestWinStreak))
        StatRow(stringResource(R.string.stat_daily_current_streak), formatNumber(social.dailyStreak.currentStreak))
        StatRow(stringResource(R.string.stat_leaderboard_entries), formatNumber(social.leaderboards.size))
        Text(stringResource(R.string.play_games_offline_note), color = TextMuted, fontSize = 11.sp)
        TextButton(onClick = { scope.launch { socialRepository.clear() } }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.reset_social_progress), color = NeonPink, fontWeight = FontWeight.Bold)
        }
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
    NeonPanel(title = stringResource(R.string.monetization), accent = RadiantGold) {
        Text(
            stringResource(if (entitlements.removeAds) R.string.remove_ads_owned else R.string.remove_ads_not_owned),
            color = if (entitlements.removeAds) Cyan else TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
        StatRow(stringResource(R.string.reward_extra_undo), formatNumber(entitlements.rewardedExtraUndoCredits))
        StatRow(stringResource(R.string.reward_revive), formatNumber(entitlements.rewardedReviveCredits))
        StatRow(stringResource(R.string.reward_daily_attempt), formatNumber(entitlements.rewardedDailyAttemptCredits))
        NeonMenuButton(
            text = stringResource(R.string.test_grant_remove_ads),
            onClick = onRemoveAds,
            enabled = !entitlements.removeAds,
            modifier = Modifier.fillMaxWidth().testTag("grant_remove_ads"),
            accent = RadiantGold,
            filled = !entitlements.removeAds,
        )
        NeonMenuButton(text = stringResource(R.string.watch_for_extra_undo), onClick = onRewardUndo, enabled = adGateway.isRewardedReady, modifier = Modifier.fillMaxWidth(), accent = Cyan)
        NeonMenuButton(text = stringResource(R.string.watch_for_revive), onClick = onRewardRevive, enabled = adGateway.isRewardedReady, modifier = Modifier.fillMaxWidth(), accent = Electric)
        NeonMenuButton(text = stringResource(R.string.watch_for_daily_attempt), onClick = onRewardDaily, enabled = adGateway.isRewardedReady, modifier = Modifier.fillMaxWidth(), accent = NeonPink)
        Text(stringResource(R.string.monetization_offline_note), color = TextMuted, fontSize = 11.sp)
        TextButton(onClick = onReset, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.reset_entitlements), color = NeonPink, fontWeight = FontWeight.Bold)
        }
    }
}

private fun playMenu(audio: GameAudio, settings: AppSettings) {
    if (settings.soundEnabled) audio.menu()
}

private fun quantumEnterTransition(): EnterTransition =
    slideInHorizontally(
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        initialOffsetX = { it / 5 },
    ) + fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow))

private fun quantumExitTransition(): ExitTransition =
    slideOutHorizontally(
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        targetOffsetX = { -it / 8 },
    ) + fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium))

private fun quantumPopEnterTransition(): EnterTransition =
    slideInHorizontally(
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        initialOffsetX = { -it / 6 },
    ) + fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow))

private fun quantumPopExitTransition(): ExitTransition =
    slideOutHorizontally(
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        targetOffsetX = { it / 8 },
    ) + fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium))

private fun playSelect(audio: GameAudio, settings: AppSettings) {
    if (settings.soundEnabled) audio.select()
}

@Composable
private fun SettingsSlider(label: String, value: Float, onValueChange: (Float) -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f), shape)
            .border(1.dp, Cyan.copy(alpha = 0.34f), shape)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Black, fontSize = 14.sp)
            Text("${(value * 100).toInt()}%", color = Cyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
        Slider(
            value = value.coerceIn(0f, 1f),
            onValueChange = { onValueChange(it.coerceIn(0f, 1f)) },
        )
    }
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
    AppLanguage.ENGLISH -> AppLanguage.PERSIAN
    AppLanguage.PERSIAN -> AppLanguage.ENGLISH
}

private fun AppThemeMode.next(): AppThemeMode = when (this) {
    AppThemeMode.LIGHT -> AppThemeMode.DARK
    AppThemeMode.DARK -> AppThemeMode.LIGHT
}

private fun AppLanguage.shortLabel(): String = when (this) {
    AppLanguage.ENGLISH -> "EN"
    AppLanguage.PERSIAN -> "FA"
}

private fun AppLanguage.labelRes(): Int = when (this) {
    AppLanguage.ENGLISH -> R.string.language_english
    AppLanguage.PERSIAN -> R.string.language_persian
}

private fun AppThemeMode.iconLabel(): String = when (this) {
    AppThemeMode.DARK -> "☾"
    AppThemeMode.LIGHT -> "☀"
}

private fun AppThemeMode.labelRes(): Int = when (this) {
    AppThemeMode.DARK -> R.string.theme_dark
    AppThemeMode.LIGHT -> R.string.theme_light
}

@Composable
private fun PauseScreen(vm: GameViewModel, onResume: () -> Unit, onMainMenu: () -> Unit) {
    val ui by vm.ui.collectAsState()
    var confirmRestart by remember { mutableStateOf(false) }
    MenuScaffold(modifier = Modifier.testTag("pause_screen")) {
        SectionTitle(stringResource(R.string.pause), ui.game.difficulty.localizedLabel())
        NeonMenuButton(text = stringResource(R.string.resume), onClick = onResume, modifier = Modifier.fillMaxWidth(), accent = Cyan, filled = true)
        NeonMenuButton(text = stringResource(R.string.new_game), onClick = { confirmRestart = true }, modifier = Modifier.fillMaxWidth(), accent = RadiantGold)
        NeonMenuButton(text = stringResource(R.string.main_menu), onClick = onMainMenu, modifier = Modifier.fillMaxWidth().testTag("pause_main_menu"), accent = Electric)
    }
    if (confirmRestart) {
        ConfirmDialog(
            title = stringResource(R.string.restart_level_title),
            body = stringResource(R.string.restart_level_body),
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
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f), shape)
            .border(1.dp, if (checked) Cyan.copy(alpha = 0.58f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.28f), shape)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = if (checked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Black, fontSize = 14.sp, letterSpacing = 0.3.sp)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Cyan.copy(alpha = 0.74f),
                checkedBorderColor = Cyan,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                uncheckedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
            ),
        )
    }
}

@Composable
private fun ConfirmDialog(title: String, body: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    QuantumDialog(
        title = title,
        onDismiss = onDismiss,
        accent = NeonPink,
        confirmText = stringResource(R.string.confirm),
        onConfirm = onConfirm,
        dismissText = stringResource(R.string.cancel),
    ) {
        Text(body, color = TextSecondary, fontSize = 13.sp)
    }
}

@Composable
internal fun MenuScaffold(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val measurementSink = LocalLayoutMeasurementSink.current
    val density = LocalDensity.current
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Box(
            modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.background,
                        ),
                    ),
                )
                .padding(padding),
        ) {
            SpaceLabBackdrop()
            Column(
                Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(unbounded = true)
                    .align(Alignment.TopCenter)
                    .testTag("menu_content")
                    .onGloballyPositioned { coordinates ->
                        measurementSink?.invoke("menu_content", with(density) { coordinates.size.height.toDp().value })
                    }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                content()
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    val titleColor = MaterialTheme.colorScheme.onBackground
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .padding(end = 12.dp)
                .size(width = 4.dp, height = 56.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.85f),
                            Cyan.copy(alpha = 0.45f),
                            NeonPink.copy(alpha = 0.22f),
                        ),
                    ),
                    RoundedCornerShape(12.dp),
                ),
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                title,
                color = titleColor,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.6.sp,
                lineHeight = 26.sp,
            )
            Text(
                subtitle.uppercase(),
                color = Cyan,
                fontWeight = FontWeight.Black,
                fontSize = 11.sp,
                letterSpacing = 1.2.sp,
            )
            Box(
                Modifier
                    .fillMaxWidth(0.58f)
                    .height(1.dp)
                    .background(Brush.horizontalGradient(listOf(Cyan.copy(alpha = 0.8f), Color.Transparent))),
            )
        }
    }
}

@Composable
private fun NeonPanel(title: String, accent: Color = Cyan, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    val surface = MaterialTheme.colorScheme.surface
    val raised = MaterialTheme.colorScheme.surfaceVariant
    Column(
        Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.085f),
                        surface.copy(alpha = 0.88f),
                        raised.copy(alpha = 0.72f),
                    ),
                ),
                shape,
            )
            .border(1.2.dp, accent.copy(alpha = 0.58f), shape)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
                title.uppercase(),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
            )
            Box(
                Modifier
                    .size(width = 46.dp, height = 2.dp)
                    .background(Brush.horizontalGradient(listOf(accent, Color.Transparent)), RoundedCornerShape(2.dp)),
            )
        }
        content()
    }
}

@Composable
private fun SpaceLabBackdrop() {
    Canvas(Modifier.fillMaxSize()) {
        val orbitCenter = androidx.compose.ui.geometry.Offset(size.width * 0.56f, size.height * 0.2f)
        repeat(3) { index ->
            rotate(degrees = -22f + index * 32f, pivot = orbitCenter) {
                drawOval(
                    color = Cyan.copy(alpha = 0.055f - index * 0.01f),
                    topLeft = androidx.compose.ui.geometry.Offset(
                        orbitCenter.x - size.width * 0.34f,
                        orbitCenter.y - size.height * (0.035f + index * 0.014f),
                    ),
                    size = androidx.compose.ui.geometry.Size(
                        size.width * 0.68f,
                        size.height * (0.07f + index * 0.028f),
                    ),
                    style = Stroke(width = 1.1f),
                )
            }
        }
        listOf(
            androidx.compose.ui.geometry.Offset(size.width * 0.16f, size.height * 0.22f) to Cyan,
            androidx.compose.ui.geometry.Offset(size.width * 0.72f, size.height * 0.24f) to NeonPink,
            androidx.compose.ui.geometry.Offset(size.width * 0.87f, size.height * 0.42f) to RadiantGold,
            androidx.compose.ui.geometry.Offset(size.width * 0.26f, size.height * 0.63f) to Electric,
            androidx.compose.ui.geometry.Offset(size.width * 0.62f, size.height * 0.78f) to Cyan,
        ).forEachIndexed { index, particle ->
            drawCircle(
                color = particle.second.copy(alpha = if (index == 2) 0.2f else 0.14f),
                radius = 1.6f + index % 2,
                center = particle.first,
            )
        }
        drawLine(
            color = Electric.copy(alpha = 0.08f),
            start = androidx.compose.ui.geometry.Offset(size.width * 0.08f, size.height * 0.52f),
            end = androidx.compose.ui.geometry.Offset(size.width * 0.92f, size.height * 0.36f),
            strokeWidth = 0.8f,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun NeonMenuButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accent: Color = Cyan,
    filled: Boolean = false,
    icon: String? = null,
) {
    val shape = RoundedCornerShape(28.dp)
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (filled) accent else MaterialTheme.colorScheme.surface.copy(alpha = 0.76f),
            contentColor = if (filled) MaterialTheme.colorScheme.onPrimary else accent,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f),
            disabledContentColor = TextMuted,
        ),
        modifier = modifier
            .height(58.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        accent.copy(alpha = if (enabled) 0.18f else 0.04f),
                        Color.Transparent,
                        NeonPink.copy(alpha = if (enabled && !filled) 0.12f else 0.02f),
                    ),
                ),
                shape,
            )
            .border(1.2.dp, accent.copy(alpha = if (enabled) 0.78f else 0.25f), shape),
    ) {
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            icon?.let { Text(it, fontSize = 15.sp, modifier = Modifier.padding(end = 7.dp)) }
            Text(
                text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.4.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun NeonPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = Cyan,
) {
    val shape = RoundedCornerShape(24.dp)
    Button(
        onClick = onClick,
        shape = shape,
        colors = ButtonDefaults.buttonColors(containerColor = GlassPanel, contentColor = accent),
        modifier = modifier
            .height(48.dp)
            .border(1.1.dp, accent.copy(alpha = 0.62f), shape),
    ) {
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp)
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
