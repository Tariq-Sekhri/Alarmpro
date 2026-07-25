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
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TimerOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
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
import ca.sekhrit.alarmpro.ui.theme.CardSurface
import ca.sekhrit.alarmpro.ui.theme.ElectricCyan
import ca.sekhrit.alarmpro.ui.theme.ElevatedSurface
import ca.sekhrit.alarmpro.util.TimeUtils
import ca.sekhrit.alarmpro.viewmodel.TimerViewModel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(
    onOpenSettings: () -> Unit,
    viewModel: TimerViewModel = viewModel()
) {
    val activeTimers by viewModel.activeTimers.collectAsState()
    val presets by viewModel.presets.collectAsState()
    val clockMillis by viewModel.clockMillis.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editPreset by remember { mutableStateOf<TimerPreset?>(null) }
    var selectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(emptySet<String>()) }

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
        TimerPresetDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { seconds, label ->
                viewModel.addPreset(seconds, label)
                showAddDialog = false
            }
        )
    }

    editPreset?.let { preset ->
        TimerPresetDialog(
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

            items(presets, key = { it.id }) { preset ->
                val timer = activeTimers[preset.id]
                val isRunningPreset = timer != null && timer.isActive(clockMillis)
                val remainingSeconds = timer
                    ?.takeIf { it.isActive(clockMillis) }
                    ?.liveRemainingSeconds(clockMillis)
                    ?: preset.totalSeconds
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
                    onEdit = { editPreset = preset },
                    onDelete = { viewModel.deletePreset(preset) },
                    selectionMode = selectionMode,
                    selected = preset.id in selectedIds,
                    onToggleSelection = { toggleSelected(preset.id) },
                    onEnterSelection = { enterSelectionMode(preset.id) }
                )
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
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    selectionMode: Boolean,
    selected: Boolean,
    onToggleSelection: () -> Unit,
    onEnterSelection: () -> Unit
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
                Switch(
                    checked = isRunning,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = ElectricCyan.copy(alpha = 0.4f),
                        checkedThumbColor = ElectricCyan
                    )
                )
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

@Composable
private fun TimerPresetDialog(
    initialTotalSeconds: Int = 0,
    initialLabel: String = "",
    onDismiss: () -> Unit,
    onConfirm: (Int, String) -> Unit
) {
    var hours by remember(initialTotalSeconds) { mutableIntStateOf(initialTotalSeconds / 3600) }
    var minutes by remember(initialTotalSeconds) {
        mutableIntStateOf((initialTotalSeconds % 3600) / 60)
    }
    var seconds by remember(initialTotalSeconds) { mutableIntStateOf(initialTotalSeconds % 60) }
    var label by remember(initialLabel) { mutableStateOf(initialLabel) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF293743),
            tonalElevation = 8.dp
        ) {
            Column {
                Text(
                    text = "Timer Duration:",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFF465561), Color(0xFF2C3945))
                            )
                        )
                        .padding(horizontal = 14.dp, vertical = 11.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 15.dp, end = 15.dp, top = 17.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.Top
                ) {
                    DurationWheel(
                        label = "hour",
                        value = hours,
                        maxValue = 99,
                        onValueChange = { hours = it },
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = ":",
                        fontSize = 16.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .width(28.dp)
                            .padding(top = 93.dp)
                    )
                    DurationWheel(
                        label = "min",
                        value = minutes,
                        maxValue = 59,
                        onValueChange = { minutes = it },
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = ":",
                        fontSize = 16.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .width(28.dp)
                            .padding(top = 93.dp)
                    )
                    DurationWheel(
                        label = "sec",
                        value = seconds,
                        maxValue = 59,
                        onValueChange = { seconds = it },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 19.dp, end = 19.dp, top = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Label:",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    BasicTextField(
                        value = label,
                        onValueChange = { label = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        decorationBox = { innerTextField ->
                            Column {
                                Box(modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) {
                                    if (label.isEmpty()) {
                                        Text(
                                            text = "none",
                                            color = Color(0xFFB9BEC2),
                                            maxLines = 1
                                        )
                                    }
                                    innerTextField()
                                }
                                HorizontalDivider(color = Color(0xFFFFB300), thickness = 2.dp)
                            }
                        },
                        modifier = Modifier
                            .width(64.dp)
                            .padding(start = 8.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 8.dp, top = 5.dp, bottom = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val buttonColors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismiss, colors = buttonColors) { Text("CANCEL") }
                    TextButton(
                        colors = buttonColors,
                        onClick = {
                            val total = hours * 3600 + minutes * 60 + seconds
                            if (total > 0) onConfirm(total, label)
                        }
                    ) {
                        Text("OK")
                    }
                }
            }
        }
    }
}

@Composable
internal fun DurationWheel(
    label: String,
    value: Int,
    maxValue: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val valueCount = maxValue + 1
    val initialCenter = remember {
        val midpoint = 50_000
        midpoint - (midpoint % valueCount) + value
    }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialCenter - 1)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val centerIndex by remember {
        derivedStateOf { listState.firstVisibleItemIndex + 1 }
    }
    var editing by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf(value.toString()) }
    var fieldWasFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val scrollScope = rememberCoroutineScope()
    val currentValue by rememberUpdatedState(value)
    val isEditing by rememberUpdatedState(editing)

    fun commitInput() {
        if (!editing) return
        val enteredValue = inputText.toIntOrNull()?.coerceIn(0, maxValue) ?: value
        editing = false
        fieldWasFocused = false
        focusManager.clearFocus()
        onValueChange(enteredValue)
    }

    LaunchedEffect(listState) {
        androidx.compose.runtime.snapshotFlow { listState.isScrollInProgress }
            .filter { !it }
            .collect {
                val selectedValue = Math.floorMod(listState.firstVisibleItemIndex + 1, valueCount)
                if (!isEditing && selectedValue != currentValue) {
                    onValueChange(selectedValue)
                }
            }
    }

    LaunchedEffect(value, editing) {
        if (editing) return@LaunchedEffect
        val currentValue = Math.floorMod(listState.firstVisibleItemIndex + 1, valueCount)
        if (currentValue != value) {
            var distance = value - currentValue
            if (distance > valueCount / 2) distance -= valueCount
            if (distance < -(valueCount / 2)) distance += valueCount
            listState.animateScrollToItem(listState.firstVisibleItemIndex + distance)
        }
    }

    LaunchedEffect(editing) {
        if (editing) {
            inputText = value.toString()
            focusRequester.requestFocus()
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(132.dp)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                flingBehavior = flingBehavior,
                userScrollEnabled = !editing
            ) {
                items(count = 100_000, key = { it }) { index ->
                    val itemValue = index % valueCount
                    val isCenter = index == centerIndex
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clickable(enabled = !editing) {
                                if (isCenter) {
                                    editing = true
                                } else {
                                    scrollScope.launch {
                                        listState.animateScrollToItem(index - 1)
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCenter && editing) {
                            BasicTextField(
                                value = inputText,
                                onValueChange = { newText ->
                                    if (newText.length <= 2 && newText.all(Char::isDigit)) {
                                        inputText = newText
                                    }
                                },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                ),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(onDone = { commitInput() }),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester)
                                    .onFocusChanged { focusState ->
                                        if (focusState.isFocused) {
                                            fieldWasFocused = true
                                        } else if (fieldWasFocused) {
                                            commitInput()
                                        }
                                    }
                            )
                        } else {
                            Text(
                                text = itemValue.toString(),
                                fontSize = 14.sp,
                                color = if (isCenter) Color.White else Color(0xFF858E95),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
            HorizontalDivider(
                color = Color(0xFFB7BDC1),
                thickness = 2.dp,
                modifier = Modifier.padding(top = 43.dp)
            )
            HorizontalDivider(
                color = Color(0xFF9CA4A9),
                thickness = 2.dp,
                modifier = Modifier.padding(top = 87.dp)
            )
        }
    }
}
