package ca.sekhrit.alarmpro.util

import ca.sekhrit.alarmpro.data.Alarm
import ca.sekhrit.alarmpro.data.RepeatSchedule
import ca.sekhrit.alarmpro.data.RepeatType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class RepeatCalculatorTest {
    @Test
    fun `one-time alarm uses its saved scheduled date`() {
        val scheduledDate = LocalDate.of(2026, 9, 21)
        val alarm = Alarm(
            time = LocalTime.of(9, 0),
            repeat = RepeatSchedule(type = RepeatType.ONCE, anchorEpochDay = scheduledDate.toEpochDay())
        )

        assertEquals(
            scheduledDate,
            RepeatCalculator.nextTriggerDate(alarm, LocalDate.of(2026, 9, 20), LocalTime.NOON)
        )
        assertEquals(
            "09-21: MON",
            RepeatCalculator.alarmCardRepeatLine(alarm, LocalDateTime.of(2026, 9, 20, 12, 0))
        )
    }
}
