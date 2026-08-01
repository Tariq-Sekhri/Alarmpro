package ca.sekhrit.alarmpro.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class StopwatchAlertPresetRepository(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _presets = MutableStateFlow(loadPresets())
    val presets: StateFlow<List<Int>> = _presets.asStateFlow()

    private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == KEY_PRESETS) {
            _presets.value = loadPresets()
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(preferenceListener)
    }

    fun savePresets(seconds: List<Int>) {
        val cleaned = seconds
            .filter { it > 0 }
            .distinct()

        prefs.edit()
            .putString(KEY_PRESETS, cleaned.joinToString(","))
            .apply()
    }

    fun resetToDefault() {
        prefs.edit()
            .remove(KEY_PRESETS)
            .remove(KEY_LEGACY_MINUTE_PRESETS)
            .apply()
    }

    fun close() {
        prefs.unregisterOnSharedPreferenceChangeListener(preferenceListener)
    }

    private fun loadPresets(): List<Int> {
        val stored = prefs.getString(KEY_PRESETS, null)
        if (stored == null) {
            return loadLegacyMinutePresets()
        }
        val presets = stored
            .split(',')
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it > 0 }
            .distinct()
        return presets
    }

    private fun loadLegacyMinutePresets(): List<Int> {
        val stored = prefs.getString(KEY_LEGACY_MINUTE_PRESETS, null) ?: return DEFAULT_PRESETS
        val presets = stored
            .split(',')
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it > 0 }
            .distinct()
            .map { it * 60 }
        return presets.ifEmpty { DEFAULT_PRESETS }
    }

    companion object {
        val DEFAULT_PRESETS = listOf(5 * 60, 15 * 60, 30 * 60, 60 * 60, 2 * 60 * 60)

        private const val PREFS_NAME = "alarmpro_prefs"
        private const val KEY_PRESETS = "stopwatch_alert_presets_seconds"
        private const val KEY_LEGACY_MINUTE_PRESETS = "stopwatch_alert_presets"
    }
}
