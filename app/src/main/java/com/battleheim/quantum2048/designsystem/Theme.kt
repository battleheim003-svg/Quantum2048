package com.battleheim.quantum2048.designsystem

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Void = Color(0xFF070A18); val Panel = Color(0xFF11162D); val Cyan = Color(0xFF37F5E8); val Violet = Color(0xFF9A6CFF); val Electric = Color(0xFF3D8BFF)
private val scheme = darkColorScheme(primary = Cyan, secondary = Violet, tertiary = Electric, background = Void, surface = Panel, onBackground = Color(0xFFF4F7FF), onSurface = Color(0xFFF4F7FF))
@Composable fun QuantumTheme(content: @Composable () -> Unit) = MaterialTheme(colorScheme = scheme, typography = Typography(), content = content)
