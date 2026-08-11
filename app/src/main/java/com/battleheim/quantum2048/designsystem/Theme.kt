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
    void = Color(0xFFEAF4FF),
    panel = Color(0xFFDCEBFF),
    panelRaised = Color(0xFFF8FCFF),
    panelSoft = Color(0xFFC9DBF4),
    glassPanel = Color(0xDDF8FCFF),
    boardGlass = Color(0xE6D8E8FF),
    textPrimary = Color(0xFF071A30),
    textSecondary = Color(0xFF25425F),
    textMuted = Color(0xFF627891),
    cyan = Color(0xFF007C89),
    violet = Color(0xFF6541D8),
    electric = Color(0xFF005EB8),
    neonPink = Color(0xFFB80083),
    radiantGold = Color(0xFF8B5D00),
    danger = Color(0xFFC2293F),
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
    Difficulty.EASY -> Color(0xFF102D3C)
    Difficulty.MEDIUM -> Color(0xFF123026)
    Difficulty.HARD -> Color(0xFF332716)
    Difficulty.QUANTUM, Difficulty.ZEN, Difficulty.HARDCORE, Difficulty.PUZZLE, Difficulty.DAILY -> Color(0xFF241B3D)
}

fun classicTileColor(value: Int): Color = when {
    value <= 2 -> Color(0xFF0E7490)
    value <= 4 -> Color(0xFF1D4ED8)
    value <= 8 -> Color(0xFF6D28D9)
    value <= 16 -> Color(0xFFBE185D)
    value <= 32 -> Color(0xFFDC2626)
    value <= 64 -> Color(0xFFEA580C)
    value <= 128 -> Color(0xFFD97706)
    value <= 256 -> Color(0xFF65A30D)
    value <= 512 -> Color(0xFF059669)
    value <= 1024 -> Color(0xFF0891B2)
    value <= 2048 -> Color(0xFFA16207)
    else -> Color(0xFF7C3AED)
}

fun classicTileTextColor(value: Int): Color = when {
    value <= 4 -> Color(0xFFF7FDFF)
    value <= 1024 -> Color.White
    else -> Color(0xFFFFF7D6)
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
    QuantumElement.CARBON -> Color(0xFF9CA3AF)
    QuantumElement.NITROGEN -> Color(0xFF7C83FF)
    QuantumElement.OXYGEN -> OxygenRed
    QuantumElement.FLUORINE -> Color(0xFFB7FF4A)
    QuantumElement.NEON -> NeonPink
    QuantumElement.SODIUM -> Color(0xFFFFD166)
    QuantumElement.SILICON -> Electric
    QuantumElement.PHOSPHORUS -> Color(0xFFFF8A3D)
    QuantumElement.SULFUR -> Color(0xFFFFE45E)
    QuantumElement.CHLORINE -> Color(0xFF7DFF8A)
    QuantumElement.CALCIUM -> Color(0xFFD8F3FF)
    QuantumElement.IRON -> Color(0xFFFFB74A)
    QuantumElement.COPPER -> Color(0xFFFF8F5A)
    QuantumElement.GOLD -> RadiantGold
    null -> PanelSoft
}

fun elementFamily(element: QuantumElement): String = when (element) {
    QuantumElement.HELIUM, QuantumElement.NEON -> "Noble gas"
    QuantumElement.BERYLLIUM, QuantumElement.CALCIUM -> "Alkaline earth"
    QuantumElement.SILICON -> "Metalloid"
    QuantumElement.SODIUM, QuantumElement.IRON, QuantumElement.COPPER, QuantumElement.GOLD -> "Metal"
    QuantumElement.HYDROGEN, QuantumElement.CARBON, QuantumElement.NITROGEN, QuantumElement.OXYGEN, QuantumElement.FLUORINE, QuantumElement.PHOSPHORUS, QuantumElement.SULFUR, QuantumElement.CHLORINE -> "Nonmetal"
}
