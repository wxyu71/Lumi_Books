package com.huangder.lumibooks.ui.reader

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
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

        fun sliderSize(expected: Float) {
            val actual = compose.onNodeWithTag("reader-font-size-slider")
                .fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo].current
            assertEquals(expected, actual, 0.01f)
            compose.runOnIdle { assertEquals(expected, renderState().fontSize, 0.01f) }
        }
        fun setSlider(size: Float) {
            compose.onNodeWithTag("reader-font-size-slider")
                .performSemanticsAction(SemanticsActions.SetProgress) { it(size) }
            sliderSize(size)
        }
        fun switch(label: Int, color: Int) {
            compose.onNodeWithContentDescription(context.getString(label)).performClick()
            compose.onNodeWithContentDescription(context.getString(label)).assertIsSelected()
            compose.runOnIdle { assertEquals(color, renderState().readerTextColor) }
        }
        setSlider(26f)
        repeat(3) { iteration ->
            switch(R.string.display_mode_night, 0xFFFFFFFF.toInt())
            sliderSize(26f + iteration)
            setSlider(27f + iteration)
            switch(R.string.display_mode_day, 0xFF111111.toInt())
            sliderSize(27f + iteration)
        }
    }
}
