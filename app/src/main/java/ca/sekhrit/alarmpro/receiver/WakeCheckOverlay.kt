package ca.sekhrit.alarmpro.receiver

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.PowerManager
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import ca.sekhrit.alarmpro.domain.AlarmActions

/**
 * A silent, full-screen overlay used only while the display is already awake.
 * Android prevents background receivers from opening activities over another
 * foreground app, but permits an explicitly user-approved overlay instead.
 */
object WakeCheckOverlay {
    private data class ActiveOverlay(
        val windowManager: WindowManager,
        val view: View
    )

    private val activeOverlays = mutableMapOf<String, ActiveOverlay>()

    fun showIfDisplayIsAwake(context: Context, alarmId: String): Boolean {
        val appContext = context.applicationContext
        val powerManager = appContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!powerManager.isInteractive || !Settings.canDrawOverlays(appContext)) return false

        dismiss(alarmId)
        val root = FrameLayout(appContext).apply {
            setBackgroundColor(Color.BLACK)
        }
        val button = Button(appContext).apply {
            text = "I'M AWAKE"
            textSize = 22f
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                cornerRadius = 28f
                setColor(Color.rgb(27, 34, 48))
            }
            setOnClickListener {
                WakeCheckScheduler(appContext).cancel(alarmId)
                AlarmActions.dismiss(appContext, alarmId, startWakeCheck = false)
                dismiss(alarmId)
            }
        }
        root.addView(
            button,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                104.dp(appContext),
                Gravity.CENTER
            ).apply {
                marginStart = 32.dp(appContext)
                marginEnd = 32.dp(appContext)
            }
        )

        val windowManager = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            android.graphics.PixelFormat.OPAQUE
        )
        try {
            windowManager.addView(root, params)
            activeOverlays[alarmId] = ActiveOverlay(windowManager, root)
            return true
        } catch (_: WindowManager.BadTokenException) {
            // The full-screen notification remains the fallback when the
            // system refuses an overlay despite the permission check.
            return false
        }
    }

    fun dismiss(alarmId: String) {
        val active = activeOverlays.remove(alarmId) ?: return
        try {
            active.windowManager.removeView(active.view)
        } catch (_: IllegalArgumentException) {
        }
    }

    private fun Int.dp(context: Context): Int =
        (this * context.resources.displayMetrics.density).toInt()
}
