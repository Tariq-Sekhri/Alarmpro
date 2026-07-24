package ca.sekhrit.alarmpro.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import ca.sekhrit.alarmpro.data.TimePickerStyle
import ca.sekhrit.alarmpro.data.TimerSpeechFormat
import ca.sekhrit.alarmpro.data.upcomingAlarmLeadLabel
import ca.sekhrit.alarmpro.ui.components.SettingsCategoryHeader
import ca.sekhrit.alarmpro.ui.components.SettingsOptionDialog
import ca.sekhrit.alarmpro.ui.components.SettingsSwitchRow
import ca.sekhrit.alarmpro.ui.components.SettingsValueRow
import ca.sekhrit.alarmpro.viewmodel.AlarmViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GeneralSettingsScreen(
    onBack: () -> Unit,
    viewModel: AlarmViewModel = viewModel()
) {
    val settings by viewModel.settings.collectAsState()
    var showSpeechDialog by remember { mutableStateOf(false) }
    var showUpcomingDialog by remember { mutableStateOf(false) }
    var showTimePickerStyleDialog by remember { mutableStateOf(false) }

    val timePickerOptions = remember { TimePickerStyle.entries.map { it.label } }
    val timePickerSelectedIndex = TimePickerStyle.entries.indexOf(settings.timePickerStyle)

    val speechOptions = remember { TimerSpeechFormat.entries.map { it.label } }
    val speechSelectedIndex = TimerSpeechFormat.entries.indexOf(settings.timerSpeechFormat)

    val upcomingLeadOptions = remember { listOf(0, 15, 30, 60) }
    val upcomingOptionLabels = remember { upcomingLeadOptions.map { upcomingAlarmLeadLabel(it) } }
    val upcomingSelectedIndex = upcomingLeadOptions.indexOf(settings.upcomingAlarmLeadMinutes)
        .takeIf { it >= 0 } ?: 0

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

    if (showUpcomingDialog) {
        SettingsOptionDialog(
            title = "Upcoming alarm notification",
            options = upcomingOptionLabels,
            selectedIndex = upcomingSelectedIndex,
            onDismiss = { showUpcomingDialog = false },
            onSelect = { index ->
                viewModel.updateSettings(
                    settings.copy(upcomingAlarmLeadMinutes = upcomingLeadOptions[index])
                )
                showUpcomingDialog = false
            }
        )
    }

    if (showTimePickerStyleDialog) {
        SettingsOptionDialog(
            title = "Time picker style",
            options = timePickerOptions,
            selectedIndex = timePickerSelectedIndex,
            onDismiss = { showTimePickerStyleDialog = false },
            onSelect = { index ->
                viewModel.updateSettings(
                    settings.copy(timePickerStyle = TimePickerStyle.entries[index])
                )
                showTimePickerStyleDialog = false
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("General Settings") },
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
            SettingsCategoryHeader("Display")
            SettingsSwitchRow(
                title = "24-hour format",
                subtitle = "Show times in 24-hour format",
                checked = settings.use24HourFormat,
                onCheckedChange = {
                    viewModel.updateSettings(settings.copy(use24HourFormat = it))
                }
            )
            SettingsValueRow(
                title = "Time picker style",
                value = settings.timePickerStyle.label,
                onClick = { showTimePickerStyleDialog = true }
            )

            SettingsCategoryHeader("Speech Settings")
            SettingsValueRow(
                title = "Timer speech format",
                value = settings.timerSpeechFormat.label,
                onClick = { showSpeechDialog = true }
            )

            SettingsCategoryHeader("Misc Settings")
            SettingsValueRow(
                title = "Upcoming alarm notification",
                value = upcomingAlarmLeadLabel(settings.upcomingAlarmLeadMinutes),
                onClick = { showUpcomingDialog = true }
            )
        }
    }
}
