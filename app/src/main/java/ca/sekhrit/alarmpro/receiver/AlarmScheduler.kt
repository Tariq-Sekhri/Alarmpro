package ca.sekhrit.alarmpro.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import ca.sekhrit.alarmpro.data.Alarm
import ca.sekhrit.alarmpro.util.RepeatCalculator

class AlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(alarm: Alarm) {
        if (!alarm.isEnabled) {
            cancel(alarm)
            return
        }

        val triggerAt = RepeatCalculator.nextTriggerMillis(alarm)
        scheduleInternal(
            requestCode = alarm.id.hashCode(),
            alarm = alarm,
            triggerAt = triggerAt
        )
    }

    fun scheduleSnooze(alarm: Alarm, snoozeMinutes: Int) {
        val triggerAt = System.currentTimeMillis() + snoozeMinutes * 60_000L
        scheduleInternal(
            requestCode = alarm.id.hashCode() + SNOOZE_OFFSET,
            alarm = alarm,
            triggerAt = triggerAt,
            isSnooze = true
        )
    }

    fun cancel(alarm: Alarm) {
        cancelRequestCode(alarm.id.hashCode())
        cancelRequestCode(alarm.id.hashCode() + SNOOZE_OFFSET)
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

        setExact(triggerAt, pendingIntent)
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

    private fun cancelRequestCode(requestCode: Int) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_ALARM
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
        const val EXTRA_ALARM_ID = "ALARM_ID"
        const val EXTRA_HOUR = "HOUR"
        const val EXTRA_MINUTE = "MINUTE"
        const val EXTRA_LABEL = "LABEL"
        const val EXTRA_VIBRATE = "VIBRATE"
        const val EXTRA_READ_LABEL_ALOUD = "READ_LABEL_ALOUD"
        const val EXTRA_IS_SNOOZE = "IS_SNOOZE"
        private const val SNOOZE_OFFSET = 100_000
    }
}
