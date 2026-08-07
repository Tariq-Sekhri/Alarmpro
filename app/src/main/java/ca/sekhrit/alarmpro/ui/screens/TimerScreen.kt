package ca.sekhrit.alarmpro.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.TimerOff
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ca.sekhrit.alarmpro.data.TimerPreset
import ca.sekhrit.alarmpro.data.TimerControlStyle
import ca.sekhrit.alarmpro.data.TimerSortMode
import ca.sekhrit.alarmpro.ui.components.DurationPickerDialog
import ca.sekhrit.alarmpro.ui.theme.CardSurface
import ca.sekhrit.alarmpro.ui.theme.ElectricCyan
import ca.sekhrit.alarmpro.ui.theme.ElevatedSurface
import ca.sekhrit.alarmpro.util.TimeUtils
import ca.sekhrit.alarmpro.util.TimerGrouping
import ca.sekhrit.alarmpro.viewmodel.AlarmViewModel
import ca.sekhrit.alarmpro.viewmodel.TimerViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(
    onOpenSettings: () -> Unit,
    viewModel: TimerViewModel = viewModel(),
    settingsViewModel: AlarmViewModel = viewModel()
) {
    val activeTimers by viewModel.activeTimers.collectAsState()
    val presets by viewModel.presets.collectAsState()
    val groups by viewModel.groups.collectAsState()
    val clockMillis by viewModel.clockMillis.collectAsState()
    val settings by settingsViewModel.settings.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editPreset by remember { mutableStateOf<TimerPreset?>(null) }
    var selectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(emptySet<String>()) }
    var showSortMenu by remember { mutableStateOf(false) }
    
    var showGroupDialog by remember { mutableStateOf(false) }
    var groupLabelInput by remember { mutableStateOf("") }
    
    var renameGroupId by remember { mutableStateOf<String?>(null) }
    var renameGroupLabel by remember { mutableStateOf("") }

    val listEntries = remember(presets, groups, activeTimers, clockMillis, settings.timerSortMode, settings.activeTimersFirst) {
        TimerGrouping.buildListEntries(
            presets = presets,
            groups = groups,
            activeTimers = activeTimers,
            clockMillis = clockMillis,
            sortMode = settings.timerSortMode,
            activeTimersFirst = settings.activeTimersFirst
        )
    }

    val listState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(listState) { from, to ->
        val fromKey = from.key as? String ?: return@rememberReorderableLazyListState
        val toKey = to.key as? String ?: return@rememberReorderableLazyListState
        
        val fromPreset = if (fromKey.startsWith("preset_")) presets.find { it.id == fromKey.removePrefix("preset_") } else null
        val toPreset = if (toKey.startsWith("preset_")) presets.find { it.id == toKey.removePrefix("preset_") } else null
        
        val isFromRoot = fromKey.startsWith("group_") || (fromPreset != null && fromPreset.groupId == null)
        
        if (isFromRoot) {
            val actualToKey = if (toPreset != null && toPreset.groupId != null) "group_${toPreset.groupId}" else toKey
            if (fromKey != actualToKey) {
                viewModel.moveRootItem(fromKey, actualToKey)
            }
        } else if (fromPreset != null && toPreset != null && fromPreset.groupId == toPreset.groupId && fromPreset.groupId != null) {
            viewModel.movePresetInsideGroup(fromPreset.groupId, fromPreset.id, toPreset.id)
        }
    }

    fun exitSelectionMode() {
        selectionMode = false
        selectedIds = emptySet()
    }

    fun toggleSelected(presetId: String) {
        selectedIds = if (presetId in selectedIds) selectedIds - presetId else selectedIds + presetId
        if (selectedIds.isEmpty()) selectionMode = false
    }

    fun enterSelectionMode(presetId: String) {
        selectionMode = true
        selectedIds = setOf(presetId)
    }

    BackHandler(enabled = selectionMode) { exitSelectionMode() }

    LaunchedEffect(presets) {
        selectedIds = selectedIds.intersect(presets.map { it.id }.toSet())
        if (selectedIds.isEmpty()) selectionMode = false
    }

    LaunchedEffect(Unit) {
        viewModel.syncFromStorage()
    }

    if (showAddDialog) {
        DurationPickerDialog(
            title = "Timer Duration:",
            onDismiss = { showAddDialog = false },
            onConfirm = { seconds, label ->
                viewModel.addPreset(seconds, label)
                showAddDialog = false
            }
        )
    }

    editPreset?.let { preset ->
        DurationPickerDialog(
            title = "Timer Duration:",
            initialTotalSeconds = preset.totalSeconds,
            initialLabel = preset.label,
            onDismiss = { editPreset = null },
            onConfirm = { seconds, label ->
                viewModel.updatePreset(preset, seconds, label)
                editPreset = null
            }
        )
    }

    if (showGroupDialog) {
        AlertDialog(
            onDismissRequest = { showGroupDialog = false },
            title = { Text("Create group") },
            text = {
                OutlinedTextField(
                    value = groupLabelInput,
                    onValueChange = { groupLabelInput = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (selectedIds.isNotEmpty()) {
                            viewModel.groupPresets(selectedIds, groupLabelInput)
                        }
                        showGroupDialog = false
                        exitSelectionMode()
                    }
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showGroupDialog = false }) { Text("Cancel") }
            }
        )
    }

    renameGroupId?.let { groupId ->
        AlertDialog(
            onDismissRequest = { renameGroupId = null },
            title = { Text("Rename group") },
            text = {
                OutlinedTextField(
                    value = renameGroupLabel,
                    onValueChange = { renameGroupLabel = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.renameGroup(groupId, renameGroupLabel)
                        renameGroupId = null
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { renameGroupId = null }) { Text("Cancel") }
            }
        )
    }

    val isActive = activeTimers.values.any { it.isActive(clockMillis) }
    val nextHeader = remember(clockMillis, activeTimers) { viewModel.nextTimerHeader }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    ca.sekhrit.alarmpro.ui.components.AutoSizingTopAppBarTitle(
                        if (selectionMode) "${selectedIds.size} selected" else "Countdown Timer"
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                navigationIcon = {
                    if (selectionMode) {
                        IconButton(onClick = { exitSelectionMode() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Exit selection")
                        }
                    }
                },
                actions = {
                    if (selectionMode) {
                        IconButton(
                            onClick = {
                                groupLabelInput = ""
                                showGroupDialog = true
                            },
                            enabled = selectedIds.isNotEmpty()
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = "Group selected")
                        }
                        IconButton(
                            onClick = {
                                viewModel.deletePresets(selectedIds)
                                exitSelectionMode()
                            },
                            enabled = selectedIds.isNotEmpty()
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete selected timers")
                        }
                    } else {
                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(Icons.Default.Sort, contentDescription = "Sort timers")
                            }
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Active Timers First") },
                                    onClick = {
                                        settingsViewModel.updateSettings(
                                            settings.copy(activeTimersFirst = !settings.activeTimersFirst)
                                        )
                                        showSortMenu = false
                                    },
                                    trailingIcon = {
                                        if (settings.activeTimersFirst) {
                                            Icon(Icons.Default.Check, contentDescription = "Checked")
                                        }
                                    }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("Manual (Drag to reorder)") },
                                    onClick = {
                                        settingsViewModel.updateSettings(settings.copy(timerSortMode = TimerSortMode.MANUAL))
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Time (Shortest first)") },
                                    onClick = {
                                        settingsViewModel.updateSettings(settings.copy(timerSortMode = TimerSortMode.TIME_ASC))
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Time (Longest first)") },
                                    onClick = {
                                        settingsViewModel.updateSettings(settings.copy(timerSortMode = TimerSortMode.TIME_DESC))
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (!selectionMode) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = ElectricCyan,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.HourglassEmpty, contentDescription = "Add timer")
                }
            }
        },
        floatingActionButtonPosition = androidx.compose.material3.FabPosition.Center
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isActive) Icons.Default.HourglassEmpty else Icons.Default.TimerOff,
                        contentDescription = null,
                        tint = if (isActive) ElectricCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Text(
                        text = nextHeader ?: "No Timers Set",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isActive) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(
                items = listEntries,
                key = { entry ->
                    when (entry) {
                        is TimerGrouping.ListEntry.GroupHeader -> "group_${entry.group.id}"
                        is TimerGrouping.ListEntry.PresetRow -> "preset_${entry.preset.id}"
                    }
                }
            ) { entry ->
                when (entry) {
                    is TimerGrouping.ListEntry.GroupHeader -> {
                        val groupPresetIds = entry.presets.map { it.id }.toSet()
                        ReorderableItem(reorderableState, key = "group_${entry.group.id}") { isDragging ->
                            TimerGroupHeaderCard(
                                header = entry,
                                selectionMode = selectionMode,
                                groupSelected = groupPresetIds.isNotEmpty() && groupPresetIds.all { it in selectedIds },
                                onToggleExpand = {
                                    if (!selectionMode) {
                                        viewModel.toggleGroupCollapsed(entry.group.id)
                                    }
                                },
                                onRename = {
                                    renameGroupLabel = entry.group.label
                                    renameGroupId = entry.group.id
                                },
                                onUngroup = {
                                    viewModel.ungroupGroup(entry.group.id)
                                },
                                onDeleteGroup = {
                                    viewModel.deleteGroup(entry.group.id)
                                    viewModel.deletePresets(entry.presets.map { it.id }.toSet())
                                },
                                onLongPress = {
                                    if (!selectionMode) {
                                        selectedIds = groupPresetIds
                                        selectionMode = true
                                    }
                                },
                                onClick = {
                                    if (selectionMode) {
                                        if (groupPresetIds.all { it in selectedIds }) {
                                            selectedIds = selectedIds - groupPresetIds
                                            if (selectedIds.isEmpty()) selectionMode = false
                                        } else {
                                            selectedIds = selectedIds + groupPresetIds
                                        }
                                    }
                                },
                                sortMode = settings.timerSortMode,
                                dragModifier = Modifier.draggableHandle()
                            )
                        }
                    }
                    is TimerGrouping.ListEntry.PresetRow -> {
                        ReorderableItem(reorderableState, key = "preset_${entry.preset.id}") { isDragging ->
                            val timer = activeTimers[entry.preset.id]
                            val isRunningPreset = timer != null && timer.isActive(clockMillis)
                            val remainingSeconds = timer?.liveRemainingSeconds(clockMillis) ?: entry.preset.totalSeconds
                            TimerPresetCard(
                                preset = entry.preset,
                                group = entry.group,
                                indexInGroup = entry.indexInGroup,
                                isRunning = isRunningPreset,
                                displaySeconds = remainingSeconds,
                                progress = if (isRunningPreset && timer.totalSeconds > 0) {
                                    1f - (remainingSeconds.toFloat() / timer.totalSeconds.toFloat())
                                } else {
                                    0f
                                },
                                onRestart = { viewModel.restartPreset(entry.preset) },
                                onToggle = { enabled -> viewModel.togglePreset(entry.preset, enabled) },
                                usePlayPauseButton = settings.timerControlStyle == TimerControlStyle.PLAY_PAUSE_BUTTON,
                                onEdit = { editPreset = entry.preset },
                                onDelete = { viewModel.deletePreset(entry.preset) },
                                onRemoveFromGroup = { viewModel.removePresetFromGroup(entry.preset.id) },
                                selectionMode = selectionMode,
                                selected = entry.preset.id in selectedIds,
                                onToggleSelection = { toggleSelected(entry.preset.id) },
                                onEnterSelection = { enterSelectionMode(entry.preset.id) },
                                sortMode = settings.timerSortMode,
                                indented = entry.group != null,
                                dragModifier = Modifier.draggableHandle()
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TimerGroupHeaderCard(
    header: TimerGrouping.ListEntry.GroupHeader,
    selectionMode: Boolean,
    groupSelected: Boolean,
    onToggleExpand: () -> Unit,
    onRename: () -> Unit,
    onUngroup: () -> Unit,
    onDeleteGroup: () -> Unit,
    onLongPress: () -> Unit,
    onClick: () -> Unit,
    sortMode: TimerSortMode,
    dragModifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(shape)
            .background(
                if (groupSelected) CardSurface.copy(alpha = 0.9f) else CardSurface
            )
            .then(
                if (groupSelected) {
                    Modifier.border(2.dp, ElectricCyan, shape)
                } else {
                    Modifier
                }
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            )
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (sortMode == TimerSortMode.MANUAL && !selectionMode) {
            Icon(
                Icons.Default.DragHandle,
                contentDescription = "Reorder",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = dragModifier.padding(end = 8.dp)
            )
        }
        if (selectionMode) {
            Checkbox(
                checked = groupSelected,
                onCheckedChange = { onClick() }
            )
        } else {
            IconButton(onClick = onToggleExpand) {
                Icon(
                    imageVector = if (header.group.isCollapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                    contentDescription = if (header.group.isCollapsed) "Expand group" else "Collapse group"
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Folder,
                    contentDescription = null,
                    tint = ElectricCyan,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = header.group.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = buildString {
                    append("${header.presets.size} timers")
                    if (header.group.isCollapsed) append(" · collapsed")
                    if (header.isActive) append(" · active")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (!selectionMode) {
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Group options")
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Rename group") },
                        onClick = {
                            showMenu = false
                            onRename()
                        },
                        leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Ungroup timers") },
                        onClick = {
                            showMenu = false
                            onUngroup()
                        },
                        leadingIcon = { Icon(Icons.Default.FolderOff, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete group") },
                        onClick = {
                            showMenu = false
                            onDeleteGroup()
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TimerPresetCard(
    preset: TimerPreset,
    group: ca.sekhrit.alarmpro.data.TimerGroup?,
    indexInGroup: Int?,
    isRunning: Boolean,
    displaySeconds: Int,
    progress: Float,
    onRestart: () -> Unit,
    onToggle: (Boolean) -> Unit,
    usePlayPauseButton: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRemoveFromGroup: () -> Unit,
    selectionMode: Boolean,
    selected: Boolean,
    onToggleSelection: () -> Unit,
    onEnterSelection: () -> Unit,
    sortMode: TimerSortMode,
    indented: Boolean,
    dragModifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .padding(start = if (indented) 20.dp else 0.dp)
            .clip(shape)
            .background(if (isRunning) CardSurface else ElevatedSurface.copy(alpha = 0.75f))
            .then(
                if (selected) Modifier.border(2.dp, ElectricCyan, shape) else Modifier
            )
            .combinedClickable(
                onClick = { if (selectionMode) onToggleSelection() else onEdit() },
                onLongClick = {
                    if (selectionMode) onToggleSelection() else onEnterSelection()
                }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (sortMode == TimerSortMode.MANUAL && !selectionMode && group == null) {
                Icon(
                    Icons.Default.DragHandle,
                    contentDescription = "Reorder",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = dragModifier.padding(end = 8.dp)
                )
            }
            if (selectionMode) {
                Checkbox(checked = selected, onCheckedChange = { onToggleSelection() })
            } else {
                IconButton(onClick = onRestart) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Restart timer",
                        tint = if (isRunning) ElectricCyan else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = TimeUtils.formatDuration(displaySeconds.toLong()),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.combinedClickable(
                        onClick = { if (selectionMode) onToggleSelection() else onEdit() },
                        onLongClick = {
                            if (selectionMode) onToggleSelection() else onEnterSelection()
                        }
                    )
                )
                val displayLabel = TimerGrouping.effectiveLabel(preset, group, indexInGroup)
                if (displayLabel.isNotBlank()) {
                    Text(
                        text = displayLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!selectionMode) {
                if (usePlayPauseButton) {
                    IconButton(onClick = { onToggle(!isRunning) }) {
                        Icon(
                            imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isRunning) "Pause timer" else "Start timer",
                            tint = if (isRunning) ElectricCyan else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Switch(
                        checked = isRunning,
                        onCheckedChange = onToggle,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = ElectricCyan.copy(alpha = 0.4f),
                            checkedThumbColor = ElectricCyan
                        )
                    )
                }
                
                var showMenu by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Timer options")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Edit timer") },
                            onClick = {
                                showMenu = false
                                onEdit()
                            },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                        )
                        if (group != null) {
                            DropdownMenuItem(
                                text = { Text("Remove from group") },
                                onClick = {
                                    showMenu = false
                                    onRemoveFromGroup()
                                },
                                leadingIcon = { Icon(Icons.Default.FolderOff, contentDescription = null) }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Delete timer") },
                            onClick = {
                                showMenu = false
                                onDelete()
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        )
                    }
                }
            }
        }

        if (isRunning) {
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 10.dp),
                color = ElectricCyan,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}
