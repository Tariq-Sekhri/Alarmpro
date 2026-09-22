package ca.sekhrit.alarmpro.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class OneTimeAlarmDeletionTest {
    private val oneTimeAlarm = Alarm(time = LocalTime.NOON)

    @Test
    fun `enabled alarm deletes itself on dismissal`() {
        assertTrue(oneTimeAlarm.copy(deleteAfterDismiss = true).shouldDeleteOnDismiss())
    }

    @Test
    fun `setting never deletes recurring alarms`() {
        val recurring = oneTimeAlarm.copy(repeat = RepeatSchedule(type = RepeatType.DAILY))
        assertFalse(recurring.copy(deleteAfterDismiss = true).shouldDeleteOnDismiss())
    }
}
