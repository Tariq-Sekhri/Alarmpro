package ca.sekhrit.alarmpro.util

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import ca.sekhrit.alarmpro.data.AlarmRepository
import ca.sekhrit.alarmpro.data.TimerRepository
import ca.sekhrit.alarmpro.receiver.AlarmScheduler
import ca.sekhrit.alarmpro.receiver.TimerScheduler

object BackupRestore {
    private const val PREFS_NAME = "alarmpro_prefs"
    
    suspend fun exportData(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = JSONObject()
            
            for ((key, value) in prefs.all) {
                if (value is String) {
                    json.put(key, value)
                }
            }
            
            val jsonString = json.toString(2)
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(jsonString.toByteArray(Charsets.UTF_8))
            }
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

            val json = JSONObject(jsonString)
            
            // Cancel old alarms and timers
            val alarmRepo = AlarmRepository(context)
            val oldAlarms = alarmRepo.loadAlarms()
            val alarmScheduler = AlarmScheduler(context)
            oldAlarms.forEach { alarmScheduler.cancel(it) }

            val timerRepo = TimerRepository(context)
            val oldTimers = timerRepo.loadActiveTimers()
            val timerScheduler = TimerScheduler(context)
            oldTimers.values.forEach { timerScheduler.cancel(it) }

            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val editor = prefs.edit()
            
            editor.clear()
            
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val value = json.getString(key)
                editor.putString(key, value)
            }
            
            editor.commit()
            
            // Schedule new alarms and timers
            val newAlarms = alarmRepo.loadAlarms()
            newAlarms.filter { it.isEnabled }.forEach { alarmScheduler.schedule(it) }

            val newTimers = timerRepo.loadActiveTimers()
            newTimers.values.filter { it.isRunning }.forEach { timerScheduler.schedule(it) }
            
            true
        } catch (e: Exception) {
            Log.e("BackupRestore", "Error importing data", e)
            false
        }
    }
}
