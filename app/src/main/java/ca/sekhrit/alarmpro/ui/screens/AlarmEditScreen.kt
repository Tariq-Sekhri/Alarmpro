package ca.sekhrit.alarmpro.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ca.sekhrit.alarmpro.data.Alarm
import ca.sekhrit.alarmpro.data.RepeatSchedule
import ca.sekhrit.alarmpro.data.RepeatType
import ca.sekhrit.alarmpro.data.TimePickerStyle
import ca.sekhrit.alarmpro.ui.components.AlarmSoundPickerRow
import ca.sekhrit.alarmpro.ui.components.WheelTimePicker
import ca.sekhrit.alarmpro.ui.theme.CardSurface
import ca.sekhrit.alarmpro.ui.theme.ElectricCyan
import ca.sekhrit.alarmpro.ui.theme.TextSecondary
import ca.sekhrit.alarmpro.ui.theme.WarmAmber
import ca.sekhrit.alarmpro.util.AlarmGrouping
import ca.sekhrit.alarmpro.util.AlarmSoundUtils
import ca.sekhrit.alarmpro.util.RepeatCalculator
import ca.sekhrit.alarmpro.util.TimeUtils
import ca.sekhrit.alarmpro.viewmodel.AlarmViewModel
import java.time.LocalDate
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
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
    val existingRepeat = existing?.repeat
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
    var repeatType by remember(existing?.id) { mutableStateOf(initialRepeatType(existingRepeat)) }
    var isRepeating by remember(existing?.id) { mutableStateOf(initialIsRepeating(existingRepeat)) }
    var selectedDays by remember(existing?.id) { mutableStateOf(initialSelectedDays(existingRepeat)) }
    var weekIntervalText by remember(existing?.id) {
        mutableStateOf((existingRepeat?.weekInterval ?: 2).toString())
    }
    val weekInterval = weekIntervalText.toIntOrNull()?.coerceAtLeast(2) ?: 2
    var monthInterval by remember(existing?.id) { mutableIntStateOf(existingRepeat?.monthInterval ?: 1) }
    var dayOfMonth by remember(existing?.id) { mutableIntStateOf(existingRepeat?.dayOfMonth ?: LocalDate.now().dayOfMonth) }
    var repeatAnchorDate by remember(existing?.id) {
        mutableStateOf(LocalDate.ofEpochDay(initialRepeatAnchorEpochDay(existingRepeat, existing?.createdEpochDay)))
    }
    var oneTimeDate by remember(existing?.id) {
        mutableStateOf(LocalDate.ofEpochDay(initialOneTimeAnchorEpochDay(existingRepeat)))
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
    var groupMenuExpanded by remember(existing?.id) { mutableStateOf(false) }
    var repeatMenuExpanded by remember(existing?.id) { mutableStateOf(false) }
    var newGroupName by remember(existing?.id) { mutableStateOf("") }
    var customSoundUri by remember(existing?.id) { mutableStateOf(existing?.soundUri) }

    val dayLetters = listOf("S", "M", "T", "W", "T", "F", "S")
    val dayValues = listOf(7, 1, 2, 3, 4, 5, 6)
    fun selectRepeatType(type: RepeatType) {
        repeatType = type
        if ((type == RepeatType.WEEKLY || type == RepeatType.INTERVAL_WEEKS) && selectedDays.isEmpty()) {
            selectedDays = setOf(LocalDate.now().dayOfWeek.value)
        }
    }

    val previewSchedule = RepeatSchedule(
        type = if (isRepeating) repeatType else RepeatType.ONCE,
        daysOfWeek = selectedDays,
        weekInterval = weekInterval,
        monthInterval = monthInterval,
        dayOfMonth = dayOfMonth,
        anchorEpochDay = (if (isRepeating) repeatAnchorDate else oneTimeDate).toEpochDay()
    )
    val selectedTime = if (settings.timePickerStyle == TimePickerStyle.ANALOG) {
        LocalTime.of(timePickerState.hour, timePickerState.minute)
    } else {
        LocalTime.of(selectedHour, selectedMinute)
    }
    fun nextOneTimeDate(days: Set<Int>, now: java.time.LocalDateTime = java.time.LocalDateTime.now()): LocalDate {
        val today = now.toLocalDate()
        val nextOffset = (0..7).firstOrNull { offset ->
            val candidate = today.plusDays(offset.toLong())
            candidate.dayOfWeek.value in days &&
                (offset > 0 || selectedTime.isAfter(now.toLocalTime()))
        } ?: 0
        return today.plusDays(nextOffset.toLong())
    }
    val previewAlarm = Alarm(time = selectedTime, repeat = previewSchedule, isEnabled = true)
    val countdownLine = TimeUtils.nextAlarmHeader(listOf(previewAlarm), settings.use24HourFormat)
        ?.countdownLine ?: "(less than a minute from now)"
    val nextDate = RepeatCalculator.nextTriggerDate(previewAlarm, LocalDate.now(), LocalTime.now())
    val timeUntilText = if (nextDate.isAfter(LocalDate.now().plusDays(1))) {
        "Next · " + nextDate.format(java.time.format.DateTimeFormatter.ofPattern("EEE, MMM d"))
    } else {
        "in " + countdownLine
            .removePrefix("(")
            .removeSuffix(")")
            .removeSuffix(" from now")
    }

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
                deleteAfterDismiss = !isRepeating && deleteAfterDismiss,
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
                    deleteAfterDismiss = !isRepeating && deleteAfterDismiss,
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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    ca.sekhrit.alarmpro.ui.components.AutoSizingTopAppBarTitle(
                        if (existing == null) "New alarm" else "Edit alarm"
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
                    Switch(
                        checked = isActive,
                        onCheckedChange = { isActive = it },
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }
            )
        },
        bottomBar = {
            Button(
                onClick = { saveAlarm() },
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(if (existing == null) "Create alarm" else "Save changes")
            }
        }
    ) { innerPadding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .background(CardSurface)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (settings.timePickerStyle == TimePickerStyle.ANALOG) {
                    TimeInput(state = timePickerState)
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
                    color = TextSecondary
                )
            }
            EditorSection("Schedule") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Repeating")
                RepeatTypeDropdown(
                    selected = if (isRepeating) repeatType else RepeatType.ONCE,
                    expanded = repeatMenuExpanded,
                    onExpandedChange = { repeatMenuExpanded = it },
                    onSelected = { type ->
                        if (type == RepeatType.ONCE) {
                            oneTimeDate = nextOneTimeDate(selectedDays)
                            isRepeating = false
                        } else {
                            selectRepeatType(type)
                            isRepeating = true
                        }
                        repeatMenuExpanded = false
                    },
                    modifier = Modifier.width(210.dp)
                )
            }
            if (isRepeating) {
                if (repeatType == RepeatType.INTERVAL_WEEKS) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Every", style = MaterialTheme.typography.labelLarge)
                        OutlinedTextField(
                            value = weekIntervalText,
                            onValueChange = { value ->
                                if (value.length <= 3 && value.all { it.isDigit() }) {
                                    weekIntervalText = value
                                }
                            },
                            modifier = Modifier.width(88.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        Text("weeks", style = MaterialTheme.typography.labelLarge)
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
                    InlineYearlyDateField(
                        date = repeatAnchorDate,
                        onDateSelected = { repeatAnchorDate = it }
                    )
                }
            }

            if (!isRepeating || repeatType == RepeatType.WEEKLY || repeatType == RepeatType.INTERVAL_WEEKS) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (isRepeating) "Days" else "Day", style = MaterialTheme.typography.labelLarge)
                    if (isRepeating) {
                        TextButton(onClick = { selectedDays = (1..7).toSet() }) {
                            Text("Every day")
                        }
                    }
                }
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
                                    if (isRepeating) {
                                        if (selected && selectedDays.size == 1) return@clickable
                                        selectedDays = if (selected) selectedDays - day else selectedDays + day
                                    } else {
                                        if (selected && selectedDays.size == 1) return@clickable
                                        selectedDays = if (selected) selectedDays - day else selectedDays + day
                                        oneTimeDate = nextOneTimeDate(selectedDays)
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(letter)
                        }
                    }
                }
            }

                if (!isRepeating) {
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
            }
            EditorSection("Details") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Name") },
                    placeholder = { Text(defaultLabelPreview ?: "Optional") },
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
                    },
                    buttonTextPrefix = "Sound ·",
                    inlineChoices = true
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Vibrate during alarm")
                    Switch(checked = vibrate, onCheckedChange = { vibrate = it })
                }

                val selectedGroupLabel = when {
                    createNewGroup -> "New group"
                    selectedGroupId == null -> "No group"
                    else -> groups.find { it.id == selectedGroupId }?.label ?: "No group"
                }
                ExposedDropdownMenuBox(
                    expanded = groupMenuExpanded,
                    onExpandedChange = { groupMenuExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedGroupLabel,
                        onValueChange = {},
                        readOnly = true,
                        singleLine = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = groupMenuExpanded)
                        },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = groupMenuExpanded,
                        onDismissRequest = { groupMenuExpanded = false },
                        modifier = Modifier.exposedDropdownSize(matchTextFieldWidth = true)
                    ) {
                        DropdownMenuItem(
                            text = { Text("No group") },
                            onClick = {
                                createNewGroup = false
                                selectedGroupId = null
                                groupMenuExpanded = false
                            }
                        )
                        groups.forEach { group ->
                            DropdownMenuItem(
                                text = { Text(group.label) },
                                onClick = {
                                    createNewGroup = false
                                    selectedGroupId = group.id
                                    groupMenuExpanded = false
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("New group") },
                            onClick = {
                                createNewGroup = true
                                selectedGroupId = null
                                groupMenuExpanded = false
                            }
                        )
                    }
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

            }
            }
            EditorSection("After ringing") {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Allow snooze")
                    }
                    Switch(checked = snoozeEnabled, onCheckedChange = { snoozeEnabled = it })
                }
                if (snoozeEnabled) {
                    MinuteControl(
                        label = "Snooze for",
                        value = if (useDefaultSnoozeLength) settings.defaultSnoozeMinutes else customSnoozeMinutes,
                        onValueChange = {
                            customSnoozeMinutes = it
                            useDefaultSnoozeLength = false
                        }
                    )
                    if (!useDefaultSnoozeLength) {
                        TextButton(onClick = { useDefaultSnoozeLength = true }) {
                            Text("Use default (${TimeUtils.formatSnoozeDuration(settings.defaultSnoozeMinutes)})")
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                        MinuteControl("Check after", wakeCheckDelayMinutes) { wakeCheckDelayMinutes = it }
                        MinuteControl("Re-ring after", wakeCheckResponseMinutes) { wakeCheckResponseMinutes = it }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorSection(title: String, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(CardSurface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = WarmAmber)
        content()
    }
}

@Composable
private fun MinuteControl(label: String, value: Int, onValueChange: (Int) -> Unit) {
    var text by remember { mutableStateOf(value.toString()) }
    androidx.compose.runtime.LaunchedEffect(value) {
        if (text.toIntOrNull() != value) text = value.toString()
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = { onValueChange((value - 1).coerceAtLeast(1)) }) { Text("−") }
        OutlinedTextField(
            value = text,
            onValueChange = { next ->
                if (next.length <= 4 && next.all(Char::isDigit)) {
                    text = next
                    next.toIntOrNull()?.let { onValueChange(it.coerceAtLeast(1)) }
                }
            },
            modifier = Modifier.width(72.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center)
        )
        TextButton(onClick = { onValueChange((value + 1).coerceAtMost(9999)) }) { Text("+") }
        Text("min", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InlineYearlyDateField(date: LocalDate, onDateSelected: (LocalDate) -> Unit) {
    var monthMenuExpanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ExposedDropdownMenuBox(
            expanded = monthMenuExpanded,
            onExpandedChange = { monthMenuExpanded = it },
            modifier = Modifier.weight(1f)
        ) {
            OutlinedTextField(
                value = date.month.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.getDefault()),
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = monthMenuExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = monthMenuExpanded, onDismissRequest = { monthMenuExpanded = false }) {
                java.time.Month.entries.forEach { month ->
                    DropdownMenuItem(
                        text = { Text(month.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.getDefault())) },
                        onClick = {
                            onDateSelected(date.withDayOfMonth(1).withMonth(month.value)
                                .withDayOfMonth(date.dayOfMonth.coerceAtMost(month.length(date.isLeapYear))))
                            monthMenuExpanded = false
                        }
                    )
                }
            }
        }
        TextButton(onClick = { onDateSelected(date.withDayOfMonth((date.dayOfMonth - 1).coerceAtLeast(1))) }) { Text("−") }
        Text("${date.dayOfMonth}", style = MaterialTheme.typography.titleMedium)
        TextButton(onClick = {
            onDateSelected(date.withDayOfMonth((date.dayOfMonth + 1).coerceAtMost(date.lengthOfMonth())))
        }) { Text("+") }
    }
}

private fun initialRepeatType(repeat: RepeatSchedule?): RepeatType {
    if (repeat == null) return RepeatType.WEEKLY
    return when (repeat.type) {
        RepeatType.DAILY, RepeatType.ONCE -> RepeatType.WEEKLY
        else -> repeat.type
    }
}

private fun initialIsRepeating(repeat: RepeatSchedule?): Boolean =
    repeat != null && repeat.type != RepeatType.ONCE

private fun initialSelectedDays(repeat: RepeatSchedule?): Set<Int> {
    if (repeat == null) return setOf(LocalDate.now().dayOfWeek.value)
    return when (repeat.type) {
        RepeatType.DAILY -> (1..7).toSet()
        RepeatType.ONCE -> repeat.daysOfWeek?.takeIf { it.isNotEmpty() }
            ?: setOf(LocalDate.ofEpochDay(repeat.anchorEpochDay ?: LocalDate.now().toEpochDay()).dayOfWeek.value)
        else -> repeat.daysOfWeek?.takeIf { it.isNotEmpty() }
            ?: setOf(LocalDate.now().dayOfWeek.value)
    }
}

private fun initialRepeatAnchorEpochDay(repeat: RepeatSchedule?, createdEpochDay: Long?): Long =
    repeat?.anchorEpochDay ?: createdEpochDay ?: LocalDate.now().toEpochDay()

private fun initialOneTimeAnchorEpochDay(repeat: RepeatSchedule?): Long =
    if (repeat != null && repeat.type == RepeatType.ONCE) {
        repeat.anchorEpochDay ?: LocalDate.now().toEpochDay()
    } else {
        LocalDate.now().toEpochDay()
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RepeatTypeDropdown(
    selected: RepeatType,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelected: (RepeatType) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf(
        RepeatType.ONCE to "One-time",
        RepeatType.WEEKLY to "Weekly",
        RepeatType.INTERVAL_WEEKS to "Every N weeks",
        RepeatType.MONTHLY to "Monthly",
        RepeatType.YEARLY to "Yearly"
    )
    val selectedLabel = when (selected) {
        RepeatType.ONCE -> "One-time"
        RepeatType.INTERVAL_WEEKS -> "Every N weeks"
        RepeatType.MONTHLY -> "Monthly"
        RepeatType.YEARLY -> "Yearly"
        else -> "Weekly"
    }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            modifier = Modifier.exposedDropdownSize(matchTextFieldWidth = true)
        ) {
            options.forEach { (type, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = { onSelected(type) }
                )
            }
        }
    }
}
