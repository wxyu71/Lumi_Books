package com.huangder.lumibooks.ui.reader.engine

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises the packaged font bytes on actual Android resources, including
 * Font.Builder on API 29+ and the stream fallback on API 26-28.
 */
@RunWith(AndroidJUnit4::class)
class ReaderVariableFontsInstrumentedTest {
    @Test
    fun packagedSerifVariableWeightAxisIsReadable() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val range = readerVariableWeightRange(context, "serif", customFontPath = null)
        assertNotNull("Packaged Source Serif 4 font lost its variable-weight axis", range)
        val weights = requireNotNull(range)
        assertTrue("400 must be in the supported font weight range: $weights",
            weights.start <= 400f && weights.endInclusive >= 400f)
        assertTrue("700 must be in the supported font weight range: $weights",
            weights.start <= 700f && weights.endInclusive >= 700f)
    }
}
