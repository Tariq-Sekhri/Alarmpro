package ca.sekhrit.alarmpro.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ca.sekhrit.alarmpro.data.TimerPreset
import ca.sekhrit.alarmpro.data.TimerPresetRepository
import ca.sekhrit.alarmpro.data.TimerRepository
import ca.sekhrit.alarmpro.data.TimerState
import ca.sekhrit.alarmpro.receiver.TimerScheduler
import ca.sekhrit.alarmpro.receiver.NotificationHelper
import ca.sekhrit.alarmpro.util.TimeUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicReference

class TimerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TimerRepository(application)
    private val presetRepository = TimerPresetRepository(application)
    private val scheduler = TimerScheduler(application)

    private val _activeTimers = MutableStateFlow<Map<String, TimerState>>(emptyMap())
    val activeTimers: StateFlow<Map<String, TimerState>> = _activeTimers.asStateFlow()

    private val _presets = MutableStateFlow(presetRepository.loadPresets())
    val presets: StateFlow<List<TimerPreset>> = _presets.asStateFlow()

    private val _clockMillis = MutableStateFlow(System.currentTimeMillis())
    val clockMillis: StateFlow<Long> = _clockMillis.asStateFlow()

    private var tickerJob: Job? = null
    private val notificationSeconds = mutableMapOf<String, Int>()

    init {
        instance.set(WeakReference(this))
        syncFromStorage()
        ensureTicker()
    }

    fun syncFromStorage() {
        val refreshed = repository.loadAll().map { refreshState(it) }
        _activeTimers.value = refreshed.associateBy { timerKey(it) }
        _presets.value = presetRepository.loadPresets()
        ensureTicker()
    }

    val nextTimerHeader: String?
        get() {
            val next = _activeTimers.value.values
                .filter { it.isActive(_clockMillis.value) }
                .minByOrNull { it.endTimeMillis }
                ?: return null
            val remaining = TimeUtils.formatDuration(next.liveRemainingSeconds(_clockMillis.value).toLong())
            val total = TimeUtils.formatDuration(next.totalSeconds.toLong())
            return "$remaining / $total"
        }

    fun startPreset(preset: TimerPreset) {
        stopPreset(preset, persist = false)
        val endTime = System.currentTimeMillis() + preset.totalSeconds * 1000L
        val label = preset.label.ifBlank { formatPresetLabel(preset.totalSeconds) }
        val newState = TimerState(
            presetId = preset.id,
            totalSeconds = preset.totalSeconds,
            remainingSeconds = preset.totalSeconds,
            endTimeMillis = endTime,
            isRunning = true,
            label = label
        )
        scheduler.schedule(newState.id, endTime, newState.label, preset.totalSeconds)
        updateActiveTimer(preset.id, newState)
        NotificationHelper.showActiveTimerNotification(getApplication(), newState)
        notificationSeconds[preset.id] = newState.remainingSeconds
        ensureTicker()
    }

    fun togglePreset(preset: TimerPreset, enabled: Boolean) {
        if (enabled) {
            resumePreset(preset)
        } else {
            pausePreset(preset)
        }
    }

    fun pausePreset(preset: TimerPreset) {
        val current = _activeTimers.value[preset.id] ?: return
        val remainingSeconds = current.liveRemainingSeconds()
        scheduler.cancel(current.id)
        notificationSeconds.remove(preset.id)
        val paused = current.copy(remainingSeconds = remainingSeconds, endTimeMillis = 0L, isRunning = false)
        updateActiveTimer(
            preset.id,
            paused
        )
        NotificationHelper.showPausedTimerNotification(getApplication(), paused)
        ensureTicker()
    }

    fun resumePreset(preset: TimerPreset) {
        val current = _activeTimers.value[preset.id]
        if (current == null || current.remainingSeconds <= 0) {
            startPreset(preset)
            return
        }
        val endTime = System.currentTimeMillis() + current.remainingSeconds * 1000L
        val resumed = current.copy(endTimeMillis = endTime, isRunning = true)
        scheduler.schedule(resumed.id, endTime, resumed.label, resumed.totalSeconds)
        updateActiveTimer(preset.id, resumed)
        NotificationHelper.showActiveTimerNotification(getApplication(), resumed)
        notificationSeconds[preset.id] = resumed.remainingSeconds
        ensureTicker()
    }

    fun restartPreset(preset: TimerPreset) {
        startPreset(preset)
    }

    fun addPreset(totalSeconds: Int, label: String = "") {
        if (totalSeconds <= 0) return
        val updated = _presets.value + TimerPreset(totalSeconds = totalSeconds, label = label.trim())
        _presets.value = updated
        presetRepository.savePresets(updated)
    }

    fun updatePreset(preset: TimerPreset, totalSeconds: Int, label: String) {
        if (totalSeconds <= 0) return
        val trimmed = label.trim()
        val updatedPreset = preset.copy(totalSeconds = totalSeconds, label = trimmed)
        val updated = _presets.value.map { if (it.id == preset.id) updatedPreset else it }
        _presets.value = updated
        presetRepository.savePresets(updated)

        val active = _activeTimers.value[preset.id] ?: return
        if (!active.isActive() && active.isRunning) return
        if (totalSeconds != preset.totalSeconds) {
            startPreset(updatedPreset)
            return
        }
        val displayLabel = trimmed.ifBlank { formatPresetLabel(totalSeconds) }
        if (active.isActive()) {
            scheduler.cancel(active.id)
            scheduler.schedule(active.id, active.endTimeMillis, displayLabel, totalSeconds)
        }
        updateActiveTimer(preset.id, active.copy(label = displayLabel))
    }

    fun deletePreset(preset: TimerPreset) {
        deletePresets(setOf(preset.id))
    }

    fun deletePresets(presetIds: Set<String>) {
        if (presetIds.isEmpty()) return
        _presets.value
            .filter { it.id in presetIds }
            .forEach { stopPreset(it, persist = false) }
        val updated = _presets.value.filterNot { it.id in presetIds }
        _presets.value = updated
        presetRepository.savePresets(updated)
        persistActiveTimers()
    }

    fun stopPreset(preset: TimerPreset, persist: Boolean = true) {
        val current = _activeTimers.value[preset.id] ?: return
        scheduler.cancel(current.id)
        NotificationHelper.cancelTimerNotification(getApplication(), current.id)
        notificationSeconds.remove(preset.id)
        val updated = _activeTimers.value.toMutableMap()
        updated.remove(preset.id)
        _activeTimers.value = updated
        if (persist) {
            persistActiveTimers()
        }
        ensureTicker()
    }

    private fun ensureTicker() {
        val hasActive = _activeTimers.value.values.any { it.isActive() }
        if (!hasActive) {
            tickerJob?.cancel()
            tickerJob = null
            return
        }
        if (tickerJob?.isActive == true) return
        tickerJob = viewModelScope.launch {
            while (isActive) {
                val now = System.currentTimeMillis()
                _clockMillis.value = now
                tick(now)
                if (_activeTimers.value.values.none { it.isActive(now) }) {
                    break
                }
                delay(200)
            }
            tickerJob = null
        }
    }

    private fun tick(now: Long = System.currentTimeMillis()) {
        val current = _activeTimers.value
        if (current.isEmpty()) return

        val refreshed = current.mapValues { (_, state) -> refreshState(state, now) }.toMutableMap()
        refreshed.forEach { (key, state) ->
            if (state.isActive(now) && notificationSeconds[key] != state.remainingSeconds) {
                notificationSeconds[key] = state.remainingSeconds
                NotificationHelper.showActiveTimerNotification(getApplication(), state)
            }
        }
        val finishedEntries = refreshed.filter { (_, state) ->
            state.totalSeconds > 0 && state.endTimeMillis > 0L && !state.isActive(now)
        }

        if (finishedEntries.isEmpty()) {
            if (refreshed != current) {
                _activeTimers.value = refreshed
            }
            return
        }

        finishedEntries.forEach { (key, state) ->
            NotificationHelper.cancelTimerNotification(getApplication(), state.id)
            notificationSeconds.remove(key)
            refreshed.remove(key)
        }
        _activeTimers.value = refreshed
        persistActiveTimers()
    }

    private fun updateActiveTimer(key: String, state: TimerState) {
        val updated = _activeTimers.value.toMutableMap()
        updated[key] = state
        _activeTimers.value = updated
        persistActiveTimers()
    }

    private fun persistActiveTimers() {
        repository.saveAll(_activeTimers.value.values.toList())
    }

    private fun timerKey(state: TimerState): String {
        return state.presetId ?: state.id
    }

    private fun formatPresetLabel(totalSeconds: Int): String {
        val minutes = totalSeconds / 60
        return if (minutes >= 60) {
            "${minutes / 60} hr timer"
        } else if (minutes > 0) {
            "$minutes min timer"
        } else {
            "$totalSeconds sec timer"
        }
    }

    private fun refreshState(state: TimerState, now: Long = System.currentTimeMillis()): TimerState {
        if (!state.isRunning || state.endTimeMillis <= 0L) {
            return state.copy(isRunning = false, endTimeMillis = 0L)
        }
        val remaining = state.liveRemainingSeconds(now)
        return if (remaining <= 0) {
            state.copy(remainingSeconds = 0, isRunning = false)
        } else {
            state.copy(remainingSeconds = remaining, isRunning = true)
        }
    }

    override fun onCleared() {
        if (instance.get()?.get() === this) {
            instance.set(null)
        }
        super.onCleared()
    }

    companion object {
        private val instance = AtomicReference<WeakReference<TimerViewModel>?>(null)

        fun instance(): TimerViewModel? = instance.get()?.get()
    }
}
