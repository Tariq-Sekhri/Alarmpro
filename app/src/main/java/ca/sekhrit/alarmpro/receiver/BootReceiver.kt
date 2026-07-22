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

        val timerState = TimerRepository(context).load()
        if (timerState.isRunning && timerState.endTimeMillis > System.currentTimeMillis()) {
            TimerScheduler(context).schedule(timerState.endTimeMillis, timerState.label)
        } else if (timerState.isRunning) {
            TimerRepository(context).clear()
        }
    }
}
