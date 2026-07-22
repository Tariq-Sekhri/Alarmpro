package ca.sekhrit.alarmpro.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class TimerRepository(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadAll(): List<TimerState> {
        val raw = prefs.getString(KEY_ACTIVE, null)
        if (raw != null) {
            return parseArray(JSONArray(raw))
        }
        return migrateLegacyTimer()
    }

    fun saveAll(timers: List<TimerState>) {
        val array = JSONArray()
        timers.forEach { timer -> array.put(timerToJson(timer)) }
        prefs.edit().putString(KEY_ACTIVE, array.toString()).apply()
        clearLegacyKeys()
    }

    fun removeTimer(timerId: String) {
        val updated = loadAll().filter { it.id != timerId }
        if (updated.isEmpty()) {
            prefs.edit().remove(KEY_ACTIVE).apply()
        } else {
            saveAll(updated)
        }
    }

    fun clear() {
        prefs.edit()
            .remove(KEY_ACTIVE)
            .apply()
        clearLegacyKeys()
    }

    private fun migrateLegacyTimer(): List<TimerState> {
        if (!prefs.contains(KEY_TOTAL) && !prefs.contains(KEY_RUNNING)) {
            return emptyList()
        }
        val legacy = TimerState(
            totalSeconds = prefs.getInt(KEY_TOTAL, 0),
            remainingSeconds = prefs.getInt(KEY_REMAINING, 0),
            endTimeMillis = prefs.getLong(KEY_END, 0L),
            isRunning = prefs.getBoolean(KEY_RUNNING, false),
            label = prefs.getString(KEY_LABEL, "").orEmpty()
        )
        clearLegacyKeys()
        if (legacy.totalSeconds <= 0) {
            return emptyList()
        }
        saveAll(listOf(legacy))
        return listOf(legacy)
    }

    private fun parseArray(array: JSONArray): List<TimerState> {
        return buildList {
            for (index in 0 until array.length()) {
                add(jsonToTimer(array.getJSONObject(index)))
            }
        }
    }

    private fun timerToJson(timer: TimerState): JSONObject {
        return JSONObject().apply {
            put("id", timer.id)
            if (timer.presetId != null) {
                put("presetId", timer.presetId)
            }
            put("totalSeconds", timer.totalSeconds)
            put("remainingSeconds", timer.remainingSeconds)
            put("endTimeMillis", timer.endTimeMillis)
            put("isRunning", timer.isRunning)
            put("label", timer.label)
        }
    }

    private fun jsonToTimer(item: JSONObject): TimerState {
        return TimerState(
            id = item.getString("id"),
            presetId = item.optString("presetId").takeIf { it.isNotBlank() },
            totalSeconds = item.getInt("totalSeconds"),
            remainingSeconds = item.getInt("remainingSeconds"),
            endTimeMillis = item.getLong("endTimeMillis"),
            isRunning = item.getBoolean("isRunning"),
            label = item.optString("label", "")
        )
    }

    private fun clearLegacyKeys() {
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
        private const val KEY_ACTIVE = "timer_active"
        private const val KEY_TOTAL = "timer_total"
        private const val KEY_REMAINING = "timer_remaining"
        private const val KEY_END = "timer_end"
        private const val KEY_RUNNING = "timer_running"
        private const val KEY_LABEL = "timer_label"
    }
}
