package ca.sekhrit.alarmpro.data

import android.content.Context
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class StopwatchChange(
    val stopwatchId: String,
    val state: StopwatchRuntimeState
)

/**
 * The single durable state for a stopwatch session.
 *
 * Actions from the notification and PiP can arrive while no Compose screen (or
 * ViewModel) exists, so they must not depend on an in-memory UI instance.
 */
data class StopwatchRuntimeState(
    val elapsedMs: Long = 0L,
    val isRunning: Boolean = false,
    val startedAtMillis: Long = 0L
) {
    fun liveElapsedMs(nowMillis: Long = System.currentTimeMillis()): Long =
        if (isRunning && startedAtMillis > 0L) {
            elapsedMs + (nowMillis - startedAtMillis).coerceAtLeast(0L)
        } else {
            elapsedMs
        }
}

class StopwatchStateRepository(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(stopwatchId: String): StopwatchRuntimeState {
        val prefix = "$stopwatchId."
        return StopwatchRuntimeState(
            elapsedMs = prefs.getLong(prefix + KEY_ELAPSED, 0L),
            isRunning = prefs.getBoolean(prefix + KEY_RUNNING, false),
            startedAtMillis = prefs.getLong(prefix + KEY_STARTED_AT, 0L)
        )
    }

    fun save(stopwatchId: String, state: StopwatchRuntimeState) {
        val prefix = "$stopwatchId."
        prefs.edit()
            .putLong(prefix + KEY_ELAPSED, state.elapsedMs)
            .putBoolean(prefix + KEY_RUNNING, state.isRunning)
            .putLong(prefix + KEY_STARTED_AT, state.startedAtMillis)
            .commit()
        _changes.tryEmit(StopwatchChange(stopwatchId, state))
    }

    fun clear(stopwatchId: String) {
        val prefix = "$stopwatchId."
        prefs.edit()
            .remove(prefix + KEY_ELAPSED)
            .remove(prefix + KEY_RUNNING)
            .remove(prefix + KEY_STARTED_AT)
            .commit()
        _changes.tryEmit(StopwatchChange(stopwatchId, StopwatchRuntimeState()))
    }

    companion object {
        private const val PREFS_NAME = "stopwatch_runtime"
        private const val KEY_ELAPSED = "elapsed_ms"
        private const val KEY_RUNNING = "running"
        private const val KEY_STARTED_AT = "started_at_ms"

        private val _changes = MutableSharedFlow<StopwatchChange>(extraBufferCapacity = 64)
        val changes: SharedFlow<StopwatchChange> = _changes.asSharedFlow()
    }
}
