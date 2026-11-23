package com.huangder.lumibooks.edition

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.huangder.lumibooks.ui.bookshelf.CommunityAnnotationFeedExportButton
import com.huangder.lumibooks.ui.statistics.journal.CommunityReadingJournalEntry
import com.huangder.lumibooks.ui.statistics.report.ReadingReportPeriod
import com.huangder.lumibooks.ui.tags.CommunityTagManagementScreen

@Composable
internal fun EditionMembershipEntry() = Unit

@Composable
internal fun EditionTagManagementDetail(type: String, onBack: () -> Unit) =
    CommunityTagManagementScreen(type = type, onBack = onBack)


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
    val context = androidx.compose.ui.platform.LocalContext.current
    com.huangder.lumibooks.ui.components.LiquidGlassIconButton(
        imageVector = com.huangder.lumibooks.ui.icons.AppIcons.Tag,
        contentDescription = androidx.compose.ui.res.stringResource(
            if (type == com.huangder.lumibooks.domain.model.TagTypes.BOOK)
                com.huangder.lumibooks.R.string.community_manage_book_tags
            else com.huangder.lumibooks.R.string.community_manage_annotation_tags
        ),
        modifier = modifier,
        size = if (toolbar) com.huangder.lumibooks.ui.components.PageToolbarButtonSize else 44.dp,
        iconSize = if (toolbar) com.huangder.lumibooks.ui.components.PageToolbarIconSize else 20.dp,
        onClick = {
            val category = if (type == com.huangder.lumibooks.domain.model.TagTypes.BOOK) {
                "book_tags"
            } else {
                "annotation_tags"
            }
            context.startActivity(
                com.huangder.lumibooks.ui.settings.DetailActivity.createIntent(context, category)
            )
        }
    )
}

/** A visible, reachable 48dp action directly alongside "My tags" in the library. */
@Composable
internal fun EditionBookTagManagementAction(modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val description = androidx.compose.ui.res.stringResource(
        com.huangder.lumibooks.R.string.community_manage_book_tags
    )
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(com.huangder.lumibooks.ui.theme.AppRoundedCornerShape(com.huangder.lumibooks.ui.theme.AppRadius.md))
            .clickable {
                context.startActivity(
                    com.huangder.lumibooks.ui.settings.DetailActivity.createIntent(context, "book_tags")
                )
            }
            .semantics {
                contentDescription = description
                role = androidx.compose.ui.semantics.Role.Button
            }
            .padding(horizontal = 12.dp),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        androidx.compose.material3.Text(
            text = androidx.compose.ui.res.stringResource(com.huangder.lumibooks.R.string.annotation_tag_manage),
            color = com.huangder.lumibooks.ui.theme.AppColors.Accent,
            fontSize = com.huangder.lumibooks.ui.theme.AppType.Footnote,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
        )
    }
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
