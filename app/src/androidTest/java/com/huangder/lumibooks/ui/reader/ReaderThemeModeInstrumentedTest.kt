package com.huangder.lumibooks.ui.reader

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.huangder.lumibooks.R
import com.huangder.lumibooks.domain.model.ReaderLayoutTarget
import com.huangder.lumibooks.domain.model.ReaderThemeSettings
import com.huangder.lumibooks.domain.model.ReaderThemeSuite
import com.huangder.lumibooks.ui.theme.EBookReaderTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Real settings-sheet controls and the same theme resolution used by ReaderScreen.
 * This is a component integration test, not a full book-opening Activity journey.
 */
@RunWith(AndroidJUnit4::class)
class ReaderThemeModeInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    @Test fun dayNightSwitchesKeepSliderAndRenderStateInSyncEvenWhenEditingDay() {
        val layout = ReaderLayoutTarget.READER_LAYOUT
        val light = ReaderThemeSettings(textColor = 0xFF111111.toInt())
        val dark = ReaderThemeSettings(
            backgroundSelection = "night", backgroundColorSelection = "night",
            textColor = 0xFFFFFFFF.toInt()
        )
        val suite = mutableStateOf(ReaderThemeSuite(
            id = "day", settings = light, lightSettings = light, darkSettings = dark
        ))
        val mode = mutableStateOf("day")
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Reproduce the original mismatch: appearance editing remains on the day tab
        // while the display switches to night. Typography must not depend on that tab.
        val editingDark = false
        fun renderState() = ReaderUiState(readerThemeSuites = listOf(suite.value))
            .withActiveThemeSettingsForMode(mode.value == "night")
        fun setSize(size: Float) {
            suite.value = suite.value.withModeSettings(layout, editingDark,
                suite.value.settingsFor(layout, editingDark).copy(fontSize = size))
        }

        compose.setContent {
            val state = renderState()
            val editing = suite.value.settingsFor(layout, editingDark)
            EBookReaderTheme(appTheme = "lumi", darkTheme = mode.value == "night") {
                Box(Modifier.fillMaxSize()) {
                    ThemeSettingsSheet(
                        visible = true,
                        currentFontSize = editing.fontSize,
                        currentFontType = editing.fontType,
                        currentBodyFontWeight = editing.bodyFontWeight,
                        currentTheme = state.readerTheme,
                        readerThemeSuites = listOf(suite.value),
                        currentDisplayMode = mode.value,
                        editingDark = editingDark,
                        isAppDark = mode.value == "night",
                        supportsPageTransitions = false,
                        onDisplayModeChange = { mode.value = it },
                        onFontSizeChange = ::setSize,
                        onFontSizePreview = ::setSize,
                        onThemeChange = {},
                        onOpenAdvanced = {},
                        onDismiss = {}
                    )
                }
            }
        }

        fun visibleSlider() = compose.onNodeWithTag("reader-font-size-slider")
            .performScrollTo().assertIsDisplayed()

        fun sliderSize(expected: Float) {
            val actual = visibleSlider()
                .fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo].current
            assertEquals(expected, actual, 0.01f)
            compose.runOnIdle { assertEquals(expected, renderState().fontSize, 0.01f) }
        }
        fun setSlider(size: Float) {
            visibleSlider().performSemanticsAction(SemanticsActions.SetProgress) { action ->
                org.junit.Assert.assertTrue("Slider rejected size $size", action(size))
            }
            sliderSize(size)
        }
        fun switch(label: Int, expectedMode: String, color: Int) {
            // The display selector is below the fold in the scrollable sheet.
            // A click on a clipped semantics node can miss its actual hit target.
            val option = compose.onNodeWithContentDescription(context.getString(label))
            option.performScrollTo().assertIsDisplayed().performClick()
            compose.runOnIdle {
                assertEquals("Display selection callback", expectedMode, mode.value)
                assertEquals(color, renderState().readerTextColor)
            }
            option.assertIsSelected()
        }
        setSlider(26f)
        repeat(3) { iteration ->
            switch(R.string.display_mode_night, "night", 0xFFFFFFFF.toInt())
            sliderSize(26f + iteration)
            setSlider(27f + iteration)
            switch(R.string.display_mode_day, "day", 0xFF111111.toInt())
            sliderSize(27f + iteration)
        }
    }
}
