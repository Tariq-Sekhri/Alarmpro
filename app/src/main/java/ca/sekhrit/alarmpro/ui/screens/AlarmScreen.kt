package ca.sekhrit.alarmpro.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AlarmAdd
import androidx.compose.material.icons.filled.AlarmOff
import android.content.Intent
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import androidx.lifecycle.viewmodel.compose.viewModel
import ca.sekhrit.alarmpro.AlarmRingActivity
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.TextButton
import ca.sekhrit.alarmpro.data.Alarm
import ca.sekhrit.alarmpro.data.AlarmSortMode
import ca.sekhrit.alarmpro.data.isSnoozeAllowed
import ca.sekhrit.alarmpro.data.resolveSnoozeMinutes
import ca.sekhrit.alarmpro.util.AlarmGrouping
import ca.sekhrit.alarmpro.util.AlarmSoundUtils
import ca.sekhrit.alarmpro.ui.theme.CardSurface
import ca.sekhrit.alarmpro.ui.theme.ElectricCyan
import ca.sekhrit.alarmpro.ui.theme.ElevatedSurface
import ca.sekhrit.alarmpro.ui.theme.WarmAmber
import ca.sekhrit.alarmpro.util.RepeatCalculator
import ca.sekhrit.alarmpro.util.TimeUtils
import ca.sekhrit.alarmpro.viewmodel.AlarmViewModel
import kotlinx.coroutines.delay
import java.time.LocalDateTime

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun AlarmScreen(    onOpenSettings: () -> Unit,
    onCreateAlarm: () -> Unit,
    onEditAlarm: (String) -> Unit,
    viewModel: AlarmViewModel = viewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val allAlarms by viewModel.alarms.collectAsState()
    val groups by viewModel.groups.collectAsState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val searchFocusRequester = remember { FocusRequester() }
    var tick by remember { mutableStateOf(0) }
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchFieldFocused by remember { mutableStateOf(false) }
    val sortMode = settings.defaultAlarmSortMode
    val activeAlarmsFirst = settings.activeAlarmsFirst
    var showSortMenu by remember { mutableStateOf(false) }
    var renameGroupId by remember { mutableStateOf<String?>(null) }
    var renameGroupLabel by remember { mutableStateOf("") }
    var selectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var showGroupDialog by remember { mutableStateOf(false) }
    var groupDialogName by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var deleteGroupId by remember { mutableStateOf<String?>(null) }
    var deleteGroupLabel by remember { mutableStateOf("") }

    fun closeSearch() {
        if (!showSearch) return
        showSearch = false
        searchQuery = ""
        searchFieldFocused = false
        focusManager.clearFocus()
    }

    fun exitSelectionMode() {
        selectionMode = false
        selectedIds = emptySet()
    }

    fun toggleSelected(alarmId: String) {
        selectedIds = if (alarmId in selectedIds) selectedIds - alarmId else selectedIds + alarmId
    }

    fun enterSelectionMode(alarmId: String) {
        selectionMode = true
        selectedIds = setOf(alarmId)
    }

    LaunchedEffect(Unit) {
        while (true) {
            tick++
            delay(1000)
        }
    }

    val now = remember(tick) { LocalDateTime.now() }
    val nextHeader = remember(tick, allAlarms, settings.use24HourFormat) {
        viewModel.nextAlarmHeader
    }

    val filteredAlarms = remember(allAlarms, searchQuery, settings.use24HourFormat, now) {
        if (searchQuery.isBlank()) {
            allAlarms
        } else {
            allAlarms.filter { alarm ->
                val groupLabel = groups.find { it.id == alarm.groupId }?.label.orEmpty()
                alarm.label.contains(searchQuery, ignoreCase = true) ||
                    groupLabel.contains(searchQuery, ignoreCase = true) ||
                    TimeUtils.formatTime(alarm.time, settings.use24HourFormat)
                        .contains(searchQuery, ignoreCase = true) ||
                    RepeatCalculator.alarmCardRepeatLine(alarm, now, settings.use24HourFormat)
                        .contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val listEntries = remember(filteredAlarms, groups, searchQuery, sortMode, activeAlarmsFirst, now) {
        if (searchQuery.isNotBlank()) {
            val timeComparator = compareBy<Alarm> {
                if (sortMode == AlarmSortMode.NEXT_TRIGGER) {
                    TimeUtils.nextTriggerMillis(it, now)
                } else {
                    it.time.toSecondOfDay().toLong()
                }
            }
            val comparator = if (activeAlarmsFirst) {
                compareBy<Alarm> { !it.isEnabled }.then(timeComparator)
            } else {
                timeComparator
            }
            filteredAlarms
                .sortedWith(comparator)
                .map { alarm ->
                    val group = groups.find { it.id == alarm.groupId }
                    val members = alarm.groupId?.let { AlarmGrouping.membersOf(it, allAlarms) }.orEmpty()
                    AlarmGrouping.ListEntry.AlarmRow(
                        alarm = alarm,
                        group = group,
                        indexInGroup = AlarmGrouping.indexInGroup(alarm, members)
                    )
                }
        } else {
            AlarmGrouping.buildListEntries(
                alarms = filteredAlarms,
                groups = groups,
                now = now,
                sortByNextTrigger = sortMode == AlarmSortMode.NEXT_TRIGGER,
                activeAlarmsFirst = activeAlarmsFirst
            )
        }
    }

    val selectableAlarmIds = remember(listEntries) {
        listEntries.filterIsInstance<AlarmGrouping.ListEntry.AlarmRow>().map { it.alarm.id }.toSet()
    }

    BackHandler(enabled = selectionMode) {
        exitSelectionMode()
    }

    BackHandler(enabled = showSearch && !selectionMode) {
        closeSearch()
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
                    },
                    enabled = renameGroupLabel.isNotBlank()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { renameGroupId = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showGroupDialog) {
        AlertDialog(
            onDismissRequest = { showGroupDialog = false },
            title = { Text("Add to group") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (groups.isNotEmpty()) {
                        Text(
                            text = "Existing groups",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            groups.forEach { group ->
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        viewModel.assignAlarmsToGroup(selectedIds, group.id)
                                        showGroupDialog = false
                                        exitSelectionMode()
                                    },
                                    label = { Text(group.label) }
                                )
                            }
                        }
                        HorizontalDivider()
                    }
                    Text(
                        text = "New group",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = groupDialogName,
                        onValueChange = { groupDialogName = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Group name") }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.groupAlarms(selectedIds, groupDialogName)
                        showGroupDialog = false
                        exitSelectionMode()
                    },
                    enabled = groupDialogName.isNotBlank() && selectedIds.isNotEmpty()
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGroupDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete alarms") },
            text = {
                Text(
                    if (selectedIds.size == 1) "Delete this alarm?"
                    else "Delete ${selectedIds.size} alarms?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val idsToDelete = selectedIds
                        viewModel.deleteAlarms(idsToDelete)
                        showDeleteConfirm = false
                        exitSelectionMode()
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    deleteGroupId?.let { groupId ->
        AlertDialog(
            onDismissRequest = { deleteGroupId = null },
            title = { Text("Delete group") },
            text = {
                Text("Delete \"$deleteGroupLabel\" and all alarms in this group?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteGroup(groupId)
                        selectedIds = selectedIds - allAlarms.filter { it.groupId == groupId }.map { it.id }.toSet()
                        deleteGroupId = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteGroupId = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (selectionMode) {
                TopAppBar(
                    title = {
                        ca.sekhrit.alarmpro.ui.components.AutoSizingTopAppBarTitle(
                            "${selectedIds.size} selected"
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { exitSelectionMode() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Exit selection")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                selectedIds = if (selectedIds.size == selectableAlarmIds.size) {
                                    emptySet()
                                } else {
                                    selectableAlarmIds
                                }
                            }
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Select all")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            } else {
                Column {
                    TopAppBar(
                        title = {
                            ca.sekhrit.alarmpro.ui.components.AutoSizingTopAppBarTitle("Alarm Clock")
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        ),
                        actions = {
                            Box {
                                IconButton(onClick = {
                                    closeSearch()
                                    showSortMenu = true
                                }) {
                                    Icon(Icons.Default.Sort, contentDescription = "Sort alarms")
                                }
                                DropdownMenu(
                                    expanded = showSortMenu,
                                    onDismissRequest = { showSortMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Next alarm") },
                                        onClick = {
                                            viewModel.updateSettings(
                                                settings.copy(defaultAlarmSortMode = AlarmSortMode.NEXT_TRIGGER)
                                            )
                                            showSortMenu = false
                                        },
                                        leadingIcon = {
                                            if (sortMode == AlarmSortMode.NEXT_TRIGGER) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = null)
                                            }
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Time of day") },
                                        onClick = {
                                            viewModel.updateSettings(
                                                settings.copy(defaultAlarmSortMode = AlarmSortMode.TIME_OF_DAY)
                                            )
                                            showSortMenu = false
                                        },
                                        leadingIcon = {
                                            if (sortMode == AlarmSortMode.TIME_OF_DAY) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = null)
                                            }
                                        }
                                    )
                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        text = { Text("Active alarms first") },
                                        onClick = {
                                            viewModel.updateSettings(
                                                settings.copy(activeAlarmsFirst = !settings.activeAlarmsFirst)
                                            )
                                            showSortMenu = false
                                        },
                                        leadingIcon = {
                                            if (activeAlarmsFirst) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = null)
                                            }
                                        }
                                    )
                                }
                            }
                            IconButton(onClick = { if (showSearch) closeSearch() else showSearch = true }) {
                                Icon(Icons.Default.Search, contentDescription = "Search alarms")
                            }
                            IconButton(onClick = {
                                closeSearch()
                                onOpenSettings()
                            }) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings")
                            }
                        }
                    )
                    if (showSearch) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search alarms") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 8.dp)
                                .focusRequester(searchFocusRequester)
                                .onFocusChanged { focusState ->
                                    if (focusState.isFocused) {
                                        searchFieldFocused = true
                                    } else if (searchFieldFocused) {
                                        closeSearch()
                                    }
                                }
                        )
                        LaunchedEffect(Unit) {
                            searchFocusRequester.requestFocus()
                        }
                    }
                }
            }
        },
        bottomBar = {
            if (selectionMode && selectedIds.isNotEmpty()) {
                SelectionBottomBar(
                    onDelete = { showDeleteConfirm = true },
                    onEnable = { 
                        viewModel.setAlarmsEnabled(selectedIds, enabled = true)
                        exitSelectionMode()
                    },
                    onDisable = { 
                        viewModel.setAlarmsEnabled(selectedIds, enabled = false)
                        exitSelectionMode()
                    },
                    onSkipNext = { 
                        viewModel.skipNextAlarms(selectedIds)
                        exitSelectionMode()
                    },
                    onGroup = {
                        groupDialogName = "groupLabel ${groups.size + 1}"
                        showGroupDialog = true
                    }
                )
            }
        },
        floatingActionButton = {
            if (!selectionMode) {
                FloatingActionButton(
                    onClick = onCreateAlarm,
                    containerColor = ElectricCyan,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.AlarmAdd, contentDescription = "Add alarm")
                }
            }
        },
        floatingActionButtonPosition = androidx.compose.material3.FabPosition.Center
    ) { innerPadding ->
        if (listEntries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .then(
                        if (showSearch) {
                            Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { closeSearch() }
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (allAlarms.isEmpty()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AlarmAdd,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "No alarms set",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Create an alarm to get started.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text(
                        text = "No matching alarms",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = if (selectionMode && selectedIds.isNotEmpty()) 120.dp else 88.dp
                )
            ) {
                if (nextHeader != null && searchQuery.isBlank()) {
                    item(key = "next_alarm_header") {
                        NextAlarmHeader(
                            timeLine = nextHeader.timeLine,
                            countdownLine = nextHeader.countdownLine
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                } else if (allAlarms.isNotEmpty() && searchQuery.isBlank()) {
                    item(key = "no_active_alarm_header") {
                        NextAlarmHeader(
                            timeLine = "No active alarms",
                            countdownLine = "Turn on an alarm to schedule it"
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                items(
                    items = listEntries,
                    key = { entry ->
                        when (entry) {
                            is AlarmGrouping.ListEntry.GroupHeader -> "group_${entry.group.id}"
                            is AlarmGrouping.ListEntry.AlarmRow -> "alarm_${entry.alarm.id}"
                        }
                    }
                ) { entry ->
                    when (entry) {
                        is AlarmGrouping.ListEntry.GroupHeader -> {
                            val groupAlarmIds = entry.alarms.map { it.id }.toSet()
                            GroupHeaderCard(
                                header = entry,
                                selectionMode = selectionMode,
                                groupSelected = groupAlarmIds.isNotEmpty() && groupAlarmIds.all { it in selectedIds },
                                onToggleExpand = {
                                    if (!selectionMode) {
                                        closeSearch()
                                        viewModel.toggleGroupCollapsed(entry.group.id)
                                    }
                                },
                                onToggleEnabled = {
                                    if (!selectionMode) {
                                        closeSearch()
                                        viewModel.toggleGroupEnabled(entry.group.id)
                                    }
                                },
                                onSkipNext = { viewModel.skipNextGroup(entry.group.id) },
                                onRename = {
                                    renameGroupId = entry.group.id
                                    renameGroupLabel = entry.group.label
                                },
                                onUngroup = { viewModel.ungroupGroup(entry.group.id) },
                                onDeleteGroup = {
                                    deleteGroupId = entry.group.id
                                    deleteGroupLabel = entry.group.label
                                },
                                onLongPress = {
                                    if (selectionMode) {
                                        selectedIds = selectedIds + groupAlarmIds
                                    } else {
                                        selectionMode = true
                                        selectedIds = groupAlarmIds
                                    }
                                },
                                onClick = {
                                    closeSearch()
                                    if (selectionMode) {
                                        selectedIds = if (groupAlarmIds.all { it in selectedIds }) {
                                            selectedIds - groupAlarmIds
                                        } else {
                                            selectedIds + groupAlarmIds
                                        }
                                    } else {
                                        viewModel.toggleGroupCollapsed(entry.group.id)
                                    }
                                }
                            )
                        }
                        is AlarmGrouping.ListEntry.AlarmRow -> {
                            val displayLabel = AlarmGrouping.effectiveLabel(
                                entry.alarm,
                                entry.group,
                                entry.indexInGroup
                            )
                            AlarmCard(
                                alarm = entry.alarm,
                                displayLabel = displayLabel,
                                use24Hour = settings.use24HourFormat,
                                repeatLine = RepeatCalculator.alarmCardRepeatLine(entry.alarm, now, settings.use24HourFormat),
                                skipScheduled = RepeatCalculator.hasSkipScheduled(entry.alarm, now),
                                indented = entry.group != null,
                                selectionMode = selectionMode,
                                selected = entry.alarm.id in selectedIds,
                                onSelectToggle = { toggleSelected(entry.alarm.id) },
                                onLongPress = {
                                    closeSearch()
                                    enterSelectionMode(entry.alarm.id)
                                },
                                onToggle = {
                                    closeSearch()
                                    viewModel.toggleAlarm(entry.alarm)
                                },
                                onEdit = {
                                    closeSearch()
                                    onEditAlarm(entry.alarm.id)
                                },
                                onCopy = { viewModel.copyAlarm(entry.alarm) },
                                onPreview = {
                                    context.startActivity(
                                        Intent(context, AlarmRingActivity::class.java).apply {
                                            putExtra(AlarmRingActivity.EXTRA_RING_TYPE, AlarmRingActivity.TYPE_PREVIEW)
                                            putExtra(AlarmRingActivity.EXTRA_ALARM_ID, entry.alarm.id)
                                            putExtra(AlarmRingActivity.EXTRA_HOUR, entry.alarm.time.hour)
                                            putExtra(AlarmRingActivity.EXTRA_MINUTE, entry.alarm.time.minute)
                                            putExtra(
                                                AlarmRingActivity.EXTRA_LABEL,
                                                displayLabel.ifBlank { entry.alarm.label }
                                            )
                                            putExtra(AlarmRingActivity.EXTRA_VIBRATE, entry.alarm.vibrate)
                                            putExtra(AlarmRingActivity.EXTRA_READ_LABEL_ALOUD, entry.alarm.readLabelAloud)
                                            putExtra(
                                                AlarmRingActivity.EXTRA_SNOOZE_ALLOWED,
                                                entry.alarm.isSnoozeAllowed(settings)
                                            )
                                            putExtra(
                                                AlarmRingActivity.EXTRA_SNOOZE_MINUTES,
                                                entry.alarm.resolveSnoozeMinutes(settings)
                                            )
                                            putExtra(
                                                AlarmRingActivity.EXTRA_SOUND_URI,
                                                AlarmSoundUtils.resolvePlaybackUri(context, entry.alarm, settings).toString()
                                            )
                                        }
                                    )
                                },
                                onSkipNext = { viewModel.skipNextAlarm(entry.alarm) },
                                onCancelSnooze = { viewModel.cancelSnooze(entry.alarm) },
                                onRemoveFromGroup = entry.group?.let {
                                    { viewModel.removeAlarmFromGroup(entry.alarm.id) }
                                },
                                onDelete = { viewModel.deleteAlarm(entry.alarm) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NextAlarmHeader(
    timeLine: String,
    countdownLine: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Notifications,
            contentDescription = null,
            tint = ElectricCyan,
            modifier = Modifier.size(36.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = timeLine,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = countdownLine,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GroupHeaderCard(
    header: AlarmGrouping.ListEntry.GroupHeader,
    selectionMode: Boolean,
    groupSelected: Boolean,
    onToggleExpand: () -> Unit,
    onToggleEnabled: () -> Unit,
    onSkipNext: () -> Unit,
    onRename: () -> Unit,
    onUngroup: () -> Unit,
    onDeleteGroup: () -> Unit,
    onLongPress: () -> Unit,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    var showMenu by remember { mutableStateOf(false) }
    val skipScheduled = header.skipScheduledCount > 0
    val allSkipped = header.skipScheduledCount == header.alarms.count { it.isEnabled }

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
                    append("${header.alarms.size} alarms")
                    if (header.group.isCollapsed) append(" · collapsed")
                    if (skipScheduled) append(" · skip scheduled")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = header.allEnabled,
            onCheckedChange = { onToggleEnabled() },
            enabled = !selectionMode,
            colors = SwitchDefaults.colors(
                checkedTrackColor = ElectricCyan.copy(alpha = 0.4f),
                checkedThumbColor = ElectricCyan
            )
        )

        if (!selectionMode) {
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Group options")
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(
                    text = { Text(if (allSkipped && skipScheduled) "Cancel skip" else "Skip next time") },
                    onClick = {
                        showMenu = false
                        onSkipNext()
                    },
                    leadingIcon = { Icon(Icons.Default.SkipNext, contentDescription = null) }
                )
                DropdownMenuItem(
                    text = { Text("Rename group") },
                    onClick = {
                        showMenu = false
                        onRename()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Ungroup alarms") },
                    onClick = {
                        showMenu = false
                        onUngroup()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete group") },
                    onClick = {
                        showMenu = false
                        onDeleteGroup()
                    },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                )
            }
        }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlarmCard(
    alarm: Alarm,
    displayLabel: String,
    use24Hour: Boolean,
    repeatLine: String,
    skipScheduled: Boolean,
    indented: Boolean,
    selectionMode: Boolean,
    selected: Boolean,
    onSelectToggle: () -> Unit,
    onLongPress: () -> Unit,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onCopy: () -> Unit,
    onPreview: () -> Unit,
    onSkipNext: () -> Unit,
    onCancelSnooze: () -> Unit,
    onRemoveFromGroup: (() -> Unit)?,
    onDelete: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val timeParts = TimeUtils.formatAlarmTimeParts(alarm.time, use24Hour)
    val contentColor = if (alarm.isEnabled) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .padding(start = if (indented) 20.dp else 0.dp)
            .clip(shape)
            .background(
                when {
                    selected -> CardSurface.copy(alpha = 0.9f)
                    alarm.isEnabled -> CardSurface
                    else -> ElevatedSurface.copy(alpha = 0.75f)
                }
            )
            .then(
                if (selected) {
                    Modifier.border(2.dp, ElectricCyan, shape)
                } else {
                    Modifier
                }
            )
            .combinedClickable(
                onClick = {
                    if (selectionMode) onSelectToggle() else onEdit()
                },
                onLongClick = onLongPress
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selectionMode) {
            Checkbox(
                checked = selected,
                onCheckedChange = { onSelectToggle() }
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            if (displayLabel.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Label,
                        contentDescription = null,
                        tint = WarmAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = displayLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = WarmAmber,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = timeParts.time,
                    style = MaterialTheme.typography.displaySmall,
                    color = contentColor
                )
                timeParts.period?.let { period ->
                    Text(
                        text = period,
                        style = MaterialTheme.typography.titleMedium,
                        color = contentColor,
                        modifier = Modifier.padding(start = 6.dp, bottom = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (alarm.repeat.type != ca.sekhrit.alarmpro.data.RepeatType.ONCE) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = repeatLine,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (skipScheduled) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Next occurrence skipped",
                    style = MaterialTheme.typography.labelMedium,
                    color = WarmAmber
                )
            }
        }

        if (!selectionMode) {
            Switch(
                checked = alarm.isEnabled,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedTrackColor = ElectricCyan.copy(alpha = 0.4f),
                    checkedThumbColor = ElectricCyan
                )
            )

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Alarm options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                DropdownMenuItem(
                    text = { Text("Copy") },
                    onClick = {
                        showMenu = false
                        onCopy()
                    },
                    leadingIcon = {
                        Icon(Icons.Default.ContentCopy, contentDescription = null)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Preview") },
                    onClick = {
                        showMenu = false
                        onPreview()
                    },
                    leadingIcon = {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                    }
                )
                val isSnoozed = alarm.snoozedUntilEpochMillis != null && alarm.snoozedUntilEpochMillis > System.currentTimeMillis()
                if (isSnoozed) {
                    DropdownMenuItem(
                        text = { Text("Cancel snooze") },
                        onClick = {
                            showMenu = false
                            onCancelSnooze()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.AlarmOff, contentDescription = null)
                        }
                    )
                }
                if (onRemoveFromGroup != null) {
                    DropdownMenuItem(
                        text = { Text("Remove from group") },
                        onClick = {
                            showMenu = false
                            onRemoveFromGroup()
                        }
                    )
                }
                DropdownMenuItem(
                    text = {
                        Text(if (skipScheduled) "Cancel skip" else "Skip next time")
                    },
                    onClick = {
                        showMenu = false
                        onSkipNext()
                    },
                    enabled = alarm.isEnabled,
                    leadingIcon = {
                        Icon(Icons.Default.SkipNext, contentDescription = null)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = {
                        showMenu = false
                        onDelete()
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Delete, contentDescription = null)
                    }
                )
            }
        }
        }
    }
}

@Composable
private fun SelectionBottomBar(
    onDelete: () -> Unit,
    onEnable: () -> Unit,
    onDisable: () -> Unit,
    onSkipNext: () -> Unit,
    onGroup: () -> Unit
) {
    Surface(color = CardSurface) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SelectionActionButton(
                    icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = WarmAmber) },
                    label = "Delete",
                    onClick = onDelete
                )
                SelectionActionButton(
                    icon = {
                        Icon(Icons.Default.Notifications, contentDescription = null, tint = ElectricCyan)
                    },
                    label = "Enable",
                    onClick = onEnable
                )
                SelectionActionButton(
                    icon = { Icon(Icons.Default.Close, contentDescription = null) },
                    label = "Disable",
                    onClick = onDisable
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SelectionActionButton(
                    icon = { Icon(Icons.Default.SkipNext, contentDescription = null) },
                    label = "Skip",
                    onClick = onSkipNext
                )
                SelectionActionButton(
                    icon = { Icon(Icons.Default.Folder, contentDescription = null, tint = ElectricCyan) },
                    label = "Group",
                    onClick = onGroup
                )
            }
        }
    }
}

@Composable
private fun SelectionActionButton(
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit
) {
    TextButton(onClick = onClick) {
        icon()
        Spacer(modifier = Modifier.width(4.dp))
        Text(label)
    }
}
