package ca.sekhrit.alarmpro

import android.app.NotificationManager
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.test.platform.app.InstrumentationRegistry
import ca.sekhrit.alarmpro.data.TimerRepository
import ca.sekhrit.alarmpro.data.TimerState
import ca.sekhrit.alarmpro.receiver.NotificationHelper
import ca.sekhrit.alarmpro.receiver.TimerScheduler
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class NotificationLayoutTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private fun awaitNotification(matches: (android.service.notification.StatusBarNotification) -> Boolean): android.app.Notification {
        repeat(50) {
            manager.activeNotifications.firstOrNull(matches)?.let {
                return it.notification
            }
            android.os.SystemClock.sleep(100)
        }
        error("Timer notification was not posted")
    }

    private fun timerNotification(timer: TimerState) = awaitNotification { it.id == TimerScheduler.notificationIdFor(timer.id) }

    @Test fun controlsFitNarrowAndWideNotificationsAtLargeFontScales() {
        val timer = TimerState(id = "notification-layout-test", totalSeconds = 960, remainingSeconds = 959)
        try {
            NotificationHelper.showPausedTimerNotification(context, timer)
            val paused = timerNotification(timer)
            NotificationHelper.showActiveTimerNotification(context, timer.copy(
                isRunning = true, endTimeMillis = System.currentTimeMillis() + 959_000
            ))
            val running = awaitNotification {
                it.id == TimerScheduler.notificationIdFor(timer.id) && it.notification.extras.getString("android.text") != "Paused"
            }
            NotificationHelper.showActiveStopwatchNotification(context, "layout-test", 123_000)
            val stopwatch = awaitNotification { it.notification.category == "stopwatch" }
            NotificationHelper.showUpcomingAlarmNotification(context, "layout-test", "A long alarm label", "7:30 AM", 600_000, true)
            val upcoming = awaitNotification { it.notification.category == "reminder" }
            val cases = listOf(
                Triple("paused", paused, 2), Triple("running", running, 2),
                Triple("stopwatch", stopwatch, 3), Triple("upcoming", upcoming, 1)
            )
            instrumentation.runOnMainSync {
                for (widthDp in listOf(220, 520)) {
                    for (fontScale in listOf(1f, 1.5f, 2f)) {
                        val config = Configuration(context.resources.configuration).apply { this.fontScale = fontScale }
                        val configured = context.createConfigurationContext(config)
                        val density = configured.resources.displayMetrics.density
                        for ((name, notification, actionCount) in cases) {
                            for (expanded in listOf(false, true)) {
                                val remote = if (expanded) notification.bigContentView else notification.contentView
                                val root = remote.apply(configured, FrameLayout(configured))
                                val maxHeight = ((if (expanded) 252 else 48) * density).toInt()
                                root.measure(
                                    View.MeasureSpec.makeMeasureSpec((widthDp * density).toInt(), View.MeasureSpec.EXACTLY),
                                    View.MeasureSpec.makeMeasureSpec(maxHeight, View.MeasureSpec.AT_MOST)
                                )
                                root.layout(0, 0, root.measuredWidth, root.measuredHeight)
                                val ids = listOf(R.id.notification_action_one, R.id.notification_action_two, R.id.notification_action_three)
                                ids.take(actionCount).forEach { id ->
                                    val action = root.findViewById<View>(id)
                                    val rect = android.graphics.Rect()
                                    assertTrue("$name action must be visible", action.getGlobalVisibleRect(rect))
                                    assertEquals("$name button must not be vertically clipped at $widthDp dp / $fontScale", action.height, rect.height())
                                    assertEquals("$name button must not be horizontally clipped", action.width, rect.width())
                                    assertTrue(action.contentDescription.isNotBlank())
                                    if (action is TextView) {
                                        assertTrue("$name label must fit", action.layout.height <= action.height - action.totalPaddingTop - action.totalPaddingBottom)
                                    }
                                }
                                val directory = File(context.getExternalFilesDir(null), "notification-qa").apply { mkdirs() }
                                val bitmap = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
                                Canvas(bitmap).apply { drawColor(android.graphics.Color.DKGRAY); root.draw(this) }
                                File(directory, "$name-$widthDp-$fontScale-$expanded.png").outputStream().use {
                                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                                }
                                bitmap.recycle()
                            }
                        }
                    }
                }
            }
        } finally {
            manager.cancelAll()
        }
    }

    @Test fun compactTimerActionsResumePauseAndClose() {
        val timer = TimerState(id = "notification-action-test", totalSeconds = 960, remainingSeconds = 959)
        val repository = TimerRepository(context)
        repository.saveAll(listOf(timer))
        try {
            NotificationHelper.showPausedTimerNotification(context, timer)
            fun click(id: Int) {
                val notification = timerNotification(timer)
                instrumentation.runOnMainSync {
                    notification.contentView.apply(context, FrameLayout(context)).findViewById<View>(id).performClick()
                }
                instrumentation.waitForIdleSync()
                android.os.SystemClock.sleep(500)
            }
            click(R.id.notification_action_one)
            assertTrue(repository.loadAll().single().isRunning)
            click(R.id.notification_action_one)
            assertFalse(repository.loadAll().single().isRunning)
            click(R.id.notification_action_two)
            assertTrue(repository.loadAll().isEmpty())
        } finally {
            TimerScheduler(context).cancel(timer.id)
            repository.clear()
            manager.cancelAll()
        }
    }

    @Test fun captureNotificationShade() {
        val timer = TimerState(id = "notification-shade-test", totalSeconds = 960, remainingSeconds = 959)
        fun shell(command: String) {
            android.os.ParcelFileDescriptor.AutoCloseInputStream(
                instrumentation.uiAutomation.executeShellCommand(command)
            ).use { it.readBytes() }
        }
        fun nodes(node: android.view.accessibility.AccessibilityNodeInfo): List<android.view.accessibility.AccessibilityNodeInfo> =
            listOf(node) + (0 until node.childCount).flatMap { index -> node.getChild(index)?.let { nodes(it) }.orEmpty() }
        fun capture(state: String) {
            android.os.SystemClock.sleep(700)
            val bitmap = instrumentation.uiAutomation.takeScreenshot()
            val directory = File(context.getExternalFilesDir(null), "notification-shade").apply { mkdirs() }
            File(directory, "${bitmap.width}-$state.png").outputStream().use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            bitmap.recycle()
        }
        try {
            shell("input keyevent KEYCODE_WAKEUP")
            shell("wm dismiss-keyguard")
            NotificationHelper.showPausedTimerNotification(context, timer)
            timerNotification(timer)
            shell("cmd statusbar expand-notifications")
            android.os.SystemClock.sleep(1000)
            fun expander(): android.view.accessibility.AccessibilityNodeInfo? {
                var container = nodes(instrumentation.uiAutomation.rootInActiveWindow)
                    .firstOrNull { it.text?.toString() == "15:59" }
                repeat(8) {
                    val current = container ?: return null
                    nodes(current).firstOrNull {
                        it.contentDescription?.toString() in listOf("Expand", "Collapse", "Expand notification", "Collapse notification")
                    }?.let { return it }
                    container = current.parent
                }
                return null
            }
            val expand = expander()
            if (expand?.contentDescription.toString().startsWith("Expand")) {
                expand?.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK)
            }
            android.os.SystemClock.sleep(500)
            nodes(instrumentation.uiAutomation.rootInActiveWindow).firstOrNull { it.isScrollable }
                ?.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
            capture("expanded")
            val collapse = expander()
            assertNotNull("Notification must have an expansion control", collapse)
            collapse?.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK)
            capture("collapsed")
        } finally {
            manager.cancelAll()
            shell("cmd statusbar collapse")
        }
    }
}
