package ca.sekhrit.alarmpro.util

import ca.sekhrit.alarmpro.data.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TimerSoundTest {
    @Test
    fun `default timer sound uri is null`() {
        val settings = AppSettings()
        assertNull(settings.timerSoundUri)
    }

    @Test
    fun `custom timer sound uri is stored in AppSettings`() {
        val customUri = "content://media/external/audio/media/42"
        val settings = AppSettings(timerSoundUri = customUri)
        assertEquals(customUri, settings.timerSoundUri)
    }
}
