package com.huangder.lumibooks.edition

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.huangder.lumibooks.ui.statistics.report.ReadingReportPeriod

@Composable
internal fun EditionMembershipEntry() = Unit

@Composable
internal fun EditionTagManagementSettingsEntry() {
    var type by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    androidx.compose.foundation.layout.Column {
        com.huangder.lumibooks.ui.settings.CategoryItem(
            icon = com.huangder.lumibooks.ui.icons.AppIcons.Tile.Folders,
            label = androidx.compose.ui.res.stringResource(com.huangder.lumibooks.R.string.community_manage_book_tags),
            tileColor = com.huangder.lumibooks.ui.components.SettingsTileColors.Blue
        ) { type = com.huangder.lumibooks.domain.model.TagTypes.BOOK }
        com.huangder.lumibooks.ui.settings.CategoryItem(
            icon = com.huangder.lumibooks.ui.icons.AppIcons.Tile.Folders,
            label = androidx.compose.ui.res.stringResource(com.huangder.lumibooks.R.string.community_manage_annotation_tags),
            tileColor = com.huangder.lumibooks.ui.components.SettingsTileColors.Indigo
        ) { type = com.huangder.lumibooks.domain.model.TagTypes.ANNOTATION }
    }
    type?.let { selected -> CommunityTagManagementSheet(selected) { type = null } }
}



@Composable
internal fun EditionReadingReviewEntry(period: ReadingReportPeriod, compact: Boolean = false) = Unit

/** The base edition records nothing and shows no reading journal. */
internal const val editionHasReadingJournal: Boolean = false

@Composable
internal fun EditionReadingJournalCard() = Unit

@Composable
internal fun EditionBookJournalEntry(bookId: String) = Unit

/** Opens the to-read list from the home header; the base edition has no such page. */
@Composable
internal fun rememberToReadHomeAction(): (() -> Unit)? = null

@Composable
internal fun EditionTagManagementButton(type: String, modifier: Modifier = Modifier, toolbar: Boolean = false) {
    var shown by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    androidx.compose.material3.IconButton(modifier = modifier, onClick = { shown = true }) {
        androidx.compose.material3.Icon(
            imageVector = com.huangder.lumibooks.ui.icons.AppIcons.DotsThreeVertical,
            contentDescription = androidx.compose.ui.res.stringResource(com.huangder.lumibooks.R.string.annotation_tag_manage)
        )
    }
    if (shown) CommunityTagManagementSheet(type) { shown = false }
}

@Composable
internal fun rememberRemoteSourcePermission(sourceCount: Int): () -> Boolean = { true }

@Composable
internal fun rememberBookNotesExportAction(
    book: com.huangder.lumibooks.domain.model.Book?,
    notes: List<com.huangder.lumibooks.domain.model.Note>,
    bookmarks: List<com.huangder.lumibooks.domain.model.Bookmark>,
    onExportTxt: () -> Unit
): () -> Unit = onExportTxt

@Composable
internal fun EditionAnnotationFeedExportButton(
    entries: List<com.huangder.lumibooks.ui.bookshelf.AnnotationFeedEntry>,
    enabled: Boolean,
    modifier: Modifier = Modifier
) = Unit

internal fun canOpenBookNotesExport(hasAnnotations: Boolean): Boolean = hasAnnotations

/** The OSS build keeps the original last welcome page (thanks and sponsorship). */
@Composable
internal fun EditionWelcomeFinalPage(
    isDark: Boolean,
    isLiquidGlass: Boolean,
    onFinished: () -> Unit,
    default: @Composable () -> Unit
) = default()

@Composable
internal fun EditionLaunchNotice() = Unit
