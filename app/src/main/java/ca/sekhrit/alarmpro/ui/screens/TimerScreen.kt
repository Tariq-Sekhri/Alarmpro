package ca.sekhrit.alarmpro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.TimerOff
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.activity.compose.BackHandler
import androidx.lifecycle.viewmodel.compose.viewModel
import ca.sekhrit.alarmpro.data.TimerPreset
import ca.sekhrit.alarmpro.data.TimerControlStyle
import ca.sekhrit.alarmpro.data.TimerSortMode
import ca.sekhrit.alarmpro.ui.components.DurationPickerDialog
import ca.sekhrit.alarmpro.ui.theme.CardSurface
import ca.sekhrit.alarmpro.ui.theme.ElectricCyan
import ca.sekhrit.alarmpro.ui.theme.ElevatedSurface
import ca.sekhrit.alarmpro.util.TimeUtils
import ca.sekhrit.alarmpro.viewmodel.TimerViewModel
import ca.sekhrit.alarmpro.viewmodel.AlarmViewModel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
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
    val clockMillis by viewModel.clockMillis.collectAsState()
    val settings by settingsViewModel.settings.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editPreset by remember { mutableStateOf<TimerPreset?>(null) }
    var selectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(emptySet<String>()) }
    var showSortMenu by remember { mutableStateOf(false) }

    val sortedPresets = remember(presets, settings.timerSortMode) {
        when (settings.timerSortMode) {
            TimerSortMode.TIME_ASC -> presets.sortedBy { it.totalSeconds }
            TimerSortMode.TIME_DESC -> presets.sortedByDescending { it.totalSeconds }
            TimerSortMode.MANUAL -> presets
        }
    }

    val listState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(listState) { from, to ->
        val fromPreset = presets.find { it.id == from.key }
        val toPreset = presets.find { it.id == to.key }
        if (fromPreset != null && toPreset != null) {
            viewModel.movePreset(presets.indexOf(fromPreset), presets.indexOf(toPreset))
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

            items(sortedPresets, key = { it.id }) { preset ->
                ReorderableItem(reorderableState, key = preset.id) { isDragging ->
                    val timer = activeTimers[preset.id]
                    val isRunningPreset = timer != null && timer.isActive(clockMillis)
                    val remainingSeconds = timer?.liveRemainingSeconds(clockMillis) ?: preset.totalSeconds
                    TimerPresetCard(
                        preset = preset,
                        isRunning = isRunningPreset,
                        displaySeconds = remainingSeconds,
                        progress = if (isRunningPreset && timer.totalSeconds > 0) {
                            1f - (remainingSeconds.toFloat() / timer.totalSeconds.toFloat())
                        } else {
                            0f
                        },
                        onRestart = { viewModel.restartPreset(preset) },
                        onToggle = { enabled -> viewModel.togglePreset(preset, enabled) },
                        usePlayPauseButton = settings.timerControlStyle == TimerControlStyle.PLAY_PAUSE_BUTTON,
                        onEdit = { editPreset = preset },
                        onDelete = { viewModel.deletePreset(preset) },
                        selectionMode = selectionMode,
                        selected = preset.id in selectedIds,
                        onToggleSelection = { toggleSelected(preset.id) },
                        onEnterSelection = { enterSelectionMode(preset.id) },
                        sortMode = settings.timerSortMode,
                        dragModifier = Modifier.draggableHandle()
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
    isRunning: Boolean,
    displaySeconds: Int,
    progress: Float,
    onRestart: () -> Unit,
    onToggle: (Boolean) -> Unit,
    usePlayPauseButton: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    selectionMode: Boolean,
    selected: Boolean,
    onToggleSelection: () -> Unit,
    onEnterSelection: () -> Unit,
    sortMode: TimerSortMode,
    dragModifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(shape)
            .background(if (isRunning) CardSurface else ElevatedSurface.copy(alpha = 0.75f))
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
            if (sortMode == TimerSortMode.MANUAL && !selectionMode) {
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
                if (preset.label.isNotBlank()) {
                    Text(
                        text = preset.label,
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
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit timer")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete timer")
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
