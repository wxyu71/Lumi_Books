package com.huangder.lumibooks.ui.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.huangder.lumibooks.ui.brand.LumiWordmark
import com.huangder.lumibooks.ui.theme.LocalIsDarkTheme

// Same as @color/launch_background, the neutral cold-start window colour.
private val NeutralLight = Color(0xFFF6F7FA)
private val NeutralDark = Color(0xFF0E1014)

@Composable
fun SplashScreen(isDark: Boolean, @Suppress("UNUSED_PARAMETER") iconStyle: String) {
    val trace = com.huangder.lumibooks.util.diagnostics.startupTraceModifier("app_splash")

    // Community builds never use the legacy Classic splash artwork because its baked-in
    // release codename can outlive the product branding. Every launcher icon style gets the
    // same live wordmark, so the first rendered app frame is always "LUMI Community".
    CompositionLocalProvider(LocalIsDarkTheme provides isDark) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(trace)
                .background(if (isDark) NeutralDark else NeutralLight),
            contentAlignment = Alignment.Center
        ) {
            LumiWordmark(modifier = Modifier.padding(bottom = 24.dp), animate = false)
        }
    }
}
