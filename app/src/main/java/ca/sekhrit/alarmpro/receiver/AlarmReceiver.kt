package ca.sekhrit.alarmpro.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ca.sekhrit.alarmpro.AlarmRingActivity
import ca.sekhrit.alarmpro.data.AlarmGroupRepository
import ca.sekhrit.alarmpro.data.AlarmRepository
import ca.sekhrit.alarmpro.data.SettingsRepository
import ca.sekhrit.alarmpro.data.isSnoozeAllowed
import ca.sekhrit.alarmpro.data.resolveSnoozeMinutes
import ca.sekhrit.alarmpro.data.TimerRepository
import ca.sekhrit.alarmpro.domain.AlarmActions
import ca.sekhrit.alarmpro.data.upcomingAlarmLeadLabel
import ca.sekhrit.alarmpro.util.AlarmGrouping
import ca.sekhrit.alarmpro.util.AlarmSoundUtils
import ca.sekhrit.alarmpro.util.TimeUtils
import ca.sekhrit.alarmpro.service.AlarmRingingService

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            AlarmScheduler.ACTION_ALARM -> handleAlarm(context, intent)
            AlarmScheduler.ACTION_UPCOMING_ALARM -> handleUpcomingAlarm(context, intent)
            TimerScheduler.ACTION_TIMER -> handleTimer(context, intent)
            ACTION_DISMISS_ALARM -> {
                val alarmId = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_ID) ?: return
                AlarmActions.dismiss(context, alarmId)
                AlarmRingingService.stop(context)
                AlarmRingActivity.notifyRingingStopped(context)
            }
            ACTION_SNOOZE_ALARM -> {
                val alarmId = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_ID) ?: return
                AlarmActions.snooze(context, alarmId)
                AlarmRingingService.stop(context)
                AlarmRingActivity.notifyRingingStopped(context)
            }
            ACTION_DISMISS_TIMER -> {
                val timerId = intent.getStringExtra(TimerScheduler.EXTRA_TIMER_ID) ?: return
                TimerRepository(context).removeTimer(timerId)
                TimerScheduler(context).cancel(timerId)
                NotificationHelper.cancelTimerNotification(context, timerId)
                AlarmRingingService.stop(context)
                AlarmRingActivity.notifyRingingStopped(context)
            }
        }
    }

    private fun handleAlarm(context: Context, intent: Intent) {
        val alarmId = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_ID) ?: return
        val settings = SettingsRepository(context).load()
        val alarms = AlarmRepository(context).loadAlarms()
        val alarm = alarms.find { it.id == alarmId }
        if (alarm == null || !alarm.isEnabled) {
            AlarmScheduler(context).cancel(alarmId)
            NotificationHelper.cancelAlarmNotification(context, alarmId)
            return
        }
        val group = alarm.groupId?.let { groupId ->
            AlarmGroupRepository(context).loadGroups().find { it.id == groupId }
        }
        val members = alarm.groupId?.let { AlarmGrouping.membersOf(it, alarms) }.orEmpty()
        val spokenLabel = alarm.let {
            AlarmGrouping.effectiveLabel(
                it,
                group,
                AlarmGrouping.indexInGroup(it, members)
            )
        }
        val snoozeAllowed = alarm.isSnoozeAllowed(settings)
        val snoozeMinutes = alarm.resolveSnoozeMinutes(settings)
        val soundUri = AlarmSoundUtils.resolvePlaybackUri(context, alarm, settings).toString()

        NotificationHelper.cancelUpcomingNotification(context, alarmId)

        AlarmRingingService.startAlarm(
            context = context,
            alarmId = alarmId,
            hour = alarm.time.hour,
            minute = alarm.time.minute,
            label = spokenLabel,
            vibrate = alarm.vibrate,
            readLabelAloud = alarm.readLabelAloud,
            snoozeAllowed = snoozeAllowed,
            snoozeMinutes = snoozeMinutes,
            soundUri = soundUri
        )
    }

    private fun handleUpcomingAlarm(context: Context, intent: Intent) {
        val alarmId = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_ID) ?: return
        val alarm = AlarmRepository(context).loadAlarms()
            .find { it.id == alarmId && it.isEnabled } ?: run {
            AlarmScheduler(context).cancel(alarmId)
            NotificationHelper.cancelUpcomingNotification(context, alarmId)
            return
        }
        val settings = SettingsRepository(context).load()
        val leadMinutes = settings.upcomingAlarmLeadMinutes
        if (leadMinutes <= 0) {
            NotificationHelper.cancelUpcomingNotification(context, alarmId)
            return
        }
        val timeText = TimeUtils.formatTime(alarm.time, settings.use24HourFormat)
        val leadText = upcomingAlarmLeadLabel(leadMinutes)

        NotificationHelper.showUpcomingAlarmNotification(
            context = context,
            alarmId = alarmId,
            label = alarm.label,
            timeText = timeText,
            leadText = leadText
        )
    }

    private fun handleTimer(context: Context, intent: Intent) {
        val timerId = intent.getStringExtra(TimerScheduler.EXTRA_TIMER_ID) ?: return
        val timer = TimerRepository(context).loadAll().find { it.id == timerId } ?: run {
            TimerScheduler(context).cancel(timerId)
            NotificationHelper.cancelTimerNotification(context, timerId)
            return
        }
        val label = timer.label
        val totalSeconds = timer.totalSeconds
        NotificationHelper.cancelTimerNotification(context, timerId)
        TimerRepository(context).removeTimer(timerId)
        AlarmRingingService.startTimer(context, timerId, label, totalSeconds)
    }

    companion object {
        const val ACTION_DISMISS_ALARM = "ca.sekhrit.alarmpro.DISMISS_ALARM"
        const val ACTION_SNOOZE_ALARM = "ca.sekhrit.alarmpro.SNOOZE_ALARM"
        const val ACTION_DISMISS_TIMER = "ca.sekhrit.alarmpro.DISMISS_TIMER"
    }
}
