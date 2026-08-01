package ca.sekhrit.alarmpro.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ca.sekhrit.alarmpro.viewmodel.StopwatchViewModel

class StopwatchActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_TOGGLE -> intent.getStringExtra(EXTRA_STOPWATCH_ID)?.let {
                StopwatchViewModel.instance(it)?.startPause()
            }
            ACTION_ADD_LAP -> intent.getStringExtra(EXTRA_STOPWATCH_ID)?.let {
                StopwatchViewModel.instance(it)?.lap()
            }
            ACTION_STOP -> intent.getStringExtra(EXTRA_STOPWATCH_ID)?.let {
                StopwatchViewModel.instance(it)?.stop()
            }
        }
    }

    companion object {
        const val ACTION_TOGGLE = "ca.sekhrit.alarmpro.TOGGLE_STOPWATCH"
        const val ACTION_ADD_LAP = "ca.sekhrit.alarmpro.ADD_STOPWATCH_LAP"
        const val ACTION_STOP = "ca.sekhrit.alarmpro.STOP_STOPWATCH"
        const val EXTRA_STOPWATCH_ID = "STOPWATCH_ID"
    }
}
