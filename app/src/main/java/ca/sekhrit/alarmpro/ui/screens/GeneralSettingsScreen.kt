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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import kotlinx.coroutines.launch
import ca.sekhrit.alarmpro.util.BackupRestore
import ca.sekhrit.alarmpro.viewmodel.TimerViewModel
import ca.sekhrit.alarmpro.data.AlarmSortMode
import ca.sekhrit.alarmpro.data.TimePickerStyle
import ca.sekhrit.alarmpro.data.TimerSpeechFormat
import ca.sekhrit.alarmpro.data.SpeechRate
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
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val settings by viewModel.settings.collectAsState()
    var showSpeechDialog by remember { mutableStateOf(false) }
    var showSpeechRateDialog by remember { mutableStateOf(false) }
    var showUpcomingDialog by remember { mutableStateOf(false) }
    var showTimePickerStyleDialog by remember { mutableStateOf(false) }
    var showAlarmSortDialog by remember { mutableStateOf(false) }

    val timePickerOptions = remember { TimePickerStyle.entries.map { it.label } }
    val timePickerSelectedIndex = TimePickerStyle.entries.indexOf(settings.timePickerStyle)

    val speechOptions = remember { TimerSpeechFormat.entries.map { it.label } }
    val speechSelectedIndex = TimerSpeechFormat.entries.indexOf(settings.timerSpeechFormat)

    val speechRateOptions = remember { SpeechRate.entries.map { it.label } }
    val speechRateSelectedIndex = SpeechRate.entries.indexOf(settings.speechRate)

    val upcomingLeadOptions = remember { listOf(0, 15, 30, 60) }
    val upcomingOptionLabels = remember { upcomingLeadOptions.map { upcomingAlarmLeadLabel(it) } }
    val upcomingSelectedIndex = upcomingLeadOptions.indexOf(settings.upcomingAlarmLeadMinutes)
        .takeIf { it >= 0 } ?: 0

    val alarmSortOptions = remember { AlarmSortMode.entries.map { it.label } }
    val alarmSortSelectedIndex = AlarmSortMode.entries.indexOf(settings.defaultAlarmSortMode)

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val success = BackupRestore.exportData(context, uri)
                if (success) {
                    Toast.makeText(context, "Export successful", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Export failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val success = BackupRestore.importData(context, uri)
                if (success) {
                    viewModel.refreshFromStorage()
                    TimerViewModel.instance.get()?.get()?.syncFromStorage()
                    Toast.makeText(context, "Import successful", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Import failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

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

    if (showSpeechRateDialog) {
        SettingsOptionDialog(
            title = "Speech rate",
            options = speechRateOptions,
            selectedIndex = speechRateSelectedIndex,
            onDismiss = { showSpeechRateDialog = false },
            onSelect = { index ->
                viewModel.updateSettings(
                    settings.copy(speechRate = SpeechRate.entries[index])
                )
                showSpeechRateDialog = false
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

    if (showAlarmSortDialog) {
        SettingsOptionDialog(
            title = "Default alarm sorting",
            options = alarmSortOptions,
            selectedIndex = alarmSortSelectedIndex,
            onDismiss = { showAlarmSortDialog = false },
            onSelect = { index ->
                viewModel.updateSettings(
                    settings.copy(defaultAlarmSortMode = AlarmSortMode.entries[index])
                )
                showAlarmSortDialog = false
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    ca.sekhrit.alarmpro.ui.components.AutoSizingTopAppBarTitle("General Settings")
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
            SettingsCategoryHeader("Display")
            SettingsSwitchRow(
                title = "Military time",
                subtitle = "Use 24-hour time, for example 18:30",
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
            SettingsValueRow(
                title = "Speech rate",
                value = settings.speechRate.label,
                onClick = { showSpeechRateDialog = true }
            )

            SettingsCategoryHeader("Misc Settings")
            SettingsSwitchRow(
                title = "Silent notifications",
                subtitle = "Send notifications silently (without sound/vibration)",
                checked = settings.silentNotifications,
                onCheckedChange = {
                    viewModel.updateSettings(settings.copy(silentNotifications = it))
                }
            )
            SettingsValueRow(
                title = "Default alarm sorting",
                value = settings.defaultAlarmSortMode.label,
                onClick = { showAlarmSortDialog = true }
            )
            SettingsValueRow(
                title = "Upcoming alarm notification",
                value = upcomingAlarmLeadLabel(settings.upcomingAlarmLeadMinutes),
                onClick = { showUpcomingDialog = true }
            )

            SettingsCategoryHeader("Backup & Restore")
            SettingsValueRow(
                title = "Export Data",
                value = "",
                onClick = { exportLauncher.launch("alarmpro_backup.json") }
            )
            SettingsValueRow(
                title = "Import Data",
                value = "",
                onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) }
            )
        }
    }
}
