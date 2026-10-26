package ca.sekhrit.alarmpro.data

import ca.sekhrit.alarmpro.util.RepeatCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class AlarmRepeatTest {
    @Test
    fun `group rule preserves member settings and removes old scheduling state`() {
        val alarm = Alarm(
            time = LocalTime.of(7, 15), label = "Wake up", isEnabled = false,
            groupId = "morning", soundUri = "content://sound", vibrate = false,
            snoozeMinutes = 15, deleteAfterDismiss = true,
            skipUntilEpochDay = 123, snoozedUntilEpochMillis = 456
        )
        val rule = RepeatSchedule(type = RepeatType.WEEKLY, daysOfWeek = setOf(1, 3, 5))
        val updated = alarm.withRepeatSchedule(rule)

        assertEquals(alarm.copy(
            repeat = rule, skipUntilEpochDay = null,
            snoozedUntilEpochMillis = null, deleteAfterDismiss = false
        ), updated)
        assertFalse(updated.isEnabled)
        assertNull(updated.skipUntilEpochDay)
        assertNull(updated.snoozedUntilEpochMillis)
    }

    @Test
    fun `all group members use selected weekdays at their own times`() {
        val monday = LocalDate.of(2026, 10, 26)
        val rule = RepeatSchedule(type = RepeatType.WEEKLY, daysOfWeek = setOf(1, 3))
        val early = Alarm(time = LocalTime.of(7, 0)).withRepeatSchedule(rule)
        val late = Alarm(time = LocalTime.of(9, 0)).withRepeatSchedule(rule)
        assertEquals(monday.plusDays(2), RepeatCalculator.nextTriggerDate(early, monday, LocalTime.of(8, 0)))
        assertEquals(monday, RepeatCalculator.nextTriggerDate(late, monday, LocalTime.of(8, 0)))
    }
}
