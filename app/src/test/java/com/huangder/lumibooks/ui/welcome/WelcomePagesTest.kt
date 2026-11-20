package com.huangder.lumibooks.ui.welcome

import org.junit.Assert.assertEquals
import org.junit.Test

class WelcomePagesTest {
    @Test
    fun `new installation proceeds directly to consent after language selection`() {
        assertEquals(
            listOf(WelcomePage.LANGUAGE_SETUP, WelcomePage.INTRODUCTION),
            welcomePages(shouldShowLanguageSetup = true, isNewInstallation = true, isEInkMode = false)
        )
    }

    @Test
    fun `all users can accept without a promotional tour`() {
        for (fresh in listOf(true, false)) for (ink in listOf(true, false)) {
            assertEquals(
                listOf(WelcomePage.INTRODUCTION),
                welcomePages(shouldShowLanguageSetup = false, isNewInstallation = fresh, isEInkMode = ink)
            )
        }
    }

    @Test
    fun `page order matches enum order so transitions slide the right way`() {
        for (language in listOf(true, false)) for (fresh in listOf(true, false)) for (ink in listOf(true, false)) {
            val pages = welcomePages(language, fresh, ink)
            assertEquals(pages.sortedBy { it.ordinal }, pages)
        }
    }

    @Test
    fun `finishing language setup does not change the remaining page order`() {
        for (fresh in listOf(true, false)) {
            for (ink in listOf(true, false)) {
                assertEquals(welcomePages(false, fresh, ink), welcomePages(true, fresh, ink).drop(1))
            }
        }
    }
}
