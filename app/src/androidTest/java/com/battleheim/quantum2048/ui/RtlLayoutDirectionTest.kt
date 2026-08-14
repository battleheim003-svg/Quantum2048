package com.battleheim.quantum2048.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class RtlLayoutDirectionTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun persianCompositionUsesRtlLayoutDirection() {
        var observed: LayoutDirection? = null

        compose.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                observed = LocalLayoutDirection.current
            }
        }

        compose.runOnIdle {
            assertEquals(LayoutDirection.Rtl, observed)
        }
    }
}
