package com.battleheim.quantum2048.ui

import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.platform.ComposeView
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RobolectricComposeSmokeTest {
    @Test
    fun rendersTextHeadlessly() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val composeView = ComposeView(activity)
        activity.setContentView(composeView)

        composeView.setContent {
            Text("compose-smoke")
        }
        shadowOf(activity.mainLooper).idle()
        composeView.measure(
            View.MeasureSpec.makeMeasureSpec(360, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(640, View.MeasureSpec.AT_MOST),
        )
        composeView.layout(0, 0, composeView.measuredWidth, composeView.measuredHeight)

        assertTrue(composeView.measuredWidth == 360)
        assertTrue(composeView.measuredHeight > 0)
    }
}
