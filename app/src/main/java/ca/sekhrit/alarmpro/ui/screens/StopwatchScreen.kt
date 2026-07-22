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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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
            onAdd = { hours, minutes, seconds ->
                viewModel.addCustomMark(hours, minutes, seconds)
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
