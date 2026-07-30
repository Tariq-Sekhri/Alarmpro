package ca.sekhrit.alarmpro.receiver

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import ca.sekhrit.alarmpro.AlarmRingActivity
import ca.sekhrit.alarmpro.MainActivity
import ca.sekhrit.alarmpro.R
import ca.sekhrit.alarmpro.data.SettingsRepository
import ca.sekhrit.alarmpro.data.TimerState
import ca.sekhrit.alarmpro.util.TimeUtils

object NotificationHelper {
    // Ringing is handled by AlarmRingActivity so the selected sound is the only sound.
    // Version the channel to replace older installs whose channel still had a default sound.
    private const val ALARM_CHANNEL = "alarm_channel_v2"
    private const val UPCOMING_CHANNEL = "upcoming_alarm_channel"
    private const val TIMER_CHANNEL = "timer_channel_v2"
    private const val ACTIVE_TIMER_CHANNEL = "active_timer_channel_v1"
    private const val STOPWATCH_MARK_NOTIFICATION_ID = 9002
    private const val STOPWATCH_NOTIFICATION_ID = 9003

    fun canPostNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun buildAlarmNotification(
        context: Context,
        alarmId: String,
        hour: Int,
        minute: Int,
        label: String,
        snoozeAllowed: Boolean,
        snoozeMinutes: Int
    ): Notification {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureAlarmChannel(notificationManager)

        val ringIntent = Intent(context, AlarmRingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(AlarmRingActivity.EXTRA_RING_TYPE, AlarmRingActivity.TYPE_ALARM)
            putExtra(AlarmRingActivity.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmRingActivity.EXTRA_HOUR, hour)
            putExtra(AlarmRingActivity.EXTRA_MINUTE, minute)
            putExtra(AlarmRingActivity.EXTRA_LABEL, label)
            putExtra(AlarmRingActivity.EXTRA_SNOOZE_ALLOWED, snoozeAllowed)
            putExtra(AlarmRingActivity.EXTRA_SNOOZE_MINUTES, snoozeMinutes)
        }
        val ringPendingIntent = PendingIntent.getActivity(
            context,
            alarmId.hashCode(),
            ringIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_DISMISS_ALARM
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId.hashCode() + 1,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_SNOOZE_ALARM
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId.hashCode() + 2,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (label.isBlank()) "Alarm" else label
        val silent = SettingsRepository(context).load().silentNotifications
        val builder = NotificationCompat.Builder(context, ALARM_CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText("Tap to open")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setFullScreenIntent(ringPendingIntent, true)
            .setContentIntent(ringPendingIntent)
            .setSilent(silent)
            .addAction(0, "Dismiss", dismissPendingIntent)

        if (snoozeAllowed) {
            builder.addAction(0, "Snooze ${snoozeMinutes}m", snoozePendingIntent)
        }

        return builder.build()
    }

    fun showUpcomingAlarmNotification(
        context: Context,
        alarmId: String,
        label: String,
        timeText: String,
        leadText: String
    ) {
        if (!canPostNotifications(context)) return

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureUpcomingChannel(notificationManager)

        val title = if (label.isBlank()) "Upcoming alarm" else label
        val content = if (leadText == "Off") {
            "Alarm at $timeText"
        } else {
            "Alarm at $timeText ($leadText)"
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(AlarmRingActivity.EXTRA_ALARM_ID, alarmId)
            putExtra(MainActivity.EXTRA_TARGET_TAB, "alarm")
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            upcomingNotificationId(alarmId),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val silent = SettingsRepository(context).load().silentNotifications
        val notification = NotificationCompat.Builder(context, UPCOMING_CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .setSilent(silent)
            .build()

        notifySafely(context, notificationManager, upcomingNotificationId(alarmId), notification)
    }

    fun cancelUpcomingNotification(context: Context, alarmId: String) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(upcomingNotificationId(alarmId))
    }

    private fun upcomingNotificationId(alarmId: String): Int {
        return alarmId.hashCode() + 50_000
    }

    fun buildTimerNotification(
        context: Context,
        timerId: String,
        label: String,
        totalSeconds: Int
    ): Notification {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureTimerChannel(notificationManager)
        val notificationId = TimerScheduler.notificationIdFor(timerId)

        val openIntent = Intent(context, AlarmRingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(AlarmRingActivity.EXTRA_RING_TYPE, AlarmRingActivity.TYPE_TIMER)
            putExtra(AlarmRingActivity.EXTRA_TIMER_ID, timerId)
            putExtra(AlarmRingActivity.EXTRA_LABEL, label)
            putExtra(AlarmRingActivity.EXTRA_TIMER_TOTAL_SECONDS, totalSeconds)
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_DISMISS_TIMER
            putExtra(TimerScheduler.EXTRA_TIMER_ID, timerId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 1,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (label.isBlank()) "Timer finished" else label
        val silent = SettingsRepository(context).load().silentNotifications
        return NotificationCompat.Builder(context, TIMER_CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText("Timer complete")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setFullScreenIntent(openPendingIntent, true)
            .setContentIntent(openPendingIntent)
            .setSilent(silent)
            .addAction(0, "Dismiss", dismissPendingIntent)
            .build()
    }

    fun showActiveTimerNotification(context: Context, timer: TimerState) {
        if (!timer.isActive() || !canPostNotifications(context)) return

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureActiveTimerChannel(notificationManager)
        val notificationId = TimerScheduler.notificationIdFor(timer.id)
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_TARGET_TAB, "timer")
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val title = if (timer.label.isBlank()) "Timer running" else timer.label
        val silent = SettingsRepository(context).load().silentNotifications
        val builder = NotificationCompat.Builder(context, ACTIVE_TIMER_CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(TimeUtils.formatDuration(timer.liveRemainingSeconds().toLong()))
            .setWhen(timer.endTimeMillis)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setContentIntent(openPendingIntent)
            .setSilent(silent)

        if (LiveUpdateCompatibility.shouldRequestPromotion(Build.VERSION.SDK_INT)) {
            // Android 16 may surface this as a status-bar chip (for example, OxygenOS Live Alerts).
            // The system and user settings decide whether the request is promoted.
            builder
                .setOngoing(true)
                .setRequestPromotedOngoing(true)
        } else {
            // Preserve the existing, dismissible notification behavior on Android 15 and lower.
            builder.setOngoing(false)
        }

        val notification = builder.build()

        notifySafely(context, notificationManager, notificationId, notification)
    }

    fun showStopwatchMarkNotification(context: Context, targetLabel: String) {
        vibrate(context)
        if (!canPostNotifications(context)) return

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureTimerChannel(notificationManager)

        val silent = SettingsRepository(context).load().silentNotifications
        val notification = NotificationCompat.Builder(context, TIMER_CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Stopwatch alert")
            .setContentText("Reached $targetLabel")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setSilent(silent)
            .build()

        notifySafely(context, notificationManager, STOPWATCH_MARK_NOTIFICATION_ID, notification)
    }

    fun showActiveStopwatchNotification(context: Context, stopwatchId: String, elapsedMs: Long) {
        if (!canPostNotifications(context)) return

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureActiveTimerChannel(notificationManager)
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_TARGET_TAB, "stopwatch")
        }
        val notificationId = stopwatchNotificationId(stopwatchId)
        val openPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val lapIntent = Intent(context, StopwatchActionReceiver::class.java).apply {
            action = StopwatchActionReceiver.ACTION_ADD_LAP
            putExtra(StopwatchActionReceiver.EXTRA_STOPWATCH_ID, stopwatchId)
        }
        val lapPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 1,
            lapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = Intent(context, StopwatchActionReceiver::class.java).apply {
            action = StopwatchActionReceiver.ACTION_STOP
            putExtra(StopwatchActionReceiver.EXTRA_STOPWATCH_ID, stopwatchId)
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 2,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val silent = SettingsRepository(context).load().silentNotifications
        val notification = NotificationCompat.Builder(context, ACTIVE_TIMER_CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Stopwatch")
            .setContentText(TimeUtils.formatDuration(elapsedMs / 1000))
            .setWhen(System.currentTimeMillis() - elapsedMs)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setOngoing(false)
            .setContentIntent(openPendingIntent)
            .addAction(0, "Add lap", lapPendingIntent)
            .addAction(0, "Stop", stopPendingIntent)
            .setSilent(silent)
            .build()

        notifySafely(context, notificationManager, notificationId, notification)
    }

    fun cancelActiveStopwatchNotification(context: Context, stopwatchId: String) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(stopwatchNotificationId(stopwatchId))
    }

    private fun stopwatchNotificationId(stopwatchId: String): Int =
        STOPWATCH_NOTIFICATION_ID + (stopwatchId.hashCode() and 0xFFFF)

    fun cancelStopwatchMarkNotification(context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(STOPWATCH_MARK_NOTIFICATION_ID)
    }

    fun cancelAlarmNotification(context: Context, alarmId: String) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(alarmId.hashCode())
    }

    fun alarmNotificationId(alarmId: String): Int = alarmId.hashCode()

    fun cancelTimerNotification(context: Context, timerId: String) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(TimerScheduler.notificationIdFor(timerId))
    }

    private fun notifySafely(
        context: Context,
        notificationManager: NotificationManager,
        id: Int,
        notification: Notification
    ) {
        if (!canPostNotifications(context) || !notificationManager.areNotificationsEnabled()) return
        try {
            notificationManager.notify(id, notification)
        } catch (_: SecurityException) {
        }
    }

    private fun ensureAlarmChannel(notificationManager: NotificationManager) {
        val channel = NotificationChannel(
            ALARM_CHANNEL,
            "Alarms",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alarm notifications"
            enableVibration(true)
            setSound(null, null)
            setBypassDnd(true)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun ensureUpcomingChannel(notificationManager: NotificationManager) {
        val channel = NotificationChannel(
            UPCOMING_CHANNEL,
            "Upcoming alarms",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifications before alarms ring"
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun ensureTimerChannel(notificationManager: NotificationManager) {
        val channel = NotificationChannel(
            TIMER_CHANNEL,
            "Timers",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Timer and stopwatch notifications"
            enableVibration(true)
            setSound(null, null)
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun ensureActiveTimerChannel(notificationManager: NotificationManager) {
        val channel = NotificationChannel(
            ACTIVE_TIMER_CHANNEL,
            "Active timers",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Quiet live countdown and stopwatch notifications"
            enableVibration(false)
            setSound(null, null)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun vibrate(context: Context) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500), -1))
    }
}
