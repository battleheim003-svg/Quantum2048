package com.battleheim.quantum2048.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import com.battleheim.quantum2048.domain.AppThemeMode
import com.battleheim.quantum2048.engine.Difficulty
import com.battleheim.quantum2048.engine.QuantumElement
import com.battleheim.quantum2048.engine.TileKind

val Void = Color(0xFF070A14)
val Panel = Color(0xFF101626)
val PanelRaised = Color(0xFF172033)
val PanelSoft = Color(0xFF1D2740)
val GlassPanel = Color(0x6616203A)
val BoardGlass = Color(0xCC0B1228)
val TextPrimary = Color(0xFFF5F8FF)
val TextSecondary = Color(0xFFB7C3E6)
val TextMuted = Color(0xFF7885A8)
val Cyan = Color(0xFF4EF2E4)
val Violet = Color(0xFFA980FF)
val Electric = Color(0xFF5BA7FF)
val Warning = Color(0xFFFFD166)
val Danger = Color(0xFFFF6B7A)
val NeonPink = Color(0xFFFF4FD8)
val AcidGreen = Color(0xFFB8FF38)
val OxygenRed = Color(0xFFFF5A36)
val RadiantGold = Color(0xFFFFD95A)

val EasyAccent = Color(0xFF8EC5FF)
val MediumAccent = Color(0xFF56E0B5)
val HardAccent = Color(0xFFFFC857)
val QuantumAccent = Color(0xFFB894FF)

private val scheme = darkColorScheme(
    primary = Cyan,
    secondary = Violet,
    tertiary = Electric,
    background = Void,
    surface = Panel,
    surfaceVariant = PanelSoft,
    error = Danger,
    onPrimary = Color(0xFF001C22),
    onSecondary = Color(0xFF160E24),
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
)

@Immutable
data class QuantumPalette(
    val void: Color,
    val panel: Color,
    val panelRaised: Color,
    val panelSoft: Color,
    val glassPanel: Color,
    val boardGlass: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val cyan: Color,
    val violet: Color,
    val electric: Color,
    val neonPink: Color,
    val radiantGold: Color,
    val danger: Color,
)

val DarkQuantumPalette = QuantumPalette(
    void = Void,
    panel = Panel,
    panelRaised = PanelRaised,
    panelSoft = PanelSoft,
    glassPanel = GlassPanel,
    boardGlass = BoardGlass,
    textPrimary = TextPrimary,
    textSecondary = TextSecondary,
    textMuted = TextMuted,
    cyan = Cyan,
    violet = Violet,
    electric = Electric,
    neonPink = NeonPink,
    radiantGold = RadiantGold,
    danger = Danger,
)

val LightQuantumPalette = QuantumPalette(
    void = Color(0xFFF3F8FF),
    panel = Color(0xFFEAF2FF),
    panelRaised = Color(0xFFFFFFFF),
    panelSoft = Color(0xFFDCE9FF),
    glassPanel = Color(0xCCFFFFFF),
    boardGlass = Color(0xDDE8F1FF),
    textPrimary = Color(0xFF06172A),
    textSecondary = Color(0xFF314968),
    textMuted = Color(0xFF6E7F95),
    cyan = Color(0xFF007D8B),
    violet = Color(0xFF714DE8),
    electric = Color(0xFF006DCC),
    neonPink = Color(0xFFC11690),
    radiantGold = Color(0xFF9B6B00),
    danger = Color(0xFFC93046),
)

val LocalQuantumPalette = compositionLocalOf { DarkQuantumPalette }

@Composable
fun quantumPalette(): QuantumPalette = LocalQuantumPalette.current

@Composable
fun QuantumTheme(themeMode: AppThemeMode = AppThemeMode.DARK, content: @Composable () -> Unit) {
    val dark = themeMode == AppThemeMode.DARK
    val lightScheme = lightColorScheme(
        primary = LightQuantumPalette.cyan,
        secondary = LightQuantumPalette.violet,
        tertiary = LightQuantumPalette.electric,
        background = LightQuantumPalette.void,
        surface = LightQuantumPalette.panelRaised,
        surfaceVariant = LightQuantumPalette.panelSoft,
        error = LightQuantumPalette.danger,
        onPrimary = Color.White,
        onSecondary = Color.White,
        onBackground = LightQuantumPalette.textPrimary,
        onSurface = LightQuantumPalette.textPrimary,
        onSurfaceVariant = LightQuantumPalette.textSecondary,
    )
    androidx.compose.runtime.CompositionLocalProvider(LocalQuantumPalette provides if (dark) DarkQuantumPalette else LightQuantumPalette) {
        MaterialTheme(colorScheme = if (dark) scheme else lightScheme, typography = Typography(), content = content)
    }
}

fun difficultyAccent(difficulty: Difficulty): Color = when (difficulty) {
    Difficulty.EASY -> EasyAccent
    Difficulty.MEDIUM -> MediumAccent
    Difficulty.HARD -> HardAccent
    Difficulty.QUANTUM, Difficulty.ZEN, Difficulty.HARDCORE, Difficulty.PUZZLE, Difficulty.DAILY -> QuantumAccent
}

fun difficultySurface(difficulty: Difficulty): Color = when (difficulty) {
    Difficulty.EASY -> Color(0xFF152238)
    Difficulty.MEDIUM -> Color(0xFF123026)
    Difficulty.HARD -> Color(0xFF332716)
    Difficulty.QUANTUM, Difficulty.ZEN, Difficulty.HARDCORE, Difficulty.PUZZLE, Difficulty.DAILY -> Color(0xFF241B3D)
}

fun tileKindColor(kind: TileKind): Color = when (kind) {
    TileKind.ELECTRON -> Color(0xFF1D5DFF)
    TileKind.PROTON -> Color(0xFFFF3D8D)
    TileKind.ELEMENT -> Cyan
    TileKind.CLASSIC -> PanelSoft
}

fun elementColor(element: QuantumElement?): Color = when (element) {
    QuantumElement.HYDROGEN -> Color(0xFF72F7FF)
    QuantumElement.HELIUM -> Violet
    QuantumElement.BERYLLIUM -> AcidGreen
    QuantumElement.OXYGEN -> OxygenRed
    QuantumElement.NEON -> NeonPink
    QuantumElement.SILICON -> Electric
    QuantumElement.IRON -> Color(0xFFFFB74A)
    QuantumElement.GOLD -> RadiantGold
    null -> PanelSoft
}

fun elementFamily(element: QuantumElement): String = when (element) {
    QuantumElement.HELIUM, QuantumElement.NEON -> "Noble gas"
    QuantumElement.BERYLLIUM -> "Alkaline earth"
    QuantumElement.SILICON -> "Metalloid"
    QuantumElement.IRON, QuantumElement.GOLD -> "Metal"
    QuantumElement.HYDROGEN, QuantumElement.OXYGEN -> "Nonmetal"
}
