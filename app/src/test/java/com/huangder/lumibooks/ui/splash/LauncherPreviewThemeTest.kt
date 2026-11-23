package com.huangder.lumibooks.ui.splash

import android.app.Application
import android.util.TypedValue
import androidx.test.core.app.ApplicationProvider
import com.huangder.lumibooks.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Resolve the actual Android themes/manifest label, not the source XML as text. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28, 35], application = Application::class, qualifiers = "zh-rCN")
class LauncherPreviewThemeTest {
    @Test fun classicNativeLaunchWindowCannotRenderTheOldBitmap() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val theme = context.resources.newTheme().apply {
            applyStyle(R.style.Theme_EBookReader_Launch_Classic, true)
        }
        val background = TypedValue()
        assertTrue(theme.resolveAttribute(android.R.attr.windowBackground, background, true))
        assertEquals(R.drawable.splash_background_dawn, background.resourceId)
        assertEquals("LUMI", context.applicationInfo.loadLabel(context.packageManager).toString())
    }
}
