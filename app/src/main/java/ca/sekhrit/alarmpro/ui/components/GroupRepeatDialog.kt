package ca.sekhrit.alarmpro.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ca.sekhrit.alarmpro.data.RepeatSchedule
import ca.sekhrit.alarmpro.data.RepeatType
import ca.sekhrit.alarmpro.util.RepeatCalculator
import java.time.LocalDate

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GroupRepeatDialog(
    groupLabel: String,
    schedules: List<RepeatSchedule>,
    onDismiss: () -> Unit,
    onSave: (RepeatSchedule) -> Unit
) {
    val initial = remember { schedules.firstOrNull() ?: RepeatSchedule(type = RepeatType.WEEKLY) }
    var type by remember { mutableStateOf(initial.type) }
    var days by remember {
        mutableStateOf(initial.daysOfWeek.ifEmpty { setOf(LocalDate.now().dayOfWeek.value) })
    }
    var weeks by remember { mutableStateOf(initial.weekInterval.toString()) }
    var months by remember { mutableStateOf(initial.monthInterval.toString()) }
    var monthDay by remember { mutableStateOf(initial.dayOfMonth.toString()) }
    var anchor by remember { mutableStateOf(LocalDate.ofEpochDay(initial.anchorEpochDay)) }
    val weekly = type == RepeatType.WEEKLY || type == RepeatType.INTERVAL_WEEKS
    val monthly = type == RepeatType.MONTHLY || type == RepeatType.INTERVAL_MONTHS
    val valid = (!weekly || days.isNotEmpty()) &&
        (type != RepeatType.INTERVAL_WEEKS || (weeks.toIntOrNull() ?: 0) in 1..104) &&
        (type != RepeatType.INTERVAL_MONTHS || (months.toIntOrNull() ?: 0) in 1..120) &&
        (!monthly || (monthDay.toIntOrNull() ?: 0) in 1..31)
    val schedule = RepeatSchedule(
        type = type,
        daysOfWeek = if (weekly) days else emptySet(),
        weekInterval = weeks.toIntOrNull() ?: 2,
        monthInterval = months.toIntOrNull() ?: 1,
        dayOfMonth = monthDay.toIntOrNull() ?: 1,
        anchorEpochDay = if (type == RepeatType.ONCE) LocalDate.now().toEpochDay() else anchor.toEpochDay()
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Repeating rules") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Apply to all ${schedules.size} alarms in \"$groupLabel\". Each alarm keeps its time and enabled state.")
                if (schedules.distinct().size > 1) {
                    Text("This group has different repeat rules. Applying replaces them all.")
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        RepeatType.ONCE to "One-time",
                        RepeatType.DAILY to "Daily",
                        RepeatType.WEEKLY to "Weekly",
                        RepeatType.INTERVAL_WEEKS to "Every N weeks",
                        RepeatType.MONTHLY to "Monthly",
                        RepeatType.INTERVAL_MONTHS to "Every N months",
                        RepeatType.YEARLY to "Yearly"
                    ).forEach { (option, label) ->
                        FilterChip(selected = type == option, onClick = { type = option }, label = { Text(label) })
                    }
                }
                if (weekly) {
                    Text("Days")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEachIndexed { index, label ->
                            val day = index + 1
                            FilterChip(
                                selected = day in days,
                                onClick = { days = if (day in days) days - day else days + day },
                                label = { Text(label) }
                            )
                        }
                    }
                    if (days.isEmpty()) Text("Select at least one day.", color = MaterialTheme.colorScheme.error)
                }
                if (type == RepeatType.INTERVAL_WEEKS) {
                    OutlinedTextField(
                        value = weeks, onValueChange = { weeks = it },
                        label = { Text("Every N weeks (1–104)") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = (weeks.toIntOrNull() ?: 0) !in 1..104
                    )
                    Text("Starting from")
                    YearlyDatePickerField(date = anchor, onDateSelected = { anchor = it })
                }
                if (type == RepeatType.INTERVAL_MONTHS) {
                    OutlinedTextField(
                        value = months, onValueChange = { months = it },
                        label = { Text("Every N months (1–120)") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = (months.toIntOrNull() ?: 0) !in 1..120
                    )
                    Text("Starting from")
                    YearlyDatePickerField(date = anchor, onDateSelected = { anchor = it })
                }
                if (monthly) {
                    OutlinedTextField(
                        value = monthDay, onValueChange = { monthDay = it },
                        label = { Text("Day of month (1–31)") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = (monthDay.toIntOrNull() ?: 0) !in 1..31
                    )
                    Text("Shorter months use their last day.")
                }
                if (type == RepeatType.YEARLY) {
                    Text("Date")
                    YearlyDatePickerField(date = anchor, onDateSelected = { anchor = it })
                }
                if (type == RepeatType.ONCE) Text("Each alarm runs once at its next upcoming time.")
                if (valid) Text(RepeatCalculator.summary(schedule), color = MaterialTheme.colorScheme.primary)
                Text("Pending skips and snoozes will be cleared. You can still edit each alarm separately.")
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(schedule) }, enabled = valid && schedules.isNotEmpty()) { Text("Apply to group") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
