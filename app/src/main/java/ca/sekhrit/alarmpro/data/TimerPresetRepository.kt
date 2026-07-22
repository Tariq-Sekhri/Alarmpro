package ca.sekhrit.alarmpro.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class TimerPresetRepository(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadPresets(): List<TimerPreset> {
        val raw = prefs.getString(KEY_PRESETS, null) ?: return defaultPresets().also { savePresets(it) }
        val array = JSONArray(raw)
        if (array.length() == 0) return defaultPresets().also { savePresets(it) }
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    TimerPreset(
                        id = item.getString("id"),
                        totalSeconds = item.getInt("totalSeconds"),
                        label = item.optString("label", "")
                    )
                )
            }
        }
    }

    fun savePresets(presets: List<TimerPreset>) {
        val array = JSONArray()
        presets.forEach { preset ->
            array.put(
                JSONObject().apply {
                    put("id", preset.id)
                    put("totalSeconds", preset.totalSeconds)
                    put("label", preset.label)
                }
            )
        }
        prefs.edit().putString(KEY_PRESETS, array.toString()).apply()
    }

    private fun defaultPresets(): List<TimerPreset> = listOf(
        TimerPreset(totalSeconds = 10 * 60),
        TimerPreset(totalSeconds = 15 * 60),
        TimerPreset(totalSeconds = 30 * 60),
        TimerPreset(totalSeconds = 60 * 60),
        TimerPreset(totalSeconds = 80 * 60),
        TimerPreset(totalSeconds = 124 * 60)
    )

    companion object {
        private const val PREFS_NAME = "alarmpro_prefs"
        private const val KEY_PRESETS = "timer_presets"
    }
}
