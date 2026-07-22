package ca.sekhrit.alarmpro.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TimerOff
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import ca.sekhrit.alarmpro.data.TimerPreset
import ca.sekhrit.alarmpro.ui.theme.CardSurface
import ca.sekhrit.alarmpro.ui.theme.ElectricCyan
import ca.sekhrit.alarmpro.ui.theme.ElevatedSurface
import ca.sekhrit.alarmpro.util.TimeUtils
import ca.sekhrit.alarmpro.viewmodel.TimerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(
    onOpenSettings: () -> Unit,
    viewModel: TimerViewModel = viewModel()
) {
    val activeTimers by viewModel.activeTimers.collectAsState()
    val presets by viewModel.presets.collectAsState()
    val finishedLabels by viewModel.finishedLabels.collectAsState()
    val clockMillis by viewModel.clockMillis.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var editPreset by remember { mutableStateOf<TimerPreset?>(null) }

    LaunchedEffect(Unit) {
        viewModel.syncFromStorage()
    }

    finishedLabels.firstOrNull()?.let { finishedLabel ->
        AlertDialog(
            onDismissRequest = { viewModel.acknowledgeFinished() },
            title = { Text("Timer finished") },
            text = { Text(if (finishedLabel.isBlank()) "Time is up" else finishedLabel) },
            confirmButton = {
                TextButton(onClick = { viewModel.acknowledgeFinished() }) {
                    Text("OK")
                }
            }
        )
    }

    if (showAddDialog) {
        TimerPresetDialog(
            onDismiss = { showAddDialog = false },
            onSettings = {
                showAddDialog = false
                onOpenSettings()
            },
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
            onSettings = {
                editPreset = null
                onOpenSettings()
            },
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
                title = { Text("Countdown Timer") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Settings") },
                                onClick = {
                                    showMenu = false
                                    onOpenSettings()
                                },
                                leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = ElectricCyan,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.HourglassEmpty, contentDescription = "Add timer")
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
                    progress = if (isRunningPreset && timer != null && timer.totalSeconds > 0) {
                        1f - (remainingSeconds.toFloat() / timer.totalSeconds.toFloat())
                    } else {
                        0f
                    },
                    onRestart = { viewModel.restartPreset(preset) },
                    onToggle = { enabled -> viewModel.togglePreset(preset, enabled) },
                    onEdit = { editPreset = preset },
                    onDelete = { viewModel.deletePreset(preset) }
                )
            }
        }
    }
}

@Composable
private fun TimerPresetCard(
    preset: TimerPreset,
    isRunning: Boolean,
    displaySeconds: Int,
    progress: Float,
    onRestart: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    var showMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(shape)
            .background(if (isRunning) CardSurface else ElevatedSurface.copy(alpha = 0.75f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onRestart) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Restart timer",
                    tint = if (isRunning) ElectricCyan else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                if (preset.label.isNotBlank()) {
                    Text(
                        text = preset.label,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = TimeUtils.formatDuration(displaySeconds.toLong()),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = TimeUtils.formatDuration(displaySeconds.toLong()),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Switch(
                checked = isRunning,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = ElectricCyan.copy(alpha = 0.4f),
                    checkedThumbColor = ElectricCyan
                )
            )

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Timer options")
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = {
                            showMenu = false
                            onEdit()
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                    )
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
    initialTotalSeconds: Int = 5 * 60,
    initialLabel: String = "",
    onDismiss: () -> Unit,
    onSettings: () -> Unit,
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
            shape = RoundedCornerShape(10.dp),
            color = CardSurface,
            tonalElevation = 8.dp
        ) {
            Column {
                Text(
                    text = "Timer Duration:",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DurationStepper(
                        label = "hour",
                        value = hours,
                        onDecrement = { hours = (hours + 99) % 100 },
                        onIncrement = { hours = (hours + 1) % 100 }
                    )
                    Text(
                        text = ":",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    DurationStepper(
                        label = "min",
                        value = minutes,
                        onDecrement = { minutes = (minutes + 59) % 60 },
                        onIncrement = { minutes = (minutes + 1) % 60 }
                    )
                    Text(
                        text = ":",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    DurationStepper(
                        label = "sec",
                        value = seconds,
                        onDecrement = { seconds = (seconds + 59) % 60 },
                        onIncrement = { seconds = (seconds + 1) % 60 }
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp),
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
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    innerTextField()
                                }
                                HorizontalDivider(color = ElectricCyan, thickness = 2.dp)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onSettings) { Text("SETTINGS") }
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("CANCEL") }
                    TextButton(
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
private fun DurationStepper(
    label: String,
    value: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        IconButton(onClick = onIncrement) {
            Icon(Icons.Default.Add, contentDescription = "Increase $label")
        }
        Text(
            text = value.toString().padStart(2, '0'),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(56.dp)
        )
        IconButton(onClick = onDecrement) {
            Icon(Icons.Default.Remove, contentDescription = "Decrease $label")
        }
    }
}
