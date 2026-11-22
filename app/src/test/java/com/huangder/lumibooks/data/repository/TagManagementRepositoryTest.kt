package com.huangder.lumibooks.data.repository

import android.app.Application
import androidx.room.Room
import com.huangder.lumibooks.data.local.database.AppDatabase
import com.huangder.lumibooks.data.sync.SyncIdentityStore
import com.huangder.lumibooks.domain.model.TagTypes
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class TagManagementRepositoryTest {
    private fun manager(db: AppDatabase): TagManagementRepository {
        val identity = SyncIdentityStore(db.syncStateDao())
        val reading = ReadingRepositoryImpl(
            db.readingRecordDao(),
            db.bookmarkDao(),
            db.noteDao(),
            db.syncStateDao(),
            identity,
            db
        )
        val tags = TagRepositoryImpl(db.tagDao(), db.syncStateDao(), identity)
        return TagManagementRepository(db, reading, tags)
    }

    @Test fun bookGroupsAreLocalCapabilitiesAndDeletingAGroupPreservesItsTags() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            AppDatabase::class.java
        ).build()
        try {
            val repository = manager(db)
            assertTrue(repository.createGroup(TagTypes.BOOK, "Shelf").success)
            val group = db.tagGroupDao().getAllGroups().single { !it.isDeleted }

            assertTrue(repository.createTag(TagTypes.BOOK, "Fiction", group.id).success)
            val tag = db.tagDao().getAllTagsSnapshot().single()
            assertEquals(group.id, tag.groupId)

            val duplicate = repository.createTag(TagTypes.BOOK, " fiction ", group.id)
            assertFalse(duplicate.success)
            assertEquals("duplicate_name", duplicate.message)

            assertTrue(repository.renameGroup(TagTypes.BOOK, group.id, "Library").success)
            assertEquals(
                "Library",
                db.tagGroupDao().getAllGroups().single { it.id == group.id }.name
            )

            assertTrue(repository.deleteGroup(TagTypes.BOOK, group.id).success)
            assertTrue(db.tagGroupDao().getAllGroups().single { it.id == group.id }.isDeleted)
            val preserved = db.tagDao().getAllTagsSnapshot().single { it.id == tag.id }
            assertEquals("Fiction", preserved.name)
            assertNull(preserved.groupId)
        } finally {
            db.close()
        }
    }

    @Test fun annotationCatalogMovesRenamesAndDeletesThroughRealRepositories() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            AppDatabase::class.java
        ).build()
        try {
            val repository = manager(db)
            assertTrue(repository.createGroup(TagTypes.ANNOTATION, "Review").success)
            val group = db.tagGroupDao().getAllGroups().single { !it.isDeleted }

            assertTrue(repository.createTag(TagTypes.ANNOTATION, "Quote", group.id).success)
            var catalog = repository.observeCatalog(TagTypes.ANNOTATION).first()
            assertEquals(listOf("Quote"), catalog.map { it.name })
            assertEquals(group.id, catalog.single().groupId)

            assertTrue(repository.renameTag(TagTypes.ANNOTATION, "Quote", "Excerpt").success)
            catalog = repository.observeCatalog(TagTypes.ANNOTATION).first()
            assertEquals(listOf("Excerpt"), catalog.map { it.name })
            assertEquals(group.id, catalog.single().groupId)

            assertTrue(repository.deleteGroup(TagTypes.ANNOTATION, group.id).success)
            catalog = repository.observeCatalog(TagTypes.ANNOTATION).first()
            assertEquals("Excerpt", catalog.single().name)
            assertNull(catalog.single().groupId)

            assertTrue(repository.deleteTag(TagTypes.ANNOTATION, "Excerpt").success)
            assertTrue(repository.observeCatalog(TagTypes.ANNOTATION).first().isEmpty())
        } finally {
            db.close()
        }
    }
}
