package ca.sekhrit.alarmpro.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

class TimerScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(endTimeMillis: Long, label: String) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_TIMER
            putExtra(EXTRA_TIMER_LABEL, label)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            TIMER_REQUEST_CODE,
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

    fun cancel() {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_TIMER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            TIMER_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    companion object {
        const val ACTION_TIMER = "ca.sekhrit.alarmpro.TIMER"
        const val EXTRA_TIMER_LABEL = "TIMER_LABEL"
        const val TIMER_NOTIFICATION_ID = 9001
        private const val TIMER_REQUEST_CODE = 9001
    }
}
