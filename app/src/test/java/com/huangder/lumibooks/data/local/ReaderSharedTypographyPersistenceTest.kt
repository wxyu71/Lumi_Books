package com.huangder.lumibooks.data.local

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.huangder.lumibooks.domain.model.ReaderLayoutTarget
import com.huangder.lumibooks.domain.model.ReaderThemeSuiteCodec
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class ReaderSharedTypographyPersistenceTest {
    @Test fun repeatedModeChangesKeepTypographyAndIndependentColorsAfterReload() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.dataStore.edit { it.clear() }
        val manager = DataStoreManager(context)
        manager.migrateReaderThemeSuites()
        suspend fun suite() = DataStoreManager(context).readerThemeSuiteState.first()
            .suites.first { it.id == "day" }
        for (layout in ReaderLayoutTarget.entries) {
            val initial = suite()
            val other = if (layout == ReaderLayoutTarget.READER_LAYOUT)
                ReaderLayoutTarget.BOOK_LAYOUT else ReaderLayoutTarget.READER_LAYOUT
            val day = initial.settingsFor(layout, false).copy(
                fontSize = 26f, fontType = "serif", bodyFontWeight = 600f, lineHeight = 1.8f,
                textColor = 0xFF111111.toInt()
            )
            manager.updateReaderThemeSuite("day", layout, day, false)
            val night = suite().settingsFor(layout, true)
            assertEquals(day.fontSize, night.fontSize)
            assertEquals(day.fontType, night.fontType)
            assertEquals(day.bodyFontWeight, night.bodyFontWeight)
            assertEquals(day.lineHeight, night.lineHeight)
            manager.updateReaderThemeSuite("day", layout,
                night.copy(fontSize = 30f, textColor = 0xFFFFFFFF.toInt()), true)
            var reloaded = suite()
            assertEquals(30f, reloaded.settingsFor(layout, false).fontSize)
            assertEquals(30f, reloaded.settingsFor(layout, true).fontSize)
            for ((dark, size) in listOf(false to 24f, true to 28f, false to 26f)) {
                manager.updateReaderThemeSuite("day", layout,
                    reloaded.settingsFor(layout, dark).copy(fontSize = size), dark)
                reloaded = suite()
                assertEquals(size, reloaded.settingsFor(layout, false).fontSize)
                assertEquals(size, reloaded.settingsFor(layout, true).fontSize)
                assertEquals(0xFF111111.toInt(), reloaded.settingsFor(layout, false).textColor)
                assertEquals(0xFFFFFFFF.toInt(), reloaded.settingsFor(layout, true).textColor)
            }
            assertEquals(initial.settingsFor(other), reloaded.settingsFor(other))
        }
    }

    @Test fun legacyOverridesMigratePerFieldAndDoNotReappearAfterAnEdit() {
        val legacy = ReaderThemeSuiteCodec.decode(
            """[{"id":"day","settings":{"background":"day","fontSize":16},"light":{"background":"day","fontSize":26},"dark":{"background":"night","fontSize":30,"fontType":"serif"},"bookLayoutSettings":{"background":"publisher","fontSize":20}}]"""
        ).single()
        val layout = ReaderLayoutTarget.READER_LAYOUT
        assertEquals(26f, legacy.settingsFor(layout, false).fontSize)
        assertEquals(26f, legacy.settingsFor(layout, true).fontSize)
        assertEquals("serif", legacy.settingsFor(layout, false).fontType)
        assertEquals("night", legacy.settingsFor(layout, true).backgroundSelection)
        assertEquals(20f, legacy.bookLayoutSettings.fontSize)
        val edited = legacy.withModeSettings(layout, true,
            legacy.settingsFor(layout, true).copy(fontSize = 16f))
        val restored = ReaderThemeSuiteCodec.decode(
            ReaderThemeSuiteCodec.encodeExact(listOf(edited))
        ).single()
        assertEquals(16f, restored.settingsFor(layout, false).fontSize)
        assertEquals(16f, restored.settingsFor(layout, true).fontSize)
        assertEquals("night", restored.settingsFor(layout, true).backgroundSelection)
    }
}
