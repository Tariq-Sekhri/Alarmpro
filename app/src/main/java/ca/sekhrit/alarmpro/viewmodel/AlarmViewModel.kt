package ca.sekhrit.alarmpro.viewmodel



import android.app.Application

import androidx.lifecycle.AndroidViewModel

import ca.sekhrit.alarmpro.data.Alarm

import ca.sekhrit.alarmpro.data.AlarmGroup

import ca.sekhrit.alarmpro.data.AlarmGroupRepository

import ca.sekhrit.alarmpro.data.AlarmRepository

import ca.sekhrit.alarmpro.data.AppSettings

import ca.sekhrit.alarmpro.data.RepeatSchedule

import ca.sekhrit.alarmpro.data.SettingsRepository

import ca.sekhrit.alarmpro.receiver.AlarmScheduler

import ca.sekhrit.alarmpro.util.RepeatCalculator

import ca.sekhrit.alarmpro.util.TimeUtils

import kotlinx.coroutines.flow.MutableStateFlow

import kotlinx.coroutines.flow.StateFlow

import kotlinx.coroutines.flow.asStateFlow

import java.time.LocalTime



class AlarmViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AlarmRepository(application)

    private val groupRepository = AlarmGroupRepository(application)

    private val settingsRepository = SettingsRepository(application)

    private val scheduler = AlarmScheduler(application)



    private val _alarms = MutableStateFlow(repository.loadAlarms())

    val alarms: StateFlow<List<Alarm>> = _alarms.asStateFlow()



    private val _groups = MutableStateFlow(groupRepository.loadGroups())

    val groups: StateFlow<List<AlarmGroup>> = _groups.asStateFlow()



    private val _settings = MutableStateFlow(settingsRepository.load())

    val settings: StateFlow<AppSettings> = _settings.asStateFlow()



    init {

        _alarms.value.filter { it.isEnabled }.forEach { scheduler.schedule(it) }

        cleanupEmptyGroups()

    }



    val sortedAlarms: List<Alarm>

        get() {

            val now = java.time.LocalDateTime.now()

            return _alarms.value.sortedWith(

                compareBy<Alarm> { !it.isEnabled }

                    .thenBy { RepeatCalculator.nextTriggerMillis(it, now) }

            )

        }



    val nextAlarmHeader

        get() = TimeUtils.nextAlarmHeader(_alarms.value, _settings.value.use24HourFormat)



    private fun persistAlarms(alarms: List<Alarm>) {

        _alarms.value = alarms

        repository.saveAlarms(alarms)

    }



    private fun persistGroups(groups: List<AlarmGroup>) {

        _groups.value = groups

        groupRepository.saveGroups(groups)

    }



    private fun reschedule(alarm: Alarm) {

        if (alarm.isEnabled) scheduler.schedule(alarm) else scheduler.cancel(alarm)

    }



    private fun cleanupEmptyGroups() {

        val usedGroupIds = _alarms.value.mapNotNull { it.groupId }.toSet()

        val cleaned = _groups.value.filter { it.id in usedGroupIds }

        if (cleaned.size != _groups.value.size) {

            persistGroups(cleaned)

        }

    }



    fun addAlarm(

        time: LocalTime,

        label: String,

        repeat: RepeatSchedule,

        vibrate: Boolean,

        readLabelAloud: Boolean,

        snoozeEnabled: Boolean,

        snoozeMinutes: Int?,

        isEnabled: Boolean = true,

        groupId: String? = null,

        soundUri: String? = null

    ) {

        val newAlarm = Alarm(

            time = time,

            label = label.trim(),

            repeat = repeat,

            vibrate = vibrate,

            readLabelAloud = readLabelAloud,

            snoozeEnabled = snoozeEnabled,

            snoozeMinutes = snoozeMinutes,

            isEnabled = isEnabled,

            groupId = groupId,

            soundUri = soundUri

        )

        persistAlarms(_alarms.value + newAlarm)

        if (newAlarm.isEnabled) {

            scheduler.schedule(newAlarm)

        }

    }



    fun updateAlarm(updated: Alarm) {

        val old = _alarms.value.find { it.id == updated.id }

        if (old != null) {

            scheduler.cancel(old)

        }

        persistAlarms(_alarms.value.map { if (it.id == updated.id) updated else it })

        if (updated.isEnabled) {

            scheduler.schedule(updated)

        }

        cleanupEmptyGroups()

    }



    fun toggleAlarm(alarm: Alarm) {

        val updated = alarm.copy(isEnabled = !alarm.isEnabled)

        persistAlarms(_alarms.value.map { if (it.id == alarm.id) updated else it })

        reschedule(updated)

    }



    fun deleteAlarm(alarm: Alarm) {

        scheduler.cancel(alarm)

        persistAlarms(_alarms.value.filter { it.id != alarm.id })

        cleanupEmptyGroups()

    }



    fun copyAlarm(alarm: Alarm) {

        val copy = alarm.copy(

            id = java.util.UUID.randomUUID().toString(),

            skipUntilEpochDay = null,

            label = ""

        )

        persistAlarms(_alarms.value + copy)

        if (copy.isEnabled) {

            scheduler.schedule(copy)

        }

    }



    fun skipNextAlarm(alarm: Alarm) {

        if (!alarm.isEnabled) return

        val now = java.time.LocalDateTime.now()

        val updated = if (RepeatCalculator.hasSkipScheduled(alarm, now)) {

            alarm.copy(skipUntilEpochDay = null)

        } else {

            val skipDay = RepeatCalculator.nextUnskippedTriggerDate(alarm, now).toEpochDay()

            alarm.copy(skipUntilEpochDay = skipDay)

        }

        persistAlarms(_alarms.value.map { if (it.id == alarm.id) updated else it })

        reschedule(updated)

    }



    fun createGroup(label: String): AlarmGroup {

        val trimmed = label.trim()

        require(trimmed.isNotEmpty())

        val nextOrder = (_groups.value.maxOfOrNull { it.sortOrder } ?: -1) + 1

        val group = AlarmGroup(label = trimmed, sortOrder = nextOrder)

        persistGroups(_groups.value + group)

        return group

    }



    fun renameGroup(groupId: String, label: String) {

        val trimmed = label.trim()

        if (trimmed.isEmpty()) return

        persistGroups(_groups.value.map { if (it.id == groupId) it.copy(label = trimmed) else it })

    }



    fun deleteGroup(groupId: String) {

        _alarms.value.filter { it.groupId == groupId }.forEach { scheduler.cancel(it) }

        persistGroups(_groups.value.filter { it.id != groupId })

        persistAlarms(_alarms.value.filter { it.groupId != groupId })

    }



    fun ungroupGroup(groupId: String) {

        persistGroups(_groups.value.filter { it.id != groupId })

        persistAlarms(_alarms.value.map { if (it.groupId == groupId) it.copy(groupId = null) else it })

    }



    fun toggleGroupCollapsed(groupId: String) {

        persistGroups(

            _groups.value.map {

                if (it.id == groupId) it.copy(isCollapsed = !it.isCollapsed) else it

            }

        )

    }



    fun toggleGroupEnabled(groupId: String) {

        val members = _alarms.value.filter { it.groupId == groupId }

        if (members.isEmpty()) return

        val enableAll = !members.all { it.isEnabled }

        val updatedAlarms = _alarms.value.map { alarm ->

            if (alarm.groupId != groupId) alarm else alarm.copy(isEnabled = enableAll)

        }

        persistAlarms(updatedAlarms)

        updatedAlarms.filter { it.groupId == groupId }.forEach { reschedule(it) }

    }



    fun skipNextGroup(groupId: String) {

        val now = java.time.LocalDateTime.now()

        val members = _alarms.value.filter { it.groupId == groupId && it.isEnabled }

        if (members.isEmpty()) return

        val cancelAllSkips = members.all { RepeatCalculator.hasSkipScheduled(it, now) }

        val updatedAlarms = _alarms.value.map { alarm ->

            if (alarm.groupId != groupId || !alarm.isEnabled) return@map alarm

            if (cancelAllSkips) {

                alarm.copy(skipUntilEpochDay = null)

            } else if (!RepeatCalculator.hasSkipScheduled(alarm, now)) {

                val skipDay = RepeatCalculator.nextUnskippedTriggerDate(alarm, now).toEpochDay()

                alarm.copy(skipUntilEpochDay = skipDay)

            } else {

                alarm

            }

        }

        persistAlarms(updatedAlarms)

        updatedAlarms.filter { it.groupId == groupId && it.isEnabled }.forEach { reschedule(it) }

    }



    fun assignAlarmToGroup(alarmId: String, groupId: String?) {

        val alarm = _alarms.value.find { it.id == alarmId } ?: return

        updateAlarm(alarm.copy(groupId = groupId))

    }



    fun removeAlarmFromGroup(alarmId: String) {
        assignAlarmToGroup(alarmId, null)
    }

    fun deleteAlarms(alarmIds: Set<String>) {
        if (alarmIds.isEmpty()) return
        alarmIds.forEach { id ->
            _alarms.value.find { it.id == id }?.let { scheduler.cancel(it) }
        }
        persistAlarms(_alarms.value.filter { it.id !in alarmIds })
        cleanupEmptyGroups()
    }

    fun setAlarmsEnabled(alarmIds: Set<String>, enabled: Boolean) {
        if (alarmIds.isEmpty()) return
        val updated = _alarms.value.map { alarm ->
            if (alarm.id in alarmIds) alarm.copy(isEnabled = enabled) else alarm
        }
        persistAlarms(updated)
        updated.filter { it.id in alarmIds }.forEach { reschedule(it) }
    }

    fun skipNextAlarms(alarmIds: Set<String>) {
        if (alarmIds.isEmpty()) return
        val now = java.time.LocalDateTime.now()
        val selected = _alarms.value.filter { it.id in alarmIds && it.isEnabled }
        if (selected.isEmpty()) return
        val cancelAll = selected.all { RepeatCalculator.hasSkipScheduled(it, now) }
        val updated = _alarms.value.map { alarm ->
            if (alarm.id !in alarmIds || !alarm.isEnabled) return@map alarm
            if (cancelAll) {
                alarm.copy(skipUntilEpochDay = null)
            } else if (!RepeatCalculator.hasSkipScheduled(alarm, now)) {
                val skipDay = RepeatCalculator.nextUnskippedTriggerDate(alarm, now).toEpochDay()
                alarm.copy(skipUntilEpochDay = skipDay)
            } else {
                alarm
            }
        }
        persistAlarms(updated)
        updated.filter { it.id in alarmIds && it.isEnabled }.forEach { reschedule(it) }
    }

    fun assignAlarmsToGroup(alarmIds: Set<String>, groupId: String) {
        if (alarmIds.isEmpty()) return
        persistGroups(
            _groups.value.map {
                if (it.id == groupId) it.copy(isCollapsed = false) else it
            }
        )
        val updated = _alarms.value.map { alarm ->
            if (alarm.id in alarmIds) alarm.copy(groupId = groupId) else alarm
        }
        persistAlarms(updated)
        updated.filter { it.id in alarmIds && it.isEnabled }.forEach { reschedule(it) }
    }

    fun groupAlarms(alarmIds: Set<String>, groupLabel: String): AlarmGroup {
        val group = createGroup(groupLabel)
        persistGroups(
            _groups.value.map {
                if (it.id == group.id) it.copy(isCollapsed = false) else it
            }
        )
        val updated = _alarms.value.map { alarm ->
            if (alarm.id in alarmIds) alarm.copy(groupId = group.id) else alarm
        }
        persistAlarms(updated)
        updated.filter { it.id in alarmIds && it.isEnabled }.forEach { reschedule(it) }
        return group
    }

    fun updateSettings(settings: AppSettings) {
        val upcomingChanged = _settings.value.upcomingAlarmLeadMinutes != settings.upcomingAlarmLeadMinutes
        _settings.value = settings
        settingsRepository.save(settings)
        if (upcomingChanged) {
            _alarms.value.filter { it.isEnabled }.forEach { reschedule(it) }
        }
    }



    fun updateDefaultAlarmSound(uri: String?, applyToAllAlarms: Boolean) {

        updateSettings(_settings.value.copy(defaultAlarmSoundUri = uri))

        if (applyToAllAlarms) {

            persistAlarms(_alarms.value.map { it.copy(soundUri = null) })

        }

    }

}


