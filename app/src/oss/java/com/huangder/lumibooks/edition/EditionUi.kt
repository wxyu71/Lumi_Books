package com.huangder.lumibooks.edition

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.huangder.lumibooks.ui.bookshelf.CommunityAnnotationFeedExportButton
import com.huangder.lumibooks.ui.statistics.journal.CommunityReadingJournalEntry
import com.huangder.lumibooks.ui.statistics.report.ReadingReportPeriod
import com.huangder.lumibooks.ui.tags.CommunityTagManagementSheet

@Composable
internal fun EditionMembershipEntry() = Unit

@Composable
internal fun EditionTagManagementSettingsEntry() {
    var type by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    // Exactly one inset grouped card, matching other Settings sections.
    com.huangder.lumibooks.ui.components.GroupedCard {
        com.huangder.lumibooks.ui.settings.CategoryItem(
            icon = com.huangder.lumibooks.ui.icons.AppIcons.Tag,
            label = androidx.compose.ui.res.stringResource(com.huangder.lumibooks.R.string.community_manage_book_tags),
            grouped = true,
            tileColor = com.huangder.lumibooks.ui.components.SettingsTileColors.Blue
        ) { type = com.huangder.lumibooks.domain.model.TagTypes.BOOK }
        com.huangder.lumibooks.ui.components.GroupedRowDivider(
            startInset = com.huangder.lumibooks.ui.theme.AppSpace.md +
                com.huangder.lumibooks.ui.components.SettingsIconTileSize +
                com.huangder.lumibooks.ui.components.SettingsIconTileGap
        )
        com.huangder.lumibooks.ui.settings.CategoryItem(
            icon = com.huangder.lumibooks.ui.icons.AppIcons.Tag,
            label = androidx.compose.ui.res.stringResource(com.huangder.lumibooks.R.string.community_manage_annotation_tags),
            grouped = true,
            tileColor = com.huangder.lumibooks.ui.components.SettingsTileColors.Indigo
        ) { type = com.huangder.lumibooks.domain.model.TagTypes.ANNOTATION }
    }
    type?.let { selected -> CommunityTagManagementSheet(selected) { type = null } }
}



@Composable
internal fun EditionReadingReviewEntry(period: ReadingReportPeriod, compact: Boolean = false) = Unit

/** Recording is strictly opt-in and stores events only in the existing local Room table. */
internal const val editionHasReadingJournal: Boolean = true

@Composable
internal fun EditionReadingJournalCard() = CommunityReadingJournalEntry()

@Composable
internal fun EditionBookJournalEntry(bookId: String) = CommunityReadingJournalEntry(bookId)

/** Opens the to-read list from the home header; the base edition has no such page. */
@Composable
internal fun rememberToReadHomeAction(): (() -> Unit)? = null

@Composable
internal fun EditionTagManagementButton(type: String, modifier: Modifier = Modifier, toolbar: Boolean = false) {
    var shown by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    com.huangder.lumibooks.ui.components.LiquidGlassIconButton(
        imageVector = com.huangder.lumibooks.ui.icons.AppIcons.Tag,
        contentDescription = androidx.compose.ui.res.stringResource(com.huangder.lumibooks.R.string.annotation_tag_manage),
        modifier = modifier,
        size = if (toolbar) com.huangder.lumibooks.ui.components.PageToolbarButtonSize else 44.dp,
        iconSize = if (toolbar) com.huangder.lumibooks.ui.components.PageToolbarIconSize else 20.dp,
        onClick = { shown = true }
    )
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
) = CommunityAnnotationFeedExportButton(entries, enabled, modifier)

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
