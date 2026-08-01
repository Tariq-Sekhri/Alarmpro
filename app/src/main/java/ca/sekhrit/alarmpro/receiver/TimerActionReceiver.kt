package ca.sekhrit.alarmpro.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ca.sekhrit.alarmpro.data.TimerRepository
import ca.sekhrit.alarmpro.viewmodel.TimerViewModel

class TimerActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val timerId = intent.getStringExtra(TimerScheduler.EXTRA_TIMER_ID) ?: return
        val repository = TimerRepository(context)
        val timers = repository.loadAll()
        val timer = timers.find { it.id == timerId } ?: run {
            NotificationHelper.cancelTimerNotification(context, timerId)
            return
        }

        when (intent.action) {
            ACTION_PAUSE -> {
                val remaining = timer.liveRemainingSeconds()
                TimerScheduler(context).cancel(timerId)
                val paused = timer.copy(
                    remainingSeconds = remaining,
                    endTimeMillis = 0L,
                    isRunning = false
                )
                saveTimer(repository, timers, paused)
                NotificationHelper.showPausedTimerNotification(context, paused)
            }
            ACTION_RESUME -> {
                if (timer.remainingSeconds <= 0) return
                val resumed = timer.copy(
                    endTimeMillis = System.currentTimeMillis() + timer.remainingSeconds * 1000L,
                    isRunning = true
                )
                TimerScheduler(context).schedule(
                    resumed.id,
                    resumed.endTimeMillis,
                    resumed.label,
                    resumed.totalSeconds
                )
                saveTimer(repository, timers, resumed)
                NotificationHelper.showActiveTimerNotification(context, resumed)
            }
            ACTION_CLOSE -> {
                TimerScheduler(context).cancel(timerId)
                repository.removeTimer(timerId)
                NotificationHelper.cancelTimerNotification(context, timerId)
                TimerViewModel.instance()?.syncFromStorage()
            }
        }
    }

    private fun saveTimer(repository: TimerRepository, timers: List<ca.sekhrit.alarmpro.data.TimerState>, updated: ca.sekhrit.alarmpro.data.TimerState) {
        repository.saveAll(timers.map { if (it.id == updated.id) updated else it })
        TimerViewModel.instance()?.syncFromStorage()
    }

    companion object {
        const val ACTION_PAUSE = "ca.sekhrit.alarmpro.PAUSE_TIMER"
        const val ACTION_RESUME = "ca.sekhrit.alarmpro.RESUME_TIMER"
        const val ACTION_CLOSE = "ca.sekhrit.alarmpro.CLOSE_TIMER"
    }
}
