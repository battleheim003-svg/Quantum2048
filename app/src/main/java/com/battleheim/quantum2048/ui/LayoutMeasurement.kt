package com.battleheim.quantum2048.ui

import androidx.compose.runtime.staticCompositionLocalOf

internal val LocalLayoutMeasurementSink = staticCompositionLocalOf<((String, Float) -> Unit)?> { null }
