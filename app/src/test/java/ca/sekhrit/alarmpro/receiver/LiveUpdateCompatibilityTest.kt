package ca.sekhrit.alarmpro.receiver

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveUpdateCompatibilityTest {
    @Test
    fun `does not request a promoted notification before Android 16`() {
        assertFalse(LiveUpdateCompatibility.shouldRequestPromotion(35))
    }

    @Test
    fun `requests a promoted notification on Android 16`() {
        assertTrue(LiveUpdateCompatibility.shouldRequestPromotion(36))
    }

    @Test
    fun `requests a promoted notification on newer Android releases`() {
        assertTrue(LiveUpdateCompatibility.shouldRequestPromotion(37))
    }
}
