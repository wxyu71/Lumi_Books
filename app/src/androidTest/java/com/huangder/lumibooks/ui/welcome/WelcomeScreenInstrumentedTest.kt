package com.huangder.lumibooks.ui.welcome

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.huangder.lumibooks.R
import com.huangder.lumibooks.ui.theme.EBookReaderTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WelcomeScreenInstrumentedTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun introductionShowsCodenameWordmarkAndUpdateNotice() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        showWelcome(isNewInstallation = false, startOnIntroduction = true)

        composeRule.onNodeWithText(context.getString(R.string.welcome_update_title)).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("LUMI ${com.huangder.lumibooks.ui.brand.LumiCodename.CURRENT}").assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.community_agree_continue)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.welcome_title)).assertDoesNotExist()
        composeRule.onNodeWithText(context.getString(R.string.welcome_exit)).assertDoesNotExist()
        composeRule
            .onNodeWithContentDescription(context.getString(R.string.welcome_app_icon_description))
            .assertDoesNotExist()
    }

    @Test
    fun newInstallCompletesWithoutPromotionalTour() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        var finished = false
        showWelcome(isNewInstallation = true, onFinished = { finished = true })

        composeRule.onNodeWithText(context.getString(R.string.community_agree_continue)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.welcome_story_journal_title)).assertDoesNotExist()
        composeRule.onNodeWithText(context.getString(R.string.welcome_support_title)).assertDoesNotExist()
        composeRule.onNodeWithText(context.getString(R.string.welcome_open_sponsor)).assertDoesNotExist()
        composeRule.onNodeWithText(context.getString(R.string.community_agree_continue)).performClick()
        assertTrue("New installation should finish from the introduction", finished)
    }

    @Test
    fun updateCompletesWithoutPromotionalTour() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        var finished = false
        showWelcome(isNewInstallation = false, onFinished = { finished = true })

        composeRule.onNodeWithText(context.getString(R.string.welcome_story_listening_title)).assertDoesNotExist()
        composeRule.onNodeWithText(context.getString(R.string.welcome_support_title)).assertDoesNotExist()
        composeRule.onNodeWithText(context.getString(R.string.community_agree_continue)).performClick()
        assertTrue("Update welcome should finish without promotional screens", finished)
    }

    @Test
    fun legacySupportPreviewCannotRestoreSponsorPage() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        showWelcome(isNewInstallation = false, startOnSupport = true)

        composeRule.onNodeWithText(context.getString(R.string.welcome_update_title)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.welcome_support_title)).assertDoesNotExist()
        composeRule.onNodeWithText(context.getString(R.string.welcome_open_sponsor)).assertDoesNotExist()
    }

    private fun showWelcome(
        isNewInstallation: Boolean,
        startOnIntroduction: Boolean = false,
        startOnSupport: Boolean = false,
        onFinished: () -> Unit = {}
    ) {
        composeRule.setContent {
            EBookReaderTheme(
                darkTheme = false,
                eInkMode = false
            ) {
                WelcomeScreen(
                    isNewInstallation = isNewInstallation,
                    shouldShowLanguageSetup = false,
                    initialLanguage = "zh-CN",
                    initialEInkMode = false,
                    isEInkMode = false,
                    isDark = false,
                    isLiquidGlass = false,
                    onFinished = onFinished,
                    onOpenSponsor = {},
                    onLanguageSetupComplete = { _, _ -> },
                    startOnIntroduction = startOnIntroduction,
                    startOnSupport = startOnSupport
                )
            }
        }
    }
}
