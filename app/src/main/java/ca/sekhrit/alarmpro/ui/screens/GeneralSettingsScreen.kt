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
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import ca.sekhrit.alarmpro.ui.components.SettingsCategoryHeader
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
            SettingsCategoryHeader("Look and Feel")
            SettingsValueRow(
                title = "Theme",
                value = "Dark"
            )
            SettingsValueRow(
                title = "Clock widget font-size",
                value = "Medium"
            )
            SettingsValueRow(
                title = "Next alarm widget font-size",
                value = "Medium"
            )

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
                title = "Time format",
                value = if (settings.use24HourFormat) "24-hour" else "12-hour"
            )
            SettingsValueRow(
                title = "Time picker style",
                value = "Analog clock"
            )

            SettingsCategoryHeader("Speech Settings")
            SettingsValueRow(
                title = "Timer speech format",
                value = "Time and label"
            )
            SettingsValueRow(
                title = "Speech rate",
                value = "Normal"
            )

            SettingsCategoryHeader("Misc Settings")
            SettingsValueRow(
                title = "Upcoming alarm notification",
                value = "Show 1 hour before alarm"
            )
            SettingsSwitchRow(
                title = "Alarm reminder notification",
                checked = false,
                onCheckedChange = {}
            )
        }
    }
}
