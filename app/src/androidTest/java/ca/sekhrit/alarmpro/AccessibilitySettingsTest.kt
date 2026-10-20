package ca.sekhrit.alarmpro

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import ca.sekhrit.alarmpro.data.SettingsRepository
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class AccessibilitySettingsTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    private fun capture(name: String) {
        compose.waitForIdle()
        // Allow the device compositor to present the settled Compose frame before capture.
        android.os.SystemClock.sleep(500)
        val windows = android.os.ParcelFileDescriptor.AutoCloseInputStream(
            instrumentation.uiAutomation.executeShellCommand("dumpsys window")
        ).bufferedReader().use { it.readText() }
        assertTrue("Screenshots must show AlarmPro without system dialogs",
            windows.lineSequence().any { it.contains("mCurrentFocus=") && it.contains(context.packageName) })
        val directory = File(context.getExternalFilesDir(null), "review-screenshots").apply { mkdirs() }
        instrumentation.uiAutomation.takeScreenshot().let { bitmap ->
            File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }

    @Test fun settingsPersistAndLargeLayoutsRemainUsable() {
        val repository = SettingsRepository(context)
        repository.save(repository.load().copy(rememberLastTab = false, largerButtons = false))
        repository.saveLastTab("timer")
        ca.sekhrit.alarmpro.data.TimerPresetRepository(context).savePresets(listOf(
            ca.sekhrit.alarmpro.data.TimerPreset(totalSeconds = 300, label = "Tea break"),
            ca.sekhrit.alarmpro.data.TimerPreset(totalSeconds = 36000, label = "A longer timer label for layout checking")
        ))
        ca.sekhrit.alarmpro.data.AlarmRepository(context).saveAlarms(listOf(
            ca.sekhrit.alarmpro.data.Alarm(time = java.time.LocalTime.of(7, 30), label = "Morning reminder", isEnabled = false)
        ))
        var activity = ActivityScenario.launch(MainActivity::class.java)
        try {
            compose.onNode(hasText("Alarm") and hasClickAction()).assertIsSelected()
            compose.onNodeWithContentDescription("Settings").performClick()
            compose.onNodeWithText("General Settings").performClick()
            capture("01-settings-default")
            val normalButtonWidth = compose.onNodeWithContentDescription("Back").fetchSemanticsNode().boundsInRoot.width
            compose.onAllNodes(isToggleable())[0].assertIsOff().performClick()
            compose.onAllNodes(isToggleable())[1].assertIsOff().performClick()
            capture("02-settings-larger")
            val largeButtonWidth = compose.onNodeWithContentDescription("Back").fetchSemanticsNode().boundsInRoot.width
            assertTrue("Buttons should grow by 20 percent", largeButtonWidth > normalButtonWidth * 1.15f)
            assertTrue(repository.load().largerButtons)
            assertTrue(repository.load().rememberLastTab)
            compose.onNodeWithContentDescription("Back").performClick()
            compose.onNodeWithContentDescription("Back").performClick()
            capture("03-alarm-larger")
            compose.onNodeWithText("Morning reminder").performClick()
            compose.onNodeWithText("Edit alarm").assertIsDisplayed()
            capture("09-alarm-editor-larger")
            compose.onNodeWithContentDescription("Back").performClick()
            compose.onNode(hasText("Timer") and hasClickAction()).performClick()
            capture("04-timer-larger")
            compose.onNodeWithContentDescription("Add timer").performClick()
            capture("05-add-timer-larger")
            compose.onNodeWithText("CANCEL").performClick()
            compose.onNode(hasText("Stopwatch") and hasClickAction()).performClick()
            capture("06-stopwatch-larger")
            compose.onNode(hasText("Clock") and hasClickAction()).performClick()
            capture("07-clock-larger")
            compose.onNode(hasText("Timer") and hasClickAction()).performClick()
            compose.waitForIdle()
            assertEquals("timer", repository.loadLastTab())
            activity.close()
            activity = ActivityScenario.launch(MainActivity::class.java)
            compose.onNode(hasText("Timer") and hasClickAction()).assertIsSelected()
            capture("08-restored-timer")
            activity.close()
            activity = ActivityScenario.launch(Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_TARGET_TAB, "stopwatch"))
            compose.onNode(hasText("Stopwatch") and hasClickAction()).assertIsSelected()
            activity.close()
            repository.save(repository.load().copy(rememberLastTab = false))
            activity = ActivityScenario.launch(MainActivity::class.java)
            compose.onNode(hasText("Alarm") and hasClickAction()).assertIsSelected()
        } finally {
            activity.close()
        }
    }
}
