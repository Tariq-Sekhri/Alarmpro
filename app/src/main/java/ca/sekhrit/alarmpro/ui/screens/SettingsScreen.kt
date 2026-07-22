package ca.sekhrit.alarmpro.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ca.sekhrit.alarmpro.ui.components.SettingsNavRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenGeneral: () -> Unit,
    onOpenDefaultAlarm: () -> Unit,
    onOpenTimer: () -> Unit,
    onOpenStopwatch: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
            SettingsNavRow(
                title = "General Settings",
                icon = Icons.Default.Settings,
                onClick = onOpenGeneral
            )
            SettingsNavRow(
                title = "Default Alarm Settings",
                icon = Icons.Default.Alarm,
                onClick = onOpenDefaultAlarm
            )
            SettingsNavRow(
                title = "Timer Settings",
                icon = Icons.Default.HourglassEmpty,
                onClick = onOpenTimer
            )
            SettingsNavRow(
                title = "Stopwatch Settings",
                icon = Icons.Default.Timer,
                onClick = onOpenStopwatch
            )
        }
    }
}
