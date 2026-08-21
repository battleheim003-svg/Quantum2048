package com.battleheim.quantum2048.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.battleheim.quantum2048.domain.AppThemeMode
import com.battleheim.quantum2048.engine.Difficulty
import com.battleheim.quantum2048.engine.FusionRules
import com.battleheim.quantum2048.engine.QuantumElement
import com.battleheim.quantum2048.engine.TileKind

val BrandDarkBackground = Color(0xFF1B1931)
val BrandDarkSurface = Color(0xFF44174E)
val BrandDarkSurfaceVariant = Color(0xFF662249)
val BrandDarkPrimary = Color(0xFFED9E59)
val BrandDarkSecondary = Color(0xFFE9BCB9)
val BrandDarkOnSurface = Color(0xFFF8F1F3)

val BrandLightPrimary = Color(0xFF0D1E4C)
val BrandLightOnSurface = Color(0xFF0B1B32)
val BrandLightSecondary = Color(0xFF26415E)
val BrandLightSurfaceVariant = Color(0xFF83A6CE)
val BrandLightSurface = Color(0xFFC48CB3)
val BrandLightBackground = Color(0xFFE5C9D7)

val Void = BrandDarkBackground
val Panel = BrandDarkSurface
val PanelRaised = lerp(BrandDarkSurface, BrandDarkSecondary, 0.10f)
val PanelSoft = BrandDarkSurfaceVariant
val GlassPanel = BrandDarkSurface.copy(alpha = 0.72f)
val BoardGlass = BrandDarkBackground.copy(alpha = 0.86f)
val TextPrimary = BrandDarkOnSurface
val TextSecondary = BrandDarkSecondary
val TextMuted = lerp(BrandDarkSurfaceVariant, BrandDarkSecondary, 0.58f)
val Cyan = BrandDarkPrimary
val Violet = BrandDarkSecondary
val Electric = lerp(BrandDarkPrimary, BrandDarkSecondary, 0.35f)
val Warning = BrandDarkPrimary
val Danger = Color(0xFFF2B8B5)
val NeonPink = BrandDarkSurfaceVariant
val AcidGreen = BrandDarkSecondary
val OxygenRed = BrandDarkSurfaceVariant
val RadiantGold = BrandDarkPrimary

val EasyAccent = BrandDarkPrimary
val MediumAccent = BrandDarkSecondary
val HardAccent = BrandDarkPrimary
val QuantumAccent = BrandDarkSecondary

private val darkScheme = darkColorScheme(
    primary = BrandDarkPrimary,
    secondary = BrandDarkSecondary,
    tertiary = lerp(BrandDarkPrimary, BrandDarkSecondary, 0.45f),
    background = BrandDarkBackground,
    surface = BrandDarkSurface,
    surfaceVariant = BrandDarkSurfaceVariant,
    onPrimary = BrandDarkBackground,
    onSecondary = BrandDarkBackground,
    onBackground = BrandDarkOnSurface,
    onSurface = BrandDarkOnSurface,
    onSurfaceVariant = BrandDarkSecondary,
    outline = lerp(BrandDarkSurfaceVariant, BrandDarkSecondary, 0.50f),
    outlineVariant = lerp(BrandDarkSurface, BrandDarkSecondary, 0.26f),
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
    void = BrandDarkBackground,
    panel = BrandDarkSurface,
    panelRaised = lerp(BrandDarkSurface, BrandDarkSecondary, 0.10f),
    panelSoft = BrandDarkSurfaceVariant,
    glassPanel = BrandDarkSurface.copy(alpha = 0.72f),
    boardGlass = BrandDarkBackground.copy(alpha = 0.86f),
    textPrimary = BrandDarkOnSurface,
    textSecondary = BrandDarkSecondary,
    textMuted = lerp(BrandDarkSurfaceVariant, BrandDarkSecondary, 0.58f),
    cyan = BrandDarkPrimary,
    violet = BrandDarkSecondary,
    electric = lerp(BrandDarkPrimary, BrandDarkSecondary, 0.35f),
    neonPink = BrandDarkSurfaceVariant,
    radiantGold = BrandDarkPrimary,
    danger = Danger,
)

val LightQuantumPalette = QuantumPalette(
    void = BrandLightBackground,
    panel = BrandLightSurface,
    panelRaised = lerp(BrandLightSurface, BrandLightBackground, 0.25f),
    panelSoft = BrandLightSurfaceVariant,
    glassPanel = BrandLightSurface.copy(alpha = 0.82f),
    boardGlass = BrandLightSurfaceVariant.copy(alpha = 0.74f),
    textPrimary = BrandLightOnSurface,
    textSecondary = BrandLightSecondary,
    textMuted = lerp(BrandLightSurfaceVariant, BrandLightOnSurface, 0.42f),
    cyan = BrandLightPrimary,
    violet = BrandLightSecondary,
    electric = lerp(BrandLightPrimary, BrandLightSurfaceVariant, 0.35f),
    neonPink = BrandLightSurface,
    radiantGold = BrandLightPrimary,
    danger = Color(0xFFBA1A1A),
)

val LocalQuantumPalette = compositionLocalOf { DarkQuantumPalette }

@Composable
fun quantumPalette(): QuantumPalette = LocalQuantumPalette.current

@Composable
fun QuantumTheme(themeMode: AppThemeMode = AppThemeMode.DARK, content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
    }
    val lightScheme = lightColorScheme(
        primary = BrandLightPrimary,
        secondary = BrandLightSecondary,
        tertiary = lerp(BrandLightPrimary, BrandLightSurfaceVariant, 0.35f),
        background = BrandLightBackground,
        surface = BrandLightSurface,
        surfaceVariant = BrandLightSurfaceVariant,
        onPrimary = BrandLightBackground,
        onSecondary = BrandLightBackground,
        onBackground = BrandLightOnSurface,
        onSurface = BrandLightOnSurface,
        onSurfaceVariant = BrandLightSecondary,
        outline = lerp(BrandLightSecondary, BrandLightSurfaceVariant, 0.46f),
        outlineVariant = lerp(BrandLightSurface, BrandLightSurfaceVariant, 0.55f),
    )
    androidx.compose.runtime.CompositionLocalProvider(LocalQuantumPalette provides if (dark) DarkQuantumPalette else LightQuantumPalette) {
        MaterialTheme(colorScheme = if (dark) darkScheme else lightScheme, typography = Typography(), content = content)
    }
}

fun difficultyAccent(difficulty: Difficulty): Color = when (difficulty) {
    Difficulty.EASY -> EasyAccent
    Difficulty.MEDIUM -> MediumAccent
    Difficulty.HARD -> HardAccent
    Difficulty.QUANTUM, Difficulty.ZEN, Difficulty.HARDCORE, Difficulty.PUZZLE, Difficulty.DAILY -> QuantumAccent
}

fun difficultySurface(difficulty: Difficulty): Color = when (difficulty) {
    Difficulty.EASY -> lerp(BrandDarkBackground, BrandDarkPrimary, 0.20f)
    Difficulty.MEDIUM -> lerp(BrandDarkBackground, BrandDarkSecondary, 0.18f)
    Difficulty.HARD -> lerp(BrandDarkSurface, BrandDarkPrimary, 0.24f)
    Difficulty.QUANTUM, Difficulty.ZEN, Difficulty.HARDCORE, Difficulty.PUZZLE, Difficulty.DAILY -> BrandDarkSurface
}

fun classicTileColor(value: Int): Color = when {
    value <= 2 -> BrandDarkSurface
    value <= 4 -> BrandDarkSurfaceVariant
    value <= 8 -> lerp(BrandDarkSurfaceVariant, BrandDarkPrimary, 0.18f)
    value <= 16 -> lerp(BrandDarkSurfaceVariant, BrandDarkPrimary, 0.32f)
    value <= 32 -> lerp(BrandDarkSurfaceVariant, BrandDarkPrimary, 0.46f)
    value <= 64 -> lerp(BrandDarkSurfaceVariant, BrandDarkPrimary, 0.60f)
    value <= 128 -> lerp(BrandDarkPrimary, BrandDarkSecondary, 0.20f)
    value <= 256 -> lerp(BrandDarkPrimary, BrandDarkSecondary, 0.36f)
    value <= 512 -> lerp(BrandDarkPrimary, BrandDarkSecondary, 0.52f)
    value <= 1024 -> lerp(BrandDarkPrimary, BrandDarkSecondary, 0.68f)
    value <= 2048 -> BrandDarkPrimary
    else -> BrandDarkSecondary
}

fun classicTileTextColor(value: Int): Color = when {
    value <= 64 -> BrandDarkOnSurface
    else -> BrandDarkBackground
}

fun tileKindColor(kind: TileKind): Color = when (kind) {
    TileKind.ELECTRON -> BrandDarkSurfaceVariant
    TileKind.PROTON -> BrandDarkPrimary
    TileKind.ELEMENT -> Cyan
    TileKind.CLASSIC -> PanelSoft
}

fun elementColor(element: QuantumElement?): Color = when (element) {
    null -> PanelSoft
    else -> elementRampColor(element, dark = true)
}

@Composable
fun themedElementColor(element: QuantumElement?): Color =
    elementRampColor(element, dark = LocalQuantumPalette.current === DarkQuantumPalette)

fun elementRampColor(element: QuantumElement?, dark: Boolean): Color {
    if (element == null) return if (dark) BrandDarkSurfaceVariant else BrandLightSurfaceVariant
    val colors = if (dark) {
        listOf(BrandDarkPrimary, lerp(BrandDarkPrimary, BrandDarkSurfaceVariant, 0.42f), BrandDarkSurfaceVariant, BrandDarkSurface)
    } else {
        listOf(BrandLightPrimary, BrandLightSurfaceVariant, BrandLightSurface, BrandLightBackground)
    }
    val index = FusionRules.elementChain.indexOf(element).coerceAtLeast(0)
    val t = index.toFloat() / (FusionRules.elementChain.lastIndex).coerceAtLeast(1).toFloat()
    val scaled = t * (colors.lastIndex)
    val start = scaled.toInt().coerceIn(0, colors.lastIndex - 1)
    val localT = scaled - start
    return lerp(colors[start], colors[start + 1], localT)
}

fun elementFamily(element: QuantumElement): String = when (element) {
    QuantumElement.HELIUM, QuantumElement.NEON -> "Noble gas"
    QuantumElement.BERYLLIUM, QuantumElement.CALCIUM -> "Alkaline earth"
    QuantumElement.SILICON -> "Metalloid"
    QuantumElement.LITHIUM, QuantumElement.SODIUM, QuantumElement.IRON, QuantumElement.COPPER, QuantumElement.GOLD -> "Metal"
    QuantumElement.BORON -> "Metalloid"
    QuantumElement.HYDROGEN, QuantumElement.CARBON, QuantumElement.NITROGEN, QuantumElement.OXYGEN, QuantumElement.FLUORINE, QuantumElement.PHOSPHORUS, QuantumElement.SULFUR, QuantumElement.CHLORINE -> "Nonmetal"
}
