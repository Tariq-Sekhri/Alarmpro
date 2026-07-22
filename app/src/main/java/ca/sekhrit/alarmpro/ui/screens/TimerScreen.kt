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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TimerOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ca.sekhrit.alarmpro.data.TimerPreset
import ca.sekhrit.alarmpro.ui.theme.CardSurface
import ca.sekhrit.alarmpro.ui.theme.ElectricCyan
import ca.sekhrit.alarmpro.ui.theme.ElevatedSurface
import ca.sekhrit.alarmpro.util.TimeUtils
import ca.sekhrit.alarmpro.viewmodel.TimerViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(
    onOpenSettings: () -> Unit,
    viewModel: TimerViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val presets by viewModel.presets.collectAsState()
    val activePresetId by viewModel.activePresetId.collectAsState()
    val finished by viewModel.finished.collectAsState()
    val finishedLabel by viewModel.finishedLabel.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.syncFromStorage()
    }

    LaunchedEffect(state.isRunning) {
        while (state.isRunning) {
            delay(250)
            viewModel.tick()
        }
    }

    if (finished) {
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
        AddTimerPresetDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { seconds ->
                viewModel.addPreset(seconds)
                showAddDialog = false
            }
        )
    }

    val nextHeader = viewModel.nextTimerHeader
    val isActive = state.totalSeconds > 0 && state.remainingSeconds > 0

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
                        color = if (isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(presets, key = { it.id }) { preset ->
                val isRunningPreset = activePresetId == preset.id && state.totalSeconds > 0
                TimerPresetCard(
                    preset = preset,
                    isRunning = isRunningPreset,
                    progress = if (isRunningPreset && state.totalSeconds > 0) {
                        1f - (state.remainingSeconds.toFloat() / state.totalSeconds.toFloat())
                    } else {
                        0f
                    },
                    onRestart = { viewModel.restartPreset(preset) },
                    onToggle = { enabled -> viewModel.togglePreset(preset, enabled) },
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
    progress: Float,
    onRestart: () -> Unit,
    onToggle: (Boolean) -> Unit,
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

            Text(
                text = TimeUtils.formatDuration(preset.totalSeconds.toLong()),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

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
private fun AddTimerPresetDialog(
    onDismiss: () -> Unit,
    onAdd: (Int) -> Unit
) {
    var hours by remember { mutableIntStateOf(0) }
    var minutes by remember { mutableIntStateOf(5) }
    var seconds by remember { mutableIntStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add timer") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Hours")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { hours = (hours + 23) % 24 }) { Text("-") }
                        Text("$hours")
                        OutlinedButton(onClick = { hours = (hours + 1) % 24 }) { Text("+") }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Minutes")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { minutes = (minutes + 59) % 60 }) { Text("-") }
                        Text("$minutes")
                        OutlinedButton(onClick = { minutes = (minutes + 1) % 60 }) { Text("+") }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Seconds")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { seconds = (seconds + 59) % 60 }) { Text("-") }
                        Text("$seconds")
                        OutlinedButton(onClick = { seconds = (seconds + 1) % 60 }) { Text("+") }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val total = hours * 3600 + minutes * 60 + seconds
                    if (total > 0) onAdd(total)
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
