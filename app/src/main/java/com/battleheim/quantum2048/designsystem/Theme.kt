package com.battleheim.quantum2048.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.battleheim.quantum2048.engine.Difficulty
import com.battleheim.quantum2048.engine.QuantumSpecies

val Void = Color(0xFF070A14)
val Panel = Color(0xFF101626)
val PanelRaised = Color(0xFF172033)
val PanelSoft = Color(0xFF1D2740)
val TextPrimary = Color(0xFFF5F8FF)
val TextSecondary = Color(0xFFB7C3E6)
val TextMuted = Color(0xFF7885A8)
val Cyan = Color(0xFF4EF2E4)
val Violet = Color(0xFFA980FF)
val Electric = Color(0xFF5BA7FF)
val Warning = Color(0xFFFFD166)
val Danger = Color(0xFFFF6B7A)

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

@Composable
fun QuantumTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = Typography(), content = content)
}

fun difficultyAccent(difficulty: Difficulty): Color = when (difficulty) {
    Difficulty.EASY -> EasyAccent
    Difficulty.MEDIUM -> MediumAccent
    Difficulty.HARD -> HardAccent
    Difficulty.QUANTUM -> QuantumAccent
}

fun difficultySurface(difficulty: Difficulty): Color = when (difficulty) {
    Difficulty.EASY -> Color(0xFF152238)
    Difficulty.MEDIUM -> Color(0xFF123026)
    Difficulty.HARD -> Color(0xFF332716)
    Difficulty.QUANTUM -> Color(0xFF241B3D)
}

fun elementColor(species: QuantumSpecies?): Color = when (species) {
    QuantumSpecies.ELECTRON -> Color(0xFF2563A7)
    QuantumSpecies.PROTON -> Color(0xFFA53F61)
    QuantumSpecies.HYDROGEN -> Color(0xFF2BAF8A)
    QuantumSpecies.HELIUM, QuantumSpecies.NEON -> Color(0xFF7F6CE1)
    QuantumSpecies.LITHIUM, QuantumSpecies.SODIUM -> Color(0xFFC0A236)
    QuantumSpecies.BERYLLIUM, QuantumSpecies.MAGNESIUM, QuantumSpecies.CALCIUM -> Color(0xFF88A747)
    QuantumSpecies.BORON, QuantumSpecies.SILICON -> Color(0xFFB87949)
    QuantumSpecies.CARBON -> Color(0xFF597084)
    QuantumSpecies.NITROGEN, QuantumSpecies.OXYGEN, QuantumSpecies.CHLORINE -> Color(0xFF2E98A6)
    QuantumSpecies.IRON -> Color(0xFF9E7464)
    QuantumSpecies.GOLD -> Color(0xFFD7AE3E)
    null -> PanelSoft
}

fun elementFamily(species: QuantumSpecies): String = when (species) {
    QuantumSpecies.ELECTRON, QuantumSpecies.PROTON -> "Particle"
    QuantumSpecies.HELIUM, QuantumSpecies.NEON -> "Noble gas"
    QuantumSpecies.LITHIUM, QuantumSpecies.SODIUM -> "Alkali metal"
    QuantumSpecies.BERYLLIUM, QuantumSpecies.MAGNESIUM, QuantumSpecies.CALCIUM -> "Alkaline earth"
    QuantumSpecies.CHLORINE -> "Halogen"
    QuantumSpecies.IRON, QuantumSpecies.GOLD -> "Metal"
    QuantumSpecies.BORON, QuantumSpecies.SILICON -> "Metalloid"
    QuantumSpecies.CARBON, QuantumSpecies.NITROGEN, QuantumSpecies.OXYGEN, QuantumSpecies.HYDROGEN -> "Nonmetal"
}
