package ca.sekhrit.alarmpro.data

enum class TimerSpeechFormat(val label: String) {
    TIME("Time"),
    LABEL("Label"),
    TIME_AND_LABEL("Time and label");

    companion object {
        fun fromStored(value: String?): TimerSpeechFormat {
            return entries.find { it.name == value } ?: TIME_AND_LABEL
        }
    }
}

fun timerSpeechText(
    format: TimerSpeechFormat,
    label: String,
    totalSeconds: Int
): String? {
    val timeText = formatDurationForSpeech(totalSeconds)
    return when (format) {
        TimerSpeechFormat.TIME -> timeText
        TimerSpeechFormat.LABEL -> label.takeIf { it.isNotBlank() }
        TimerSpeechFormat.TIME_AND_LABEL -> {
            when {
                label.isNotBlank() -> "$timeText. $label"
                else -> timeText
            }
        }
    }
}

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
