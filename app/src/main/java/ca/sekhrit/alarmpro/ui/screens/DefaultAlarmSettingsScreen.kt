package ca.sekhrit.alarmpro.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ca.sekhrit.alarmpro.ui.components.SettingsSwitchRow
import ca.sekhrit.alarmpro.viewmodel.AlarmViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DefaultAlarmSettingsScreen(
    onBack: () -> Unit,
    viewModel: AlarmViewModel = viewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val snoozeOptions = listOf(5, 10, 15, 20, 30, 45, 60)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Default Alarm Settings") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SettingsSwitchRow(
                title = "Snooze",
                subtitle = "Allow snooze on new alarms",
                checked = settings.defaultSnoozeEnabled,
                onCheckedChange = {
                    viewModel.updateSettings(settings.copy(defaultSnoozeEnabled = it))
                }
            )

            if (settings.defaultSnoozeEnabled) {
                Text("Default snooze length", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    snoozeOptions.forEach { minutes ->
                        FilterChip(
                            selected = settings.defaultSnoozeMinutes == minutes,
                            onClick = {
                                viewModel.updateSettings(settings.copy(defaultSnoozeMinutes = minutes))
                            },
                            label = { Text("${minutes}m") }
                        )
                    }
                }
            }

            SettingsSwitchRow(
                title = "Vibrate during alarm",
                subtitle = "Default for new alarms",
                checked = settings.defaultVibrate,
                onCheckedChange = {
                    viewModel.updateSettings(settings.copy(defaultVibrate = it))
                }
            )

            SettingsSwitchRow(
                title = "Speak time & label",
                subtitle = "Default for new alarms",
                checked = settings.defaultReadLabelAloud,
                onCheckedChange = {
                    viewModel.updateSettings(settings.copy(defaultReadLabelAloud = it))
                }
            )
        }
    }
}
