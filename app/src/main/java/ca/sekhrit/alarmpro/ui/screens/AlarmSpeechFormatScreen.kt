package ca.sekhrit.alarmpro.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ca.sekhrit.alarmpro.data.DEFAULT_ALARM_SPEECH_WITH_LABEL
import ca.sekhrit.alarmpro.data.DEFAULT_ALARM_SPEECH_WITHOUT_LABEL
import ca.sekhrit.alarmpro.ui.theme.WarmAmber
import ca.sekhrit.alarmpro.viewmodel.AlarmViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmSpeechFormatScreen(
    onBack: () -> Unit,
    viewModel: AlarmViewModel = viewModel()
) {
    val settings by viewModel.settings.collectAsState()
    var withLabel by remember(settings.alarmSpeechWithLabel) { mutableStateOf(settings.alarmSpeechWithLabel) }
    var withoutLabel by remember(settings.alarmSpeechWithoutLabel) { mutableStateOf(settings.alarmSpeechWithoutLabel) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { ca.sekhrit.alarmpro.ui.components.AutoSizingTopAppBarTitle("Alarm Speech Format") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Cancel") }
                Button(
                    onClick = {
                        viewModel.updateSettings(
                            settings.copy(
                                alarmSpeechWithLabel = withLabel.trim().ifBlank { DEFAULT_ALARM_SPEECH_WITH_LABEL },
                                alarmSpeechWithoutLabel = withoutLabel.trim()
                            )
                        )
                        onBack()
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Save") }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding)
                .verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
        ) {
            TextButton(
                onClick = {
                    withLabel = DEFAULT_ALARM_SPEECH_WITH_LABEL
                    withoutLabel = DEFAULT_ALARM_SPEECH_WITHOUT_LABEL
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Restore defaults") }
            Text(
                text = "1) Primary Speech Text",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp, bottom = 12.dp)
            )
            Text(
                text = "Use \$ placeholders where you want them spoken.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
            AlarmToken("\$t", "time")
            AlarmToken("\$l", "alarm label")
            AlarmToken("\$d", "day of week (example: Saturday)")
            AlarmToken("\$m", "month & day (example: August 15)")
            AlarmToken("\$w", "weather (example: 20°)")
            Text("a) When label present", modifier = Modifier.padding(top = 20.dp))
            OutlinedTextField(
                value = withLabel,
                onValueChange = { withLabel = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Speech text") }
            )
            Text("b) When no label", modifier = Modifier.padding(top = 16.dp))
            Text(
                "Leave blank to use the text above.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
            OutlinedTextField(
                value = withoutLabel,
                onValueChange = { withoutLabel = it },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                label = { Text("Speech text") }
            )
        }
    }
}

@Composable
private fun AlarmToken(token: String, description: String) {
    Row(modifier = Modifier.padding(top = 10.dp)) {
        Text(token, color = WarmAmber, style = MaterialTheme.typography.bodyLarge)
        Text("  $description", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
