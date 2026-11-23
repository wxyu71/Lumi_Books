package com.huangder.lumibooks.ui.bookshelf

import android.app.Application
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.huangder.lumibooks.R
import com.huangder.lumibooks.ui.settings.DetailActivity
import com.huangder.lumibooks.ui.theme.EBookReaderTheme
import com.huangder.lumibooks.ui.theme.MotionPreference
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, qualifiers = "en-w393dp-h852dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BookshelfTagSectionHeadingTest {
    @get:Rule val compose = createComposeRule()
    private val app get() = RuntimeEnvironment.getApplication() as Application
    private fun label(res: Int) = app.getString(res)

    private fun assertDirectBookTagRoute(compact: Boolean) {
        compose.setContent {
            EBookReaderTheme(appTheme = "lumi", motionPreference = MotionPreference.REDUCED) {
                BookshelfTagSectionHeading(compact = compact)
            }
        }
        compose.onNodeWithText(label(R.string.bookshelf_user_tags)).assertIsDisplayed()
        compose.onNodeWithContentDescription(label(R.string.community_manage_book_tags))
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        val intent = Shadows.shadowOf(app).nextStartedActivity
        assertEquals(DetailActivity::class.java.name, intent.component?.className)
        assertEquals("book_tags", intent.getStringExtra("category"))
    }

    @Test fun compactLibraryCategoriesManageTagsWithoutSettingsIntermediary() {
        assertDirectBookTagRoute(compact = true)
    }

    @Test fun fullLibraryCategoriesManageTagsWithoutSettingsIntermediary() {
        assertDirectBookTagRoute(compact = false)
    }
}
