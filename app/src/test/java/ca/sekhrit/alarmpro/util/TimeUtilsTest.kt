package ca.sekhrit.alarmpro.util

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeUtilsTest {
    @Test
    fun `stopwatch retains centiseconds after one hour`() {
        assertEquals("1:38:10.76", TimeUtils.formatStopwatch(5_890_760L))
    }

    @Test
    fun `alarm edit countdown uses minutes hours days and weeks`() {
        val minute = 60_000L
        assertEquals("in less than a minute", TimeUtils.formatAlarmEditCountdown(1_000L, 0L))
        assertEquals("in 45 minutes", TimeUtils.formatAlarmEditCountdown(45 * minute, 0L))
        assertEquals("in 3 hours", TimeUtils.formatAlarmEditCountdown(3 * 60 * minute, 0L))
        assertEquals("in 3 hours and 15 minutes", TimeUtils.formatAlarmEditCountdown(195 * minute, 0L))
        assertEquals("in 2 days and 4 hours", TimeUtils.formatAlarmEditCountdown((2 * 24 + 4) * 60 * minute, 0L))
        assertEquals(
            "in 1 week, 2 days and 3 hours",
            TimeUtils.formatAlarmEditCountdown(((7 + 2) * 24 + 3) * 60 * minute, 0L)
        )
    }
}
