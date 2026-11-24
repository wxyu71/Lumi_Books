package com.huangder.lumibooks.ui.tags

import android.app.Application
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.huangder.lumibooks.R
import com.huangder.lumibooks.data.local.database.AppDatabase
import com.huangder.lumibooks.data.repository.ReadingRepositoryImpl
import com.huangder.lumibooks.data.repository.TagManagementRepository
import com.huangder.lumibooks.data.repository.TagRepositoryImpl
import com.huangder.lumibooks.data.sync.SyncIdentityStore
import com.huangder.lumibooks.domain.model.TagTypes
import com.huangder.lumibooks.ui.theme.EBookReaderTheme
import com.huangder.lumibooks.ui.theme.MotionPreference
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
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Exercises the real Compose page against the production Room repository.
 * This deliberately does not mock the catalog or inspect Kotlin source strings:
 * UI actions must persist and flow back into the displayed hierarchy.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, qualifiers = "zh-rCN-w393dp-h852dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CommunityTagManagementScreenTest {
    private val compose = createComposeRule()
    private val context get() = ApplicationProvider.getApplicationContext<Application>()
    private lateinit var db: AppDatabase
    private lateinit var repository: TagManagementRepository
    private lateinit var queryDispatcher: ExecutorCoroutineDispatcher

    // The outer resource survives until the inner Compose rule has disposed
    // its composition and cancelled the catalog/group Flow collectors.
    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(object : ExternalResource() {
        override fun before() = setUp()
        override fun after() = tearDown()
    }).around(compose)

    private fun setUp() {
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

    private fun tearDown() {
        try {
            // Cancellation can leave an already-running SQLite read finishing
            // on the query thread. Queue close on that same dispatcher, after
            // Compose disposal, rather than closing under an active cursor.
            runBlocking { withContext(queryDispatcher) { db.close() } }
        } finally {
            queryDispatcher.close()
        }
    }

    private fun label(id: Int) = context.getString(id)

    private fun show() {
        compose.setContent {
            EBookReaderTheme(appTheme = "lumi", motionPreference = MotionPreference.REDUCED) {
                CommunityTagManagementScreen(
                    type = TagTypes.BOOK,
                    onBack = {},
                    repository = repository
                )
            }
        }
    }

    private fun openPageActions() {
        compose.onNodeWithContentDescription(label(R.string.more_options)).performClick()
    }

    private fun createTag(name: String) {
        openPageActions()
        compose.onNodeWithText(label(R.string.community_create_tag)).performClick()
        compose.onNode(hasSetTextAction()).performTextInput(name)
        compose.onNodeWithText(label(R.string.confirm)).performClick()
        compose.waitUntil(timeoutMillis = 10_000) {
            runBlocking { db.tagDao().getAllTagsSnapshot().any { it.name == name } }
        }
    }

    @Test fun emptyCatalogHasOneEmptyStateAndCreationIsInToolbar() {
        show()
        compose.onNodeWithText(label(R.string.community_no_tags)).assertIsDisplayed()

        // Redundant counts and the empty "uncategorized" section were the Beta1 bug.
        for (value in listOf(
            label(R.string.community_create_tag),
            label(R.string.community_create_group),
            context.getString(R.string.community_tag_count, 0),
            label(R.string.tag_picker_uncategorized),
            label(R.string.community_select_tags)
        )) {
            compose.onNodeWithText(value).assertDoesNotExist()
        }
        openPageActions()
        compose.onNodeWithText(label(R.string.community_create_tag)).assertIsDisplayed()
        compose.onNodeWithText(label(R.string.community_create_group)).assertIsDisplayed()
        compose.onNodeWithText(label(R.string.community_select_tags)).assertDoesNotExist()
    }

    @Test fun creatingTagThroughPagePersistsToRoomAndRefreshesContent() {
        show()
        createTag("Fiction")
        compose.onNodeWithText("Fiction").assertIsDisplayed()
        compose.onNodeWithText(label(R.string.community_no_tags)).assertDoesNotExist()

        val saved = runBlocking { db.tagDao().getAllTagsSnapshot() }
        assertEquals(listOf("Fiction"), saved.map { it.name })
        assertEquals(null, saved.single().groupId)
    }

    @Test fun movingTagThroughRowActionUpdatesItsRoomGroup() {
        val group = runBlocking {
            assertTrue(repository.createGroup(TagTypes.BOOK, "Shelf").success)
            assertTrue(repository.createTag(TagTypes.BOOK, "History").success)
            db.tagGroupDao().getAllGroups().single { !it.isDeleted }
        }
        show()
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText("History").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("History").performClick()
        compose.onNodeWithText(label(R.string.community_move_tag)).performClick()
        compose.onAllNodesWithText("Shelf").onLast().performClick()

        compose.waitUntil(timeoutMillis = 10_000) {
            runBlocking { db.tagDao().getAllTagsSnapshot().single().groupId == group.id }
        }
        compose.onNodeWithText("History").assertIsDisplayed()
        compose.onNodeWithText(label(R.string.community_no_tags)).assertDoesNotExist()
    }

    @Test fun batchDeleteViaToolbarRemovesSelectedTagsNotOtherTags() {
        runBlocking {
            assertTrue(repository.createTag(TagTypes.BOOK, "Keep").success)
            assertTrue(repository.createTag(TagTypes.BOOK, "Remove").success)
        }
        show()
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText("Remove").fetchSemanticsNodes().isNotEmpty()
        }
        openPageActions()
        compose.onNodeWithText(label(R.string.community_select_tags)).performClick()
        compose.onNodeWithText("Remove").performClick()
        compose.onNodeWithContentDescription(label(R.string.community_delete_selected)).performClick()
        compose.onAllNodesWithText(label(R.string.delete)).onLast().performClick()

        compose.waitUntil(timeoutMillis = 10_000) {
            runBlocking { db.tagDao().getAllTagsSnapshot().map { it.name } == listOf("Keep") }
        }
        compose.onNodeWithText("Keep").assertIsDisplayed()
        compose.onNodeWithText("Remove").assertDoesNotExist()
    }
}
