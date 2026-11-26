package com.huangder.lumibooks.ui.components

import android.app.Application
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.huangder.lumibooks.R
import com.huangder.lumibooks.data.local.entity.TagGroupEntity
import com.huangder.lumibooks.domain.model.ManagedTag
import com.huangder.lumibooks.domain.model.TagTypes
import com.huangder.lumibooks.ui.theme.EBookReaderTheme
import com.huangder.lumibooks.ui.theme.LocalMotionEnabled
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, qualifiers = "en-w360dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CategorizedTagPickerTest {
    @get:Rule val compose = createComposeRule()
    private fun label(id: Int) = RuntimeEnvironment.getApplication().getString(id)
    private val catalog = TagPickerCatalog(
        listOf(TagGroupEntity("topics", TagTypes.ANNOTATION, "Topics", "topics", 1)),
        listOf(ManagedTag("Alpha", "Alpha", null, 0), ManagedTag("Beta", "Beta", "topics", 0))
    )

    @Test fun choosesAcrossCategoriesAndOnlyCommitsOnConfirm() {
        var saved: List<String>? = null
        var dismissed = false
        compose.setContent {
            EBookReaderTheme(appTheme = "lumi", darkTheme = false) {
                CompositionLocalProvider(LocalMotionEnabled provides false) {
                    AnnotationTagSheet(emptyList(), emptyList(), { saved = it }, { dismissed = true }, categoryCatalog = catalog)
                }
            }
        }
        compose.onNodeWithText(label(R.string.tag_picker_uncategorized)).assertDoesNotExist()
        compose.onNodeWithText("Alpha").performClick()
        compose.runOnIdle { assertNull(saved) }
        compose.onNodeWithText("Topics").performClick()
        compose.onNodeWithText("Beta").performClick()
        compose.onNodeWithContentDescription(label(R.string.back)).performClick()
        compose.onAllNodesWithText("1 tags · 1 selected").assertCountEquals(1)
        compose.onNodeWithText("Alpha").assertIsOn()
        compose.onNodeWithText(label(R.string.confirm)).performClick()
        compose.waitUntil { dismissed }
        compose.runOnIdle { assertEquals(listOf("Alpha", "Beta"), saved) }
    }

    @Test fun cancelInsideCategoryDiscardsDraftAndClosesWholeTask() {
        var saves = 0
        var dismissed = false
        compose.setContent {
            EBookReaderTheme(appTheme = "lumi", darkTheme = false) {
                CompositionLocalProvider(LocalMotionEnabled provides false) {
                    AnnotationTagSheet(emptyList(), emptyList(), { saves++ }, { dismissed = true }, categoryCatalog = catalog)
                }
            }
        }
        compose.onNodeWithText("Topics").performClick()
        compose.onNodeWithText("Beta").performClick()
        compose.onNodeWithText(label(R.string.cancel)).performClick()
        compose.runOnIdle { assertTrue("Cancel must finish the entire selection task", dismissed); assertEquals(0, saves) }
    }

    @Test fun removedCategoryReturnsItsLabelsToRoot() {
        val data = mutableStateOf(catalog)
        compose.setContent {
            EBookReaderTheme(appTheme = "lumi", darkTheme = false) {
                CompositionLocalProvider(LocalMotionEnabled provides false) {
                    AnnotationTagSheet(listOf("Beta"), emptyList(), {}, {}, categoryCatalog = data.value)
                }
            }
        }
        compose.onNodeWithText("Topics").performClick()
        compose.onNodeWithText("Beta").assertIsOn()
        compose.runOnIdle { data.value = catalog.copy(groups = emptyList()) }
        compose.onNodeWithText("Beta").assertIsOn()
        compose.onNodeWithText("Alpha").assertIsDisplayed()
    }

    @Test fun noFoldersShowsLooseBookTagsDirectlyAndPreservesSelection() {
        val loose = TagPickerCatalog(emptyList(), listOf(
            ManagedTag("book-id", "History", null, 0)
        ))
        var saved: List<String>? = null
        compose.setContent {
            EBookReaderTheme(appTheme = "lumi", darkTheme = false) {
                CompositionLocalProvider(LocalMotionEnabled provides false) {
                    AnnotationTagSheet(listOf("book-id"), emptyList(), { saved = it }, {},
                        catalogType = TagTypes.BOOK, categoryCatalog = loose)
                }
            }
        }
        compose.onNodeWithText(label(R.string.tag_picker_uncategorized)).assertDoesNotExist()
        compose.onNodeWithText("History").assertIsOn()
        compose.onNodeWithText(label(R.string.confirm)).performClick()
        compose.runOnIdle { assertEquals(listOf("book-id"), saved) }
    }

    @Test fun emptyCatalogCanCreateWithoutEnteringAnArtificialFolder() {
        compose.setContent {
            EBookReaderTheme(appTheme = "lumi", darkTheme = false) {
                CompositionLocalProvider(LocalMotionEnabled provides false) {
                    AnnotationTagSheet(emptyList(), emptyList(), {}, {},
                        categoryCatalog = TagPickerCatalog(emptyList(), emptyList()))
                }
            }
        }
        compose.onNodeWithText(label(R.string.tag_picker_uncategorized)).assertDoesNotExist()
        compose.onNodeWithText(label(R.string.annotation_tag_new)).assertIsDisplayed()
        compose.onNodeWithText(label(R.string.confirm)).assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "zh-rCN-w360dp-h800dp-mdpi")
    fun mixedCatalogAtLargeFontKeepsFolderAndConfirmationReachable() {
        var saved: List<String>? = null
        val data = TagPickerCatalog(
            listOf(TagGroupEntity("folder", TagTypes.BOOK, "阅读计划", "阅读计划", 1)),
            listOf(ManagedTag("loose", "未整理的书籍标签", null, 0),
                ManagedTag("inside", "正在阅读的长名称标签", "folder", 0))
        )
        compose.setContent {
            EBookReaderTheme(appTheme = "lumi", darkTheme = true) {
                CompositionLocalProvider(LocalMotionEnabled provides false,
                    LocalDensity provides Density(1f, 1.5f)) {
                    AnnotationTagSheet(emptyList(), emptyList(), { saved = it }, {},
                        catalogType = TagTypes.BOOK, categoryCatalog = data)
                }
            }
        }
        compose.onNodeWithText(label(R.string.tag_picker_uncategorized)).assertDoesNotExist()
        compose.onNodeWithText("未整理的书籍标签").assertIsDisplayed().performClick()
        compose.onNodeWithText("阅读计划").assertIsDisplayed().performClick()
        compose.onNodeWithText("正在阅读的长名称标签").assertIsDisplayed().performClick()
        compose.onNodeWithText(label(R.string.confirm)).assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(setOf("loose", "inside"), saved?.toSet()) }
    }

    @Test
    @Config(qualifiers = "en-w640dp-h360dp-land-mdpi")
    fun landscapeShowsEmptyRealFolderWithoutInventingUncategorized() {
        val data = TagPickerCatalog(catalog.groups, emptyList())
        compose.setContent {
            EBookReaderTheme(appTheme = "lumi", darkTheme = false) {
                CompositionLocalProvider(LocalMotionEnabled provides false) {
                    AnnotationTagSheet(emptyList(), emptyList(), {}, {}, categoryCatalog = data)
                }
            }
        }
        compose.onNodeWithText(label(R.string.tag_picker_uncategorized)).assertDoesNotExist()
        compose.onNodeWithText("Topics").performClick()
        compose.onNodeWithText(label(R.string.annotation_tag_new)).assertIsDisplayed()
        compose.onNodeWithText(label(R.string.confirm)).assertIsDisplayed()
        compose.onNodeWithContentDescription(label(R.string.back)).performClick()
        compose.onNodeWithText("Topics").assertIsDisplayed()
    }

    @Test fun saveFailureLeavesDraftOpenForRetry() {
        var attempts = 0
        var dismissed = false
        compose.setContent {
            EBookReaderTheme(appTheme = "lumi", darkTheme = false) {
                CompositionLocalProvider(LocalMotionEnabled provides false) {
                    AnnotationTagSheet(listOf("Alpha"), emptyList(), {
                        attempts++
                        if (attempts == 1) error("disk write failed")
                    }, { dismissed = true }, categoryCatalog = catalog)
                }
            }
        }
        compose.onNodeWithText(label(R.string.confirm)).performClick()
        compose.onNodeWithText(label(R.string.tag_picker_save_failed)).assertIsDisplayed()
        compose.runOnIdle { assertFalse(dismissed) }
        compose.onNodeWithText(label(R.string.confirm)).performClick()
        compose.waitUntil { dismissed }
        compose.runOnIdle { assertEquals(2, attempts) }
    }
}
