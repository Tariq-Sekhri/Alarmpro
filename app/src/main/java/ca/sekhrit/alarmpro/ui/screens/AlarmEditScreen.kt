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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Checkbox
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
    var isActive by remember(existing?.id) { mutableStateOf(existing?.isEnabled ?: true) }
    var repeatType by remember(existing?.id) { mutableStateOf(existing?.repeat?.type ?: RepeatType.ONCE) }
    var selectedDays by remember(existing?.id) { mutableStateOf(existing?.repeat?.daysOfWeek ?: emptySet()) }
    var weekInterval by remember(existing?.id) { mutableIntStateOf(existing?.repeat?.weekInterval ?: 2) }
    var monthInterval by remember(existing?.id) { mutableIntStateOf(existing?.repeat?.monthInterval ?: 1) }
    var dayOfMonth by remember(existing?.id) { mutableIntStateOf(existing?.repeat?.dayOfMonth ?: LocalDate.now().dayOfMonth) }
    var vibrate by remember(existing?.id) { mutableStateOf(existing?.vibrate ?: settings.defaultVibrate) }
    var readLabelAloud by remember(existing?.id) { mutableStateOf(existing?.readLabelAloud ?: settings.defaultReadLabelAloud) }
    var snoozeEnabled by remember(existing?.id) { mutableStateOf(existing?.snoozeEnabled ?: settings.defaultSnoozeEnabled) }
    var useDefaultSnoozeLength by remember(existing?.id) { mutableStateOf(existing?.snoozeMinutes == null) }
    var customSnoozeMinutes by remember(existing?.id) { mutableIntStateOf(existing?.snoozeMinutes ?: settings.defaultSnoozeMinutes) }
    var selectedGroupId by remember(existing?.id) { mutableStateOf(existing?.groupId) }
    var createNewGroup by remember(existing?.id) { mutableStateOf(false) }
    var newGroupName by remember(existing?.id) { mutableStateOf("") }
    var customSoundUri by remember(existing?.id) { mutableStateOf(existing?.soundUri) }

    val snoozeLengthOptions = listOf(5, 10, 15, 20, 30, 45, 60)
    val anchorEpochDay = existing?.repeat?.anchorEpochDay ?: existing?.createdEpochDay ?: LocalDate.now().toEpochDay()
    val dayLetters = listOf("S", "M", "T", "W", "T", "F", "S")
    val dayValues = listOf(7, 1, 2, 3, 4, 5, 6)
    val repeatTypes = listOf(
        RepeatType.ONCE to "One-time",
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
    val selectedTime = if (settings.timePickerStyle == TimePickerStyle.ANALOG) {
        LocalTime.of(timePickerState.hour, timePickerState.minute)
    } else {
        LocalTime.of(selectedHour, selectedMinute)
    }
    val previewAlarm = Alarm(time = selectedTime, repeat = previewSchedule, isEnabled = true)
    val countdownLine = TimeUtils.nextAlarmHeader(listOf(previewAlarm), settings.use24HourFormat)
        ?.countdownLine ?: "(less than a minute from now)"

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
                    groupId = resolvedGroupId,
                    soundUri = resolvedSoundUri
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
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Active", style = MaterialTheme.typography.bodyMedium)
                        Checkbox(
                            checked = isActive,
                            onCheckedChange = { isActive = it }
                        )
                    }
                }
            )
        },
        bottomBar = {
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier
                        .weight(1f)
                        .padding(4.dp)
                ) {
                    Text("CANCEL")
                }
                OutlinedButton(
                    onClick = { saveAlarm() },
                    modifier = Modifier
                        .weight(1f)
                        .padding(4.dp)
                ) {
                    Text("SAVE")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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
            }

            Text(
                text = countdownLine,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .clip(CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), CircleShape)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Alarm label (spoken reminder)", style = MaterialTheme.typography.bodyLarge)
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

                Text("Alarm sound", style = MaterialTheme.typography.bodyLarge, color = WarmAmber)
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

                Text("Alarm group", style = MaterialTheme.typography.bodyLarge, color = WarmAmber)
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

                Text("Alarm type", style = MaterialTheme.typography.bodyLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeatTypes.take(2).forEach { (type, name) ->
                        FilterChip(
                            selected = repeatType == type || (type == RepeatType.WEEKLY && repeatType == RepeatType.DAILY),
                            onClick = { repeatType = type },
                            label = { Text(name) }
                        )
                    }
                }

                if (repeatType == RepeatType.WEEKLY || repeatType == RepeatType.DAILY || repeatType == RepeatType.INTERVAL_WEEKS) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        dayLetters.forEachIndexed { index, letter ->
                            val day = dayValues[index]
                            val selected = day in selectedDays || (repeatType == RepeatType.DAILY)
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
                                        if (repeatType == RepeatType.DAILY) return@clickable
                                        selectedDays = if (day in selectedDays) selectedDays - day else selectedDays + day
                                        if (repeatType == RepeatType.ONCE) repeatType = RepeatType.WEEKLY
                                    }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(letter)
                            }
                        }
                    }
                }

                Text("Repeat", style = MaterialTheme.typography.bodyLarge, color = WarmAmber)
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
                    RepeatCalculator.summary(previewSchedule),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (repeatType == RepeatType.INTERVAL_WEEKS) {
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(onClick = { dayOfMonth = ((dayOfMonth - 2 + 31) % 31) + 1 }) { Text("-") }
                        Text("Day $dayOfMonth", style = MaterialTheme.typography.titleMedium)
                        OutlinedButton(onClick = { dayOfMonth = (dayOfMonth % 31) + 1 }) { Text("+") }
                    }
                }

                Text("Snooze", style = MaterialTheme.typography.bodyLarge, color = WarmAmber)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Allow snooze")
                        Text(
                            "Snooze duration: ${if (useDefaultSnoozeLength) "${settings.defaultSnoozeMinutes} minutes" else "$customSnoozeMinutes minutes"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = snoozeEnabled, onCheckedChange = { snoozeEnabled = it })
                }

                if (snoozeEnabled) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = useDefaultSnoozeLength,
                            onClick = { useDefaultSnoozeLength = true },
                            label = { Text("Default (${settings.defaultSnoozeMinutes}m)") }
                        )
                        snoozeLengthOptions.forEach { minutes ->
                            FilterChip(
                                selected = !useDefaultSnoozeLength && customSnoozeMinutes == minutes,
                                onClick = {
                                    useDefaultSnoozeLength = false
                                    customSnoozeMinutes = minutes
                                },
                                label = { Text("${minutes}m") }
                            )
                        }
                    }
                }

                Text("Dismiss", style = MaterialTheme.typography.bodyLarge, color = WarmAmber)
                Text(
                    "Dismiss method: Press on-screen button",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text("Advanced", style = MaterialTheme.typography.bodyLarge, color = WarmAmber)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Vibrate during alarm")
                    Switch(checked = vibrate, onCheckedChange = { vibrate = it })
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
            }
        }
    }
}
