package com.huangder.lumibooks.ui.tags

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.huangder.lumibooks.R
import com.huangder.lumibooks.domain.model.TagTypes
import com.huangder.lumibooks.ui.theme.EBookReaderTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, qualifiers = "zh-rCN-w393dp-h852dp-xxhdpi")
class CommunityTagManagementScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun emptyBookCatalogUsesOneSettingsStyleEmptyStateWithoutRedundantCounts() {
        val context = ApplicationProvider.getApplicationContext<Application>()

        compose.setContent {
            EBookReaderTheme {
                CommunityTagManagementScreen(type = TagTypes.BOOK, onBack = {})
            }
        }

        compose.onNodeWithText(context.getString(R.string.community_no_tags))
            .assertIsDisplayed()

        // The page body contains only data/empty state. Creation and selection are page actions,
        // not another settings card competing with the tag hierarchy.
        for (text in listOf(
            context.getString(R.string.community_create_tag),
            context.getString(R.string.community_create_group),
            context.getString(R.string.community_tag_count, 0),
            context.getString(R.string.tag_picker_uncategorized),
            context.getString(R.string.community_select_tags)
        )) {
            assertTrue(compose.onAllNodesWithText(text).fetchSemanticsNodes().isEmpty())
        }

        compose.onNodeWithContentDescription(context.getString(R.string.annotation_tag_manage))
            .performClick()
        compose.onNodeWithText(context.getString(R.string.community_create_tag))
            .assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.community_create_group))
            .assertIsDisplayed()
    }
}
