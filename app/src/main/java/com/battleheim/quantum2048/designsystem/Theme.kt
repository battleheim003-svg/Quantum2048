package com.battleheim.quantum2048.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.intl.Locale
import com.battleheim.quantum2048.R
import com.battleheim.quantum2048.domain.AppLanguage
import com.battleheim.quantum2048.domain.AppThemeMode
import com.battleheim.quantum2048.engine.Difficulty
import com.battleheim.quantum2048.engine.QuantumElement
import com.battleheim.quantum2048.engine.TileKind

val Void = Color(0xFF050711)
val Panel = Color(0xFF0D1224)
val PanelRaised = Color(0xFF151B31)
val PanelSoft = Color(0xFF1C2440)
val TextPrimary = Color(0xFFF7FAFF)
val TextSecondary = Color(0xFFC1CCEA)
val TextMuted = Color(0xFF7F8BAA)
val Cyan = Color(0xFF35F6E8)
val Violet = Color(0xFFB276FF)
val Electric = Color(0xFF4D9CFF)
val Warning = Color(0xFFFFD166)
val Danger = Color(0xFFFF6B7A)

val EasyAccent = Color(0xFF58C7FF)
val MediumAccent = Color(0xFF35F6A5)
val HardAccent = Color(0xFFFFC857)
val QuantumAccent = Color(0xFFB276FF)

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

private val persianFont = FontFamily(
    Font(R.font.a_nafis, FontWeight.Normal),
    Font(R.font.a_nafis, FontWeight.Medium),
    Font(R.font.a_nafis, FontWeight.SemiBold),
    Font(R.font.a_nafis, FontWeight.Bold),
    Font(R.font.a_nafis, FontWeight.ExtraBold),
    Font(R.font.a_nafis, FontWeight.Black),
)

private val englishFont = FontFamily(
    Font(R.font.touche_light, FontWeight.Light),
    Font(R.font.touche_regular, FontWeight.Normal),
    Font(R.font.touche_medium, FontWeight.Medium),
    Font(R.font.touche_semibold, FontWeight.SemiBold),
    Font(R.font.touche_bold, FontWeight.Bold),
    Font(R.font.touche_bold, FontWeight.ExtraBold),
    Font(R.font.touche_bold, FontWeight.Black),
)

private fun appTypography(language: AppLanguage): Typography {
    val currentLanguage = Locale.current.language
    val font = when (language) {
        AppLanguage.PERSIAN -> persianFont
        AppLanguage.ENGLISH -> englishFont
        AppLanguage.SYSTEM -> if (currentLanguage == "fa") persianFont else englishFont
    }
    return Typography(
        displayLarge = TextStyle(fontFamily = font, fontWeight = FontWeight.Black, fontSize = 42.sp),
        headlineLarge = TextStyle(fontFamily = font, fontWeight = FontWeight.Black, fontSize = 34.sp),
        headlineSmall = TextStyle(fontFamily = font, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp),
        titleLarge = TextStyle(fontFamily = font, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp),
        titleMedium = TextStyle(fontFamily = font, fontWeight = FontWeight.Bold, fontSize = 16.sp),
        bodyLarge = TextStyle(fontFamily = font, fontWeight = FontWeight.Normal, fontSize = 16.sp),
        bodyMedium = TextStyle(fontFamily = font, fontWeight = FontWeight.Normal, fontSize = 14.sp),
        labelLarge = TextStyle(fontFamily = font, fontWeight = FontWeight.Bold, fontSize = 14.sp),
    )
}

@Composable
fun QuantumTheme(themeMode: AppThemeMode = AppThemeMode.DARK, language: AppLanguage = AppLanguage.PERSIAN, content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
    }
    val lightScheme = lightColorScheme(
        primary = Color(0xFF00B8B4),
        secondary = Color(0xFF6E54C8),
        tertiary = Color(0xFF1976D2),
        background = Color(0xFFF5FDFF),
        surface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFE4F4F6),
        error = Color(0xFFBA1A1A),
        onPrimary = Color.White,
        onSecondary = Color.White,
        onBackground = Color(0xFF1C1B1F),
        onSurface = Color(0xFF1C1B1F),
        onSurfaceVariant = Color(0xFF49454F),
    )
    MaterialTheme(colorScheme = if (dark) scheme else lightScheme, typography = appTypography(language), content = content)
}

fun difficultyAccent(difficulty: Difficulty): Color = when (difficulty) {
    Difficulty.EASY -> EasyAccent
    Difficulty.MEDIUM -> MediumAccent
    Difficulty.HARD -> HardAccent
    Difficulty.QUANTUM, Difficulty.ZEN, Difficulty.HARDCORE, Difficulty.PUZZLE, Difficulty.DAILY -> QuantumAccent
}

fun difficultySurface(difficulty: Difficulty): Color = when (difficulty) {
    Difficulty.EASY -> Color(0xFF10213D)
    Difficulty.MEDIUM -> Color(0xFF102C24)
    Difficulty.HARD -> Color(0xFF302513)
    Difficulty.QUANTUM, Difficulty.ZEN, Difficulty.HARDCORE, Difficulty.PUZZLE, Difficulty.DAILY -> Color(0xFF211338)
}

fun tileKindColor(kind: TileKind): Color = when (kind) {
    TileKind.ELECTRON -> Color(0xFF2563A7)
    TileKind.PROTON -> Color(0xFFA53F61)
    TileKind.ELEMENT -> Color(0xFF2BAF8A)
    TileKind.CLASSIC -> PanelSoft
}

fun elementColor(element: QuantumElement?): Color = when (element) {
    QuantumElement.HYDROGEN -> Color(0xFF2F7B4B)
    QuantumElement.HELIUM, QuantumElement.NEON -> Color(0xFF4F8608)
    QuantumElement.BERYLLIUM -> Color(0xFF2B62A3)
    QuantumElement.OXYGEN -> Color(0xFF2F7B4B)
    QuantumElement.SILICON -> Color(0xFFAA6420)
    QuantumElement.IRON -> Color(0xFFAE3244)
    QuantumElement.GOLD -> Color(0xFFB98207)
    null -> PanelSoft
}

fun elementFamily(element: QuantumElement): String = when (element) {
    QuantumElement.HELIUM, QuantumElement.NEON -> "Noble gas"
    QuantumElement.BERYLLIUM -> "Alkaline earth"
    QuantumElement.SILICON -> "Metalloid"
    QuantumElement.IRON, QuantumElement.GOLD -> "Metal"
    QuantumElement.HYDROGEN, QuantumElement.OXYGEN -> "Nonmetal"
}
