package ca.sekhrit.alarmpro.util

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeUtilsTest {
    @Test
    fun `stopwatch retains centiseconds after one hour`() {
        assertEquals("1:38:10.76", TimeUtils.formatStopwatch(5_890_760L))
    }
}
