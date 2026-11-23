package com.huangder.lumibooks.ui.tags

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.huangder.lumibooks.R
import com.huangder.lumibooks.ui.components.SettingsTileColors
import com.huangder.lumibooks.ui.icons.AppIcons
import com.huangder.lumibooks.ui.settings.CategoryItem
import com.huangder.lumibooks.ui.settings.DetailActivity
import com.huangder.lumibooks.ui.settings.DetailPage
import com.huangder.lumibooks.ui.settings.SettingsCategoryGroup
import com.huangder.lumibooks.ui.settings.SettingsGroupDivider

/** The edition adapter only forwards to these ordinary Settings UI components. */
@Composable
internal fun CommunityTagManagementSettingsEntry() {
    val context = LocalContext.current
    CategoryItem(
        icon = AppIcons.Tag,
        label = stringResource(R.string.annotation_tag_manage),
        tileColor = SettingsTileColors.Blue
    ) {
        context.startActivity(DetailActivity.createIntent(context, "tag_management"))
    }
}

@Composable
internal fun CommunityTagManagementOverview(onBack: () -> Unit) {
    val context = LocalContext.current
    DetailPage(title = stringResource(R.string.annotation_tag_manage), onBack = onBack) {
        SettingsCategoryGroup {
            CategoryItem(
                icon = AppIcons.Tag,
                label = stringResource(R.string.community_manage_book_tags),
                grouped = true
            ) {
                context.startActivity(DetailActivity.createIntent(context, "book_tags"))
            }
            SettingsGroupDivider()
            CategoryItem(
                icon = AppIcons.Tag,
                label = stringResource(R.string.community_manage_annotation_tags),
                grouped = true
            ) {
                context.startActivity(DetailActivity.createIntent(context, "annotation_tags"))
            }
        }
    }
}
