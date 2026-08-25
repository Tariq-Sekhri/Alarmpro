package ca.sekhrit.alarmpro.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
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
import ca.sekhrit.alarmpro.viewmodel.TimerViewModel

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
            ACTION_CANCEL_ALARM -> {
                val alarmId = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_ID) ?: return
                AlarmActions.cancel(context, alarmId)
            }
            ACTION_SKIP_ALARM -> {
                val alarmId = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_ID) ?: return
                AlarmActions.skipNext(context, alarmId)
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
        val ringIntent = prepareScheduledAlarm(
            context,
            alarmId,
            intent.getBooleanExtra(AlarmScheduler.EXTRA_IS_SNOOZE, false)
        ) ?: return

        // Legacy installations can still have a BroadcastReceiver alarm
        // PendingIntent scheduled. Bring up the prepared interaction surface
        // while preserving the direct-activity path for newly scheduled alarms.
        launchRingingActivity(context, alarmId.hashCode(), ringIntent)
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
            leadText = leadText,
            isRepeating = alarm.repeat.type != ca.sekhrit.alarmpro.data.RepeatType.ONCE
        )
    }

    private fun handleTimer(context: Context, intent: Intent) {
        val timerId = intent.getStringExtra(TimerScheduler.EXTRA_TIMER_ID) ?: return
        completeTimer(context, timerId)
    }

    companion object {
        private val timerCompletionLock = Any()

        /**
         * Resolves current alarm state only after Android has launched the
         * trusted exact-alarm activity/receiver. The full-screen notification
         * is posted before audio-service startup, so SystemUI receives it in
         * the same alarm-delivery window.
         */
        fun prepareScheduledAlarm(
            context: Context,
            alarmId: String,
            isSnooze: Boolean
        ): Intent? {
        val settings = SettingsRepository(context).load()
        val alarms = AlarmRepository(context).loadAlarms()
        val alarm = alarms.find { it.id == alarmId }
        if (alarm == null || !alarm.isEnabled) {
            AlarmScheduler(context).cancel(alarmId)
            NotificationHelper.cancelAlarmNotification(context, alarmId)
            return null
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

        NotificationHelper.showRingingAlarmNotification(
            context = context,
            alarmId = alarmId,
            hour = alarm.time.hour,
            minute = alarm.time.minute,
            label = spokenLabel,
            snoozeAllowed = snoozeAllowed,
            snoozeMinutes = snoozeMinutes
        )

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
            soundUri = soundUri,
            isSnooze = isSnooze
        )

        return Intent(context, AlarmRingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(AlarmRingActivity.EXTRA_RING_TYPE, AlarmRingActivity.TYPE_ALARM)
            putExtra(AlarmRingActivity.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmRingActivity.EXTRA_HOUR, alarm.time.hour)
            putExtra(AlarmRingActivity.EXTRA_MINUTE, alarm.time.minute)
            putExtra(AlarmRingActivity.EXTRA_LABEL, spokenLabel)
            putExtra(AlarmRingActivity.EXTRA_SNOOZE_ALLOWED, snoozeAllowed)
            putExtra(AlarmRingActivity.EXTRA_SNOOZE_MINUTES, snoozeMinutes)
        }
    }

        /**
         * Claims a completed timer before starting its alert. Both the exact
         * alarm and the foreground countdown ticker use this path, so the
         * ticker can alert immediately without a delayed alarm firing twice.
         */
        fun completeTimer(context: Context, timerId: String) {
            synchronized(timerCompletionLock) {
                val repository = TimerRepository(context)
                val timer = repository.loadAll().find { it.id == timerId } ?: run {
                    TimerScheduler(context).cancel(timerId)
                    NotificationHelper.cancelTimerNotification(context, timerId)
                    return
                }
                val label = timer.label
                val totalSeconds = timer.totalSeconds
                TimerScheduler(context).cancel(timerId)
                NotificationHelper.cancelTimerNotification(context, timerId)
                repository.removeTimer(timerId)
                TimerViewModel.instance()?.syncFromStorage()
                NotificationHelper.showRingingTimerNotification(context, timerId, label, totalSeconds)
                launchRingingActivity(
                    context = context,
                    ringType = AlarmRingActivity.TYPE_TIMER,
                    timerId = timerId,
                    label = label,
                    totalSeconds = totalSeconds
                )
                AlarmRingingService.startTimer(context, timerId, label, totalSeconds)
            }
        }

        /**
         * Exact alarms are permitted to surface an alarm activity, but device
         * notification policies can decline a full-screen intent. Start the
         * same screen directly for alarms and timers; the foreground
         * notification remains the fallback when the system blocks it.
         */
        private fun launchRingingActivity(
            context: Context,
            ringType: String,
            alarmId: String = "",
            timerId: String = "",
            hour: Int = 0,
            minute: Int = 0,
            label: String = "",
            snoozeAllowed: Boolean = true,
            snoozeMinutes: Int = 10,
            totalSeconds: Int = 0
        ) {
            runCatching {
                val ringIntent = Intent(context, AlarmRingActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra(AlarmRingActivity.EXTRA_RING_TYPE, ringType)
                    putExtra(AlarmRingActivity.EXTRA_ALARM_ID, alarmId)
                    putExtra(AlarmRingActivity.EXTRA_TIMER_ID, timerId)
                    putExtra(AlarmRingActivity.EXTRA_HOUR, hour)
                    putExtra(AlarmRingActivity.EXTRA_MINUTE, minute)
                    putExtra(AlarmRingActivity.EXTRA_LABEL, label)
                    putExtra(AlarmRingActivity.EXTRA_SNOOZE_ALLOWED, snoozeAllowed)
                    putExtra(AlarmRingActivity.EXTRA_SNOOZE_MINUTES, snoozeMinutes)
                    putExtra(AlarmRingActivity.EXTRA_TIMER_TOTAL_SECONDS, totalSeconds)
                }
                val requestCode = if (alarmId.isNotBlank()) {
                    alarmId.hashCode()
                } else {
                    TimerScheduler.notificationIdFor(timerId)
                }
                launchRingingActivity(context, requestCode, ringIntent)
            }.onFailure { error ->
                // Keep this visible in logcat: the foreground-service
                // notification remains the fallback, but a swallowed failure
                // made locked-device delivery impossible to diagnose.
                Log.e("AlarmReceiver", "Could not launch ringing activity", error)
            }
        }

        private fun launchRingingActivity(
            context: Context,
            requestCode: Int,
            intent: Intent
        ) {
            NotificationHelper.launchRingingActivity(context, requestCode, intent)
        }

        const val ACTION_DISMISS_ALARM = "ca.sekhrit.alarmpro.DISMISS_ALARM"
        const val ACTION_SNOOZE_ALARM = "ca.sekhrit.alarmpro.SNOOZE_ALARM"
        const val ACTION_CANCEL_ALARM = "ca.sekhrit.alarmpro.CANCEL_ALARM"
        const val ACTION_SKIP_ALARM = "ca.sekhrit.alarmpro.SKIP_ALARM"
        const val ACTION_DISMISS_TIMER = "ca.sekhrit.alarmpro.DISMISS_TIMER"
    }
}
