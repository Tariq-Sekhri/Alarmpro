package ca.sekhrit.alarmpro.data

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

enum class TimerSpeechFormat(val label: String) {
    OFF("Off"),
    TIME("Time"),
    LABEL("Label"),
    TIME_AND_LABEL("Time and label"),
    CUSTOM("Custom template");

    companion object {
        fun fromStored(value: String?): TimerSpeechFormat {
            return entries.find { it.name == value } ?: TIME_AND_LABEL
        }
    }
}

fun timerSpeechText(
    format: TimerSpeechFormat,
    label: String,
    totalSeconds: Int,
    customTemplate: String = DEFAULT_TIMER_SPEECH_TEMPLATE
): String? {
    val timeText = formatDurationForSpeech(totalSeconds)
    return when (format) {
        TimerSpeechFormat.OFF -> null
        TimerSpeechFormat.TIME -> timeText
        TimerSpeechFormat.LABEL -> label.takeIf { it.isNotBlank() }
        TimerSpeechFormat.TIME_AND_LABEL -> {
            when {
                label.isNotBlank() -> "$timeText. $label"
                else -> timeText
            }
        }
        TimerSpeechFormat.CUSTOM -> formatTimerSpeechTemplate(
            template = customTemplate,
            label = label,
            totalSeconds = totalSeconds
        )
    }
}

enum class AlarmElapsedSpeechTiming(val label: String) {
    AFTER_EACH_SNOOZE("After each snooze"),
    ALWAYS("Always"),
    NEVER("Never")
}

fun alarmSpeechText(
    label: String,
    templateWithLabel: String,
    templateWithoutLabel: String,
    use24HourFormat: Boolean,
    weather: String = ""
): String? {
    val now = LocalDateTime.now()
    val time = now.format(
        DateTimeFormatter.ofPattern(if (use24HourFormat) "HH:mm" else "h:mm a", Locale.getDefault())
    )
    val template = if (label.isBlank()) templateWithoutLabel.ifBlank { templateWithLabel } else templateWithLabel
    return template
        .replace("\$t", time)
        .replace("\$l", label.trim())
        .replace("\$d", now.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, Locale.getDefault()))
        .replace("\$m", now.toLocalDate().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)))
        .replace("\$w", weather)
        .replace(Regex("\\s+"), " ")
        .trim()
        .takeIf { it.isNotBlank() }
}

/**
 * Expands a timer speech template. `$t` is the natural duration, `$l` the label,
 * and `$h`, `$m`, `$s` the individual hours, minutes, and seconds. Every `$`
 * starts a special token; all other text is spoken exactly as written.
 */
fun formatTimerSpeechTemplate(
    template: String,
    label: String,
    totalSeconds: Int
): String? {
    val safeSeconds = totalSeconds.coerceAtLeast(0)
    val values = mapOf(
        "\$t" to formatDurationForSpeech(safeSeconds),
        "\$l" to label.trim(),
        "\$h" to (safeSeconds / 3600).toString(),
        "\$m" to ((safeSeconds % 3600) / 60).toString(),
        "\$s" to (safeSeconds % 60).toString()
    )
    val expanded = values.entries.fold(template) { text, (token, value) ->
        text.replace(token, value)
    }.replace(Regex("\\s+"), " ").trim()
    return expanded.takeIf { it.isNotBlank() }
}

const val DEFAULT_TIMER_SPEECH_TEMPLATE = "\$l finished. \$t."
const val DEFAULT_ALARM_SPEECH_WITH_LABEL = "The time is \$t. \$l."
const val DEFAULT_ALARM_SPEECH_WITHOUT_LABEL = "The time is \$t."
const val DEFAULT_ALARM_ELAPSED_SPEECH_TEMPLATE = "Your alarm has been active for \$e."

fun formatDurationForSpeech(totalSeconds: Int): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return when {
        hours > 0 && minutes > 0 -> "$hours ${unit("hour", hours)} and $minutes ${unit("minute", minutes)}"
        hours > 0 -> "$hours ${unit("hour", hours)}"
        minutes > 0 -> "$minutes ${unit("minute", minutes)}"
        else -> "$seconds ${unit("second", seconds)}"
    }
}

fun upcomingAlarmLeadLabel(minutes: Int): String {
    return when (minutes) {
        0 -> "Off"
        15 -> "15 minutes before"
        30 -> "30 minutes before"
        60 -> "1 hour before"
        else -> "$minutes minutes before"
    }
}

private fun unit(name: String, count: Int): String {
    return if (count == 1) name else "${name}s"
}
