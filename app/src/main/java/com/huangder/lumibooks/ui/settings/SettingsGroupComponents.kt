package com.huangder.lumibooks.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.huangder.lumibooks.ui.components.GroupedCard
import com.huangder.lumibooks.ui.components.GroupedRowDivider

/** Group and inset shared by Settings rows across the app's secondary pages. */
@Composable
internal fun SettingsCategoryGroup(content: @Composable () -> Unit) {
    GroupedCard(content = content)
}

@Composable
internal fun SettingsGroupDivider() {
    GroupedRowDivider(startInset = 54.dp)
}
