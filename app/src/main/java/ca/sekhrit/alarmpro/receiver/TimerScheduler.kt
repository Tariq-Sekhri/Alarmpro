package ca.sekhrit.alarmpro.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

class TimerScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(timerId: String, endTimeMillis: Long, label: String, totalSeconds: Int = 0) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_TIMER
            putExtra(EXTRA_TIMER_ID, timerId)
            putExtra(EXTRA_TIMER_LABEL, label)
            putExtra(EXTRA_TIMER_TOTAL_SECONDS, totalSeconds)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCodeFor(timerId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S &&
            !alarmManager.canScheduleExactAlarms()
        ) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                endTimeMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                endTimeMillis,
                pendingIntent
            )
        }
    }

    fun cancel(timerId: String) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_TIMER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCodeFor(timerId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun cancelAll(timerIds: Collection<String>) {
        timerIds.forEach { cancel(it) }
    }

    private fun requestCodeFor(timerId: String): Int {
        return TIMER_REQUEST_CODE_BASE + (timerId.hashCode() and 0xFFFF)
    }

    companion object {
        const val ACTION_TIMER = "ca.sekhrit.alarmpro.TIMER"
        const val EXTRA_TIMER_ID = "TIMER_ID"
        const val EXTRA_TIMER_LABEL = "TIMER_LABEL"
        const val EXTRA_TIMER_TOTAL_SECONDS = "TIMER_TOTAL_SECONDS"
        const val TIMER_NOTIFICATION_ID_BASE = 9001
        private const val TIMER_REQUEST_CODE_BASE = 9001

        fun notificationIdFor(timerId: String): Int {
            return TIMER_NOTIFICATION_ID_BASE + (timerId.hashCode() and 0xFFFF)
        }
    }
}
