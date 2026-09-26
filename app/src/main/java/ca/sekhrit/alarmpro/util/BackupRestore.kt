package ca.sekhrit.alarmpro.util

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import ca.sekhrit.alarmpro.data.AlarmRepository
import ca.sekhrit.alarmpro.data.TimerRepository
import ca.sekhrit.alarmpro.receiver.AlarmScheduler
import ca.sekhrit.alarmpro.receiver.TimerScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

object BackupRestore {
    private const val FORMAT = "alarmpro_backup"
    private const val VERSION = 1
    private const val MAIN_PREFS = "alarmpro_prefs"
    private const val STOPWATCH_PREFS = "stopwatch_runtime"
    private val preferenceFiles = listOf(MAIN_PREFS, STOPWATCH_PREFS)

    suspend fun exportData(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val backup = JSONObject()
                .put("format", FORMAT)
                .put("version", VERSION)
                .put("preferences", JSONObject().apply {
                    preferenceFiles.forEach { name ->
                        put(name, encodePreferences(context.getSharedPreferences(name, Context.MODE_PRIVATE)))
                    }
                })

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(backup.toString(2).toByteArray(Charsets.UTF_8))
            } ?: return@withContext false
            true
        } catch (e: Exception) {
            Log.e("BackupRestore", "Error exporting data", e)
            false
        }
    }

    suspend fun importData(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.bufferedReader(Charsets.UTF_8).readText()
            } ?: return@withContext false

            val backup = JSONObject(jsonString)
            val restoredPreferences = decodeBackup(backup)

            // Cancel scheduled work before replacing its saved state.
            val alarmRepo = AlarmRepository(context)
            val alarmScheduler = AlarmScheduler(context)
            alarmRepo.loadAlarms().forEach { alarmScheduler.cancel(it) }

            val timerRepo = TimerRepository(context)
            val timerScheduler = TimerScheduler(context)
            timerRepo.loadAll().forEach { timer -> timerScheduler.cancel(timer.id) }

            preferenceFiles.forEach { name ->
                val preferences = context.getSharedPreferences(name, Context.MODE_PRIVATE)
                val editor = preferences.edit().clear()
                restoredPreferences[name].orEmpty().forEach { (key, value) ->
                    putPreference(editor, key, value)
                }
                check(editor.commit()) { "Could not restore $name" }
            }

            // Recreate scheduled work from the imported alarm and timer data.
            alarmRepo.loadAlarms().filter { it.isEnabled }.forEach { alarmScheduler.schedule(it) }
            timerRepo.loadAll().forEach { timer ->
                if (timer.isRunning && timer.endTimeMillis > 0) {
                    timerScheduler.schedule(timer.id, timer.endTimeMillis, timer.label, timer.totalSeconds)
                }
            }

            true
        } catch (e: Exception) {
            Log.e("BackupRestore", "Error importing data", e)
            false
        }
    }

    private fun encodePreferences(preferences: SharedPreferences): JSONObject = JSONObject().apply {
        preferences.all.forEach { (key, value) ->
            put(key, encodeValue(value))
        }
    }

    private fun encodeValue(value: Any?): JSONObject = JSONObject().apply {
        when (value) {
            is String -> put("type", "string").put("value", value)
            is Boolean -> put("type", "boolean").put("value", value)
            is Int -> put("type", "int").put("value", value)
            is Long -> put("type", "long").put("value", value)
            is Float -> put("type", "float").put("value", value.toDouble())
            is Set<*> -> put("type", "string_set").put(
                "value",
                JSONArray().apply { value.filterIsInstance<String>().sorted().forEach(::put) }
            )
            else -> error("Unsupported preference value: ${value?.javaClass?.name ?: "null"}")
        }
    }

    private fun decodeBackup(backup: JSONObject): Map<String, Map<String, Any>> {
        if (backup.optString("format") != FORMAT) {
            // Older exports were flat JSON objects containing string preferences.
            val legacyValues = mutableMapOf<String, Any>()
            val keys = backup.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val value = backup.get(key)
                require(value is String) { "Unsupported legacy preference value" }
                legacyValues[key] = value
            }
            if (!legacyValues.containsKey("notes_enabled") &&
                (legacyValues["notes_text"] as? String)?.isNotBlank() == true
            ) {
                // Older exports omitted Boolean preferences, although they did include notes_text.
                legacyValues["notes_enabled"] = true
            }
            return mapOf(MAIN_PREFS to legacyValues, STOPWATCH_PREFS to emptyMap())
        }

        require(backup.getInt("version") <= VERSION) { "Backup version is newer than this app" }
        val preferences = backup.getJSONObject("preferences")
        return preferenceFiles.associateWith { name ->
            val values = preferences.optJSONObject(name) ?: JSONObject()
            buildMap {
                val keys = values.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    put(key, decodeValue(values.getJSONObject(key)))
                }
            }
        }
    }

    private fun decodeValue(encoded: JSONObject): Any {
        val value = encoded.get("value")
        return when (encoded.getString("type")) {
            "string" -> value as String
            "boolean" -> value as Boolean
            "int" -> (value as Number).toInt()
            "long" -> (value as Number).toLong()
            "float" -> (value as Number).toFloat()
            "string_set" -> {
                val array = value as JSONArray
                buildSet { for (index in 0 until array.length()) add(array.getString(index)) }
            }
            else -> error("Unsupported preference type")
        }
    }

    private fun putPreference(editor: SharedPreferences.Editor, key: String, value: Any) {
        when (value) {
            is String -> editor.putString(key, value)
            is Boolean -> editor.putBoolean(key, value)
            is Int -> editor.putInt(key, value)
            is Long -> editor.putLong(key, value)
            is Float -> editor.putFloat(key, value)
            is Set<*> -> editor.putStringSet(key, value.filterIsInstance<String>().toSet())
            else -> error("Unsupported preference value")
        }
    }
}
