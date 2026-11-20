package com.huangder.lumibooks.edition

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.huangder.lumibooks.R
import com.huangder.lumibooks.data.local.entity.ReadingJournalEventEntity
import com.huangder.lumibooks.domain.repository.BookRepository
import com.huangder.lumibooks.reading.journal.*
import com.huangder.lumibooks.ui.bookshare.BookShareActivity
import com.huangder.lumibooks.ui.bookshare.shareDuration
import com.huangder.lumibooks.ui.components.*
import com.huangder.lumibooks.ui.icons.AppIcons
import com.huangder.lumibooks.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Room and DataStore already persist and synchronize journal records, with explicit consent. */
internal data class CommunityJournalState(
    val preferences: ReadingJournalState = ReadingJournalState(),
    val events: List<ReadingJournalEventEntity> = emptyList(),
    val titles: Map<String, String> = emptyMap()
)

@HiltViewModel
internal class CommunityJournalViewModel @Inject constructor(
    private val journal: ReadingJournalRepository,
    books: BookRepository
) : ViewModel() {
    val state = combine(
        journal.settings.state,
        journal.observeEvents(0L, Long.MAX_VALUE),
        books.getAllBooks()
    ) { preferences, events, library ->
        CommunityJournalState(
            preferences,
            events.sortedByDescending { it.startedAt }.take(300),
            library.filterNot { it.isHidden }.associate { it.id to it.title }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CommunityJournalState())

    fun enable(done: (Boolean) -> Unit) = act(done) { journal.enable(JournalRetention(1, JournalRetentionUnit.YEAR)) }
    fun stop(done: (Boolean) -> Unit) = act(done) { journal.disable(deleteHistory = false) }
    fun erase(done: (Boolean) -> Unit) = act(done) { journal.disable(deleteHistory = true) }
    fun retain(value: JournalRetention, done: (Boolean) -> Unit) = act(done) { journal.setRetention(value) }

    private fun act(done: (Boolean) -> Unit, work: suspend () -> Unit) {
        viewModelScope.launch {
            try { work(); done(true) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { done(false) }
        }
    }
}

@Composable
internal fun CommunityReadingJournalEntry(bookId: String? = null) {
    var open by rememberSaveable { mutableStateOf(false) }
    GroupedCard(horizontalPadding = 0.dp) {
        LumiNavigationRow(
            title = stringResource(R.string.community_journal_title),
            subtitle = stringResource(R.string.community_journal_entry_hint),
            icon = AppIcons.BookOpen,
            onClick = { open = true }
        )
    }
    if (open) CommunityReadingJournalSheet(bookId) { open = false }
}

@Composable
private fun CommunityReadingJournalSheet(
    bookId: String?,
    onDismiss: () -> Unit,
    model: CommunityJournalViewModel = hiltViewModel()
) {
    val data by model.state.collectAsState()
    val context = LocalContext.current
    val zone = remember { ZoneId.systemDefault() }
    val dayFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val events = remember(data.events, bookId, data.titles) {
        data.events.filter { (bookId == null || it.bookId == bookId) && it.bookId in data.titles }
    }
    val days = remember(events) {
        events.groupBy { Instant.ofEpochMilli(it.startedAt).atZone(zone).toLocalDate() }
            .toSortedMap(reverseOrder())
    }
    var confirmEnable by remember { mutableStateOf(false) }
    var confirmErase by remember { mutableStateOf(false) }
    var chooseRetention by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    fun complete(ok: Boolean, close: () -> Unit) {
        busy = false
        if (ok) { error = false; close() } else error = true
    }
    val retentionLabel = when (data.preferences.retention.unit) {
        JournalRetentionUnit.NEVER -> stringResource(R.string.community_retention_forever)
        JournalRetentionUnit.YEAR -> stringResource(R.string.community_retention_year)
        JournalRetentionUnit.MONTH -> stringResource(R.string.community_retention_three_months)
    }

    AppBottomSheet(onDismissRequest = { if (!busy) onDismiss() }) {
        Column(
            modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp)
                .heightIn(max = appSheetMaxHeight()).padding(appSheetPadding(horizontal = AppSpace.md)),
            verticalArrangement = Arrangement.spacedBy(AppSpace.md)
        ) {
            AppSheetHeader(title = stringResource(R.string.community_journal_title), onClose = { if (!busy) onDismiss() })
            GroupedCard(horizontalPadding = 0.dp) {
                LumiSwitchRow(
                    title = stringResource(R.string.community_journal_recording),
                    subtitle = stringResource(R.string.community_journal_privacy),
                    checked = data.preferences.enabled,
                    onCheckedChange = { enabled ->
                        if (!busy) {
                            if (enabled) confirmEnable = true
                            else { busy = true; model.stop { complete(it) {} } }
                        }
                    }
                )
                GroupedRowDivider()
                LumiNavigationRow(
                    title = stringResource(R.string.community_retention_title),
                    value = retentionLabel,
                    onClick = { if (!busy) chooseRetention = true }
                )
                GroupedRowDivider()
                LumiActionRow(
                    title = stringResource(R.string.community_erase_history),
                    destructive = true,
                    onClick = { if (!busy) confirmErase = true }
                )
            }
            if (error) Text(stringResource(R.string.community_action_failed),
                fontSize = AppType.BodySmall, color = AppColors.Destructive)
            Text(stringResource(R.string.community_journal_recent), fontSize = AppType.Caption,
                color = AppColors.TextSecondary)
            LazyColumn(Modifier.fillMaxWidth().weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(AppSpace.md)) {
                if (days.isEmpty()) {
                    item("empty") {
                        Text(
                            stringResource(if (data.preferences.enabled) R.string.community_journal_empty
                                else R.string.community_journal_disabled_hint),
                            color = AppColors.TextSecondary, fontSize = AppType.BodySmall,
                            modifier = Modifier.padding(AppSpace.md)
                        )
                    }
                }
                days.forEach { (day, items) ->
                    item(key = "date:$day") {
                        Text(day.format(dayFormatter), fontSize = AppType.Caption,
                            fontWeight = FontWeight.SemiBold, color = AppColors.TextSecondary,
                            modifier = Modifier.padding(start = AppSpace.md, bottom = AppSpace.sm))
                        GroupedCard(horizontalPadding = 0.dp) {
                            items.forEachIndexed { index, event ->
                                if (index > 0) GroupedRowDivider()
                                val session = event.type == ReadingJournalEventEntity.TYPE_SESSION
                                val label = when (event.type) {
                                    ReadingJournalEventEntity.TYPE_FINISHED -> stringResource(R.string.community_journal_finished)
                                    ReadingJournalEventEntity.TYPE_WANT_TO_READ -> stringResource(R.string.community_journal_want)
                                    else -> stringResource(if (event.listening) R.string.community_journal_listening else R.string.community_journal_reading)
                                }
                                val start = Instant.ofEpochMilli(event.startedAt).atZone(zone).format(timeFormatter)
                                val summary = "$start · $label" + if (session) " · ${shareDuration(context, event.durationMs)}" else ""
                                LumiRow(
                                    title = data.titles[event.bookId].orEmpty(),
                                    subtitle = summary,
                                    showChevron = session,
                                    onClick = if (session) ({ BookShareActivity.start(context, event.bookId, day) }) else null,
                                    trailing = if (session) {
                                        { Text(stringResource(R.string.community_journal_poster),
                                            color = AppColors.Accent, fontSize = AppType.Caption) }
                                    } else null
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (confirmEnable) LiquidGlassAlertDialog(
        onDismissRequest = { if (!busy) confirmEnable = false },
        titleText = stringResource(R.string.community_journal_consent_title),
        text = { Text(stringResource(R.string.community_journal_consent_body),
            color = AppColors.TextPrimary, fontSize = AppType.BodySmall) },
        actions = listOf(
            AppDialogAction(stringResource(R.string.community_journal_enable),
                onClick = { if (!busy) { busy = true; model.enable { complete(it) { confirmEnable = false } } } },
                enabled = !busy),
            AppDialogAction(stringResource(R.string.cancel), { confirmEnable = false }, role = AppDialogActionRole.Cancel)
        )
    )
    if (confirmErase) LiquidGlassAlertDialog(
        onDismissRequest = { if (!busy) confirmErase = false },
        titleText = stringResource(R.string.community_erase_history),
        text = { Text(stringResource(R.string.community_journal_erase_warning)) },
        actions = listOf(
            AppDialogAction(stringResource(R.string.delete),
                onClick = { if (!busy) { busy = true; model.erase { complete(it) { confirmErase = false } } } },
                role = AppDialogActionRole.Destructive, enabled = !busy),
            AppDialogAction(stringResource(R.string.cancel), { confirmErase = false }, role = AppDialogActionRole.Cancel)
        )
    )
    if (chooseRetention) LiquidGlassAlertDialog(
        onDismissRequest = { if (!busy) chooseRetention = false },
        titleText = stringResource(R.string.community_retention_title),
        text = {
            Column {
                listOf(
                    JournalRetention(3, JournalRetentionUnit.MONTH) to stringResource(R.string.community_retention_three_months),
                    JournalRetention(1, JournalRetentionUnit.YEAR) to stringResource(R.string.community_retention_year),
                    JournalRetention(1, JournalRetentionUnit.NEVER) to stringResource(R.string.community_retention_forever)
                ).forEach { (value, label) ->
                    LumiCheckRow(
                        title = label, selected = data.preferences.retention == value,
                        onClick = {
                            if (!busy) {
                                busy = true
                                model.retain(value) { complete(it) { chooseRetention = false } }
                            }
                        }, enabled = !busy
                    )
                }
            }
        },
        actions = listOf(AppDialogAction(stringResource(R.string.cancel),
            onClick = { chooseRetention = false }, role = AppDialogActionRole.Cancel))
    )
}
