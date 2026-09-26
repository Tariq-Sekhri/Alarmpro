package ca.sekhrit.alarmpro.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import ca.sekhrit.alarmpro.MainActivity
import ca.sekhrit.alarmpro.data.Alarm
import ca.sekhrit.alarmpro.data.RepeatType
import ca.sekhrit.alarmpro.data.SettingsRepository
import ca.sekhrit.alarmpro.util.RepeatCalculator
import ca.sekhrit.alarmpro.util.TimeUtils

class AlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(alarm: Alarm) {
        if (!alarm.isEnabled) {
            cancel(alarm)
            return
        }

        // A reschedule can move the next occurrence past a notification that is
        // already visible (for example, after skipping one alarm or a group).
        // Clear that stale warning before scheduling the new occurrence.
        cancelUpcoming(alarm)

        val settings = SettingsRepository(context).load()
        val triggerAt = RepeatCalculator.nextTriggerMillis(alarm)
        scheduleInternal(
            requestCode = alarm.id.hashCode(),
            alarm = alarm,
            triggerAt = triggerAt
        )
        scheduleUpcoming(alarm, triggerAt, settings)
    }

    fun scheduleSnooze(alarm: Alarm, snoozeMinutes: Int) {
        val triggerAt = System.currentTimeMillis() + snoozeMinutes * 60_000L
        cancelUpcoming(alarm)
        scheduleInternal(
            requestCode = alarm.id.hashCode() + SNOOZE_OFFSET,
            alarm = alarm,
            triggerAt = triggerAt,
            isSnooze = true
        )
        val settings = SettingsRepository(context).load()
        scheduleUpcoming(alarm, triggerAt, settings)
    }

    fun cancel(alarm: Alarm) {
        cancel(alarm.id)
    }

    fun cancel(alarmId: String) {
        cancelRequestCode(alarmId.hashCode(), ACTION_ALARM)
        cancelRequestCode(alarmId.hashCode() + SNOOZE_OFFSET, ACTION_ALARM)
        cancelRequestCode(alarmId.hashCode() + UPCOMING_OFFSET, ACTION_UPCOMING_ALARM)
        WakeCheckScheduler(context).cancel(alarmId)
        NotificationHelper.cancelUpcomingNotification(context, alarmId)
    }

    fun cancelSnooze(alarmId: String) {
        cancelRequestCode(alarmId.hashCode() + SNOOZE_OFFSET, ACTION_ALARM)
    }

    fun cancelRegular(alarmId: String) {
        cancelRequestCode(alarmId.hashCode(), ACTION_ALARM)
        cancelRequestCode(alarmId.hashCode() + UPCOMING_OFFSET, ACTION_UPCOMING_ALARM)
        NotificationHelper.cancelUpcomingNotification(context, alarmId)
    }

    private fun scheduleUpcoming(
        alarm: Alarm,
        triggerAt: Long,
        settings: ca.sekhrit.alarmpro.data.AppSettings
    ) {
        val leadMinutes = settings.upcomingAlarmLeadMinutes
        if (leadMinutes <= 0) {
            cancelUpcoming(alarm)
            return
        }

        val upcomingAt = triggerAt - leadMinutes * 60_000L
        if (upcomingAt <= System.currentTimeMillis()) {
            // The alarm was created or edited within its warning window. The warning
            // should be useful immediately instead of being silently skipped.
            cancelUpcoming(alarm)
            NotificationHelper.showUpcomingAlarmNotification(
                context = context,
                alarmId = alarm.id,
                label = alarm.label,
                timeText = TimeUtils.formatNotificationTime(alarm.time, settings.use24HourFormat),
                remainingMillis = triggerAt - System.currentTimeMillis(),
                isRepeating = alarm.repeat.type != RepeatType.ONCE
            )
            return
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_UPCOMING_ALARM
            putExtra(EXTRA_ALARM_ID, alarm.id)
            putExtra(EXTRA_HOUR, alarm.time.hour)
            putExtra(EXTRA_MINUTE, alarm.time.minute)
            putExtra(EXTRA_LABEL, alarm.label)
            putExtra(EXTRA_LEAD_MINUTES, leadMinutes)
            putExtra(EXTRA_USE_24H, settings.use24HourFormat)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.hashCode() + UPCOMING_OFFSET,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        setExact(upcomingAt, pendingIntent)
    }

    private fun cancelUpcoming(alarm: Alarm) {
        cancelRequestCode(alarm.id.hashCode() + UPCOMING_OFFSET, ACTION_UPCOMING_ALARM)
        NotificationHelper.cancelUpcomingNotification(context, alarm.id)
    }

    private fun scheduleInternal(
        requestCode: Int,
        alarm: Alarm,
        triggerAt: Long,
        isSnooze: Boolean = false
    ) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_ALARM
            putExtra(EXTRA_ALARM_ID, alarm.id)
            putExtra(EXTRA_HOUR, alarm.time.hour)
            putExtra(EXTRA_MINUTE, alarm.time.minute)
            putExtra(EXTRA_LABEL, alarm.label)
            putExtra(EXTRA_VIBRATE, alarm.vibrate)
            putExtra(EXTRA_READ_LABEL_ALOUD, alarm.readLabelAloud)
            putExtra(EXTRA_IS_SNOOZE, isSnooze)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        setAlarmClock(triggerAt, pendingIntent, alarm)
    }

    /**
     * Registers user-facing alarms with Android as actual alarm clocks. Besides
     * preserving exact, idle-safe delivery, this exposes the alarm through
     * AlarmManager.getNextAlarmClock() and ACTION_NEXT_ALARM_CLOCK_CHANGED so
     * system UI and automation apps can recognize it as an alarm.
     */
    private fun setAlarmClock(
        triggerAt: Long,
        pendingIntent: PendingIntent,
        alarm: Alarm
    ) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S &&
            !alarmManager.canScheduleExactAlarms()
        ) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pendingIntent
            )
            return
        }

        val showIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_TARGET_TAB, "alarm")
        }
        val showPendingIntent = PendingIntent.getActivity(
            context,
            alarm.id.hashCode() + SHOW_ALARM_OFFSET,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(triggerAt, showPendingIntent),
            pendingIntent
        )
    }

    private fun setExact(triggerAt: Long, pendingIntent: PendingIntent) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S &&
            !alarmManager.canScheduleExactAlarms()
        ) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pendingIntent
            )
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pendingIntent
            )
        }
    }

    private fun cancelRequestCode(requestCode: Int, action: String) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            this.action = action
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    companion object {
        const val ACTION_ALARM = "ca.sekhrit.alarmpro.ALARM"
        const val ACTION_UPCOMING_ALARM = "ca.sekhrit.alarmpro.UPCOMING_ALARM"
        const val EXTRA_ALARM_ID = "ALARM_ID"
        const val EXTRA_HOUR = "HOUR"
        const val EXTRA_MINUTE = "MINUTE"
        const val EXTRA_LABEL = "LABEL"
        const val EXTRA_VIBRATE = "VIBRATE"
        const val EXTRA_READ_LABEL_ALOUD = "READ_LABEL_ALOUD"
        const val EXTRA_IS_SNOOZE = "IS_SNOOZE"
        const val EXTRA_LEAD_MINUTES = "LEAD_MINUTES"
        const val EXTRA_USE_24H = "USE_24H"
        private const val SNOOZE_OFFSET = 100_000
        private const val UPCOMING_OFFSET = 200_000
        private const val SHOW_ALARM_OFFSET = 300_000
    }
}
