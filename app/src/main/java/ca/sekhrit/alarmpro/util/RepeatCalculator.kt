package ca.sekhrit.alarmpro.util

import ca.sekhrit.alarmpro.data.Alarm
import ca.sekhrit.alarmpro.data.RepeatSchedule
import ca.sekhrit.alarmpro.data.RepeatType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.min

object RepeatCalculator {
    private val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    private val dayShort = listOf("M", "T", "W", "T", "F", "S", "S")
    private val dayNamesUpper = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")

    fun nextTriggerMillis(alarm: Alarm, from: LocalDateTime = LocalDateTime.now()): Long {
        if (alarm.snoozedUntilEpochMillis != null && alarm.snoozedUntilEpochMillis > System.currentTimeMillis()) {
            return alarm.snoozedUntilEpochMillis
        }
        val zone = ZoneId.systemDefault()
        val triggerDate = nextTriggerDate(alarm, from.toLocalDate(), from.toLocalTime())
        return LocalDateTime.of(triggerDate, alarm.time).atZone(zone).toInstant().toEpochMilli()
    }

    fun nextTriggerDate(alarm: Alarm, fromDate: LocalDate, fromTime: LocalTime): LocalDate {
        var candidate = nextTriggerDateInternal(alarm, fromDate, fromTime)
        val skipDay = alarm.skipUntilEpochDay ?: return candidate
        var safety = 0
        while (candidate.toEpochDay() == skipDay && safety < 400) {
            candidate = nextTriggerDateInternal(alarm, candidate.plusDays(1), LocalTime.MIN)
            safety++
        }
        return candidate
    }

    fun nextUnskippedTriggerDate(
        alarm: Alarm,
        from: LocalDateTime = LocalDateTime.now()
    ): LocalDate = nextTriggerDateInternal(alarm, from.toLocalDate(), from.toLocalTime())

    fun hasSkipScheduled(alarm: Alarm, from: LocalDateTime = LocalDateTime.now()): Boolean {
        val skipDay = alarm.skipUntilEpochDay ?: return false
        return skipDay == nextUnskippedTriggerDate(alarm, from).toEpochDay()
    }

    private fun nextTriggerDateInternal(alarm: Alarm, fromDate: LocalDate, fromTime: LocalTime): LocalDate {
        val schedule = alarm.repeat
        val time = alarm.time
        val anchor = LocalDate.ofEpochDay(schedule.anchorEpochDay)

        return when (schedule.type) {
            RepeatType.ONCE -> {
                val today = LocalDateTime.of(fromDate, time)
                val from = LocalDateTime.of(fromDate, fromTime)
                if (today.isAfter(from)) fromDate else fromDate.plusDays(1)
            }
            RepeatType.DAILY -> {
                if (time.isAfter(fromTime)) fromDate else fromDate.plusDays(1)
            }
            RepeatType.WEEKLY -> {
                findNextWeeklyDate(fromDate, fromTime, time, schedule.daysOfWeek.ifEmpty { setOf(fromDate.dayOfWeek.value) })
            }
            RepeatType.INTERVAL_WEEKS -> {
                val days = schedule.daysOfWeek.ifEmpty { setOf(fromDate.dayOfWeek.value) }
                findNextIntervalWeekDate(fromDate, fromTime, time, days, schedule.weekInterval.coerceAtLeast(1), anchor)
            }
            RepeatType.MONTHLY -> {
                findNextMonthlyDate(fromDate, fromTime, time, schedule.dayOfMonth, 1, anchor)
            }
            RepeatType.INTERVAL_MONTHS -> {
                findNextMonthlyDate(
                    fromDate,
                    fromTime,
                    time,
                    schedule.dayOfMonth,
                    schedule.monthInterval.coerceAtLeast(1),
                    anchor
                )
            }
            RepeatType.YEARLY -> {
                findNextYearlyDate(fromDate, fromTime, time, anchor)
            }
        }
    }

    private fun findNextWeeklyDate(
        fromDate: LocalDate,
        fromTime: LocalTime,
        alarmTime: LocalTime,
        days: Set<Int>
    ): LocalDate {
        for (offset in 0..7) {
            val date = fromDate.plusDays(offset.toLong())
            if (date.dayOfWeek.value !in days) continue
            if (offset == 0 && !alarmTime.isAfter(fromTime)) continue
            return date
        }
        return fromDate.plusDays(1)
    }

    private fun findNextIntervalWeekDate(
        fromDate: LocalDate,
        fromTime: LocalTime,
        alarmTime: LocalTime,
        days: Set<Int>,
        weekInterval: Int,
        anchor: LocalDate
    ): LocalDate {
        val anchorWeek = anchor.toEpochDay() / 7
        for (offset in 0..730) {
            val date = fromDate.plusDays(offset.toLong())
            if (date.dayOfWeek.value !in days) continue
            val dateWeek = date.toEpochDay() / 7
            if ((dateWeek - anchorWeek) % weekInterval != 0L) continue
            if (offset == 0 && !alarmTime.isAfter(fromTime)) continue
            return date
        }
        return fromDate.plusDays(7L * weekInterval)
    }

    private fun findNextMonthlyDate(
        fromDate: LocalDate,
        fromTime: LocalTime,
        alarmTime: LocalTime,
        dayOfMonth: Int,
        monthInterval: Int,
        anchor: LocalDate
    ): LocalDate {
        var cursor = YearMonth.from(fromDate)
        repeat(240) {
            val monthsBetween = ChronoUnit.MONTHS.between(YearMonth.from(anchor), cursor)
            if (monthsBetween >= 0 && monthsBetween % monthInterval == 0L) {
                val safeDay = min(dayOfMonth, cursor.lengthOfMonth())
                val candidate = cursor.atDay(safeDay)
                if (candidate.isAfter(fromDate) || (candidate == fromDate && alarmTime.isAfter(fromTime))) {
                    return candidate
                }
            }
            cursor = cursor.plusMonths(1)
        }
        return fromDate.plusMonths(1)
    }

    private fun findNextYearlyDate(
        fromDate: LocalDate,
        fromTime: LocalTime,
        alarmTime: LocalTime,
        anchor: LocalDate
    ): LocalDate {
        var year = fromDate.year
        repeat(40) {
            val safeDay = min(anchor.dayOfMonth, LocalDate.of(year, anchor.monthValue, 1).lengthOfMonth())
            val candidate = LocalDate.of(year, anchor.monthValue, safeDay)
            if (candidate.isAfter(fromDate) || (candidate == fromDate && alarmTime.isAfter(fromTime))) {
                return candidate
            }
            year++
        }
        return fromDate.plusYears(1)
    }

    fun summary(schedule: RepeatSchedule): String {
        val daysText = schedule.daysOfWeek.sorted().joinToString(" ") { dayShort[it - 1] }
        return when (schedule.type) {
            RepeatType.ONCE -> "Once"
            RepeatType.DAILY -> "Every day"
            RepeatType.WEEKLY -> when {
                schedule.daysOfWeek.isEmpty() -> "Weekly"
                schedule.daysOfWeek.size == 7 -> "Every day"
                schedule.daysOfWeek == setOf(1, 2, 3, 4, 5) -> "Weekdays"
                schedule.daysOfWeek == setOf(6, 7) -> "Weekends"
                else -> "Weekly · $daysText"
            }
            RepeatType.INTERVAL_WEEKS -> {
                val interval = schedule.weekInterval
                val prefix = when (interval) {
                    2 -> "Every 2 weeks"
                    3 -> "Every 3 weeks"
                    else -> "Every $interval weeks"
                }
                if (schedule.daysOfWeek.isEmpty()) prefix else "$prefix · $daysText"
            }
            RepeatType.MONTHLY -> "Monthly · day ${schedule.dayOfMonth.coerceIn(1, 31)}"
            RepeatType.INTERVAL_MONTHS -> {
                val interval = schedule.monthInterval
                when (interval) {
                    1 -> "Monthly · day ${schedule.dayOfMonth.coerceIn(1, 31)}"
                    2 -> "Every 2 months · day ${schedule.dayOfMonth.coerceIn(1, 31)}"
                    3 -> "Every 3 months · day ${schedule.dayOfMonth.coerceIn(1, 31)}"
                    else -> "Every $interval months · day ${schedule.dayOfMonth.coerceIn(1, 31)}"
                }
            }
            RepeatType.YEARLY -> {
                val anchor = LocalDate.ofEpochDay(schedule.anchorEpochDay)
                val month = anchor.format(DateTimeFormatter.ofPattern("MMM", Locale.getDefault()))
                "Yearly · $month ${anchor.dayOfMonth}"
            }
        }
    }

    fun alarmCardRepeatLine(alarm: Alarm, from: LocalDateTime = LocalDateTime.now(), use24HourFormat: Boolean = false): String {
        if (alarm.snoozedUntilEpochMillis != null && alarm.snoozedUntilEpochMillis > System.currentTimeMillis()) {
            val snoozeTime = java.time.Instant.ofEpochMilli(alarm.snoozedUntilEpochMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalTime()
            val formatted = TimeUtils.formatTime(snoozeTime, use24HourFormat)
            return "Snoozed until $formatted"
        }
        val schedule = alarm.repeat
        return when (schedule.type) {
            RepeatType.ONCE -> {
                val nextDate = nextTriggerDate(alarm, from.toLocalDate(), from.toLocalTime())
                val datePart = nextDate.format(DateTimeFormatter.ofPattern("MM-dd", Locale.getDefault()))
                val dayPart = nextDate.format(DateTimeFormatter.ofPattern("EEE", Locale.getDefault()))
                    .uppercase(Locale.getDefault())
                "$datePart: $dayPart"
            }
            RepeatType.WEEKLY, RepeatType.INTERVAL_WEEKS -> {
                val days = schedule.daysOfWeek.sorted().map { dayNamesUpper[it - 1] }
                when {
                    days.isEmpty() -> summary(schedule)
                    days.size == 7 -> "SUN, MON, TUE, WED, THU, FRI, SAT"
                    else -> days.joinToString(", ")
                }
            }
            else -> summary(schedule)
        }
    }

    fun countdownText(alarm: Alarm, from: LocalDateTime = LocalDateTime.now()): String? {
        if (!alarm.isEnabled) return null
        val trigger = nextTriggerMillis(alarm, from)
        val minutes = ChronoUnit.MINUTES.between(from, LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(trigger),
            ZoneId.systemDefault()
        ))
        return when {
            minutes < 1 -> "Rings in less than 1 min"
            minutes < 60 -> "Rings in ${minutes} min"
            minutes < 24 * 60 -> {
                val hours = minutes / 60
                val mins = minutes % 60
                if (mins == 0L) "Rings in ${hours}h" else "Rings in ${hours}h ${mins}m"
            }
            else -> {
                val days = minutes / (24 * 60)
                val hours = (minutes % (24 * 60)) / 60
                if (hours == 0L) "Rings in ${days}d" else "Rings in ${days}d ${hours}h"
            }
        }
    }
}
