package ca.sekhrit.alarmpro.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ca.sekhrit.alarmpro.data.AlarmRepository
import ca.sekhrit.alarmpro.data.TimerRepository

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }

        val alarmScheduler = AlarmScheduler(context)
        AlarmRepository(context).loadAlarms()
            .filter { it.isEnabled }
            .forEach { alarmScheduler.schedule(it) }

        val timerScheduler = TimerScheduler(context)
        val timerRepository = TimerRepository(context)
        val now = System.currentTimeMillis()
        val activeTimers = timerRepository.loadAll()
        val stillRunning = activeTimers.filter { timer ->
            timer.isActive()
        }
        val expired = activeTimers.filter { timer ->
            timer.endTimeMillis > 0L && !timer.isActive()
        }
        if (expired.isNotEmpty()) {
            timerRepository.saveAll(stillRunning)
        }
        stillRunning.forEach { timer ->
            timerScheduler.schedule(timer.id, timer.endTimeMillis, timer.label, timer.totalSeconds)
            NotificationHelper.showActiveTimerNotification(context, timer)
        }
    }
}
