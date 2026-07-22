package ca.sekhrit.alarmpro.data

import android.content.Context

class TimerRepository(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): TimerState {
        return TimerState(
            totalSeconds = prefs.getInt(KEY_TOTAL, 0),
            remainingSeconds = prefs.getInt(KEY_REMAINING, 0),
            endTimeMillis = prefs.getLong(KEY_END, 0L),
            isRunning = prefs.getBoolean(KEY_RUNNING, false),
            label = prefs.getString(KEY_LABEL, "").orEmpty()
        )
    }

    fun save(state: TimerState) {
        prefs.edit()
            .putInt(KEY_TOTAL, state.totalSeconds)
            .putInt(KEY_REMAINING, state.remainingSeconds)
            .putLong(KEY_END, state.endTimeMillis)
            .putBoolean(KEY_RUNNING, state.isRunning)
            .putString(KEY_LABEL, state.label)
            .apply()
    }

    fun clear() {
        prefs.edit()
            .remove(KEY_TOTAL)
            .remove(KEY_REMAINING)
            .remove(KEY_END)
            .remove(KEY_RUNNING)
            .remove(KEY_LABEL)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "alarmpro_prefs"
        private const val KEY_TOTAL = "timer_total"
        private const val KEY_REMAINING = "timer_remaining"
        private const val KEY_END = "timer_end"
        private const val KEY_RUNNING = "timer_running"
        private const val KEY_LABEL = "timer_label"
    }
}
