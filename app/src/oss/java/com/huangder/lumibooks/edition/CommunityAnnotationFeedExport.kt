package com.huangder.lumibooks.edition

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.huangder.lumibooks.R
import com.huangder.lumibooks.ui.bookshelf.AnnotationFeedEntry
import com.huangder.lumibooks.ui.bookshelf.BookNotesExportBuilder
import com.huangder.lumibooks.ui.bookshelf.NotesExportFormat
import com.huangder.lumibooks.ui.bookshelf.notesExportBooks
import com.huangder.lumibooks.ui.components.AppDialogAction
import com.huangder.lumibooks.ui.components.AppDialogActionRole
import com.huangder.lumibooks.ui.components.LiquidGlassAlertDialog
import com.huangder.lumibooks.ui.components.LiquidGlassIconButton
import com.huangder.lumibooks.ui.icons.AppIcons
import com.huangder.lumibooks.ui.theme.AppColors
import com.huangder.lumibooks.ui.theme.AppType
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.io.IOException
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@EntryPoint
@InstallIn(SingletonComponent::class)
interface CommunityExportEntryPoint {
    fun bookNotesExportBuilder(): BookNotesExportBuilder
}

/** The All Annotations button exports exactly the current search, type and tag-filter results.
 * Source snapshots come from the existing ViewModel; no new database calls or cloud storage.
 */
@Composable
internal fun CommunityAnnotationFeedExportButton(
    entries: List<AnnotationFeedEntry>,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext
    val builder = remember(app) {
        if (app !is dagger.hilt.internal.GeneratedComponentManager<*>) null
        else EntryPointAccessors.fromApplication(app, CommunityExportEntryPoint::class.java)
            .bookNotesExportBuilder()
    }
    val scope = rememberCoroutineScope()
    var formatPicker by remember { mutableStateOf(false) }
    var preparing by remember { mutableStateOf(false) }
    var job by remember { mutableStateOf<Job?>(null) }
    var documentText by remember { mutableStateOf<String?>(null) }
    var documentName by remember { mutableStateOf<String?>(null) }

    val saveDocument: (Uri?) -> Unit = { uri ->
        val text = documentText
        documentText = null
        documentName = null
        if (uri != null && text != null) {
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        val output = context.contentResolver.openOutputStream(uri)
                            ?: throw IOException("No output stream")
                        output.bufferedWriter(Charsets.UTF_8).use { it.write(text) }
                    }
                }
                Toast.makeText(context,
                    if (result.isSuccess) R.string.export_bookmarks_success else R.string.export_bookmarks_failed,
                    Toast.LENGTH_SHORT).show()
            }
        }
    }
    val txtPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(NotesExportFormat.TXT.mimeType),
        saveDocument
    )
    val mdPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(NotesExportFormat.MARKDOWN.mimeType),
        saveDocument
    )
    val selection = remember(entries) { notesExportBooks(entries) }
    val available = enabled && selection.isNotEmpty() && builder != null
    fun export(format: NotesExportFormat) {
        if (preparing || !available) return
        formatPicker = false
        preparing = true
        val snapshot = selection
        job = scope.launch {
            try {
                val documents = snapshot.map { source ->
                    source.book.title to builder!!.build(
                        book = source.book, bookmarks = source.bookmarks, notes = source.notes,
                        format = format
                    )
                }
                val content = buildString {
                    documents.forEachIndexed { index, (bookName, doc) ->
                        if (index > 0) appendLine("\n\n---\n")
                        if (format == NotesExportFormat.TXT) appendLine("======= $bookName =======")
                        append(doc.text)
                    }
                }
                val name = "LumiCommunity-notes-${LocalDate.now()}.${format.extension}"
                documentText = content
                documentName = name
                when (format) {
                    NotesExportFormat.TXT -> txtPicker.launch(name)
                    NotesExportFormat.MARKDOWN -> mdPicker.launch(name)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                documentText = null
                documentName = null
                Toast.makeText(context, R.string.community_export_failed, Toast.LENGTH_LONG).show()
            } finally {
                preparing = false
                job = null
            }
        }
    }
    LiquidGlassIconButton(
        imageVector = AppIcons.DownloadSimple,
        contentDescription = stringResource(R.string.community_export_all_title),
        modifier = modifier,
        size = com.huangder.lumibooks.ui.components.PageToolbarButtonSize,
        iconSize = com.huangder.lumibooks.ui.components.PageToolbarIconSize,
        enabled = available && !preparing,
        onClick = { formatPicker = true }
    )

    if (formatPicker) LiquidGlassAlertDialog(
        onDismissRequest = { formatPicker = false },
        titleText = stringResource(R.string.community_export_all_title),
        text = {
            Text(stringResource(R.string.community_export_selection, entries.size, selection.size),
                color = AppColors.TextSecondary, fontSize = AppType.BodySmall)
        },
        actions = listOf(
            AppDialogAction(stringResource(R.string.community_export_txt), { export(NotesExportFormat.TXT) }),
            AppDialogAction(stringResource(R.string.community_export_md), { export(NotesExportFormat.MARKDOWN) }),
            AppDialogAction(stringResource(R.string.cancel), { formatPicker = false }, role = AppDialogActionRole.Cancel)
        )
    )
    if (preparing) LiquidGlassAlertDialog(
        onDismissRequest = { job?.cancel() },
        titleText = stringResource(R.string.community_export_preparing),
        text = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator(color = AppColors.Accent, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                Text(stringResource(R.string.community_export_selection, entries.size, selection.size))
            }
        },
        actions = listOf(
            AppDialogAction(stringResource(R.string.cancel), onClick = { job?.cancel() },
                role = AppDialogActionRole.Cancel)
        )
    )
}
