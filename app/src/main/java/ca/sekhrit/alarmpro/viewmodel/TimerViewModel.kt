package ca.sekhrit.alarmpro.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import ca.sekhrit.alarmpro.data.TimerPreset
import ca.sekhrit.alarmpro.data.TimerPresetRepository
import ca.sekhrit.alarmpro.data.TimerRepository
import ca.sekhrit.alarmpro.data.TimerState
import ca.sekhrit.alarmpro.receiver.TimerScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class TimerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TimerRepository(application)
    private val presetRepository = TimerPresetRepository(application)
    private val scheduler = TimerScheduler(application)

    private val _state = MutableStateFlow(refreshState(repository.load()))
    val state: StateFlow<TimerState> = _state.asStateFlow()

    private val _presets = MutableStateFlow(presetRepository.loadPresets())
    val presets: StateFlow<List<TimerPreset>> = _presets.asStateFlow()

    private val _activePresetId = MutableStateFlow<String?>(null)
    val activePresetId: StateFlow<String?> = _activePresetId.asStateFlow()

    private val _finished = MutableStateFlow(false)
    val finished: StateFlow<Boolean> = _finished.asStateFlow()

    private val _finishedLabel = MutableStateFlow("")
    val finishedLabel: StateFlow<String> = _finishedLabel.asStateFlow()

    fun syncFromStorage() {
        _state.value = refreshState(repository.load())
        _presets.value = presetRepository.loadPresets()
    }

    val nextTimerHeader: String?
        get() {
            val current = _state.value
            if (current.totalSeconds <= 0 || current.remainingSeconds <= 0 || current.endTimeMillis <= 0L) {
                return null
            }
            val finishTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(current.endTimeMillis),
                ZoneId.systemDefault()
            )
            val day = finishTime.format(DateTimeFormatter.ofPattern("EEE", Locale.getDefault()))
            val time = finishTime.format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))
            return "$day $time"
        }

    fun startPreset(preset: TimerPreset) {
        _activePresetId.value = preset.id
        startTimer(preset.totalSeconds, preset.label)
    }

    fun togglePreset(preset: TimerPreset, enabled: Boolean) {
        if (enabled) {
            startPreset(preset)
        } else {
            if (_activePresetId.value == preset.id || _state.value.totalSeconds > 0) {
                resetTimer()
            }
        }
    }

    fun restartPreset(preset: TimerPreset) {
        resetTimer()
        startPreset(preset)
    }

    fun addPreset(totalSeconds: Int) {
        if (totalSeconds <= 0) return
        val updated = _presets.value + TimerPreset(totalSeconds = totalSeconds)
        _presets.value = updated
        presetRepository.savePresets(updated)
    }

    fun deletePreset(preset: TimerPreset) {
        if (_activePresetId.value == preset.id) {
            resetTimer()
        }
        val updated = _presets.value.filter { it.id != preset.id }
        _presets.value = updated
        presetRepository.savePresets(updated)
    }

    fun startTimer(totalSeconds: Int, label: String = "") {
        if (totalSeconds <= 0) return
        scheduler.cancel()
        val endTime = System.currentTimeMillis() + totalSeconds * 1000L
        val newState = TimerState(
            totalSeconds = totalSeconds,
            remainingSeconds = totalSeconds,
            endTimeMillis = endTime,
            isRunning = true,
            label = label.trim()
        )
        repository.save(newState)
        scheduler.schedule(endTime, newState.label)
        _state.value = newState
        _finished.value = false
        _finishedLabel.value = ""
    }

    fun pauseTimer() {
        val current = refreshState(_state.value)
        if (!current.isRunning || current.remainingSeconds <= 0) return
        scheduler.cancel()
        val paused = current.copy(isRunning = false)
        repository.save(paused)
        _state.value = paused
    }

    fun resumeTimer() {
        val current = _state.value
        if (current.isRunning || current.remainingSeconds <= 0) return
        val endTime = System.currentTimeMillis() + current.remainingSeconds * 1000L
        val running = current.copy(isRunning = true, endTimeMillis = endTime)
        repository.save(running)
        scheduler.schedule(endTime, running.label)
        _state.value = running
    }

    fun resetTimer() {
        scheduler.cancel()
        repository.clear()
        _state.value = TimerState()
        _activePresetId.value = null
        _finished.value = false
        _finishedLabel.value = ""
    }

    fun markFinished() {
        scheduler.cancel()
        _finishedLabel.value = _state.value.label
        _state.value = _state.value.copy(isRunning = false, remainingSeconds = 0)
        repository.clear()
        _activePresetId.value = null
        _finished.value = true
    }

    fun acknowledgeFinished() {
        _finished.value = false
        _finishedLabel.value = ""
        _state.value = TimerState()
        _activePresetId.value = null
    }

    fun tick() {
        val refreshed = refreshState(_state.value)
        if (refreshed.remainingSeconds <= 0 && refreshed.totalSeconds > 0 && refreshed.isRunning) {
            markFinished()
        } else {
            _state.value = refreshed
        }
    }

    private fun refreshState(state: TimerState): TimerState {
        if (!state.isRunning || state.endTimeMillis <= 0L) {
            return state
        }
        val remaining = ((state.endTimeMillis - System.currentTimeMillis()) / 1000L).toInt()
        return if (remaining <= 0) {
            state.copy(remainingSeconds = 0, isRunning = false)
        } else {
            state.copy(remainingSeconds = remaining)
        }
    }
}
