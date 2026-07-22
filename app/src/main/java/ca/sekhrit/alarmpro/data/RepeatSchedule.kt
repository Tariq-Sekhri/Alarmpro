package ca.sekhrit.alarmpro.data

import java.time.LocalDate

enum class RepeatType {
    ONCE,
    DAILY,
    WEEKLY,
    INTERVAL_WEEKS,
    MONTHLY,
    INTERVAL_MONTHS,
    YEARLY
}

data class RepeatSchedule(
    val type: RepeatType = RepeatType.ONCE,
    val daysOfWeek: Set<Int> = emptySet(),
    val weekInterval: Int = 2,
    val monthInterval: Int = 1,
    val dayOfMonth: Int = LocalDate.now().dayOfMonth,
    val anchorEpochDay: Long = LocalDate.now().toEpochDay()
)
