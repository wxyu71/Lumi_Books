package com.huangder.lumibooks.ui.settings

import android.app.Application
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.huangder.lumibooks.R
import com.huangder.lumibooks.ui.theme.EBookReaderTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Real Compose consent interactions: acknowledgement, not elapsed time, unlocks acceptance. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, qualifiers = "zh-rCN-w393dp-h852dp-xxhdpi")
class ThirdPartyConsentInteractionTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = ApplicationProvider.getApplicationContext<Application>()
    private fun label(id: Int) = context.getString(id)

    @Test fun externalTtsCanBeEnabledImmediatelyAfterExplicitAcknowledgement() {
        var accepts = 0
        compose.setContent {
            EBookReaderTheme {
                ExternalTtsConsentSheet(
                    allowHttp = false,
                    onDismiss = {},
                    onAccept = { accepts++ }
                )
            }
        }
        compose.onNodeWithText(label(R.string.external_tts_consent_accept))
            .performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText(label(R.string.external_tts_consent_checkbox))
            .performScrollTo().performClick()
        // No test clock is advanced: the consent does not impose a 30-second gate.
        compose.onNodeWithText(label(R.string.external_tts_consent_accept))
            .performScrollTo().assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(1, accepts) }
    }

    @Test fun mineruCanBeEnabledImmediatelyAfterExplicitAcknowledgement() {
        var accepts = 0
        compose.setContent {
            EBookReaderTheme {
                MineruConsentSheet(
                    onDismiss = {},
                    onAccept = { accepts++ }
                )
            }
        }
        compose.onNodeWithText(label(R.string.mineru_consent_accept))
            .performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText(label(R.string.mineru_consent_checkbox))
            .performScrollTo().performClick()
        compose.onNodeWithText(label(R.string.mineru_consent_accept))
            .performScrollTo().assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(1, accepts) }
    }
}
