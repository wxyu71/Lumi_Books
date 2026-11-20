package com.huangder.lumibooks.edition

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.huangder.lumibooks.R
import com.huangder.lumibooks.data.local.entity.TagGroupEntity
import com.huangder.lumibooks.domain.model.ManagedTag
import com.huangder.lumibooks.domain.model.TagOperationResult
import com.huangder.lumibooks.domain.model.TagTypes
import com.huangder.lumibooks.ui.components.*
import com.huangder.lumibooks.ui.icons.AppIcons
import com.huangder.lumibooks.ui.theme.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

/**
 * Shared manager for Settings and reader/annotation toolbar.
 *
 * Reuses the actual Room-backed TagManagementRepository rather than creating a
 * separate community tag store. Per-tag operations are explicit, deleting is
 * confirmed, and selection never changes the current book or note draft.
 */
private sealed interface TagTask {
    data object CreateTag : TagTask
    data object CreateGroup : TagTask
    data class RenameTag(val tag: ManagedTag) : TagTask
    data class RenameGroup(val group: TagGroupEntity) : TagTask
    data class DeleteTag(val tag: ManagedTag) : TagTask
    data class DeleteGroup(val group: TagGroupEntity) : TagTask
    data class DeleteMany(val keys: List<String>) : TagTask
    data class Move(val keys: List<String>) : TagTask
}

@Composable
private fun TagManagerAction(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color = AppColors.TextPrimary
) {
    // Unlike the one-line capsule convenience API, these are full task labels.
    // Allow line wrapping and height growth in narrow / large-font layouts.
    LiquidGlassButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        contentColor = color
    ) {
        Text(
            text = label,
            fontSize = AppType.BodySmall,
            fontWeight = FontWeight.SemiBold,
            color = color,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
internal fun CommunityTagManagementSheet(type: String, onDismiss: () -> Unit) {
    val repository = rememberTagPickerRepository()
    val groups by remember(repository, type) {
        repository?.observeGroups(type) ?: flowOf(emptyList<TagGroupEntity>())
    }.collectAsState(initial = emptyList())
    val tags by remember(repository, type) {
        repository?.observeCatalog(type) ?: flowOf(emptyList<ManagedTag>())
    }.collectAsState(initial = emptyList())

    val scope = rememberCoroutineScope()
    val title = stringResource(
        if (type == TagTypes.BOOK) R.string.community_manage_book_tags
        else R.string.community_manage_annotation_tags
    )
    val noTags = stringResource(R.string.community_no_tags)
    val groupDeleteHint = stringResource(R.string.community_delete_group_hint)
    val tagDeleteHint = stringResource(R.string.community_delete_tag_hint)
    val duplicate = stringResource(R.string.tag_name_exists)
    val invalid = stringResource(R.string.tag_management_invalid_name)
    val saveFailed = stringResource(R.string.tag_picker_save_failed)

    // Prefer a single full-width action per row on compact screens or large fonts.
    val layoutConfig = androidx.compose.ui.platform.LocalConfiguration.current
    val verticalActions = layoutConfig.screenWidthDp < 420 || layoutConfig.fontScale >= 1.25f

    var selecting by rememberSaveable(type) { mutableStateOf(false) }
    var selected by rememberSaveable(type) { mutableStateOf(emptySet<String>()) }
    var task by remember { mutableStateOf<TagTask?>(null) }
    var actionTag by remember { mutableStateOf<ManagedTag?>(null) }
    var draft by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    // Global rename/delete and sync can change the catalog while selection is open.
    LaunchedEffect(tags) { selected = selected.intersect(tags.map { it.key }.toSet()) }
    fun start(next: TagTask) {
        if (!busy) {
            draft = when (next) {
                is TagTask.RenameTag -> next.tag.name
                is TagTask.RenameGroup -> next.group.name
                else -> ""
            }
            error = null
            actionTag = null
            task = next
        }
    }
    fun perform(operation: suspend () -> TagOperationResult) {
        if (busy || repository == null) return
        busy = true
        scope.launch {
            try {
                val result = operation()
                if (result.success) {
                    if (task is TagTask.DeleteMany || task is TagTask.Move) {
                        selected = emptySet()
                        selecting = false
                    }
                    task = null
                    error = null
                } else {
                    error = when (result.message) {
                        "duplicate_name" -> duplicate
                        "invalid_name" -> invalid
                        else -> saveFailed
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                error = saveFailed
            } finally {
                busy = false
            }
        }
    }

    AppBottomSheet(onDismissRequest = { if (!busy) onDismiss() }) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp)
                .heightIn(max = appSheetMaxHeight())
                .padding(appSheetPadding(horizontal = AppSpace.md)),
            verticalArrangement = Arrangement.spacedBy(AppSpace.sm)
        ) {
            AppSheetHeader(title = title, onClose = { if (!busy) onDismiss() })
            if (verticalActions) {
                Column(verticalArrangement = Arrangement.spacedBy(AppSpace.sm)) {
                    TagManagerAction(
                        label = stringResource(R.string.community_create_tag),
                        onClick = { start(TagTask.CreateTag) },
                        enabled = repository != null && !busy,
                        modifier = Modifier.fillMaxWidth()
                    )
                    TagManagerAction(
                        label = stringResource(R.string.community_create_group),
                        onClick = { start(TagTask.CreateGroup) },
                        enabled = repository != null && !busy,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppSpace.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TagManagerAction(
                        label = stringResource(R.string.community_create_tag),
                        onClick = { start(TagTask.CreateTag) },
                        enabled = repository != null && !busy,
                        modifier = Modifier.weight(1f)
                    )
                    TagManagerAction(
                        label = stringResource(R.string.community_create_group),
                        onClick = { start(TagTask.CreateGroup) },
                        enabled = repository != null && !busy,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.community_tag_count, tags.size),
                    fontSize = AppType.Caption, color = AppColors.TextSecondary,
                    modifier = Modifier.weight(1f))
                TagManagerAction(
                    label = stringResource(if (selecting) R.string.cancel else R.string.community_select_tags),
                    onClick = { selecting = !selecting; selected = emptySet() },
                    enabled = tags.isNotEmpty() && !busy
                )
            }
            if (selecting) {
                Text(
                    stringResource(R.string.community_selected_count, selected.size),
                    fontSize = AppType.Caption,
                    color = AppColors.TextSecondary
                )
                if (verticalActions) {
                    Column(verticalArrangement = Arrangement.spacedBy(AppSpace.sm)) {
                        TagManagerAction(
                            label = stringResource(R.string.community_move_selected),
                            enabled = selected.isNotEmpty() && !busy,
                            onClick = { start(TagTask.Move(selected.toList())) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        TagManagerAction(
                            label = stringResource(R.string.community_delete_selected),
                            enabled = selected.isNotEmpty() && !busy,
                            onClick = { start(TagTask.DeleteMany(selected.toList())) },
                            modifier = Modifier.fillMaxWidth(),
                            color = AppColors.Destructive
                        )
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(AppSpace.sm)) {
                        TagManagerAction(
                            label = stringResource(R.string.community_move_selected),
                            enabled = selected.isNotEmpty() && !busy,
                            onClick = { start(TagTask.Move(selected.toList())) },
                            modifier = Modifier.weight(1f)
                        )
                        TagManagerAction(
                            label = stringResource(R.string.community_delete_selected),
                            enabled = selected.isNotEmpty() && !busy,
                            onClick = { start(TagTask.DeleteMany(selected.toList())) },
                            modifier = Modifier.weight(1f),
                            color = AppColors.Destructive
                        )
                    }
                }
            }
            // Compose only visible groups, not every group in a large synced catalog.
            // Keep each group's rows inside one shared GroupedCard to preserve the
            // settings-style separators, corner geometry and accessible row semantics.
            val groupIds = remember(groups) { groups.mapTo(mutableSetOf()) { it.id } }
            LazyColumn(
                Modifier.fillMaxWidth().weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(AppSpace.md)
            ) {
                items(
                    items = listOf<TagGroupEntity?>(null) + groups,
                    key = { group -> group?.let { "group:${it.id}" } ?: "group:ungrouped" }
                ) { group ->
                    val members = tags.filter { tag ->
                        if (group == null) tag.groupId == null || tag.groupId !in groupIds
                        else tag.groupId == group.id
                    }
                    GroupedCard(horizontalPadding = 0.dp) {
                        LumiRow(
                            title = group?.name ?: stringResource(R.string.tag_picker_uncategorized),
                            subtitle = stringResource(R.string.community_tag_count, members.size),
                            icon = AppIcons.Folder,
                            trailing = if (group == null || selecting) null else {
                                {
                                    LiquidGlassIconButton(
                                        imageVector = AppIcons.PencilSimple,
                                        contentDescription = stringResource(R.string.community_rename_group),
                                        onClick = { start(TagTask.RenameGroup(group)) },
                                        size = 44.dp
                                    )
                                    LiquidGlassIconButton(
                                        imageVector = AppIcons.Trash,
                                        contentDescription = stringResource(R.string.community_delete_group),
                                        onClick = { start(TagTask.DeleteGroup(group)) },
                                        size = 44.dp,
                                        contentColor = AppColors.Destructive
                                    )
                                }
                            }
                        )
                        if (members.isEmpty()) {
                            GroupedRowDivider()
                            Text(noTags, modifier = Modifier.padding(horizontal = AppSpace.md, vertical = AppSpace.sm),
                                color = AppColors.TextSecondary, fontSize = AppType.BodySmall)
                        } else {
                            members.forEach { tag ->
                                GroupedRowDivider()
                                LumiRow(
                                    title = tag.name,
                                    modifier = if (selecting) Modifier.semantics {
                                        toggleableState = if (tag.key in selected) ToggleableState.On else ToggleableState.Off
                                    } else Modifier,
                                    subtitle = stringResource(R.string.community_usage_count, tag.usageCount),
                                    role = if (selecting) Role.Checkbox else Role.Button,
                                    onClick = {
                                        if (selecting) selected = if (tag.key in selected) selected - tag.key else selected + tag.key
                                        else actionTag = tag
                                    },
                                    trailing = {
                                        if (selecting) {
                                            LumiCheckbox(checked = tag.key in selected, onCheckedChange = null)
                                        } else {
                                            LiquidGlassIconButton(
                                                imageVector = AppIcons.DotsThreeVertical,
                                                contentDescription = stringResource(R.string.annotation_tag_manage),
                                                onClick = { actionTag = tag },
                                                size = 44.dp
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                item(key = "list-bottom-spacer") { Spacer(Modifier.height(AppSpace.sm)) }
            }
        }
    }

    actionTag?.let { tag ->
        LiquidGlassAlertDialog(
            onDismissRequest = { if (!busy) actionTag = null },
            titleText = tag.name,
            text = {
                Text(stringResource(R.string.community_usage_count, tag.usageCount), color = AppColors.TextSecondary)
            },
            actions = listOf(
                AppDialogAction(stringResource(R.string.annotation_tag_rename), onClick = { start(TagTask.RenameTag(tag)) }),
                AppDialogAction(stringResource(R.string.community_move_tag), onClick = { start(TagTask.Move(listOf(tag.key))) }),
                AppDialogAction(stringResource(R.string.delete), onClick = { start(TagTask.DeleteTag(tag)) },
                    role = AppDialogActionRole.Destructive),
                AppDialogAction(stringResource(R.string.cancel), onClick = { actionTag = null },
                    role = AppDialogActionRole.Cancel)
            )
        )
    }

    task?.let { current ->
        val isMove = current is TagTask.Move
        val isDelete = current is TagTask.DeleteTag || current is TagTask.DeleteGroup || current is TagTask.DeleteMany
        val name = when (current) {
            TagTask.CreateTag -> stringResource(R.string.community_create_tag)
            TagTask.CreateGroup -> stringResource(R.string.community_create_group)
            is TagTask.RenameTag -> stringResource(R.string.annotation_tag_rename)
            is TagTask.RenameGroup -> stringResource(R.string.community_rename_group)
            is TagTask.DeleteTag, is TagTask.DeleteMany -> stringResource(R.string.delete)
            is TagTask.DeleteGroup -> stringResource(R.string.community_delete_group)
            is TagTask.Move -> stringResource(R.string.community_move_tag)
        }
        LiquidGlassAlertDialog(
            onDismissRequest = { if (!busy) { task = null; error = null } },
            titleText = name,
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(AppSpace.sm)) {
                    when (current) {
                        TagTask.CreateTag, TagTask.CreateGroup,
                        is TagTask.RenameTag, is TagTask.RenameGroup -> {
                            LumiTextField(
                                value = draft,
                                onValueChange = { draft = it; error = null },
                                enabled = !busy,
                                singleLine = true,
                                placeholder = { Text(stringResource(R.string.community_name_hint)) },
                                isError = error != null
                            )
                        }
                        is TagTask.DeleteGroup -> Text(groupDeleteHint)
                        is TagTask.DeleteMany -> {
                            Text(stringResource(R.string.community_selected_count, current.keys.size),
                                color = AppColors.TextSecondary, fontSize = AppType.BodySmall)
                            Text(tagDeleteHint)
                        }
                        is TagTask.DeleteTag -> Text(tagDeleteHint)
                        is TagTask.Move -> {
                            Text(stringResource(R.string.community_move_hint))
                            val keys = current.keys
                            val choices = listOf<TagGroupEntity?>(null) + groups
                            Column(Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
                                choices.forEach { group ->
                                    LumiRow(
                                        title = group?.name ?: stringResource(R.string.tag_picker_uncategorized),
                                        onClick = { perform { repository!!.moveTags(type, keys, group?.id) } },
                                        enabled = !busy,
                                        icon = AppIcons.Folder
                                    )
                                }
                            }
                        }
                    }
                    error?.let { Text(it, color = AppColors.Destructive, fontSize = AppType.BodySmall) }
                }
            },
            actions = if (isMove) {
                listOf(AppDialogAction(stringResource(R.string.cancel), { task = null }, role = AppDialogActionRole.Cancel))
            } else {
                listOf(
                    AppDialogAction(
                        text = stringResource(if (isDelete) R.string.delete else R.string.confirm),
                        enabled = !busy && (isDelete || draft.isNotBlank()),
                        role = if (isDelete) AppDialogActionRole.Destructive else AppDialogActionRole.Primary,
                        onClick = {
                            when (current) {
                                TagTask.CreateTag -> perform { repository!!.createTag(type, draft.trim()) }
                                TagTask.CreateGroup -> perform { repository!!.createGroup(type, draft.trim()) }
                                is TagTask.RenameTag -> perform { repository!!.renameTag(type, current.tag.key, draft.trim()) }
                                is TagTask.RenameGroup -> perform { repository!!.renameGroup(type, current.group.id, draft.trim()) }
                                is TagTask.DeleteTag -> perform { repository!!.deleteTag(type, current.tag.key) }
                                is TagTask.DeleteGroup -> perform { repository!!.deleteGroup(type, current.group.id) }
                                is TagTask.DeleteMany -> perform { repository!!.deleteTags(type, current.keys) }
                                is TagTask.Move -> Unit
                            }
                        }
                    ),
                    AppDialogAction(stringResource(R.string.cancel), onClick = { task = null }, role = AppDialogActionRole.Cancel)
                )
            }
        )
    }
}
