package ca.sekhrit.alarmpro.data

enum class TimePickerStyle(val label: String) {
    ANALOG("Analog (Round Clock)"),
    SPINNER("Spinner (Scroll Wheel)")
}

enum class TimerControlStyle(val label: String) {
    SWITCH("Switch"),
    PLAY_PAUSE_BUTTON("Play/pause button")
}

data class AppSettings(
    val defaultSnoozeEnabled: Boolean = true,
    val defaultSnoozeMinutes: Int = 10,
    val defaultVibrate: Boolean = true,
    val defaultReadLabelAloud: Boolean = false,
    val defaultAlarmSoundUri: String? = null,
    val use24HourFormat: Boolean = false,
    val timerSpeechFormat: TimerSpeechFormat = TimerSpeechFormat.TIME_AND_LABEL,
    val upcomingAlarmLeadMinutes: Int = 60,
    val timePickerStyle: TimePickerStyle = TimePickerStyle.ANALOG,
    val timerControlStyle: TimerControlStyle = TimerControlStyle.SWITCH
) {
    val snoozeMinutes: Int get() = defaultSnoozeMinutes
    val vibrationEnabled: Boolean get() = defaultVibrate
}

fun Alarm.resolveSnoozeMinutes(settings: AppSettings): Int =
    snoozeMinutes ?: settings.defaultSnoozeMinutes

fun Alarm.isSnoozeAllowed(settings: AppSettings): Boolean =
    snoozeEnabled && settings.defaultSnoozeEnabled
