package ca.sekhrit.alarmpro.data

enum class TimePickerStyle(val label: String) {
    ANALOG("Analog (Round Clock)"),
    SPINNER("Spinner (Scroll Wheel)")
}

enum class TimerControlStyle(val label: String) {
    SWITCH("Switch"),
    PLAY_PAUSE_BUTTON("Play/pause button")
}

enum class TimerSortMode {
    MANUAL,
    TIME_ASC,
    TIME_DESC
}

enum class AlarmSortMode(val label: String) {
    NEXT_TRIGGER("Next trigger"),
    TIME_OF_DAY("Time of day")
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
    val timerControlStyle: TimerControlStyle = TimerControlStyle.SWITCH,
    val timerSortMode: TimerSortMode = TimerSortMode.MANUAL,
    val silentNotifications: Boolean = false,
    val defaultAlarmSortMode: AlarmSortMode = AlarmSortMode.TIME_OF_DAY,
    val activeAlarmsFirst: Boolean = false
) {
}

fun Alarm.resolveSnoozeMinutes(settings: AppSettings): Int =
    snoozeMinutes ?: settings.defaultSnoozeMinutes

fun Alarm.isSnoozeAllowed(settings: AppSettings): Boolean =
    snoozeEnabled && settings.defaultSnoozeEnabled
