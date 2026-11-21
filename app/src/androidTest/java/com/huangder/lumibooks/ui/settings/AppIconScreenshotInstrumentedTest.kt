package com.huangder.lumibooks.ui.settings

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.huangder.lumibooks.R
import com.huangder.lumibooks.domain.model.AppIconStyle
import com.huangder.lumibooks.ui.theme.EBookReaderTheme
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Pixel-diff regression on a real emulator, with before/after PNG evidence.
 * This compares a real screen before and after user interaction rather than
 * hardcoding device-dependent glyph anti-aliasing into a brittle golden file.
 */
@RunWith(AndroidJUnit4::class)
class AppIconScreenshotInstrumentedTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun changingIconSelectionUpdatesPixelsAndSavesScreenshots() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val classicLabel = context.getString(R.string.icon_style_classic)
        var selected by mutableStateOf(AppIconStyle.LUMI_2.storedValue)
        composeRule.setContent {
            EBookReaderTheme(darkTheme = false) {
                Box(Modifier.fillMaxSize().background(Color.White)) {
                    AppIconStyleOptions(selectedStyle = selected, onSelect = { selected = it })
                }
            }
        }
        composeRule.waitForIdle()
        val before = composeRule.onRoot().captureToImage().asAndroidBitmap()
        composeRule.onNodeWithText(classicLabel).performClick()
        composeRule.mainClock.advanceTimeBy(500L)
        composeRule.waitForIdle()
        composeRule.onNodeWithText(classicLabel).assertIsSelected()
        val after = composeRule.onRoot().captureToImage().asAndroidBitmap()
        val dir = checkNotNull(context.getExternalFilesDir("ci-screenshots")).apply { mkdirs() }
        writePng(before, File(dir, "icon-style-before.png"))
        writePng(after, File(dir, "icon-style-after.png"))
        assertEquals("Image width changed", before.width, after.width)
        assertEquals("Image height changed", before.height, after.height)
        var differences = 0
        for (y in 0 until before.height step 4) {
            for (x in 0 until before.width step 4) {
                if (before.getPixel(x, y) != after.getPixel(x, y)) differences++
            }
        }
        assertTrue("Selection changed semantics but not visible screenshot pixels: diff=$differences", differences > 40)
        before.recycle()
        after.recycle()
    }

    private fun writePng(bitmap: Bitmap, destination: File) {
        destination.outputStream().use { stream ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream))
        }
    }
}
