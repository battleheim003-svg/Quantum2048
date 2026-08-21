package com.battleheim.quantum2048.ui

import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.ComposeView
import com.battleheim.quantum2048.ads.NoOpAdGateway
import com.battleheim.quantum2048.analytics.NoOpAnalyticsGateway
import com.battleheim.quantum2048.audio.SilentGameAudio
import com.battleheim.quantum2048.designsystem.QuantumTheme
import com.battleheim.quantum2048.domain.AppSettings
import com.battleheim.quantum2048.domain.BillingRepository
import com.battleheim.quantum2048.domain.CollectionRepository
import com.battleheim.quantum2048.domain.CollectionState
import com.battleheim.quantum2048.domain.DailyChallengeRepository
import com.battleheim.quantum2048.domain.DailyChallengeState
import com.battleheim.quantum2048.domain.EntitlementState
import com.battleheim.quantum2048.domain.GameRepository
import com.battleheim.quantum2048.domain.ProfileRepository
import com.battleheim.quantum2048.domain.ProfileState
import com.battleheim.quantum2048.domain.ProgressResetRepository
import com.battleheim.quantum2048.domain.RewardEntitlement
import com.battleheim.quantum2048.domain.SettingsRepository
import com.battleheim.quantum2048.domain.SocialRepository
import com.battleheim.quantum2048.domain.SocialState
import com.battleheim.quantum2048.domain.StatisticsRepository
import com.battleheim.quantum2048.domain.StatsSnapshot
import com.battleheim.quantum2048.engine.Compound
import com.battleheim.quantum2048.engine.Difficulty
import com.battleheim.quantum2048.engine.Direction
import com.battleheim.quantum2048.engine.FusionRules
import com.battleheim.quantum2048.engine.GameEngine
import com.battleheim.quantum2048.engine.GameMode
import com.battleheim.quantum2048.engine.GameState
import com.battleheim.quantum2048.engine.QuantumElement
import com.battleheim.quantum2048.engine.SeededRandomProvider
import com.battleheim.quantum2048.engine.Tile
import com.battleheim.quantum2048.engine.TileKind
import com.battleheim.quantum2048.engine.TutorialEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import kotlin.math.roundToInt

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NoScrollRobolectricMeasurementTest {
    @Test
    fun measureMainMenu() = measureScreen("Main Menu") { env ->
        MainMenuScreen(
            vm = env.vm,
            settings = AppSettings(musicEnabled = false),
            settingsRepository = env.settings,
            analytics = NoOpAnalyticsGateway,
            audio = env.audio,
            onContinue = {},
            onNewClassic = {},
            onNewQuantum = {},
            onNewGame = {},
            onCollection = {},
            onAchievements = {},
            onStatistics = {},
            onDailyChallenge = {},
            onAbout = {},
            onPeriodicPath = {},
            onTutorial = {},
            onSettings = {},
        )
    }

    @Test
    fun measureSettings() = measureScreen("Settings") { env ->
        SettingsScreen(env.settings, env.collection, env.profile, env.social, env.billing, env.reset, AppSettings(musicEnabled = false), EntitlementState(), NoOpAdGateway, env.audio, env.vm, NoOpAnalyticsGateway, {}, {})
    }

    @Test
    fun measureGameClassic() = measureScreen("Game Classic") { env ->
        env.vm.newGame(Difficulty.EASY, 4)
        GameScreen(env.vm, AppSettings(musicEnabled = false), env.audio, NoOpAdGateway, {})
    }

    @Test
    fun measureGameQuantum() = measureScreen("Game Quantum") { env ->
        env.vm.newGame(Difficulty.QUANTUM, 4)
        GameScreen(env.vm, AppSettings(musicEnabled = false), env.audio, NoOpAdGateway, {})
    }

    @Test
    fun measureCollapseDialog() = measureScreen("Collapse Dialog") {
        SuperpositionDialog(Tile(1, 2, TileKind.PROTON, superpositionValues = listOf(2, 4, 8)), {}, {})
    }

    @Test
    fun measureLabCodex() = measureScreen("Lab Codex") {
        CompoundLab(GameState(mode = GameMode.QUANTUM, difficulty = Difficulty.QUANTUM), emptyList(), {})
    }

    @Test
    fun measureCollection() = measureScreen("Collection") { env ->
        CollectionScreen(env.collection, env.profile, {})
    }

    @Test
    fun measureStats() = measureScreen("Stats") { env ->
        StatisticsScreen(env.statistics, env.daily, {})
    }

    @Test
    fun measureTutorialSteps() {
        val lessons = generateSequence(TutorialEngine.start()) {
            if (it.step == enumValues<com.battleheim.quantum2048.engine.TutorialStep>().last()) null else TutorialEngine.next(it)
        }.toList()
        lessons.forEach { lesson ->
            measureScreen("Tutorial ${lesson.step}") {
                TutorialScreen(onDone = {}, initialLesson = lesson)
            }
        }
        assertTrue(lessons.isNotEmpty())
    }

    private fun measureScreen(name: String, content: @Composable (MeasurementEnv) -> Unit) {
        val at640 = measureDp(name, 640, content)
        val at800 = measureDp(name, 800, content)
        println("MEASURE $name ${at640.roundToInt()}dp @640 / ${at800.roundToInt()}dp @800")
    }

    private fun measureDp(name: String, heightDp: Int, content: @Composable (MeasurementEnv) -> Unit): Float {
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val activity = controller.get()
        val composeView = ComposeView(activity)
        val env = MeasurementEnv(activity)
        val heights = linkedMapOf<String, Float>()
        activity.setContentView(composeView)
        composeView.setContent {
            CompositionLocalProvider(LocalLayoutMeasurementSink provides { key, height -> heights[key] = height }) {
                QuantumTheme {
                    Box(
                        Modifier.onGloballyPositioned { coordinates ->
                            heights["root"] = coordinates.size.height / activity.resources.displayMetrics.density
                        },
                    ) {
                        content(env)
                    }
                }
            }
        }
        shadowOf(activity.mainLooper).idle()
        val density = activity.resources.displayMetrics.density
        composeView.measure(
            View.MeasureSpec.makeMeasureSpec((360 * density).roundToInt(), View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(((if (name.startsWith("Game ")) heightDp else 2000) * density).roundToInt(), View.MeasureSpec.AT_MOST),
        )
        composeView.layout(0, 0, composeView.measuredWidth, composeView.measuredHeight)
        shadowOf(activity.mainLooper).idle()
        val measured = heights["menu_content"]
            ?: heights["game_screen_content"]
            ?: heights["dialog_content"]
            ?: heights["root"]
            ?: (composeView.measuredHeight / density)
        composeView.disposeComposition()
        controller.pause().stop().destroy()
        return measured
    }

    private class MeasurementEnv(activity: ComponentActivity) {
        val game = FakeGameRepository()
        val collection = FakeCollectionRepository()
        val profile = FakeProfileRepository()
        val social = FakeSocialRepository()
        val statistics = FakeStatisticsRepository()
        val daily = FakeDailyChallengeRepository()
        val settings = FakeSettingsRepository()
        val billing = FakeBillingRepository()
        val reset = FakeProgressResetRepository()
        val audio = SilentGameAudio
        val vm = GameViewModel(game, collection, profile, social, statisticsRepository = statistics, dailyChallengeRepository = daily, engine = GameEngine(SeededRandomProvider(7)))
    }
}

private class FakeGameRepository : GameRepository {
    override fun observe(mode: GameMode): Flow<GameState?> = flowOf(null)
    override fun observe(difficulty: Difficulty): Flow<GameState?> = flowOf(null)
    override fun observe(difficulty: Difficulty, size: Int): Flow<GameState?> = flowOf(null)
    override suspend fun save(state: GameState) = Unit
    override suspend fun clear(mode: GameMode) = Unit
    override suspend fun clear(difficulty: Difficulty) = Unit
    override suspend fun clear(difficulty: Difficulty, size: Int) = Unit
    override suspend fun clearAll() = Unit
}

private class FakeCollectionRepository : CollectionRepository {
    private val state = MutableStateFlow(CollectionState(unlockedElements = QuantumElement.entries.toSet()))
    override fun observe(): Flow<CollectionState> = state
    override suspend fun record(compound: Compound, difficulty: Difficulty, discoveredAtMillis: Long) = Unit
    override suspend fun recordElement(element: QuantumElement) = Unit
    override suspend fun unrecord(compoundSymbol: String) = Unit
    override suspend fun clear() = Unit
}

private class FakeProfileRepository : ProfileRepository {
    override fun observe(): Flow<ProfileState> = flowOf(ProfileState(isQuantumUnlocked = true))
    override suspend fun record(game: GameState) = Unit
    override suspend fun unlockQuantumModes() = Unit
    override suspend fun clear() = Unit
}

private class FakeSettingsRepository : SettingsRepository {
    override fun observe(): Flow<AppSettings> = flowOf(AppSettings(musicEnabled = false))
    override suspend fun save(settings: AppSettings) = Unit
}

private class FakeStatisticsRepository : StatisticsRepository {
    override fun observeStatistics(mode: GameMode): Flow<StatsSnapshot> = flowOf(StatsSnapshot(mode, highestTile = 2048, highScore = 4096, gamesPlayed = 8, totalMerges = 128, longestMergeChain = 5))
    override suspend fun recordMerge(mode: GameMode, count: Int, state: GameState) = Unit
    override suspend fun recordCollapse(mode: GameMode, lowValue: Boolean, manual: Boolean) = Unit
    override suspend fun recordEntangledCollapse(mode: GameMode, count: Int) = Unit
    override suspend fun recordGameEnded(mode: GameMode, state: GameState) = Unit
    override suspend fun clear() = Unit
}

private class FakeDailyChallengeRepository : DailyChallengeRepository {
    override fun observe(): Flow<DailyChallengeState> = flowOf(DailyChallengeState())
    override suspend fun markStarted(date: String) = Unit
    override suspend fun recordResult(date: String, score: Long) = Unit
    override suspend fun clearActiveRun(date: String) = Unit
    override suspend fun clearHistory() = Unit
}

private class FakeSocialRepository : SocialRepository {
    override fun observe(): Flow<SocialState> = flowOf(SocialState())
    override suspend fun recordGame(game: GameState) = Unit
    override suspend fun recordDuelResult(difficulty: Difficulty, opponent: com.battleheim.quantum2048.engine.DuelOpponent, botDifficulty: com.battleheim.quantum2048.engine.BotDifficulty, winner: com.battleheim.quantum2048.engine.DuelPlayer?) = Unit
    override suspend fun syncAchievements(achievementIds: Set<String>) = Unit
    override suspend fun clear() = Unit
}

private class FakeBillingRepository : BillingRepository {
    override fun observe(): Flow<EntitlementState> = flowOf(EntitlementState())
    override suspend fun grant(productId: String) = Unit
    override suspend fun grantReward(reward: RewardEntitlement) = Unit
    override suspend fun consumeReward(reward: RewardEntitlement) = Unit
    override suspend fun clear() = Unit
}

private class FakeProgressResetRepository : ProgressResetRepository {
    override suspend fun resetAllProgress() = Unit
}
