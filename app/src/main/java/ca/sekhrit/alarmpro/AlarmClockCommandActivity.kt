package ca.sekhrit.alarmpro

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.AlarmClock
import android.widget.Toast
import ca.sekhrit.alarmpro.data.Alarm
import ca.sekhrit.alarmpro.data.AlarmRepository
import ca.sekhrit.alarmpro.data.SettingsRepository
import ca.sekhrit.alarmpro.data.TimerRepository
import ca.sekhrit.alarmpro.data.TimerState
import ca.sekhrit.alarmpro.receiver.AlarmScheduler
import ca.sekhrit.alarmpro.receiver.NotificationHelper
import ca.sekhrit.alarmpro.receiver.TimerScheduler
import java.time.LocalTime

/**
 * Handles Android's public alarm-clock contract.  This is deliberately a
 * separate activity from MainActivity: callers such as Assistant need a
 * resolvable action which immediately performs the requested operation.
 */
class AlarmClockCommandActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleCommand(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleCommand(intent)
    }

    private fun handleCommand(command: Intent) {
        when (command.action) {
            AlarmClock.ACTION_SET_ALARM -> setAlarm(command)
            AlarmClock.ACTION_SET_TIMER -> setTimer(command)
            AlarmClock.ACTION_SHOW_ALARMS -> openMain("alarm")
            AlarmClock.ACTION_SHOW_TIMERS -> openMain("timer")
            else -> openMain("alarm")
        }
    }

    private fun setAlarm(command: Intent) {
        val hour = command.getIntExtra(AlarmClock.EXTRA_HOUR, -1)
        val minute = command.getIntExtra(AlarmClock.EXTRA_MINUTES, -1)
        if (hour !in 0..23 || minute !in 0..59) {
            openMain("alarm")
            return
        }

        val settings = SettingsRepository(this).load()
        val alarm = Alarm(
            time = LocalTime.of(hour, minute),
            label = command.getStringExtra(AlarmClock.EXTRA_MESSAGE).orEmpty().trim(),
            vibrate = settings.defaultVibrate,
            readLabelAloud = settings.defaultReadLabelAloud,
            snoozeEnabled = settings.defaultSnoozeEnabled,
            snoozeMinutes = settings.defaultSnoozeMinutes,
            soundUri = settings.defaultAlarmSoundUri
        )
        val repository = AlarmRepository(this)
        repository.saveAlarms(repository.loadAlarms() + alarm)
        AlarmScheduler(this).schedule(alarm)
        openMain("alarm", "Alarm set for %02d:%02d".format(hour, minute))
    }

    private fun setTimer(command: Intent) {
        val seconds = command.getIntExtra(AlarmClock.EXTRA_LENGTH, 0)
        if (seconds <= 0) {
            openMain("timer")
            return
        }

        val label = command.getStringExtra(AlarmClock.EXTRA_MESSAGE).orEmpty().trim()
            .ifBlank { formatTimerLabel(seconds) }
        val timer = TimerState(
            totalSeconds = seconds,
            remainingSeconds = seconds,
            endTimeMillis = System.currentTimeMillis() + seconds * 1_000L,
            isRunning = true,
            label = label
        )
        val repository = TimerRepository(this)
        repository.saveAll(repository.loadAll() + timer)
        TimerScheduler(this).schedule(timer.id, timer.endTimeMillis, timer.label, timer.totalSeconds)
        NotificationHelper.showActiveTimerNotification(this, timer)
        openMain("timer", "Timer started")
    }

    private fun openMain(tab: String, confirmation: String? = null) {
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_TARGET_TAB, tab)
        })
        confirmation?.let { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }
        finish()
    }

    private fun formatTimerLabel(seconds: Int): String = when {
        seconds % 3_600 == 0 -> "${seconds / 3_600} hr timer"
        seconds % 60 == 0 -> "${seconds / 60} min timer"
        else -> "$seconds sec timer"
    }
}
