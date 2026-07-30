package ca.sekhrit.alarmpro.data

import android.content.Context

class SettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): AppSettings {
        return AppSettings(
            defaultSnoozeEnabled = prefs.getBoolean(KEY_DEFAULT_SNOOZE_ENABLED, true),
            defaultSnoozeMinutes = prefs.getInt(KEY_DEFAULT_SNOOZE_MINUTES, prefs.getInt(KEY_LEGACY_SNOOZE, 10)),
            defaultVibrate = prefs.getBoolean(KEY_DEFAULT_VIBRATE, prefs.getBoolean(KEY_LEGACY_VIBRATION, true)),
            defaultReadLabelAloud = prefs.getBoolean(KEY_DEFAULT_READ_ALOUD, false),
            defaultAlarmSoundUri = prefs.getString(KEY_DEFAULT_ALARM_SOUND, null)?.ifBlank { null },
            use24HourFormat = prefs.getBoolean(KEY_24H, false),
            timerSpeechFormat = TimerSpeechFormat.fromStored(prefs.getString(KEY_TIMER_SPEECH, null)),
            upcomingAlarmLeadMinutes = prefs.getInt(KEY_UPCOMING_ALARM_LEAD, 60),
            timePickerStyle = try {
                TimePickerStyle.valueOf(prefs.getString(KEY_TIME_PICKER_STYLE, TimePickerStyle.ANALOG.name) ?: TimePickerStyle.ANALOG.name)
            } catch (e: Exception) {
                TimePickerStyle.ANALOG
            },
            timerControlStyle = try {
                TimerControlStyle.valueOf(prefs.getString(KEY_TIMER_CONTROL_STYLE, TimerControlStyle.SWITCH.name) ?: TimerControlStyle.SWITCH.name)
            } catch (e: Exception) {
                TimerControlStyle.SWITCH
            },
            silentNotifications = prefs.getBoolean(KEY_SILENT_NOTIFICATIONS, false),
            defaultAlarmSortMode = try {
                AlarmSortMode.valueOf(prefs.getString(KEY_DEFAULT_ALARM_SORT_MODE, AlarmSortMode.TIME_OF_DAY.name) ?: AlarmSortMode.TIME_OF_DAY.name)
            } catch (e: Exception) {
                AlarmSortMode.TIME_OF_DAY
            },
            activeAlarmsFirst = prefs.getBoolean(KEY_ACTIVE_ALARMS_FIRST, false)
        )
    }

    fun save(settings: AppSettings) {
        prefs.edit()
            .putBoolean(KEY_DEFAULT_SNOOZE_ENABLED, settings.defaultSnoozeEnabled)
            .putInt(KEY_DEFAULT_SNOOZE_MINUTES, settings.defaultSnoozeMinutes)
            .putBoolean(KEY_DEFAULT_VIBRATE, settings.defaultVibrate)
            .putBoolean(KEY_DEFAULT_READ_ALOUD, settings.defaultReadLabelAloud)
            .putString(KEY_DEFAULT_ALARM_SOUND, settings.defaultAlarmSoundUri.orEmpty())
            .putBoolean(KEY_24H, settings.use24HourFormat)
            .putString(KEY_TIMER_SPEECH, settings.timerSpeechFormat.name)
            .putInt(KEY_UPCOMING_ALARM_LEAD, settings.upcomingAlarmLeadMinutes)
            .putString(KEY_TIME_PICKER_STYLE, settings.timePickerStyle.name)
            .putString(KEY_TIMER_CONTROL_STYLE, settings.timerControlStyle.name)
            .putBoolean(KEY_SILENT_NOTIFICATIONS, settings.silentNotifications)
            .putString(KEY_DEFAULT_ALARM_SORT_MODE, settings.defaultAlarmSortMode.name)
            .putBoolean(KEY_ACTIVE_ALARMS_FIRST, settings.activeAlarmsFirst)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "alarmpro_prefs"
        private const val KEY_DEFAULT_SNOOZE_ENABLED = "default_snooze_enabled"
        private const val KEY_DEFAULT_SNOOZE_MINUTES = "default_snooze_minutes"
        private const val KEY_DEFAULT_VIBRATE = "default_vibrate"
        private const val KEY_DEFAULT_READ_ALOUD = "default_read_aloud"
        private const val KEY_DEFAULT_ALARM_SOUND = "default_alarm_sound"
        private const val KEY_24H = "use_24h"
        private const val KEY_TIMER_SPEECH = "timer_speech_format"
        private const val KEY_UPCOMING_ALARM_LEAD = "upcoming_alarm_lead_minutes"
        private const val KEY_TIME_PICKER_STYLE = "time_picker_style"
        private const val KEY_TIMER_CONTROL_STYLE = "timer_control_style"
        private const val KEY_SILENT_NOTIFICATIONS = "silent_notifications"
        private const val KEY_DEFAULT_ALARM_SORT_MODE = "default_alarm_sort_mode"
        private const val KEY_ACTIVE_ALARMS_FIRST = "active_alarms_first"
        private const val KEY_LEGACY_SNOOZE = "snooze_minutes"
        private const val KEY_LEGACY_VIBRATION = "vibration_enabled"
    }
}
