package ca.sekhrit.alarmpro.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import ca.sekhrit.alarmpro.data.Alarm

class WakeCheckScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleCheck(alarm: Alarm) {
        schedule(alarm.id, CHECK_OFFSET, AlarmReceiver.ACTION_WAKE_CHECK, alarm.wakeCheckDelayMinutes)
    }

    fun scheduleTimeout(alarmId: String, responseMinutes: Int) {
        schedule(alarmId, TIMEOUT_OFFSET, AlarmReceiver.ACTION_WAKE_CHECK_TIMEOUT, responseMinutes)
    }

    fun cancel(alarmId: String) {
        cancel(alarmId, CHECK_OFFSET, AlarmReceiver.ACTION_WAKE_CHECK)
        cancel(alarmId, TIMEOUT_OFFSET, AlarmReceiver.ACTION_WAKE_CHECK_TIMEOUT)
        WakeCheckOverlay.dismiss(alarmId)
        NotificationHelper.cancelWakeCheckNotification(context, alarmId)
    }

    private fun schedule(alarmId: String, offset: Int, action: String, minutes: Int) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            this.action = action
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId.hashCode() + offset,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerAt = System.currentTimeMillis() + minutes.coerceAtLeast(1) * 60_000L
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S &&
            !alarmManager.canScheduleExactAlarms()
        ) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
    }

    private fun cancel(alarmId: String, offset: Int, action: String) {
        val intent = Intent(context, AlarmReceiver::class.java).apply { this.action = action }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId.hashCode() + offset,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    private companion object {
        const val CHECK_OFFSET = 400_000
        const val TIMEOUT_OFFSET = 500_000
    }
}
