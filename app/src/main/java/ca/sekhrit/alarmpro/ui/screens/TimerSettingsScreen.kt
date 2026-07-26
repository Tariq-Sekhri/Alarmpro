package ca.sekhrit.alarmpro.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import ca.sekhrit.alarmpro.data.TimerSpeechFormat
import ca.sekhrit.alarmpro.data.TimerControlStyle
import ca.sekhrit.alarmpro.ui.components.SettingsCategoryHeader
import ca.sekhrit.alarmpro.ui.components.SettingsOptionDialog
import ca.sekhrit.alarmpro.ui.components.SettingsSwitchRow
import ca.sekhrit.alarmpro.ui.components.SettingsValueRow
import ca.sekhrit.alarmpro.viewmodel.AlarmViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerSettingsScreen(
    onBack: () -> Unit,
    viewModel: AlarmViewModel = viewModel()
) {
    val settings by viewModel.settings.collectAsState()
    var showSpeechDialog by remember { mutableStateOf(false) }
    var showControlDialog by remember { mutableStateOf(false) }
    val speechOptions = remember { TimerSpeechFormat.entries.map { it.label } }
    val speechSelectedIndex = TimerSpeechFormat.entries.indexOf(settings.timerSpeechFormat)
    val controlOptions = remember { TimerControlStyle.entries.map { it.label } }
    val controlSelectedIndex = TimerControlStyle.entries.indexOf(settings.timerControlStyle)

    if (showSpeechDialog) {
        SettingsOptionDialog(
            title = "Timer speech format",
            options = speechOptions,
            selectedIndex = speechSelectedIndex,
            onDismiss = { showSpeechDialog = false },
            onSelect = { index ->
                viewModel.updateSettings(
                    settings.copy(timerSpeechFormat = TimerSpeechFormat.entries[index])
                )
                showSpeechDialog = false
            }
        )
    }

    if (showControlDialog) {
        SettingsOptionDialog(
            title = "Timer control",
            options = controlOptions,
            selectedIndex = controlSelectedIndex,
            onDismiss = { showControlDialog = false },
            onSelect = { index ->
                viewModel.updateSettings(
                    settings.copy(timerControlStyle = TimerControlStyle.entries[index])
                )
                showControlDialog = false
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    ca.sekhrit.alarmpro.ui.components.AutoSizingTopAppBarTitle("Timer Settings")
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
        ) {
            SettingsCategoryHeader("Timer")
            SettingsValueRow(
                title = "Timer speech format",
                value = settings.timerSpeechFormat.label,
                onClick = { showSpeechDialog = true }
            )
            SettingsValueRow(
                title = "Timer control",
                value = settings.timerControlStyle.label,
                onClick = { showControlDialog = true }
            )
            SettingsSwitchRow(
                title = "Gradually increase volume",
                checked = false,
                onCheckedChange = {}
            )
            SettingsValueRow(
                title = "Timer finished notification",
                value = "Show notification"
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StopwatchSettingsScreen(onBack: () -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    ca.sekhrit.alarmpro.ui.components.AutoSizingTopAppBarTitle(
                        "Stopwatch Settings"
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
        ) {
            SettingsCategoryHeader("Stopwatch")
            SettingsSwitchRow(
                title = "Time alerts",
                subtitle = "Alert when stopwatch hits targets",
                checked = true,
                onCheckedChange = {}
            )
            SettingsValueRow(
                title = "Alert notification",
                value = "Show notification and dialog"
            )
        }
    }
}
