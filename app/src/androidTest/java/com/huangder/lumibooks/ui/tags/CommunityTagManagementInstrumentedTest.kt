package com.huangder.lumibooks.ui.tags

import android.app.Application
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.huangder.lumibooks.R
import com.huangder.lumibooks.data.local.database.AppDatabase
import com.huangder.lumibooks.data.repository.ReadingRepositoryImpl
import com.huangder.lumibooks.data.repository.TagManagementRepository
import com.huangder.lumibooks.data.repository.TagRepositoryImpl
import com.huangder.lumibooks.data.sync.SyncIdentityStore
import com.huangder.lumibooks.domain.model.TagTypes
import com.huangder.lumibooks.ui.theme.EBookReaderTheme
import com.huangder.lumibooks.ui.theme.LocalAppTheme
import com.huangder.lumibooks.ui.theme.LocalMotionEnabled
import java.util.concurrent.Executors
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/** Exercise the populated page on the Android renderer, not only Robolectric. */
@RunWith(AndroidJUnit4::class)
class CommunityTagManagementInstrumentedTest {
    private val compose = createComposeRule()
    private val context get() = ApplicationProvider.getApplicationContext<Application>()
    private lateinit var db: AppDatabase
    private lateinit var queryDispatcher: ExecutorCoroutineDispatcher
    private lateinit var repository: TagManagementRepository

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(object : ExternalResource() {
        override fun before() {
            queryDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
            db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
                .setQueryCoroutineContext(queryDispatcher)
                .build()
            val identity = SyncIdentityStore(db.syncStateDao())
            repository = TagManagementRepository(
                db,
                ReadingRepositoryImpl(
                    db.readingRecordDao(), db.bookmarkDao(), db.noteDao(),
                    db.syncStateDao(), identity, db
                ),
                TagRepositoryImpl(db.tagDao(), db.syncStateDao(), identity)
            )
        }
        override fun after() {
            try {
                runBlocking { withContext(queryDispatcher) { db.close() } }
            } finally {
                queryDispatcher.close()
            }
        }
    }).around(compose)

    @Test fun populatedPageOpensInDefaultAndLiquidGlassThemes() {
        runBlocking {
            assertTrue(repository.createTag(TagTypes.BOOK, "Existing").success)
            assertTrue(repository.createGroup(TagTypes.BOOK, "Topics").success)
            val group = db.tagGroupDao().getAllGroups().single { !it.isDeleted }
            assertTrue(repository.createTag(TagTypes.BOOK, "Grouped", group.id).success)
        }
        val theme = mutableStateOf("lumi")
        val opened = mutableStateOf(true)
        compose.setContent {
            EBookReaderTheme(appTheme = theme.value, darkTheme = false) {
                CompositionLocalProvider(LocalAppTheme provides theme.value, LocalMotionEnabled provides false) {
                    if (opened.value) CommunityTagManagementScreen(TagTypes.BOOK,
                        onBack = { opened.value = false }, repository = repository)
                }
            }
        }
        for (style in listOf("lumi", "liquid_glass", "material3")) {
            compose.runOnIdle { theme.value = style; opened.value = true }
            compose.waitUntil(timeoutMillis = 10_000) {
                compose.onAllNodesWithText("Existing").fetchSemanticsNodes().isNotEmpty() &&
                    compose.onAllNodesWithText("Grouped").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("Existing").assertIsDisplayed()
            compose.onNodeWithText("Topics").assertIsDisplayed()
            compose.onNodeWithText("Grouped").assertIsDisplayed()
            compose.onNodeWithText(context.getString(R.string.tag_picker_uncategorized)).assertDoesNotExist()
            compose.onNodeWithContentDescription(context.getString(R.string.more_options)).performClick()
            for (id in listOf(R.string.community_create_tag, R.string.community_create_group)) {
                val layouts = mutableListOf<TextLayoutResult>()
                compose.onNodeWithText(context.getString(id), useUnmergedTree = true)
                    .assertIsDisplayed()
                    .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
                assertEquals(1, layouts.single().lineCount)
            }
            // Dispose the page and its menu before reopening it with the next renderer.
            compose.runOnIdle { opened.value = false }
            compose.waitForIdle()
        }
    }
}
