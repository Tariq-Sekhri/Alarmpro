package ca.sekhrit.alarmpro.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ca.sekhrit.alarmpro.data.DEFAULT_TIMER_SPEECH_TEMPLATE
import ca.sekhrit.alarmpro.data.TimerSpeechFormat
import ca.sekhrit.alarmpro.data.formatTimerSpeechTemplate
import ca.sekhrit.alarmpro.ui.theme.WarmAmber
import ca.sekhrit.alarmpro.viewmodel.AlarmViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerSpeechFormatScreen(
    onBack: () -> Unit,
    viewModel: AlarmViewModel = viewModel()
) {
    val settings by viewModel.settings.collectAsState()
    var format by remember(settings.timerSpeechFormat) { mutableStateOf(settings.timerSpeechFormat) }
    var template by remember(settings.timerSpeechTemplate) { mutableStateOf(settings.timerSpeechTemplate) }
    val preview = formatTimerSpeechTemplate(template, label = "Pasta", totalSeconds = 300)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { ca.sekhrit.alarmpro.ui.components.AutoSizingTopAppBarTitle("Timer Speech Format") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(
                    onClick = onBack,
                    modifier = Modifier.weight(1f)
                ) { Text("Cancel") }
                Button(
                    onClick = {
                        viewModel.updateSettings(
                            settings.copy(
                                timerSpeechFormat = format,
                                timerSpeechTemplate = template.trim().ifBlank {
                                    DEFAULT_TIMER_SPEECH_TEMPLATE
                                }
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
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Choose what Alarmpro says when a timer finishes.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp)
            )
            TextButton(
                onClick = {
                    format = TimerSpeechFormat.TIME_AND_LABEL
                    template = DEFAULT_TIMER_SPEECH_TEMPLATE
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Restore defaults")
            }

            Text(
                text = "Speech format",
                style = MaterialTheme.typography.titleMedium,
                color = WarmAmber,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
            TimerSpeechFormat.entries.forEach { option ->
                SpeechFormatChoice(
                    option = option,
                    selected = format == option,
                    onClick = { format = option }
                )
            }

            Text(
                text = "Custom speech text",
                style = MaterialTheme.typography.titleMedium,
                color = WarmAmber,
                modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
            )
            Text(
                text = "Every \$ begins a special token. Everything else is spoken exactly as you type it.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TokenDescription(token = "\$t", description = "timer duration (example: 10 minutes)")
            TokenDescription(token = "\$l", description = "timer label")
            TokenDescription(token = "\$h", description = "total hours")
            TokenDescription(token = "\$m", description = "remaining minutes")
            TokenDescription(token = "\$s", description = "remaining seconds")
            OutlinedTextField(
                value = template,
                onValueChange = { template = it },
                enabled = format == TimerSpeechFormat.CUSTOM,
                label = { Text("Speech text") },
                placeholder = { Text("Timer finished: \$t. \$l.") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            )
            Text(
                text = "Preview: ${preview ?: "No speech"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        }
    }
}

@Composable
private fun SpeechFormatChoice(
    option: TimerSpeechFormat,
    selected: Boolean,
    onClick: () -> Unit
) {
    val description = when (option) {
        TimerSpeechFormat.OFF -> "Do not speak when a timer finishes"
        TimerSpeechFormat.TIME -> "Speak the timer duration"
        TimerSpeechFormat.LABEL -> "Speak only the timer label"
        TimerSpeechFormat.TIME_AND_LABEL -> "Speak the duration, then the label"
        TimerSpeechFormat.CUSTOM -> "Write exactly what should be spoken"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(option.label, style = MaterialTheme.typography.bodyLarge)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TokenDescription(token: String, description: String) {
    Row(modifier = Modifier.padding(top = 8.dp)) {
        Text(token, color = WarmAmber, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = "  $description",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
