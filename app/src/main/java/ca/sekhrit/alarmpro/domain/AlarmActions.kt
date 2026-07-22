package ca.sekhrit.alarmpro.domain

import android.content.Context
import ca.sekhrit.alarmpro.data.AlarmRepository
import ca.sekhrit.alarmpro.data.RepeatType
import ca.sekhrit.alarmpro.data.SettingsRepository
import ca.sekhrit.alarmpro.data.isSnoozeAllowed
import ca.sekhrit.alarmpro.data.resolveSnoozeMinutes
import ca.sekhrit.alarmpro.receiver.AlarmScheduler
import ca.sekhrit.alarmpro.receiver.NotificationHelper

object AlarmActions {
    fun dismiss(context: Context, alarmId: String) {
        val repository = AlarmRepository(context)
        val scheduler = AlarmScheduler(context)
        val alarms = repository.loadAlarms()
        val alarm = alarms.find { it.id == alarmId } ?: return

        if (alarm.repeat.type == RepeatType.ONCE) {
            repository.saveAlarms(
                alarms.map { if (it.id == alarmId) it.copy(isEnabled = false, skipUntilEpochDay = null) else it }
            )
        } else {
            val today = java.time.LocalDate.now().toEpochDay()
            val cleared = alarm.copy(
                skipUntilEpochDay = alarm.skipUntilEpochDay?.takeIf { it > today }
            )
            if (cleared != alarm) {
                repository.saveAlarms(
                    alarms.map { if (it.id == alarmId) cleared else it }
                )
            }
            scheduler.schedule(cleared)
        }

        NotificationHelper.cancelAlarmNotification(context, alarmId)
    }

    fun snooze(context: Context, alarmId: String) {
        val repository = AlarmRepository(context)
        val settings = SettingsRepository(context).load()
        val alarm = repository.loadAlarms().find { it.id == alarmId } ?: return
        if (!alarm.isSnoozeAllowed(settings)) return
        AlarmScheduler(context).scheduleSnooze(alarm, alarm.resolveSnoozeMinutes(settings))
        NotificationHelper.cancelAlarmNotification(context, alarmId)
    }
}
