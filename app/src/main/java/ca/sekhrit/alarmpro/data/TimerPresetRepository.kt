package ca.sekhrit.alarmpro.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class TimerPresetRepository(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadPresets(): List<TimerPreset> {
        val raw = prefs.getString(KEY_PRESETS, null) ?: return emptyList()
        val array = JSONArray(raw)
        if (array.length() == 0) return emptyList()
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

    companion object {
        private const val PREFS_NAME = "alarmpro_prefs"
        private const val KEY_PRESETS = "timer_presets"
    }
}
