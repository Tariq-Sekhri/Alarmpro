package ca.sekhrit.alarmpro.util

import ca.sekhrit.alarmpro.data.Alarm
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class NextAlarmHeader(
    val timeLine: String,
    val countdownLine: String
)

data class AlarmTimeParts(
    val time: String,
    val period: String?
)

object TimeUtils {
    fun formatTime(time: LocalTime, use24Hour: Boolean): String {
        val pattern = if (use24Hour) "HH:mm" else "h:mm a"
        return time.format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
    }

    fun formatNotificationTime(time: LocalTime, use24Hour: Boolean): String {
        val parts = formatAlarmTimeParts(time, use24Hour)
        return parts.time + parts.period.orEmpty()
    }

    fun formatAlarmTimeParts(time: LocalTime, use24Hour: Boolean): AlarmTimeParts {
        if (use24Hour) {
            return AlarmTimeParts(
                time = time.format(DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())),
                period = null
            )
        }
        return AlarmTimeParts(
            time = time.format(DateTimeFormatter.ofPattern("h:mm", Locale.getDefault())),
            period = time.format(DateTimeFormatter.ofPattern("a", Locale.getDefault()))
                .lowercase(Locale.getDefault())
        )
    }

    fun formatDuration(totalSeconds: Long): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        }
    }

    fun formatSnoozeDuration(minutes: Int): String {
        val hours = minutes / 60
        val mins = minutes % 60
        return when {
            hours > 0 && mins > 0 ->
                "${formatSnoozeUnit(hours, "hour")} ${formatSnoozeUnit(mins, "minute")}"
            hours > 0 -> formatSnoozeUnit(hours, "hour")
            else -> formatSnoozeUnit(mins, "minute")
        }
    }

    fun formatAlarmEditCountdown(triggerEpochMillis: Long, nowEpochMillis: Long): String {
        val remainingMillis = (triggerEpochMillis - nowEpochMillis).coerceAtLeast(0L)
        if (remainingMillis < 60_000L) return "in less than a minute"
        val totalMinutes = (remainingMillis + 59_999L) / 60_000L

        val weeks = totalMinutes / (7L * 24L * 60L)
        val days = totalMinutes / (24L * 60L) % 7L
        val hours = totalMinutes / 60L % 24L
        val minutes = totalMinutes % 60L
        val parts = buildList {
            if (weeks > 0L) add(pluralize(weeks, "week"))
            if (days > 0L) add(pluralize(days, "day"))
            if (hours > 0L) add(pluralize(hours, "hour"))
            if (weeks == 0L && days == 0L && minutes > 0L) add(pluralize(minutes, "minute"))
        }
        val body = if (parts.size <= 1) parts.first() else {
            parts.dropLast(1).joinToString(", ") + " and " + parts.last()
        }
        return "in $body"
    }

    private fun formatSnoozeUnit(value: Int, unit: String): String {
        val label = if (value == 1) unit else "${unit}s"
        return "$value $label"
    }

    fun snoozeMinutesFromDurationSeconds(totalSeconds: Int): Int =
        ((totalSeconds + 59) / 60).coerceAtLeast(1)

    fun formatStopwatch(ms: Long): String {
        val totalCentiseconds = ms / 10
        val totalSeconds = totalCentiseconds / 100
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val centiseconds = totalCentiseconds % 100
        return if (minutes >= 60) {
            String.format(Locale.getDefault(), "%d:%02d:%02d.%02d", minutes / 60, minutes % 60, seconds, centiseconds)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d.%02d", minutes, seconds, centiseconds)
        }
    }

    fun repeatSummary(alarm: Alarm): String = RepeatCalculator.summary(alarm.repeat)

    fun nextTriggerMillis(alarm: Alarm, from: LocalDateTime = LocalDateTime.now()): Long =
        RepeatCalculator.nextTriggerMillis(alarm, from)

    fun nextAlarmHeader(alarms: List<Alarm>, use24Hour: Boolean): NextAlarmHeader? {
        val enabled = alarms.filter { it.isEnabled }
        if (enabled.isEmpty()) return null

        val now = LocalDateTime.now()
        val next = enabled.minByOrNull { nextTriggerMillis(it, now) } ?: return null
        val trigger = nextTriggerMillis(next, now)
        val triggerDateTime = LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(trigger),
            ZoneId.systemDefault()
        )
        val dayText = triggerDateTime.format(DateTimeFormatter.ofPattern("EEE", Locale.getDefault()))
        val timePattern = if (use24Hour) "HH:mm" else "h:mm a"
        val timeText = next.time.format(DateTimeFormatter.ofPattern(timePattern, Locale.getDefault()))
        return NextAlarmHeader(
            timeLine = "$dayText $timeText",
            countdownLine = formatRelativeCountdown(now, triggerDateTime)
        )
    }

    private fun formatRelativeCountdown(from: LocalDateTime, trigger: LocalDateTime): String {
        val totalMinutes = java.time.temporal.ChronoUnit.MINUTES.between(from, trigger).coerceAtLeast(0)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        val body = when {
            totalMinutes < 1 -> "less than a minute from now"
            hours == 0L -> pluralize(minutes, "minute") + " from now"
            minutes == 0L -> pluralize(hours, "hour") + " from now"
            else -> pluralize(hours, "hour") + " and " + pluralize(minutes, "minute") + " from now"
        }
        return "($body)"
    }

    private fun pluralize(value: Long, unit: String): String {
        val label = if (value == 1L) unit else "${unit}s"
        return "$value $label"
    }
}
