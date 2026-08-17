package ca.sekhrit.alarmpro.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime

class AlarmRepository(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadAlarms(): List<Alarm> {
        val raw = prefs.getString(KEY_ALARMS, null) ?: return emptyList()
        val array = JSONArray(raw)
        return buildList {
            for (index in 0 until array.length()) {
                add(parseAlarm(array.getJSONObject(index)))
            }
        }
    }

    fun saveAlarms(alarms: List<Alarm>) {
        val array = JSONArray()
        alarms.forEach { alarm ->
            array.put(
                JSONObject().apply {
                    put("id", alarm.id)
                    put("hour", alarm.time.hour)
                    put("minute", alarm.time.minute)
                    put("label", alarm.label)
                    put("enabled", alarm.isEnabled)
                    put("vibrate", alarm.vibrate)
                    put("readLabelAloud", alarm.readLabelAloud)
                    put("snoozeEnabled", alarm.snoozeEnabled)
                    put("snoozeMinutes", alarm.snoozeMinutes ?: -1)
                    put("snoozedUntilEpochMillis", alarm.snoozedUntilEpochMillis ?: -1L)
                    put("skipUntilEpochDay", alarm.skipUntilEpochDay ?: -1)
                    put("soundUri", alarm.soundUri.orEmpty())
                    put("groupId", alarm.groupId.orEmpty())
                    put("createdEpochDay", alarm.createdEpochDay)
                    put("repeat", serializeRepeat(alarm.repeat))
                }
            )
        }
        prefs.edit().putString(KEY_ALARMS, array.toString()).apply()
    }

    private fun parseAlarm(item: JSONObject): Alarm {
        val repeatObject = item.optJSONObject("repeat")
        val repeat = if (repeatObject != null) {
            parseRepeat(repeatObject)
        } else {
            migrateLegacyRepeat(item.optJSONArray("days"))
        }
        return Alarm(
            id = item.getString("id"),
            time = LocalTime.of(item.getInt("hour"), item.getInt("minute")),
            label = item.optString("label", ""),
            isEnabled = item.optBoolean("enabled", true),
            repeat = repeat,
                        vibrate = item.optBoolean("vibrate", true),
                        readLabelAloud = item.optBoolean("readLabelAloud", false),
                        snoozeEnabled = item.optBoolean("snoozeEnabled", true),
                        snoozeMinutes = item.optInt("snoozeMinutes", -1).let { if (it < 0) null else it },
                        snoozedUntilEpochMillis = item.optLong("snoozedUntilEpochMillis", -1L).let { if (it < 0L) null else it },
                        skipUntilEpochDay = item.optLong("skipUntilEpochDay", -1).let { if (it < 0) null else it },
                        soundUri = item.optString("soundUri", "").ifBlank { null },
                        groupId = item.optString("groupId", "").ifBlank { null },
                        createdEpochDay = item.optLong("createdEpochDay", LocalDate.now().toEpochDay())
        )
    }

    private fun migrateLegacyRepeat(daysArray: JSONArray?): RepeatSchedule {
        val days = buildSet {
            if (daysArray != null) {
                for (index in 0 until daysArray.length()) {
                    add(daysArray.getInt(index))
                }
            }
        }
        return when {
            days.isEmpty() -> RepeatSchedule(type = RepeatType.ONCE)
            days.size == 7 -> RepeatSchedule(type = RepeatType.DAILY)
            else -> RepeatSchedule(type = RepeatType.WEEKLY, daysOfWeek = days)
        }
    }

    private fun parseRepeat(json: JSONObject): RepeatSchedule {
        val daysArray = json.optJSONArray("days")
        val days = buildSet {
            if (daysArray != null) {
                for (index in 0 until daysArray.length()) {
                    add(daysArray.getInt(index))
                }
            }
        }
        return RepeatSchedule(
            type = RepeatType.valueOf(json.optString("type", RepeatType.ONCE.name)),
            daysOfWeek = days,
            weekInterval = json.optInt("weekInterval", 2),
            monthInterval = json.optInt("monthInterval", 1),
            dayOfMonth = json.optInt("dayOfMonth", LocalDate.now().dayOfMonth),
            anchorEpochDay = json.optLong("anchorEpochDay", LocalDate.now().toEpochDay())
        )
    }

    private fun serializeRepeat(repeat: RepeatSchedule): JSONObject {
        return JSONObject().apply {
            put("type", repeat.type.name)
            put("weekInterval", repeat.weekInterval)
            put("monthInterval", repeat.monthInterval)
            put("dayOfMonth", repeat.dayOfMonth)
            put("anchorEpochDay", repeat.anchorEpochDay)
            put(
                "days",
                JSONArray().apply {
                    repeat.daysOfWeek.sorted().forEach { put(it) }
                }
            )
        }
    }

    companion object {
        private const val PREFS_NAME = "alarmpro_prefs"
        private const val KEY_ALARMS = "alarms"
    }
}
