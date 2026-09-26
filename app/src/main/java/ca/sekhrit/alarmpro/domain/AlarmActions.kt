package ca.sekhrit.alarmpro.domain

import android.content.Context
import ca.sekhrit.alarmpro.data.AlarmRepository
import ca.sekhrit.alarmpro.data.RepeatType
import ca.sekhrit.alarmpro.data.SettingsRepository
import ca.sekhrit.alarmpro.data.isSnoozeAllowed
import ca.sekhrit.alarmpro.data.resolveSnoozeMinutes
import ca.sekhrit.alarmpro.data.shouldDeleteOnDismiss
import ca.sekhrit.alarmpro.receiver.AlarmScheduler
import ca.sekhrit.alarmpro.receiver.NotificationHelper
import ca.sekhrit.alarmpro.receiver.WakeCheckScheduler
import ca.sekhrit.alarmpro.util.RepeatCalculator
import java.time.LocalDate
import java.time.LocalDateTime

object AlarmActions {
    fun cancel(context: Context, alarmId: String) {
        val repository = AlarmRepository(context)
        val scheduler = AlarmScheduler(context)
        val alarms = repository.loadAlarms()
        val alarm = alarms.find { it.id == alarmId }

        scheduler.cancel(alarmId)
        if (alarm != null) {
            repository.saveAlarms(
                alarms.map {
                    if (it.id == alarmId) it.copy(isEnabled = false, skipUntilEpochDay = null, snoozedUntilEpochMillis = null) else it
                }
            )
        }
        NotificationHelper.cancelAlarmNotification(context, alarmId)
        NotificationHelper.cancelUpcomingNotification(context, alarmId)
    }

    fun skipNext(context: Context, alarmId: String) {
        val repository = AlarmRepository(context)
        val scheduler = AlarmScheduler(context)
        val alarms = repository.loadAlarms()
        val alarm = alarms.find { it.id == alarmId } ?: return
        if (!alarm.isEnabled || alarm.repeat.type == RepeatType.ONCE) {
            cancel(context, alarmId)
            return
        }

        val skipDay = RepeatCalculator.nextUnskippedTriggerDate(alarm).toEpochDay()
        val updated = alarm.copy(skipUntilEpochDay = skipDay, snoozedUntilEpochMillis = null)
        repository.saveAlarms(alarms.map { if (it.id == alarmId) updated else it })
        scheduler.schedule(updated)
        NotificationHelper.cancelAlarmNotification(context, alarmId)
        NotificationHelper.cancelUpcomingNotification(context, alarmId)
    }

    fun dismiss(context: Context, alarmId: String, startWakeCheck: Boolean = true) {
        val repository = AlarmRepository(context)
        val scheduler = AlarmScheduler(context)
        val alarms = repository.loadAlarms()
        val alarm = alarms.find { it.id == alarmId }

        if (alarm == null) {
            scheduler.cancel(alarmId)
            NotificationHelper.cancelAlarmNotification(context, alarmId)
            NotificationHelper.cancelUpcomingNotification(context, alarmId)
            return
        }

        scheduler.cancelSnooze(alarmId)

        if (startWakeCheck && alarm.wakeCheckEnabled) {
            scheduler.cancelRegular(alarmId)
            WakeCheckScheduler(context).scheduleCheck(alarm)
            if (alarm.repeat.type != RepeatType.ONCE) scheduler.schedule(alarm)
            NotificationHelper.cancelAlarmNotification(context, alarmId)
            NotificationHelper.cancelUpcomingNotification(context, alarmId)
            return
        }

        if (alarm.repeat.type == RepeatType.ONCE) {
            scheduler.cancel(alarmId)
            val scheduledDate = LocalDate.ofEpochDay(alarm.repeat.anchorEpochDay)
            val remainingDays = alarm.repeat.daysOfWeek - scheduledDate.dayOfWeek.value
            val now = LocalDateTime.now()
            val nextDate = if (alarm.deleteAfterDismiss && remainingDays.isNotEmpty()) {
                (0..7)
                    .map { now.toLocalDate().plusDays(it.toLong()) }
                    .firstOrNull { date ->
                        date.dayOfWeek.value in remainingDays &&
                            (date.isAfter(now.toLocalDate()) || alarm.time.isAfter(now.toLocalTime()))
                    }
            } else {
                null
            }

            if (nextDate != null) {
                val updated = alarm.copy(
                    repeat = alarm.repeat.copy(
                        daysOfWeek = remainingDays,
                        anchorEpochDay = nextDate.toEpochDay()
                    ),
                    skipUntilEpochDay = null,
                    snoozedUntilEpochMillis = null
                )
                repository.saveAlarms(alarms.map { if (it.id == alarmId) updated else it })
                scheduler.schedule(updated)
            } else if (alarm.shouldDeleteOnDismiss()) {
                repository.saveAlarms(alarms.filterNot { it.id == alarmId })
            } else {
                repository.saveAlarms(
                    alarms.map { if (it.id == alarmId) it.copy(isEnabled = false, skipUntilEpochDay = null, snoozedUntilEpochMillis = null) else it }
                )
            }
        } else {
            val today = java.time.LocalDate.now().toEpochDay()
            val cleared = alarm.copy(
                skipUntilEpochDay = alarm.skipUntilEpochDay?.takeIf { it > today },
                snoozedUntilEpochMillis = null
            )
            if (cleared != alarm) {
                repository.saveAlarms(
                    alarms.map { if (it.id == alarmId) cleared else it }
                )
            }
            scheduler.schedule(cleared)
        }

        NotificationHelper.cancelAlarmNotification(context, alarmId)
        NotificationHelper.cancelUpcomingNotification(context, alarmId)
    }

    fun snooze(context: Context, alarmId: String) {
        val repository = AlarmRepository(context)
        val settings = SettingsRepository(context).load()
        val alarm = repository.loadAlarms().find { it.id == alarmId }
        if (alarm == null || !alarm.isEnabled || !alarm.isSnoozeAllowed(settings)) {
            AlarmScheduler(context).cancelSnooze(alarmId)
            NotificationHelper.cancelAlarmNotification(context, alarmId)
            return
        }
        val snoozeMinutes = alarm.resolveSnoozeMinutes(settings)
        AlarmScheduler(context).cancelRegular(alarmId)
        val targetMillis = System.currentTimeMillis() + snoozeMinutes * 60_000L
        val updated = alarm.copy(snoozedUntilEpochMillis = targetMillis)
        repository.saveAlarms(repository.loadAlarms().map { if (it.id == alarmId) updated else it })
        AlarmScheduler(context).scheduleSnooze(alarm, snoozeMinutes)
        NotificationHelper.cancelAlarmNotification(context, alarmId)
        NotificationHelper.cancelUpcomingNotification(context, alarmId)
    }
}
