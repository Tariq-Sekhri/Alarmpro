package ca.sekhrit.alarmpro.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import ca.sekhrit.alarmpro.ui.theme.CardSurface
import ca.sekhrit.alarmpro.ui.theme.ElevatedSurface
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ca.sekhrit.alarmpro.receiver.NotificationHelper
import ca.sekhrit.alarmpro.ui.theme.ElectricCyan
import ca.sekhrit.alarmpro.util.TimeUtils
import ca.sekhrit.alarmpro.viewmodel.LapEntry
import ca.sekhrit.alarmpro.viewmodel.StopwatchMark
import ca.sekhrit.alarmpro.viewmodel.StopwatchViewModel

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun StopwatchScreen(
    onOpenSettings: () -> Unit = {},
    viewModel: StopwatchViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val bestLapMs = state.laps.minOfOrNull { it.lapTimeMs }
    val context = LocalContext.current
    var showCustomDialog by remember { mutableStateOf(false) }
    var alertsExpanded by remember { mutableStateOf(true) }
    var lapsExpanded by remember { mutableStateOf(true) }
    var showMenu by remember { mutableStateOf(false) }
    val currentLapMs = state.laps.lastOrNull()?.let { state.elapsedMs - it.totalTimeMs }
        ?: state.elapsedMs
    val markPresets = listOf(
        5 to "5 min",
        15 to "15 min",
        30 to "30 min",
        60 to "1 hr",
        120 to "2 hr"
    )

    state.alertEvent?.let { event ->
        AlertDialog(
            onDismissRequest = {
                NotificationHelper.cancelStopwatchMarkNotification(context)
                viewModel.acknowledgeAlert()
            },
            title = { Text("Stopwatch alert") },
            text = { Text("Reached ${event.label}") },
            confirmButton = {
                TextButton(onClick = {
                    NotificationHelper.cancelStopwatchMarkNotification(context)
                    viewModel.acknowledgeAlert()
                }) {
                    Text("OK")
                }
            }
        )
    }

    if (showCustomDialog) {
        CustomMarkDialog(
            onDismiss = { showCustomDialog = false },
            onAdd = { hours, minutes ->
                viewModel.addCustomMark(hours, minutes)
                showCustomDialog = false
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Stopwatch") },
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
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = TimeUtils.formatStopwatch(state.elapsedMs),
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.startPause() },
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
                        onClick = { viewModel.reset() },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(ElevatedSurface)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset")
                    }
                }

                FilledTonalButton(
                    onClick = { viewModel.lap() },
                    enabled = state.isRunning,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Text("LAP")
                }
            }
        }

        Text(
            text = "Current Lap: ${TimeUtils.formatStopwatch(currentLapMs)}",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 24.dp, bottom = 12.dp)
        )

        CollapsibleSection(
            title = "Laps",
            summary = when {
                state.laps.isEmpty() -> "No laps yet"
                state.laps.size == 1 -> "1 lap"
                else -> "${state.laps.size} laps"
            },
            expanded = lapsExpanded,
            onToggle = { lapsExpanded = !lapsExpanded }
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
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Lap Time:", style = MaterialTheme.typography.labelLarge)
                    Text("Total Time:", style = MaterialTheme.typography.labelLarge)
                }
                state.laps.forEach { lap ->
                    LapRow(
                        lap = lap,
                        isBest = lap.lapTimeMs == bestLapMs
                    )
                }
            }
        }

        CollapsibleSection(
            title = "Time alerts",
            summary = when (state.marks.size) {
                0 -> "No alerts"
                1 -> "1 alert"
                else -> "${state.marks.size} alerts"
            },
            expanded = alertsExpanded,
            onToggle = { alertsExpanded = !alertsExpanded },
            modifier = Modifier.padding(top = 24.dp)
        ) {
            Text(
                "Get alerted when the stopwatch reaches a target time",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                markPresets.forEach { (minutes, label) ->
                    FilterChip(
                        selected = state.marks.any { it.targetMs == minutes * 60_000L },
                        onClick = {
                            if (state.marks.any { it.targetMs == minutes * 60_000L }) {
                                state.marks.find { it.targetMs == minutes * 60_000L }?.let {
                                    viewModel.removeMark(it.id)
                                }
                            } else {
                                viewModel.addMark(minutes)
                            }
                        },
                        label = { Text(label) }
                    )
                }
                FilterChip(
                    selected = false,
                    onClick = { showCustomDialog = true },
                    label = { Text("Custom") }
                )
            }

            state.marks.forEach { mark ->
                MarkRow(
                    mark = mark,
                    elapsedMs = state.elapsedMs,
                    onRemove = { viewModel.removeMark(mark.id) }
                )
            }
        }
    }
    }
}

@Composable
private fun CollapsibleSection(
    title: String,
    summary: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                if (!expanded) {
                    Text(
                        summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onToggle) {
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Minimize" else "Expand"
                )
            }
        }

        AnimatedVisibility(visible = expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun CustomMarkDialog(
    onDismiss: () -> Unit,
    onAdd: (hours: Int, minutes: Int) -> Unit
) {
    var hours by remember { mutableIntStateOf(0) }
    var minutes by remember { mutableIntStateOf(30) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Custom alert") },
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
                        Text("$hours", style = MaterialTheme.typography.titleLarge)
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
                        Text("$minutes", style = MaterialTheme.typography.titleLarge)
                        OutlinedButton(onClick = { minutes = (minutes + 1) % 60 }) { Text("+") }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(hours, minutes) },
                enabled = hours > 0 || minutes > 0
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
                .padding(horizontal = 12.dp, vertical = 8.dp),
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
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${lap.number}:",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(0.2f)
        )
        Text(
            text = TimeUtils.formatStopwatch(lap.lapTimeMs),
            fontWeight = if (isBest) FontWeight.Bold else FontWeight.Normal,
            color = if (isBest) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = TimeUtils.formatStopwatch(lap.totalTimeMs),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f)
        )
    }
}
