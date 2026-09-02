package ca.sekhrit.alarmpro.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ca.sekhrit.alarmpro.MainActivity
import ca.sekhrit.alarmpro.data.StopwatchRuntimeState
import ca.sekhrit.alarmpro.data.StopwatchStateRepository
import ca.sekhrit.alarmpro.receiver.NotificationHelper
import ca.sekhrit.alarmpro.viewmodel.StopwatchViewModel

class StopwatchActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val stopwatchId = intent.getStringExtra(EXTRA_STOPWATCH_ID) ?: return
        val viewModel = StopwatchViewModel.instance(stopwatchId)
        if (viewModel != null) {
            when (intent.action) {
                ACTION_TOGGLE -> viewModel.startPause()
                ACTION_ADD_LAP -> viewModel.lap()
                ACTION_STOP -> viewModel.stop()
                ACTION_RESET -> viewModel.reset()
                else -> return
            }
            val ui = viewModel.state.value
            MainActivity.notifyStopwatchPipChanged(
                stopwatchId,
                StopwatchRuntimeState(
                    elapsedMs = ui.elapsedMs,
                    isRunning = ui.isRunning
                )
            )
            return
        }

        // No UI instance is alive: mutate the same persisted state directly.
        val repository = StopwatchStateRepository(context)
        val current = repository.load(stopwatchId)
        when (intent.action) {
            ACTION_TOGGLE -> {
                val next = if (current.isRunning) {
                    StopwatchRuntimeState(elapsedMs = current.liveElapsedMs())
                } else {
                    StopwatchRuntimeState(current.elapsedMs, true, System.currentTimeMillis())
                }
                repository.save(stopwatchId, next)
                StopwatchViewModel.instance(stopwatchId)?.syncFromStorage()
                MainActivity.notifyStopwatchPipChanged(stopwatchId, next)
                val elapsed = next.liveElapsedMs()
                if (next.isRunning) {
                    NotificationHelper.showActiveStopwatchNotification(context, stopwatchId, elapsed)
                } else {
                    NotificationHelper.showPausedStopwatchNotification(context, stopwatchId, elapsed)
                }
            }
            ACTION_STOP, ACTION_RESET -> {
                repository.clear(stopwatchId)
                StopwatchViewModel.instance(stopwatchId)?.syncFromStorage()
                MainActivity.notifyStopwatchPipChanged(stopwatchId, StopwatchRuntimeState())
                NotificationHelper.cancelActiveStopwatchNotification(context, stopwatchId)
            }
            ACTION_ADD_LAP -> Unit // Laps are UI history; keep the running state unchanged.
            else -> return
        }
    }

    companion object {
        const val ACTION_TOGGLE = "ca.sekhrit.alarmpro.TOGGLE_STOPWATCH"
        const val ACTION_ADD_LAP = "ca.sekhrit.alarmpro.ADD_STOPWATCH_LAP"
        const val ACTION_STOP = "ca.sekhrit.alarmpro.STOP_STOPWATCH"
        const val ACTION_RESET = "ca.sekhrit.alarmpro.RESET_STOPWATCH"
        const val EXTRA_STOPWATCH_ID = "STOPWATCH_ID"
    }
}
