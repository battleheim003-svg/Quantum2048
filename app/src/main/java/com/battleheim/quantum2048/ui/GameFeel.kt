package com.battleheim.quantum2048.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class HapticPattern { LIGHT, MEDIUM, HEAVY }

object HapticFeedbackManager {
    fun perform(haptics: HapticFeedback, pattern: HapticPattern) {
        haptics.performHapticFeedback(
            when (pattern) {
                HapticPattern.LIGHT -> HapticFeedbackType.TextHandleMove
                HapticPattern.MEDIUM -> HapticFeedbackType.LongPress
                HapticPattern.HEAVY -> HapticFeedbackType.LongPress
            },
        )
    }
}

fun GameFeedback.hapticPattern(): HapticPattern = when (this) {
    GameFeedback.MOVE -> HapticPattern.LIGHT
    GameFeedback.MERGE,
    GameFeedback.REACTION,
    GameFeedback.COMPOUND,
    GameFeedback.TUNNEL,
    GameFeedback.COLLAPSE_LOW -> HapticPattern.MEDIUM
    GameFeedback.COLLAPSE_HIGH,
    GameFeedback.GAME_OVER -> HapticPattern.HEAVY
}

fun Modifier.shake(
    isShaking: Boolean,
    reducedMotion: Boolean = false,
    intensity: Dp = 9.dp,
): Modifier = composed {
    val progress = remember { Animatable(0f) }
    val intensityPx = with(LocalDensity.current) { intensity.toPx() }

    LaunchedEffect(isShaking, reducedMotion) {
        if (!isShaking || reducedMotion) {
            progress.snapTo(0f)
            return@LaunchedEffect
        }
        progress.snapTo(1f)
        progress.animateTo(0f, tween(durationMillis = 340, easing = FastOutSlowInEasing))
    }

    graphicsLayer {
        if (progress.value > 0f) {
            val frame = (progress.value * 100f).toInt()
            val direction = if (frame % 2 == 0) 1f else -1f
            val wobble = kotlin.math.sin(frame * 1.7f) * 0.45f
            translationX = direction * intensityPx * progress.value
            translationY = wobble * intensityPx * progress.value
            rotationZ = direction * 0.35f * progress.value
        }
    }
}
