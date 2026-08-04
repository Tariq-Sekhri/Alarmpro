package ca.sekhrit.alarmpro.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ca.sekhrit.alarmpro.data.Alarm
import ca.sekhrit.alarmpro.data.RepeatSchedule
import ca.sekhrit.alarmpro.data.RepeatType
import ca.sekhrit.alarmpro.util.RepeatCalculator
import java.time.LocalDate
import java.time.LocalTime

import ca.sekhrit.alarmpro.data.TimePickerStyle
import ca.sekhrit.alarmpro.ui.components.DurationPickerDialog
import ca.sekhrit.alarmpro.ui.components.WheelTimePicker
import ca.sekhrit.alarmpro.util.TimeUtils

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AlarmEditDialog(
    alarm: Alarm?,
    timePickerStyle: TimePickerStyle = TimePickerStyle.ANALOG,
    defaultVibrate: Boolean = true,
    defaultSnoozeEnabled: Boolean = true,
    defaultSnoozeMinutes: Int = 10,
    defaultReadLabelAloud: Boolean = false,
    use24Hour: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (LocalTime, String, RepeatSchedule, Boolean, Boolean, Boolean, Int?) -> Unit
) {
    val initialTime = alarm?.time ?: LocalTime.now().plusMinutes(1)
    val timePickerState = rememberTimePickerState(
        initialHour = initialTime.hour,
        initialMinute = initialTime.minute,
        is24Hour = use24Hour
    )
    var selectedHour by remember(alarm?.id) { mutableIntStateOf(initialTime.hour) }
    var selectedMinute by remember(alarm?.id) { mutableIntStateOf(initialTime.minute) }
    var label by remember(alarm?.id) { mutableStateOf(alarm?.label.orEmpty()) }
    var repeatType by remember(alarm?.id) { mutableStateOf(alarm?.repeat?.type ?: RepeatType.ONCE) }
    var selectedDays by remember(alarm?.id) { mutableStateOf(alarm?.repeat?.daysOfWeek ?: emptySet()) }
    var weekInterval by remember(alarm?.id) { mutableIntStateOf(alarm?.repeat?.weekInterval ?: 2) }
    var monthInterval by remember(alarm?.id) { mutableIntStateOf(alarm?.repeat?.monthInterval ?: 1) }
    var dayOfMonth by remember(alarm?.id) { mutableIntStateOf(alarm?.repeat?.dayOfMonth ?: LocalDate.now().dayOfMonth) }
    var vibrate by remember(alarm?.id) { mutableStateOf(alarm?.vibrate ?: defaultVibrate) }
    var readLabelAloud by remember(alarm?.id) { mutableStateOf(alarm?.readLabelAloud ?: defaultReadLabelAloud) }
    var snoozeEnabled by remember(alarm?.id) { mutableStateOf(alarm?.snoozeEnabled ?: defaultSnoozeEnabled) }
    var useDefaultSnoozeLength by remember(alarm?.id) { mutableStateOf(alarm?.snoozeMinutes == null) }
    var customSnoozeMinutes by remember(alarm?.id) { mutableIntStateOf(alarm?.snoozeMinutes ?: defaultSnoozeMinutes) }
    val snoozeLengthOptions = listOf(5, 10, 15, 20, 30, 45, 60)
    var showCustomSnoozeDialog by remember { mutableStateOf(false) }
    val isCustomSnooze = !useDefaultSnoozeLength && customSnoozeMinutes !in snoozeLengthOptions

    val anchorEpochDay = alarm?.repeat?.anchorEpochDay ?: alarm?.createdEpochDay ?: LocalDate.now().toEpochDay()
    val dayOptions = listOf(1 to "Mon", 2 to "Tue", 3 to "Wed", 4 to "Thu", 5 to "Fri", 6 to "Sat", 7 to "Sun")
    val repeatTypes = listOf(
        RepeatType.ONCE to "Once",
        RepeatType.DAILY to "Daily",
        RepeatType.WEEKLY to "Weekly",
        RepeatType.INTERVAL_WEEKS to "Every N weeks",
        RepeatType.MONTHLY to "Monthly",
        RepeatType.INTERVAL_MONTHS to "Every N months",
        RepeatType.YEARLY to "Yearly"
    )

    val previewSchedule = RepeatSchedule(
        type = repeatType,
        daysOfWeek = selectedDays,
        weekInterval = weekInterval,
        monthInterval = monthInterval,
        dayOfMonth = dayOfMonth,
        anchorEpochDay = anchorEpochDay
    )

    if (showCustomSnoozeDialog) {
        DurationPickerDialog(
            title = "Snooze Duration:",
            initialTotalSeconds = customSnoozeMinutes * 60,
            showLabel = false,
            showSeconds = false,
            onDismiss = { showCustomSnoozeDialog = false },
            onConfirm = { totalSeconds, _ ->
                useDefaultSnoozeLength = false
                customSnoozeMinutes = TimeUtils.snoozeMinutesFromDurationSeconds(totalSeconds)
                showCustomSnoozeDialog = false
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (alarm == null) "New alarm" else "Edit alarm") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (timePickerStyle == TimePickerStyle.ANALOG) {
                    TimePicker(state = timePickerState)
                } else {
                    WheelTimePicker(
                        hour = selectedHour,
                        minute = selectedMinute,
                        is24Hour = use24Hour,
                        onTimeChange = { h, m ->
                            selectedHour = h
                            selectedMinute = m
                        }
                    )
                }
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Label") },
                    singleLine = true
                )

                Text("Repeat pattern", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeatTypes.forEach { (type, name) ->
                        FilterChip(
                            selected = repeatType == type,
                            onClick = { repeatType = type },
                            label = { Text(name) }
                        )
                    }
                }

                Text(
                    text = RepeatCalculator.summary(previewSchedule),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                if (repeatType == RepeatType.WEEKLY || repeatType == RepeatType.INTERVAL_WEEKS) {
                    Text("Days", style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        dayOptions.forEach { (day, name) ->
                            FilterChip(
                                selected = day in selectedDays,
                                onClick = {
                                    selectedDays = if (day in selectedDays) selectedDays - day else selectedDays + day
                                },
                                label = { Text(name) }
                            )
                        }
                    }
                }

                if (repeatType == RepeatType.INTERVAL_WEEKS) {
                    Text("Week interval", style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(2, 3, 4, 6, 8).forEach { weeks ->
                            FilterChip(
                                selected = weekInterval == weeks,
                                onClick = { weekInterval = weeks },
                                label = { Text("${weeks}w") }
                            )
                        }
                    }
                }

                if (repeatType == RepeatType.MONTHLY || repeatType == RepeatType.INTERVAL_MONTHS) {
                    Text("Day of month", style = MaterialTheme.typography.titleSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(onClick = { dayOfMonth = ((dayOfMonth - 2 + 31) % 31) + 1 }) {
                            Text("-")
                        }
                        Text("$dayOfMonth", style = MaterialTheme.typography.headlineMedium)
                        OutlinedButton(onClick = { dayOfMonth = (dayOfMonth % 31) + 1 }) {
                            Text("+")
                        }
                    }
                }

                if (repeatType == RepeatType.INTERVAL_MONTHS) {
                    Text("Month interval", style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1, 2, 3, 4, 6, 12).forEach { months ->
                            FilterChip(
                                selected = monthInterval == months,
                                onClick = { monthInterval = months },
                                label = {
                                    Text(if (months == 1) "1 mo" else "${months} mo")
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Vibrate")
                    Switch(checked = vibrate, onCheckedChange = { vibrate = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Read label aloud")
                        Text(
                            "Speaks the label when the alarm rings",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = readLabelAloud, onCheckedChange = { readLabelAloud = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Snooze")
                        Text(
                            "Allow snooze when this alarm rings",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = snoozeEnabled, onCheckedChange = { snoozeEnabled = it })
                }

                if (snoozeEnabled) {
                    Text("Snooze length", style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = useDefaultSnoozeLength,
                            onClick = { useDefaultSnoozeLength = true },
                            label = { Text("Default (${defaultSnoozeMinutes}m)") }
                        )
                        snoozeLengthOptions.forEach { minutes ->
                            FilterChip(
                                selected = !useDefaultSnoozeLength && !isCustomSnooze && customSnoozeMinutes == minutes,
                                onClick = {
                                    useDefaultSnoozeLength = false
                                    customSnoozeMinutes = minutes
                                },
                                label = { Text("${minutes}m") }
                            )
                        }
                        FilterChip(
                            selected = isCustomSnooze,
                            onClick = { showCustomSnoozeDialog = true },
                            label = {
                                Text(
                                    if (isCustomSnooze) {
                                        TimeUtils.formatSnoozeDuration(customSnoozeMinutes)
                                    } else {
                                        "Custom"
                                    }
                                )
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val snoozeMinutes = if (!snoozeEnabled || useDefaultSnoozeLength) null else customSnoozeMinutes
                val selectedTime = if (timePickerStyle == TimePickerStyle.ANALOG) {
                    LocalTime.of(timePickerState.hour, timePickerState.minute)
                } else {
                    LocalTime.of(selectedHour, selectedMinute)
                }
                onSave(
                    selectedTime,
                    label,
                    previewSchedule,
                    vibrate,
                    readLabelAloud,
                    snoozeEnabled,
                    snoozeMinutes
                )
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
