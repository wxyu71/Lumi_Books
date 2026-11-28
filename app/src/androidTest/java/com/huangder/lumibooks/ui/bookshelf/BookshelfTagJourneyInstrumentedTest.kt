package com.huangder.lumibooks.ui.bookshelf

import android.content.ContentValues
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.huangder.lumibooks.R
import com.huangder.lumibooks.data.local.DataStoreManager
import com.huangder.lumibooks.data.local.entity.BookEntity
import com.huangder.lumibooks.domain.model.AppFontSize
import com.huangder.lumibooks.domain.model.TagTypes
import com.huangder.lumibooks.ui.components.TagPickerEntryPoint
import com.huangder.lumibooks.util.LocaleHelper
import com.huangder.lumibooks.widget.WidgetDataEntryPoint
import dagger.hilt.android.EntryPointAccessors
import java.io.File
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Starts real production Activities; no replacement composable, navigation or database. */
@RunWith(AndroidJUnit4::class)
class BookshelfTagJourneyInstrumentedTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext.applicationContext
    private val device = UiDevice.getInstance(instrumentation)
    private val settings = DataStoreManager(context)
    private val repository = EntryPointAccessors.fromApplication(context, TagPickerEntryPoint::class.java)
        .tagManagementRepository()
    private val books = EntryPointAccessors.fromApplication(context, WidgetDataEntryPoint::class.java).bookDao()
    private fun label(id: Int) = LocaleHelper.applyLanguage(context).getString(id)

    private fun awaitText(value: String) {
        compose.waitUntil(timeoutMillis = 20_000) {
            compose.onAllNodesWithText(value).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun capture(name: String) {
        val file = File(context.cacheDir, name)
        try {
            assertTrue("Could not capture $name", device.takeScreenshot(file))
            assertTrue(file.length() > 0)
            if (Build.VERSION.SDK_INT >= 29) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, name)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/LumiCI")
                }
                val resolver = context.contentResolver
                val uri = checkNotNull(resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values))
                checkNotNull(resolver.openOutputStream(uri)).use { output ->
                    file.inputStream().use { it.copyTo(output) }
                }
            }
        } finally {
            file.delete()
        }
    }

    private fun captureHierarchy() {
        if (Build.VERSION.SDK_INT < 29) return
        val file = File(context.cacheDir, "tag-journey-failure.xml")
        try {
            device.dumpWindowHierarchy(file)
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, file.name)
                put(MediaStore.Downloads.MIME_TYPE, "application/xml")
                put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/LumiCI")
            }
            val resolver = context.contentResolver
            val uri = checkNotNull(resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values))
            checkNotNull(resolver.openOutputStream(uri)).use { output ->
                file.inputStream().use { it.copyTo(output) }
            }
        } finally {
            file.delete()
        }
    }

    @Test fun realLibraryTagJourneyPersistsSelection() {
        val old = runBlocking {
            Preferences(settings.appLanguage.first(), settings.appTheme.first(),
                settings.bookshelfLayoutMode.first(), settings.darkMode.first(), settings.appFontSize.first())
        }
        val extended = InstrumentationRegistry.getArguments().getString("lumiExtended") == "true"
        val language = if (extended) "zh-CN" else "en"
        val id = UUID.randomUUID().toString()
        val suffix = id.take(8)
        // Unique names prevent a failed previous test from being mistaken for
        // this run's data (or being deleted by this run's cleanup).
        val tag = "Loose $suffix"
        val folder = "Folder $suffix"
        val member = "Grouped $suffix"
        val title = "CI Book $suffix"
        val file = File(context.filesDir, "$id.txt").apply { writeText("An isolated CI test book.") }
        try {
            runBlocking {
                settings.saveAppLanguage(language)
                settings.saveAppTheme(if (extended) "liquid_glass" else "lumi")
                settings.saveDarkMode(if (extended) "dark" else "light")
                settings.saveAppFontSize(if (extended) AppFontSize.LARGE else AppFontSize.STANDARD)
                settings.saveBookshelfLayoutMode(2)
                val now = System.currentTimeMillis()
                books.insertBook(BookEntity(id, title, "CI", file.absolutePath, null, "TXT", 0, 0f, createdAt = now))
                assertTrue(repository.createTag(TagTypes.BOOK, tag).success)
                assertTrue(repository.createGroup(TagTypes.BOOK, folder).success)
                val groupId = repository.observeGroups(TagTypes.BOOK).first().single { it.name == folder }.id
                assertTrue(repository.createTag(TagTypes.BOOK, member, groupId).success)
            }
            ActivityScenario.launch<BookshelfCategoriesActivity>(
                Intent(context, BookshelfCategoriesActivity::class.java)
            ).use {
                try {
                    // Drive the real navigation using the same synchronized Compose
                    // semantics tree that located the scroll target. UI Automator is
                    // retained for system back and screenshot/hierarchy evidence.
                    val manage = label(R.string.community_manage_book_tags)
                    awaitText(label(R.string.filter_all))
                    compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasContentDescription(manage))
                    compose.onNode(hasContentDescription(manage) and hasClickAction())
                        .assertIsDisplayed().performClick()
                    awaitText(member)
                    compose.onNodeWithText(tag).assertIsDisplayed()
                    compose.onNodeWithText(label(R.string.tag_picker_uncategorized)).assertDoesNotExist()
                    capture("tag-journey-manager.png")
                    compose.onNodeWithContentDescription(label(R.string.more_options)).performClick()
                    awaitText(label(R.string.community_create_group))
                    compose.onNodeWithText(label(R.string.community_create_tag)).assertIsDisplayed()
                    capture("tag-journey-menu.png")
                    device.pressBack()
                    compose.waitUntil(timeoutMillis = 20_000) {
                        compose.onAllNodesWithText(label(R.string.community_create_group)).fetchSemanticsNodes().isEmpty()
                    }
                    device.pressBack()
                    awaitText(label(R.string.bookshelf_user_tags))
                    compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(label(R.string.filter_all)))
                    compose.onNodeWithText(label(R.string.filter_all)).performClick()
                    awaitText(title)
                    fun openPicker() {
                        compose.onNode(hasText(title) and hasClickAction()).performTouchInput { longClick() }
                        awaitText(label(R.string.add_tag))
                        compose.onNodeWithText(label(R.string.add_tag)).performClick()
                        awaitText(tag)
                        compose.onNodeWithText(label(R.string.tag_picker_uncategorized)).assertDoesNotExist()
                    }
                    openPicker()
                    compose.onNodeWithText(tag).assertIsOff()
                    compose.onNodeWithText(folder).assertIsDisplayed()
                    compose.onNodeWithText(tag).performClick()
                    compose.onNodeWithText(folder).performClick()
                    awaitText(member)
                    compose.onNodeWithText(member).assertIsOff().performClick()
                    // The underlying book Activity also has a Back action.
                    // Scope this action to the picker rather than selecting by index.
                    compose.onNode(hasContentDescription(label(R.string.back)) and
                        hasAnyAncestor(hasTestTag("annotation-tag-sheet")))
                        .assertIsDisplayed().performClick()
                    awaitText(tag)
                    compose.onNodeWithText(tag).assertIsOn()
                    capture("tag-journey-picker.png")
                    compose.onNodeWithText(label(R.string.confirm)).performClick()
                    compose.waitUntil(timeoutMillis = 20_000) {
                        runBlocking {
                            val selected = repository.observeCatalog(TagTypes.BOOK).first()
                                .filter { it.name == tag || it.name == member }
                            selected.size == 2 && selected.all { it.usageCount == 1 }
                        }
                    }
                    compose.waitUntil(timeoutMillis = 20_000) {
                        compose.onAllNodesWithText(label(R.string.tag_sheet_title)).fetchSemanticsNodes().isEmpty()
                    }
                    openPicker()
                    compose.onNodeWithText(tag).assertIsOn()
                    compose.onNodeWithText(folder).performClick()
                    awaitText(member)
                    compose.onNodeWithText(member).assertIsOn().performClick()
                    // Cancel a changed draft: both saved associations must remain.
                    compose.onNodeWithText(label(R.string.cancel)).performClick()
                    compose.waitUntil(timeoutMillis = 20_000) {
                        compose.onAllNodesWithText(label(R.string.tag_sheet_title)).fetchSemanticsNodes().isEmpty()
                    }
                    val persisted = runBlocking {
                        repository.observeCatalog(TagTypes.BOOK).first()
                            .filter { it.name == tag || it.name == member }
                    }
                    assertTrue(persisted.size == 2 && persisted.all { it.usageCount == 1 })
                    device.pressBack()
                    awaitText(label(R.string.filter_all))
                } catch (failure: Throwable) {
                    // Collect the failing window before ActivityScenario teardown.
                    // Preserve the original assertion/exception if evidence capture fails.
                    runCatching { capture("tag-journey-failure.png") }
                    runCatching { captureHierarchy() }
                    throw failure
                }
            }
        } finally {
            // Only delete fixtures owned by this test. Do not clear the application DB.
            runBlocking {
                repository.observeCatalog(TagTypes.BOOK).first().filter { it.name == tag || it.name == member }
                    .forEach { repository.deleteTag(TagTypes.BOOK, it.key) }
                repository.observeGroups(TagTypes.BOOK).first().filter { it.name == folder }
                    .forEach { repository.deleteGroup(TagTypes.BOOK, it.id) }
                books.deleteBookWithRelatedData(id)
                settings.saveAppLanguage(old.language)
                settings.saveAppTheme(old.theme)
                settings.saveBookshelfLayoutMode(old.layout)
                settings.saveDarkMode(old.dark)
                settings.saveAppFontSize(old.font)
            }
            file.delete()
        }
    }

    private data class Preferences(
        val language: String, val theme: String, val layout: Int, val dark: String, val font: AppFontSize
    )
}
