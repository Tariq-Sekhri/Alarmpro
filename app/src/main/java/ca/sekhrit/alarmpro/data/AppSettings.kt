package ca.sekhrit.alarmpro.data

data class AppSettings(
    val defaultSnoozeEnabled: Boolean = true,
    val defaultSnoozeMinutes: Int = 10,
    val defaultVibrate: Boolean = true,
    val defaultReadLabelAloud: Boolean = false,
    val use24HourFormat: Boolean = false
) {
    val snoozeMinutes: Int get() = defaultSnoozeMinutes
    val vibrationEnabled: Boolean get() = defaultVibrate
}

fun Alarm.resolveSnoozeMinutes(settings: AppSettings): Int =
    snoozeMinutes ?: settings.defaultSnoozeMinutes

fun Alarm.isSnoozeAllowed(settings: AppSettings): Boolean =
    snoozeEnabled && settings.defaultSnoozeEnabled
