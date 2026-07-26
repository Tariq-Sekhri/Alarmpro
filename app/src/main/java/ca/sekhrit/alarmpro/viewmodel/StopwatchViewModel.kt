package ca.sekhrit.alarmpro.viewmodel

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ca.sekhrit.alarmpro.receiver.NotificationHelper
import ca.sekhrit.alarmpro.util.TimeUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap

data class LapEntry(
    val number: Int,
    val lapTimeMs: Long,
    val totalTimeMs: Long
)

data class StopwatchMark(
    val id: String = UUID.randomUUID().toString(),
    val targetMs: Long,
    val triggered: Boolean = false
) {
    val label: String get() = TimeUtils.formatStopwatch(targetMs)
}

data class StopwatchAlertEvent(
    val markId: String,
    val label: String
)

data class StopwatchUiState(
    val elapsedMs: Long = 0L,
    val isRunning: Boolean = false,
    val laps: List<LapEntry> = emptyList(),
    val marks: List<StopwatchMark> = emptyList(),
    val alertEvent: StopwatchAlertEvent? = null
)

class StopwatchViewModel(application: Application) : AndroidViewModel(application) {
    val stopwatchId: String = UUID.randomUUID().toString()
    private var startElapsedRealtime = 0L
    private var accumulatedMs = 0L
    private var lastLapTotalMs = 0L
    private var lastNotificationSecond = -1L
    private var tickJob: Job? = null

    private val _state = MutableStateFlow(StopwatchUiState())
    val state: StateFlow<StopwatchUiState> = _state.asStateFlow()

    init {
        instances[stopwatchId] = WeakReference(this)
    }

    private fun currentElapsedMs(): Long {
        return if (_state.value.isRunning) {
            accumulatedMs + (SystemClock.elapsedRealtime() - startElapsedRealtime)
        } else {
            accumulatedMs
        }
    }

    fun tick() {
        if (!_state.value.isRunning) return
        val elapsed = currentElapsedMs()
        _state.value = _state.value.copy(elapsedMs = elapsed)
        val elapsedSecond = elapsed / 1000L
        if (elapsedSecond != lastNotificationSecond) {
            lastNotificationSecond = elapsedSecond
            NotificationHelper.showActiveStopwatchNotification(getApplication(), stopwatchId, elapsed)
        }
        checkMarks(elapsed)
    }

    private fun checkMarks(elapsed: Long) {
        val pending = _state.value.marks.filter { !it.triggered && elapsed >= it.targetMs }
        if (pending.isEmpty()) return

        val triggeredMark = pending.minByOrNull { it.targetMs } ?: return
        val updatedMarks = _state.value.marks.map {
            if (it.id == triggeredMark.id) it.copy(triggered = true) else it
        }
        _state.value = _state.value.copy(
            marks = updatedMarks,
            alertEvent = StopwatchAlertEvent(triggeredMark.id, triggeredMark.label)
        )
        NotificationHelper.showStopwatchMarkNotification(
            getApplication(),
            triggeredMark.label
        )
    }

    fun startPause() {
        if (_state.value.isRunning) {
            accumulatedMs = currentElapsedMs()
            tickJob?.cancel()
            tickJob = null
            _state.value = _state.value.copy(isRunning = false, elapsedMs = accumulatedMs)
            lastNotificationSecond = -1L
            NotificationHelper.cancelActiveStopwatchNotification(getApplication(), stopwatchId)
        } else {
            startElapsedRealtime = SystemClock.elapsedRealtime()
            lastNotificationSecond = accumulatedMs / 1000L
            _state.value = _state.value.copy(isRunning = true)
            NotificationHelper.showActiveStopwatchNotification(getApplication(), stopwatchId, accumulatedMs)
            tickJob?.cancel()
            tickJob = viewModelScope.launch {
                while (true) {
                    tick()
                    delay(10)
                }
            }
        }
    }

    fun stop() {
        if (_state.value.isRunning) startPause()
    }

    fun lap() {
        if (!_state.value.isRunning) return
        val total = currentElapsedMs()
        val lapTime = total - lastLapTotalMs
        val lapNumber = _state.value.laps.size + 1
        val entry = LapEntry(lapNumber, lapTime, total)
        lastLapTotalMs = total
        _state.value = _state.value.copy(
            elapsedMs = total,
            laps = listOf(entry) + _state.value.laps
        )
        NotificationHelper.showActiveStopwatchNotification(getApplication(), stopwatchId, total)
    }

    fun addMark(totalMinutes: Int) {
        if (totalMinutes <= 0) return
        val targetMs = totalMinutes * 60_000L
        if (_state.value.marks.any { it.targetMs == targetMs }) return
        _state.value = _state.value.copy(
            marks = (_state.value.marks + StopwatchMark(targetMs = targetMs))
                .sortedBy { it.targetMs }
        )
    }

    fun addCustomMark(hours: Int, minutes: Int, seconds: Int) {
        val targetMs = (hours * 3600L + minutes * 60L + seconds) * 1000L
        if (targetMs <= 0L) return
        addMarkFromMs(targetMs)
    }

    private fun addMarkFromMs(targetMs: Long) {
        if (_state.value.marks.any { it.targetMs == targetMs }) return
        _state.value = _state.value.copy(
            marks = (_state.value.marks + StopwatchMark(targetMs = targetMs))
                .sortedBy { it.targetMs }
        )
        checkMarks(currentElapsedMs())
    }

    fun removeMark(id: String) {
        _state.value = _state.value.copy(
            marks = _state.value.marks.filter { it.id != id }
        )
    }

    fun acknowledgeAlert() {
        _state.value = _state.value.copy(alertEvent = null)
    }

    fun reset() {
        tickJob?.cancel()
        tickJob = null
        startElapsedRealtime = 0L
        accumulatedMs = 0L
        lastLapTotalMs = 0L
        lastNotificationSecond = -1L
        _state.value = _state.value.copy(
            elapsedMs = 0L,
            isRunning = false,
            laps = emptyList(),
            marks = _state.value.marks.map { it.copy(triggered = false) },
            alertEvent = null
        )
        NotificationHelper.cancelActiveStopwatchNotification(getApplication(), stopwatchId)
    }

    override fun onCleared() {
        tickJob?.cancel()
        instances.remove(stopwatchId)
        NotificationHelper.cancelActiveStopwatchNotification(getApplication(), stopwatchId)
        super.onCleared()
    }

    companion object {
        private val instances = ConcurrentHashMap<String, WeakReference<StopwatchViewModel>>()

        fun instance(id: String): StopwatchViewModel? = instances[id]?.get()
    }
}
