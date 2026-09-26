package ca.sekhrit.alarmpro.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ca.sekhrit.alarmpro.data.Alarm
import ca.sekhrit.alarmpro.data.RepeatSchedule
import ca.sekhrit.alarmpro.data.RepeatType
import ca.sekhrit.alarmpro.data.TimePickerStyle
import ca.sekhrit.alarmpro.ui.components.AlarmSoundPickerRow
import ca.sekhrit.alarmpro.ui.components.DurationPickerDialog
import ca.sekhrit.alarmpro.ui.components.YearlyDatePickerField
import ca.sekhrit.alarmpro.ui.components.WheelTimePicker
import ca.sekhrit.alarmpro.ui.theme.CardSurface
import ca.sekhrit.alarmpro.ui.theme.ElectricCyan
import ca.sekhrit.alarmpro.ui.theme.WarmAmber
import ca.sekhrit.alarmpro.util.AlarmGrouping
import ca.sekhrit.alarmpro.util.AlarmSoundUtils
import ca.sekhrit.alarmpro.util.RepeatCalculator
import ca.sekhrit.alarmpro.util.TimeUtils
import ca.sekhrit.alarmpro.viewmodel.AlarmViewModel
import java.time.LocalDate
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AlarmEditScreen(
    alarmId: String?,
    onBack: () -> Unit,
    viewModel: AlarmViewModel = viewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val alarms by viewModel.alarms.collectAsState()
    val groups by viewModel.groups.collectAsState()
    val context = LocalContext.current
    val existing = alarmId?.let { id -> alarms.find { it.id == id } }
    val initialTime = existing?.time ?: LocalTime.now().plusMinutes(1).withSecond(0).withNano(0)

    val timePickerState = rememberTimePickerState(
        initialHour = initialTime.hour,
        initialMinute = initialTime.minute,
        is24Hour = settings.use24HourFormat
    )
    var selectedHour by remember(existing?.id) { mutableIntStateOf(initialTime.hour) }
    var selectedMinute by remember(existing?.id) { mutableIntStateOf(initialTime.minute) }
    var label by remember(existing?.id) { mutableStateOf(existing?.label.orEmpty()) }
    var isActive by remember(existing?.id) { mutableStateOf(true) }
    var repeatType by remember(existing?.id) { mutableStateOf(existing?.repeat?.type ?: RepeatType.ONCE) }
    var selectedDays by remember(existing?.id) { mutableStateOf(existing?.repeat?.daysOfWeek ?: emptySet()) }
    var weekInterval by remember(existing?.id) { mutableIntStateOf(existing?.repeat?.weekInterval ?: 2) }
    var monthInterval by remember(existing?.id) { mutableIntStateOf(existing?.repeat?.monthInterval ?: 1) }
    var dayOfMonth by remember(existing?.id) { mutableIntStateOf(existing?.repeat?.dayOfMonth ?: LocalDate.now().dayOfMonth) }
    var repeatAnchorDate by remember(existing?.id) {
        mutableStateOf(
            LocalDate.ofEpochDay(
                existing?.repeat?.anchorEpochDay ?: existing?.createdEpochDay ?: LocalDate.now().toEpochDay()
            )
        )
    }
    var vibrate by remember(existing?.id) { mutableStateOf(existing?.vibrate ?: settings.defaultVibrate) }
    var readLabelAloud by remember(existing?.id) { mutableStateOf(existing?.readLabelAloud ?: settings.defaultReadLabelAloud) }
    var snoozeEnabled by remember(existing?.id) { mutableStateOf(existing?.snoozeEnabled ?: settings.defaultSnoozeEnabled) }
    var useDefaultSnoozeLength by remember(existing?.id) { mutableStateOf(existing?.snoozeMinutes == null) }
    var customSnoozeMinutes by remember(existing?.id) { mutableIntStateOf(existing?.snoozeMinutes ?: settings.defaultSnoozeMinutes) }
    var deleteAfterDismiss by remember(existing?.id) { mutableStateOf(existing?.deleteAfterDismiss ?: settings.defaultDeleteOneTimeAlarmsAfterDismiss) }
    var wakeCheckEnabled by remember(existing?.id) { mutableStateOf(existing?.wakeCheckEnabled ?: settings.defaultWakeCheckEnabled) }
    var wakeCheckDelayMinutes by remember(existing?.id) { mutableIntStateOf(existing?.wakeCheckDelayMinutes ?: settings.defaultWakeCheckDelayMinutes) }
    var wakeCheckResponseMinutes by remember(existing?.id) { mutableIntStateOf(existing?.wakeCheckResponseMinutes ?: settings.defaultWakeCheckResponseMinutes) }
    var selectedGroupId by remember(existing?.id) { mutableStateOf(existing?.groupId) }
    var createNewGroup by remember(existing?.id) { mutableStateOf(false) }
    var newGroupName by remember(existing?.id) { mutableStateOf("") }
    var customSoundUri by remember(existing?.id) { mutableStateOf(existing?.soundUri) }
    var showCustomSnoozeDialog by remember { mutableStateOf(false) }
    var showWakeCheckDelayDialog by remember { mutableStateOf(false) }
    var showWakeCheckResponseDialog by remember { mutableStateOf(false) }

    val dayLetters = listOf("S", "M", "T", "W", "T", "F", "S")
    val dayValues = listOf(7, 1, 2, 3, 4, 5, 6)
    val repeatTypes = listOf(
        RepeatType.ONCE to "One-time",
        RepeatType.DAILY to "Daily",
        RepeatType.WEEKLY to "Weekly",
        RepeatType.INTERVAL_WEEKS to "Every N weeks",
        RepeatType.MONTHLY to "Monthly",
        RepeatType.YEARLY to "Yearly"
    )

    fun selectRepeatType(type: RepeatType) {
        repeatType = type
        if ((type == RepeatType.WEEKLY || type == RepeatType.INTERVAL_WEEKS) && selectedDays.isEmpty()) {
            selectedDays = setOf(LocalDate.now().dayOfWeek.value)
        }
    }

    val previewSchedule = RepeatSchedule(
        type = repeatType,
        daysOfWeek = selectedDays,
        weekInterval = weekInterval,
        monthInterval = monthInterval,
        dayOfMonth = dayOfMonth,
        anchorEpochDay = repeatAnchorDate.toEpochDay()
    )
    val selectedTime = if (settings.timePickerStyle == TimePickerStyle.ANALOG) {
        LocalTime.of(timePickerState.hour, timePickerState.minute)
    } else {
        LocalTime.of(selectedHour, selectedMinute)
    }
    val previewAlarm = Alarm(time = selectedTime, repeat = previewSchedule, isEnabled = true)
    val countdownLine = TimeUtils.nextAlarmHeader(listOf(previewAlarm), settings.use24HourFormat)
        ?.countdownLine ?: "(less than a minute from now)"
    val timeUntilText = "in " + countdownLine
        .removePrefix("(")
        .removeSuffix(")")
        .removeSuffix(" from now")

    val defaultLabelPreview = remember(selectedGroupId, createNewGroup, newGroupName, groups, alarms, existing?.id) {
        val groupLabel = when {
            createNewGroup && newGroupName.isNotBlank() -> newGroupName.trim()
            selectedGroupId != null -> groups.find { it.id == selectedGroupId }?.label
            else -> null
        } ?: return@remember null
        val members = selectedGroupId?.let { AlarmGrouping.membersOf(it, alarms) }.orEmpty()
        val index = if (existing != null && existing.groupId == selectedGroupId) {
            AlarmGrouping.indexInGroup(existing, members) ?: members.size
        } else {
            members.size + 1
        }
        "$groupLabel $index"
    }

    fun saveAlarm() {
        val snoozeMinutes = if (!snoozeEnabled || useDefaultSnoozeLength) null else customSnoozeMinutes
        val resolvedGroupId = when {
            createNewGroup && newGroupName.isNotBlank() -> viewModel.createGroup(newGroupName).id
            createNewGroup -> null
            else -> selectedGroupId
        }
        val resolvedSoundUri = customSoundUri
        if (existing == null) {
            viewModel.addAlarm(
                selectedTime,
                label,
                previewSchedule,
                vibrate,
                readLabelAloud,
                snoozeEnabled,
                snoozeMinutes,
                deleteAfterDismiss = repeatType == RepeatType.ONCE && deleteAfterDismiss,
                wakeCheckEnabled = wakeCheckEnabled,
                wakeCheckDelayMinutes = wakeCheckDelayMinutes,
                wakeCheckResponseMinutes = wakeCheckResponseMinutes,
                isEnabled = isActive,
                groupId = resolvedGroupId,
                soundUri = resolvedSoundUri
            )
        } else {
            viewModel.updateAlarm(
                existing.copy(
                    time = selectedTime,
                    label = label.trim(),
                    isEnabled = isActive,
                    repeat = previewSchedule,
                    vibrate = vibrate,
                    readLabelAloud = readLabelAloud,
                    snoozeEnabled = snoozeEnabled,
                    snoozeMinutes = snoozeMinutes,
                    deleteAfterDismiss = repeatType == RepeatType.ONCE && deleteAfterDismiss,
                    wakeCheckEnabled = wakeCheckEnabled,
                    wakeCheckDelayMinutes = wakeCheckDelayMinutes,
                    wakeCheckResponseMinutes = wakeCheckResponseMinutes,
                    groupId = resolvedGroupId,
                    soundUri = resolvedSoundUri,
                    snoozedUntilEpochMillis = null
                )
            )
        }
        onBack()
    }

    if (showCustomSnoozeDialog) {
        DurationPickerDialog(
            title = "Snooze Duration:",
            initialTotalSeconds = customSnoozeMinutes * 60,
            showLabel = false,
            showSeconds = false,
            onDismiss = { showCustomSnoozeDialog = false },
            onResetToDefault = {
                useDefaultSnoozeLength = true
                showCustomSnoozeDialog = false
            },
            onConfirm = { totalSeconds, _ ->
                useDefaultSnoozeLength = false
                customSnoozeMinutes = TimeUtils.snoozeMinutesFromDurationSeconds(totalSeconds)
                showCustomSnoozeDialog = false
            }
        )
    }

    if (showWakeCheckDelayDialog) {
        DurationPickerDialog(
            title = "Check after",
            initialTotalSeconds = wakeCheckDelayMinutes * 60,
            showLabel = false,
            showSeconds = false,
            onDismiss = { showWakeCheckDelayDialog = false },
            onConfirm = { totalSeconds, _ ->
                wakeCheckDelayMinutes = TimeUtils.snoozeMinutesFromDurationSeconds(totalSeconds)
                showWakeCheckDelayDialog = false
            }
        )
    }
    if (showWakeCheckResponseDialog) {
        DurationPickerDialog(
            title = "Re-ring after no response",
            initialTotalSeconds = wakeCheckResponseMinutes * 60,
            showLabel = false,
            showSeconds = false,
            onDismiss = { showWakeCheckResponseDialog = false },
            onConfirm = { totalSeconds, _ ->
                wakeCheckResponseMinutes = TimeUtils.snoozeMinutesFromDurationSeconds(totalSeconds)
                showWakeCheckResponseDialog = false
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    ca.sekhrit.alarmpro.ui.components.AutoSizingTopAppBarTitle(
                        if (existing == null) "Create Alarm" else "Edit Alarm"
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(if (isActive) "On" else "Off", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = isActive,
                            onCheckedChange = { isActive = it }
                        )
                    }
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = { saveAlarm() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (existing == null) "Create alarm" else "Save changes")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (settings.timePickerStyle == TimePickerStyle.ANALOG) {
                    TimePicker(state = timePickerState)
                } else {
                    WheelTimePicker(
                        hour = selectedHour,
                        minute = selectedMinute,
                        is24Hour = settings.use24HourFormat,
                        onTimeChange = { h, m ->
                            selectedHour = h
                            selectedMinute = m
                        }
                    )
                }
                Text(
                    text = timeUntilText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(y = (-18).dp)
                        .padding(end = 20.dp)
                )
            }

            HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
            Text("Schedule", style = MaterialTheme.typography.titleSmall, color = WarmAmber)
            Text(
                "Choose how often this alarm should repeat.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeatTypes.forEach { (type, name) ->
                    FilterChip(
                        selected = repeatType == type,
                        onClick = { selectRepeatType(type) },
                        label = { Text(name) }
                    )
                }
            }
            Text(
                RepeatCalculator.summary(previewSchedule),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

                if (repeatType == RepeatType.WEEKLY || repeatType == RepeatType.INTERVAL_WEEKS) {
                    Text("Days", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        dayLetters.forEachIndexed { index, letter ->
                            val day = dayValues[index]
                            val selected = day in selectedDays
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (selected) ElectricCyan.copy(alpha = 0.25f) else CardSurface)
                                    .border(
                                        1.dp,
                                        if (selected) ElectricCyan else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                        CircleShape
                                    )
                                    .clickable {
                                        if (selected && selectedDays.size == 1) return@clickable
                                        selectedDays = if (selected) selectedDays - day else selectedDays + day
                                    }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(letter)
                            }
                        }
                    }
                }

                if (repeatType == RepeatType.INTERVAL_WEEKS) {
                    Text("Every", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(2, 3, 4, 6, 8).forEach { weeks ->
                            FilterChip(
                                selected = weekInterval == weeks,
                                onClick = { weekInterval = weeks },
                                label = { Text("$weeks weeks") }
                            )
                        }
                    }
                }

                if (repeatType == RepeatType.MONTHLY) {
                    Text("Day of month", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(onClick = { dayOfMonth = ((dayOfMonth - 2 + 31) % 31) + 1 }) { Text("-") }
                        Text("$dayOfMonth", style = MaterialTheme.typography.titleMedium)
                        OutlinedButton(onClick = { dayOfMonth = (dayOfMonth % 31) + 1 }) { Text("+") }
                    }
                    Text(
                        "If a month doesn't have this day, the alarm runs on that month's last day.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (repeatType == RepeatType.YEARLY) {
                    Text("Date", style = MaterialTheme.typography.labelLarge)
                    YearlyDatePickerField(
                        date = repeatAnchorDate,
                        onDateSelected = { repeatAnchorDate = it }
                    )
                }

                if (repeatType == RepeatType.ONCE) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Delete after dismissal")
                            Text(
                                "Remove this one-time alarm after it rings and is dismissed",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = deleteAfterDismiss, onCheckedChange = { deleteAfterDismiss = it })
                    }
                }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Label", style = MaterialTheme.typography.titleSmall, color = WarmAmber)
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(defaultLabelPreview ?: "Custom name (optional)")
                    },
                    singleLine = true
                )
                defaultLabelPreview?.let { preview ->
                    Text(
                        text = "Default name: $preview",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Speak time & label")
                        Text(
                            "Speaks when the alarm rings",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = readLabelAloud, onCheckedChange = { readLabelAloud = it })
                }

                HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
                Text("Sound", style = MaterialTheme.typography.titleSmall, color = WarmAmber)
                AlarmSoundPickerRow(
                    soundName = AlarmSoundUtils.getTitle(
                        context,
                        customSoundUri?.let { Uri.parse(it) }
                            ?: AlarmSoundUtils.resolvePickerUri(existing, settings)
                    ),
                    pickerUri = customSoundUri?.let { Uri.parse(it) }
                        ?: AlarmSoundUtils.resolvePickerUri(existing, settings),
                    onSoundPicked = { uri ->
                        customSoundUri = AlarmSoundUtils.uriToStorage(uri)
                    }
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Vibrate during alarm")
                    Switch(checked = vibrate, onCheckedChange = { vibrate = it })
                }

                HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
                Text("Group", style = MaterialTheme.typography.titleSmall, color = WarmAmber)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !createNewGroup && selectedGroupId == null,
                        onClick = {
                            createNewGroup = false
                            selectedGroupId = null
                        },
                        label = { Text("No group") }
                    )
                    groups.forEach { group ->
                        FilterChip(
                            selected = !createNewGroup && selectedGroupId == group.id,
                            onClick = {
                                createNewGroup = false
                                selectedGroupId = group.id
                            },
                            label = { Text(group.label) }
                        )
                    }
                    FilterChip(
                        selected = createNewGroup,
                        onClick = {
                            createNewGroup = true
                            selectedGroupId = null
                        },
                        label = { Text("New group") }
                    )
                }
                if (createNewGroup) {
                    OutlinedTextField(
                        value = newGroupName,
                        onValueChange = { newGroupName = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Group name, e.g. wake up") },
                        singleLine = true
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
                Text("Check I'm awake", style = MaterialTheme.typography.titleSmall, color = WarmAmber)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Ask me to confirm I'm awake")
                        Text(
                            "Shows silently after this alarm is dismissed",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = wakeCheckEnabled, onCheckedChange = { wakeCheckEnabled = it })
                }
                if (wakeCheckEnabled) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Check after ${TimeUtils.formatSnoozeDuration(wakeCheckDelayMinutes)}")
                        TextButton(onClick = { showWakeCheckDelayDialog = true }) { Text("Change") }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Re-ring after ${TimeUtils.formatSnoozeDuration(wakeCheckResponseMinutes)}")
                        TextButton(onClick = { showWakeCheckResponseDialog = true }) { Text("Change") }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
                Text("Snooze", style = MaterialTheme.typography.titleSmall, color = WarmAmber)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Allow snooze")
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Snooze duration: ${
                                    if (useDefaultSnoozeLength) {
                                        TimeUtils.formatSnoozeDuration(settings.defaultSnoozeMinutes)
                                    } else {
                                        TimeUtils.formatSnoozeDuration(customSnoozeMinutes)
                                    }
                                }",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (snoozeEnabled) {
                                TextButton(onClick = { showCustomSnoozeDialog = true }) {
                                    Text("Change")
                                }
                            }
                        }
                    }
                    Switch(checked = snoozeEnabled, onCheckedChange = { snoozeEnabled = it })
                }
            }
        }
    }
}
