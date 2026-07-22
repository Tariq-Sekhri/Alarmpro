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
            use24HourFormat = prefs.getBoolean(KEY_24H, false)
        )
    }

    fun save(settings: AppSettings) {
        prefs.edit()
            .putBoolean(KEY_DEFAULT_SNOOZE_ENABLED, settings.defaultSnoozeEnabled)
            .putInt(KEY_DEFAULT_SNOOZE_MINUTES, settings.defaultSnoozeMinutes)
            .putBoolean(KEY_DEFAULT_VIBRATE, settings.defaultVibrate)
            .putBoolean(KEY_DEFAULT_READ_ALOUD, settings.defaultReadLabelAloud)
            .putBoolean(KEY_24H, settings.use24HourFormat)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "alarmpro_prefs"
        private const val KEY_DEFAULT_SNOOZE_ENABLED = "default_snooze_enabled"
        private const val KEY_DEFAULT_SNOOZE_MINUTES = "default_snooze_minutes"
        private const val KEY_DEFAULT_VIBRATE = "default_vibrate"
        private const val KEY_DEFAULT_READ_ALOUD = "default_read_aloud"
        private const val KEY_24H = "use_24h"
        private const val KEY_LEGACY_SNOOZE = "snooze_minutes"
        private const val KEY_LEGACY_VIBRATION = "vibration_enabled"
    }
}
