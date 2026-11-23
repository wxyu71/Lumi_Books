package com.huangder.lumibooks.ui.tags

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import com.huangder.lumibooks.R
import com.huangder.lumibooks.data.local.entity.TagGroupEntity
import com.huangder.lumibooks.data.repository.TagManagementRepository
import com.huangder.lumibooks.domain.model.ManagedTag
import com.huangder.lumibooks.domain.model.TagOperationResult
import com.huangder.lumibooks.domain.model.TagTypes
import com.huangder.lumibooks.ui.components.*
import com.huangder.lumibooks.ui.icons.AppIcons
import com.huangder.lumibooks.ui.settings.CollapsingSettingsScaffold
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

@Composable
private fun CommunityTagGroupCard(
    group: TagGroupEntity?,
    members: List<ManagedTag>,
    selecting: Boolean,
    selected: Set<String>,
    onToggle: (ManagedTag) -> Unit,
    onTagAction: (ManagedTag) -> Unit,
    onGroupAction: (TagGroupEntity) -> Unit
) {
    GroupedCard {
        if (group != null) {
            LumiRow(
                title = group.name,
                icon = AppIcons.Folder,
                trailing = if (selecting) null else {
                    {
                        LiquidGlassIconButton(
                            imageVector = AppIcons.DotsThreeVertical,
                            contentDescription = stringResource(R.string.annotation_tag_manage) + " " + group.name,
                            onClick = { onGroupAction(group) },
                            size = 48.dp
                        )
                    }
                }
            )
            if (members.isNotEmpty()) GroupedRowDivider()
        } else {
            LumiRow(
                title = stringResource(R.string.tag_picker_uncategorized),
                icon = AppIcons.Folder
            )
            GroupedRowDivider()
        }

        members.forEachIndexed { index, tag ->
            if (index > 0) GroupedRowDivider()
            LumiRow(
                title = tag.name,
                subtitle = if (tag.usageCount > 0) {
                    stringResource(R.string.community_usage_count, tag.usageCount)
                } else null,
                modifier = if (selecting) Modifier.semantics {
                    toggleableState = if (tag.key in selected) ToggleableState.On else ToggleableState.Off
                } else Modifier,
                role = if (selecting) Role.Checkbox else Role.Button,
                onClick = {
                    if (selecting) onToggle(tag) else onTagAction(tag)
                },
                trailing = {
                    if (selecting) {
                        LumiCheckbox(checked = tag.key in selected, onCheckedChange = null)
                    } else {
                        LiquidGlassIconButton(
                            imageVector = AppIcons.DotsThreeVertical,
                            contentDescription = stringResource(R.string.annotation_tag_manage) + " " + tag.name,
                            onClick = { onTagAction(tag) },
                            size = 48.dp
                        )
                    }
                }
            )
        }
    }
}

/** Actions remain pending until the upstream menu finishes returning to its trigger. */
private enum class TagPageAction { CreateTag, CreateGroup, SelectTags }

@Composable
private fun TagPageActionsMenu(
    enabled: Boolean,
    showBatchSelection: Boolean,
    onAction: (TagPageAction) -> Unit
) {
    val host = LocalLiquidGlassMenuHost.current
    val sourceId = remember { Any() }
    var bounds by remember { mutableStateOf(Rect.Zero) }
    val createTag = stringResource(R.string.community_create_tag)
    val createGroup = stringResource(R.string.community_create_group)
    val selectTags = stringResource(R.string.community_select_tags)
    val labels = if (showBatchSelection) listOf(createTag, createGroup, selectTags)
    else listOf(createTag, createGroup)
    val width = annotationMenuWidth(labels, extraWidth = 66.dp)

    LiquidGlassIconButton(
        imageVector = AppIcons.DotsThreeVertical,
        contentDescription = stringResource(R.string.more_options),
        enabled = enabled && host != null,
        size = 48.dp,
        iconSize = 18.dp,
        modifier = Modifier.liquidGlassMenuAnchor(sourceId)
            .onGloballyPositioned { bounds = it.boundsInRoot() },
        onClick = {
            if (bounds != Rect.Zero) {
                host?.toggle(
                    LiquidGlassMenuSpec(
                        anchorBounds = bounds,
                        width = width,
                        sourceId = sourceId,
                        keepTriggerInteractive = false,
                        items = buildList {
                            add(LiquidGlassMenuItem(createTag, icon = AppIcons.Tag) {
                                onAction(TagPageAction.CreateTag)
                            })
                            add(LiquidGlassMenuItem(createGroup, icon = AppIcons.Folder) {
                                onAction(TagPageAction.CreateGroup)
                            })
                            if (showBatchSelection) add(
                                LiquidGlassMenuItem(selectTags, icon = AppIcons.Check) {
                                    onAction(TagPageAction.SelectTags)
                                }
                            )
                        }
                    )
                )
            }
        }
    )
}

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
internal fun CommunityTagManagementScreen(
    type: String,
    onBack: () -> Unit,
    repository: TagManagementRepository? = rememberTagPickerRepository()
) {
    val groups by remember(repository, type) {
        repository?.observeGroups(type) ?: flowOf(emptyList<TagGroupEntity>())
    }.collectAsState(initial = emptyList())
    val tags by remember(repository, type) {
        repository?.observeCatalog(type) ?: flowOf(emptyList<ManagedTag>())
    }.collectAsState(initial = emptyList())

    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
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

    var selecting by rememberSaveable(type) { mutableStateOf(false) }
    var selected by rememberSaveable(type) { mutableStateOf(emptySet<String>()) }
    var pendingPageAction by remember { mutableStateOf<TagPageAction?>(null) }
    var task by remember { mutableStateOf<TagTask?>(null) }
    var actionTag by remember { mutableStateOf<ManagedTag?>(null) }
    var actionGroup by remember { mutableStateOf<TagGroupEntity?>(null) }
    var draft by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    // A hardware/system back action should close batch selection just like the toolbar back.
    ConfigurableBackHandler(enabled = selecting && task == null && !busy) {
        selecting = false
        selected = emptySet()
    }

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
            actionGroup = null
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

    // Index the catalog once. Rendering each group should not rescan every tag.
    val groupIds = remember(groups) { groups.mapTo(mutableSetOf()) { it.id } }
    val tagsByGroup = remember(tags) { tags.groupBy { it.groupId } }
    val ungrouped = remember(tagsByGroup, groupIds) {
        tagsByGroup.filterKeys { it == null || it !in groupIds }.values.flatten()
    }

    LiquidGlassMenuHost(Modifier.fillMaxSize()) {
        CollapsingSettingsScaffold(
            title = title,
            onBack = {
                when {
                    busy -> Unit
                    selecting -> {
                        selecting = false
                        selected = emptySet()
                    }
                    else -> onBack()
                }
            },
            actions = {
                if (selecting) {
                    LiquidGlassIconButton(
                        imageVector = AppIcons.Folder,
                        contentDescription = stringResource(R.string.community_move_selected),
                        onClick = { start(TagTask.Move(selected.toList())) },
                        enabled = selected.isNotEmpty() && !busy,
                        size = 48.dp,
                        iconSize = 18.dp
                    )
                    LiquidGlassIconButton(
                        imageVector = AppIcons.Trash,
                        contentDescription = stringResource(R.string.community_delete_selected),
                        onClick = { start(TagTask.DeleteMany(selected.toList())) },
                        enabled = selected.isNotEmpty() && !busy,
                        size = 48.dp,
                        iconSize = 18.dp,
                        contentColor = AppColors.Destructive
                    )
                } else {
                    val host = LocalLiquidGlassMenuHost.current
                    LaunchedEffect(pendingPageAction, host?.displayedMenu) {
                        val action = pendingPageAction ?: return@LaunchedEffect
                        if (host?.displayedMenu != null) return@LaunchedEffect
                        pendingPageAction = null
                        when (action) {
                            TagPageAction.CreateTag -> start(TagTask.CreateTag)
                            TagPageAction.CreateGroup -> start(TagTask.CreateGroup)
                            TagPageAction.SelectTags -> {
                                selecting = true
                                selected = emptySet()
                            }
                        }
                    }
                    TagPageActionsMenu(
                        enabled = repository != null && !busy,
                        showBatchSelection = tags.isNotEmpty(),
                        onAction = { pendingPageAction = it }
                    )
                }
            },
            lazyListState = listState,
            lazyContent = {
                if (tags.isEmpty() && groups.isEmpty()) {
                    item(key = "empty") {
                        EmptyState(
                            message = noTags,
                            icon = AppIcons.Tag,
                            modifier = Modifier.fillMaxWidth().padding(vertical = AppSpace.xl)
                        )
                    }
                } else {
                    if (ungrouped.isNotEmpty()) {
                        item(key = "group:ungrouped") {
                            Spacer(Modifier.height(AppSpace.md))
                            CommunityTagGroupCard(
                                group = null,
                                members = ungrouped,
                                selecting = selecting,
                                selected = selected,
                                onToggle = { tag ->
                                    selected = if (tag.key in selected) selected - tag.key else selected + tag.key
                                },
                                onTagAction = { actionTag = it },
                                onGroupAction = {}
                            )
                        }
                    }
                    items(groups, key = { "group:${it.id}" }) { group ->
                        val members = tagsByGroup[group.id].orEmpty()
                        Spacer(Modifier.height(AppSpace.md))
                        CommunityTagGroupCard(
                            group = group,
                            members = members,
                            selecting = selecting,
                            selected = selected,
                            onToggle = { tag ->
                                selected = if (tag.key in selected) selected - tag.key else selected + tag.key
                            },
                            onTagAction = { actionTag = it },
                            onGroupAction = { actionGroup = it }
                        )
                    }
                }
            }
        )
    }

    actionGroup?.let { group ->
        val memberCount = tags.count { it.groupId == group.id }
        LiquidGlassAlertDialog(
            onDismissRequest = { if (!busy) actionGroup = null },
            titleText = group.name,
            text = if (memberCount > 0) {
                {
                    Text(
                        stringResource(R.string.community_tag_count, memberCount),
                        color = AppColors.TextSecondary
                    )
                }
            } else {
                null
            },
            actions = listOf(
                AppDialogAction(
                    stringResource(R.string.community_rename_group),
                    onClick = { start(TagTask.RenameGroup(group)) }
                ),
                AppDialogAction(
                    stringResource(R.string.community_delete_group),
                    onClick = { start(TagTask.DeleteGroup(group)) },
                    role = AppDialogActionRole.Destructive
                ),
                AppDialogAction(
                    stringResource(R.string.cancel),
                    onClick = { actionGroup = null },
                    role = AppDialogActionRole.Cancel
                )
            )
        )
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
