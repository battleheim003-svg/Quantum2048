package com.battleheim.quantum2048

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.battleheim.quantum2048.designsystem.QuantumTheme
import com.battleheim.quantum2048.domain.AppLanguage
import com.battleheim.quantum2048.engine.GameEngine
import com.battleheim.quantum2048.engine.KotlinRandomProvider
import com.battleheim.quantum2048.ui.QuantumAppShell
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val app: QuantumApp
        get() = application as QuantumApp

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val app = application as QuantumApp
            val settings by app.settingsRepository.observe().collectAsState(initial = com.battleheim.quantum2048.domain.AppSettings())
            val localized = LocalContext.current.localized(settings.language)
            CompositionLocalProvider(
                LocalContext provides localized,
                LocalLayoutDirection provides settings.language.layoutDirection(),
            ) {
                QuantumTheme(themeMode = settings.themeMode) {
                QuantumAppShell(
                    gameRepository = app.repository,
                    collectionRepository = app.collectionRepository,
                    profileRepository = app.profileRepository,
                    settingsRepository = app.settingsRepository,
                    socialRepository = app.socialRepository,
                    statisticsRepository = app.statisticsRepository,
                    dailyChallengeRepository = app.dailyChallengeRepository,
                    billingRepository = app.billingRepository,
                    levelCatalogRepository = app.levelCatalogRepository,
                    levelProgressRepository = app.levelProgressRepository,
                    adGateway = app.adGateway,
                    analytics = app.analyticsGateway,
                    engine = GameEngine(KotlinRandomProvider()),
                )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        app.analyticsGateway.logSessionStart()
    }

    override fun onStop() {
        app.analyticsGateway.logSessionEnd()
        super.onStop()
    }
}

private fun AppLanguage.layoutDirection(): LayoutDirection = when (this) {
    AppLanguage.ENGLISH -> LayoutDirection.Ltr
    AppLanguage.PERSIAN -> LayoutDirection.Rtl
}

private fun Context.localized(language: AppLanguage): Context {
    val tag = when (language) {
        AppLanguage.ENGLISH -> "en"
        AppLanguage.PERSIAN -> "fa"
    }
    val config = resources.configuration
    val next = android.content.res.Configuration(config)
    next.setLocale(Locale.forLanguageTag(tag))
    next.setLayoutDirection(Locale.forLanguageTag(tag))
    return createConfigurationContext(next)
}
