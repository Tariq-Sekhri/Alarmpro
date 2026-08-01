package ca.sekhrit.alarmpro.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import ca.sekhrit.alarmpro.ui.theme.CardSurface
import ca.sekhrit.alarmpro.ui.theme.ElevatedSurface
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel as composeViewModel
import ca.sekhrit.alarmpro.receiver.NotificationHelper
import ca.sekhrit.alarmpro.ui.theme.ElectricCyan
import ca.sekhrit.alarmpro.util.TimeUtils
import ca.sekhrit.alarmpro.viewmodel.LapEntry
import ca.sekhrit.alarmpro.viewmodel.StopwatchMark
import ca.sekhrit.alarmpro.viewmodel.StopwatchViewModel

private data class StopwatchTab(
    val id: String,
    val label: String
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun StopwatchScreen(
    onOpenSettings: () -> Unit = {},
    viewModel: StopwatchViewModel? = null
) {
    var stopwatchTabs by remember {
        mutableStateOf(listOf(StopwatchTab(id = "default", label = "Stopwatch 1")))
    }
    var selectedStopwatchId by remember { mutableStateOf("default") }
    val selectedViewModel = viewModel ?: composeViewModel<StopwatchViewModel>(key = selectedStopwatchId)
    SideEffect {
        selectedViewModel.bindToSession(selectedStopwatchId)
    }
    val state by selectedViewModel.state.collectAsState()
    val markPresets by selectedViewModel.suggestedAlertSeconds.collectAsState()
    val bestLapMs = state.laps.minOfOrNull { it.lapTimeMs }
    val context = LocalContext.current
    var showCustomDialog by remember { mutableStateOf(false) }
    var showPresetEditor by remember { mutableStateOf(false) }
    var renameStopwatchId by remember { mutableStateOf<String?>(null) }
    var renameStopwatchLabel by remember { mutableStateOf("") }
    var alertsExpanded by remember { mutableStateOf(true) }
    var lapsExpanded by remember { mutableStateOf(true) }
    val currentLapMs = state.laps.firstOrNull()?.let { state.elapsedMs - it.totalTimeMs }
        ?: state.elapsedMs

    state.alertEvent?.let { event ->
        AlertDialog(
            onDismissRequest = {
                NotificationHelper.cancelStopwatchMarkNotification(context)
                selectedViewModel.acknowledgeAlert()
            },
            title = { Text("Stopwatch alert") },
            text = { Text("Reached ${event.label}") },
            confirmButton = {
                TextButton(onClick = {
                    NotificationHelper.cancelStopwatchMarkNotification(context)
                    selectedViewModel.acknowledgeAlert()
                }) {
                    Text("OK")
                }
            }
        )
    }

    if (showCustomDialog) {
        CustomMarkDialog(
            onDismiss = { showCustomDialog = false },
            onAdd = { hours, minutes, seconds ->
                selectedViewModel.addCustomMark(hours, minutes, seconds)
                showCustomDialog = false
            }
        )
    }

    if (showPresetEditor) {
        SuggestedAlertPresetsDialog(
            presets = markPresets,
            onDismiss = { showPresetEditor = false },
            onSave = { selectedViewModel.saveSuggestedAlertSeconds(it) },
            onReset = { selectedViewModel.resetSuggestedAlertSeconds() }
        )
    }

    renameStopwatchId?.let { id ->
        AlertDialog(
            onDismissRequest = { renameStopwatchId = null },
            title = { Text("Rename stopwatch") },
            text = {
                OutlinedTextField(
                    value = renameStopwatchLabel,
                    onValueChange = { renameStopwatchLabel = it },
                    singleLine = true,
                    label = { Text("Name") }
                )
            },
            confirmButton = {
                TextButton(
                    enabled = renameStopwatchLabel.trim().isNotEmpty(),
                    onClick = {
                        val label = renameStopwatchLabel.trim()
                        stopwatchTabs = stopwatchTabs.map { tab ->
                            if (tab.id == id) tab.copy(label = label) else tab
                        }
                        renameStopwatchId = null
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { renameStopwatchId = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    ca.sekhrit.alarmpro.ui.components.AutoSizingTopAppBarTitle("Stopwatch")
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { innerPadding ->
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            stopwatchTabs.forEach { tab ->
                val id = tab.id
                FilterChip(
                    modifier = Modifier
                        .height(48.dp)
                        .width(IntrinsicSize.Max),
                    selected = id == selectedStopwatchId,
                    onClick = { selectedStopwatchId = id },
                    label = {
                        Row(
                            modifier = Modifier.combinedClickable(
                                onClick = { selectedStopwatchId = id },
                                onLongClick = {
                                    renameStopwatchId = id
                                    renameStopwatchLabel = tab.label
                                }
                            ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                tab.label,
                                maxLines = 1,
                                softWrap = false
                            )
                            if (stopwatchTabs.size > 1) {
                                IconButton(
                                    onClick = {
                                        // Removing a session is a reset: stop its ticker and dismiss
                                        // its own live notification before removing the tab.
                                        if (selectedStopwatchId == id) {
                                            selectedViewModel.reset()
                                        } else {
                                            StopwatchViewModel.instance(id)?.reset()
                                        }
                                        val remaining = stopwatchTabs.filter { it.id != id }
                                        stopwatchTabs = remaining
                                        if (selectedStopwatchId == id) {
                                            selectedStopwatchId = remaining.first().id
                                        }
                                    },
                                    modifier = Modifier.width(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Remove ${tab.label}"
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.width(24.dp))
                            }
                        }
                    }
                )
            }
            IconButton(
                onClick = {
                    val number = stopwatchTabs.size + 1
                    val id = "stopwatch-$number"
                    stopwatchTabs = stopwatchTabs + StopwatchTab(
                        id = id,
                        label = "Stopwatch $number"
                    )
                    selectedStopwatchId = id
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add stopwatch")
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = TimeUtils.formatStopwatch(state.elapsedMs),
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Current Lap: ${TimeUtils.formatStopwatch(currentLapMs)}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { selectedViewModel.startPause() },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(ElevatedSurface)
                    ) {
                        Icon(
                            if (state.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (state.isRunning) "Pause" else "Start"
                        )
                    }
                    Spacer(modifier = Modifier.padding(horizontal = 16.dp))
                    IconButton(
                        onClick = { selectedViewModel.reset() },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(ElevatedSurface)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset")
                    }
                }

                FilledTonalButton(
                    onClick = { selectedViewModel.lap() },
                    enabled = state.isRunning,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .height(40.dp)
                ) {
                    Text("LAP")
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (!lapsExpanded && !alertsExpanded) {
                Arrangement.SpaceBetween
            } else {
                Arrangement.spacedBy(16.dp)
            },
            verticalAlignment = Alignment.Top
        ) {
            if (lapsExpanded) {
                CollapsibleSection(
                    title = "Laps",
                    onToggle = { lapsExpanded = false },
                    collapseIcon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    modifier = Modifier.weight(1f)
                ) {
            if (state.laps.isEmpty()) {
                Text(
                    text = "Tap LAP while running to record a split.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Spacer(modifier = Modifier.weight(0.14f))
                    Text(
                        "Lap Time:",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.weight(0.54f)
                    )
                    Text(
                        "Total Time:",
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(0.32f)
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    state.laps.forEach { lap ->
                        LapRow(
                            lap = lap,
                            isBest = lap.lapTimeMs == bestLapMs
                        )
                    }
                }
                }
                }
            } else {
                CollapsedSectionToggle(
                    icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Expand laps",
                    onClick = { lapsExpanded = true }
                )
            }

            if (alertsExpanded) {
                CollapsibleSection(
                    title = "Time alerts",
                    onToggle = { alertsExpanded = false },
                    collapseIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    modifier = Modifier.weight(1f)
                ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Get alerted when the stopwatch reaches a target time",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { showPresetEditor = true }) {
                    Text("Edit")
                }
            }
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = false,
                    onClick = { showCustomDialog = true },
                    label = { Text("Custom") }
                )
                markPresets.forEach { seconds ->
                    AlertPresetChip(
                        label = TimeUtils.formatDuration(seconds.toLong()),
                        selected = state.marks.any { it.targetMs == seconds * 1000L },
                        onToggle = {
                            state.marks.find { it.targetMs == seconds * 1000L }?.let {
                                selectedViewModel.removeMark(it.id)
                            } ?: selectedViewModel.addMarkAtSeconds(seconds)
                        }
                    )
                }
            }

            state.marks.forEach { mark ->
                MarkRow(
                    mark = mark,
                    elapsedMs = state.elapsedMs,
                    onRemove = { selectedViewModel.removeMark(mark.id) }
                )
                }
                }
            } else {
                CollapsedSectionToggle(
                    icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Expand time alerts",
                    onClick = { alertsExpanded = true }
                )
            }
        }
    }
    }
}

@Composable
private fun CollapsibleSection(
    title: String,
    onToggle: () -> Unit,
    collapseIcon: ImageVector,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(vertical = 0.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onToggle) {
                Icon(
                    imageVector = collapseIcon,
                    contentDescription = "Collapse $title"
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            content()
        }
    }
}

@Composable
private fun CollapsedSectionToggle(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick) {
        Icon(imageVector = icon, contentDescription = contentDescription)
    }
}

@Composable
private fun CustomMarkDialog(
    onDismiss: () -> Unit,
    onAdd: (hours: Int, minutes: Int, seconds: Int) -> Unit
) {
    var hours by remember { mutableIntStateOf(0) }
    var minutes by remember { mutableIntStateOf(0) }
    var seconds by remember { mutableIntStateOf(0) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF293743),
            tonalElevation = 8.dp
        ) {
            Column {
                Text(
                    text = "Alert Time:",
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
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.width(28.dp).padding(top = 93.dp)
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
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.width(28.dp).padding(top = 93.dp)
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
                        .padding(start = 8.dp, end = 8.dp, top = 5.dp, bottom = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    val buttonColors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                    TextButton(onClick = onDismiss, colors = buttonColors) { Text("CANCEL") }
                    TextButton(
                        onClick = { onAdd(hours, minutes, seconds) },
                        enabled = hours > 0 || minutes > 0 || seconds > 0,
                        colors = buttonColors
                    ) {
                        Text("ADD")
                    }
                }
            }
        }
    }
}

@Composable
private fun AlertPresetChip(
    label: String,
    selected: Boolean,
    onToggle: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onToggle,
        label = { Text(label) }
    )
}

@Composable
private fun SuggestedAlertPresetsDialog(
    presets: List<Int>,
    onDismiss: () -> Unit,
    onSave: (List<Int>) -> Unit,
    onReset: () -> Unit
) {
    var hours by remember { mutableIntStateOf(0) }
    var minutes by remember { mutableIntStateOf(0) }
    var seconds by remember { mutableIntStateOf(0) }
    var editingPresetSeconds by remember { mutableStateOf<Int?>(null) }
    val pickerSeconds = hours * 3600 + minutes * 60 + seconds
    val canSave = pickerSeconds > 0 &&
        (editingPresetSeconds == pickerSeconds || pickerSeconds !in presets)

    fun clearPicker() {
        hours = 0
        minutes = 0
        seconds = 0
        editingPresetSeconds = null
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 680.dp),
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF293743),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Suggested time alerts",
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
                        modifier = Modifier.width(28.dp).padding(top = 93.dp)
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
                        modifier = Modifier.width(28.dp).padding(top = 93.dp)
                    )
                    DurationWheel(
                        label = "sec",
                        value = seconds,
                        maxValue = 59,
                        onValueChange = { seconds = it },
                        modifier = Modifier.weight(1f)
                    )
                }

                FilledTonalButton(
                    onClick = {
                        val updated = if (editingPresetSeconds == null) {
                            presets + pickerSeconds
                        } else {
                            presets.filterNot { it == editingPresetSeconds } + pickerSeconds
                        }
                        onSave(updated.distinct().sorted())
                        clearPicker()
                    },
                    enabled = canSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(if (editingPresetSeconds == null) "Add" else "Update")
                }

                Text(
                    text = "Current suggestions",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                presets.forEach { presetSeconds ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = CardSurface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = TimeUtils.formatDuration(presetSeconds.toLong()),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = {
                                hours = presetSeconds / 3600
                                minutes = (presetSeconds % 3600) / 60
                                seconds = presetSeconds % 60
                                editingPresetSeconds = presetSeconds
                            }) {
                                Text("Edit")
                            }
                            TextButton(onClick = {
                                onSave(presets.filterNot { it == presetSeconds })
                                if (editingPresetSeconds == presetSeconds) clearPicker()
                            }) {
                                Text("Delete")
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = {
                        onReset()
                        clearPicker()
                    }) {
                        Text("Reset to default")
                    }
                    TextButton(onClick = onDismiss) {
                        Text("Done")
                    }
                }
            }
        }
    }
}

@Composable
private fun MarkRow(
    mark: StopwatchMark,
    elapsedMs: Long,
    onRemove: () -> Unit
) {
    val reached = mark.triggered || elapsedMs >= mark.targetMs
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Alert at ${mark.label}",
                    textDecoration = if (mark.triggered) TextDecoration.LineThrough else null,
                    color = if (mark.triggered) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = when {
                        mark.triggered -> "Alerted"
                        elapsedMs >= mark.targetMs -> "Due now"
                        else -> "Pending"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (reached) MaterialTheme.colorScheme.secondary else ElectricCyan
                )
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Close, contentDescription = "Remove alert")
            }
        }
    }
}

@Composable
private fun LapRow(lap: LapEntry, isBest: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${lap.number}:",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(0.14f)
        )
        AutoSizingLapTime(
            text = TimeUtils.formatStopwatch(lap.lapTimeMs),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isBest) FontWeight.Bold else FontWeight.Normal,
            color = if (isBest) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.54f)
        )
        AutoSizingLapTime(
            text = TimeUtils.formatStopwatch(lap.totalTimeMs),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.32f)
        )
    }
}

@Composable
private fun AutoSizingLapTime(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier,
    textAlign: TextAlign = TextAlign.Start,
    fontWeight: FontWeight? = null
) {
    BoxWithConstraints(modifier = modifier) {
        var fontSize by remember(text, maxWidth) { mutableStateOf(style.fontSize) }
        Text(
            text = text,
            modifier = Modifier.fillMaxWidth(),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            textAlign = textAlign,
            style = style.copy(fontSize = fontSize, fontWeight = fontWeight),
            color = color,
            onTextLayout = { result ->
                if (result.hasVisualOverflow && fontSize > 9.sp) {
                    fontSize = (fontSize.value - 1f).coerceAtLeast(9f).sp
                }
            }
        )
    }
}
