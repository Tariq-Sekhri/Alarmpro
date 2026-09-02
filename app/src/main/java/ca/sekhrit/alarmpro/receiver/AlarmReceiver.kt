package ca.sekhrit.alarmpro.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ca.sekhrit.alarmpro.AlarmRingActivity
import ca.sekhrit.alarmpro.data.AlarmGroupRepository
import ca.sekhrit.alarmpro.data.AlarmRepository
import ca.sekhrit.alarmpro.data.SettingsRepository
import ca.sekhrit.alarmpro.data.isSnoozeAllowed
import ca.sekhrit.alarmpro.data.resolveSnoozeMinutes
import ca.sekhrit.alarmpro.MainActivity
import ca.sekhrit.alarmpro.data.TimerRepository
import ca.sekhrit.alarmpro.data.TimerPresetRepository
import ca.sekhrit.alarmpro.domain.AlarmActions
import ca.sekhrit.alarmpro.data.upcomingAlarmLeadLabel
import ca.sekhrit.alarmpro.util.AlarmGrouping
import ca.sekhrit.alarmpro.util.AlarmSoundUtils
import ca.sekhrit.alarmpro.util.TimeUtils
import ca.sekhrit.alarmpro.service.AlarmRingingService
import ca.sekhrit.alarmpro.viewmodel.TimerViewModel

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            AlarmScheduler.ACTION_ALARM -> handleAlarm(context, intent)
            AlarmScheduler.ACTION_UPCOMING_ALARM -> handleUpcomingAlarm(context, intent)
            TimerScheduler.ACTION_TIMER -> handleTimer(context, intent)
            ACTION_DISMISS_ALARM -> {
                val alarmId = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_ID) ?: return
                AlarmActions.dismiss(context, alarmId)
                AlarmRingingService.stop(context)
                AlarmRingActivity.notifyRingingStopped(context)
            }
            ACTION_SNOOZE_ALARM -> {
                val alarmId = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_ID) ?: return
                AlarmActions.snooze(context, alarmId)
                AlarmRingingService.stop(context)
                AlarmRingActivity.notifyRingingStopped(context)
            }
            ACTION_CANCEL_ALARM -> {
                val alarmId = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_ID) ?: return
                AlarmActions.cancel(context, alarmId)
            }
            ACTION_SKIP_ALARM -> {
                val alarmId = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_ID) ?: return
                AlarmActions.skipNext(context, alarmId)
            }
            ACTION_DISMISS_TIMER -> {
                val timerId = intent.getStringExtra(TimerScheduler.EXTRA_TIMER_ID) ?: return
                TimerRepository(context).removeTimer(timerId)
                intent.getStringExtra(AlarmRingActivity.EXTRA_ASSISTANT_TIMER_PRESET_ID)
                    ?.takeIf { SettingsRepository(context).load().deleteAssistantTimersOnDismiss }
                    ?.let { TimerPresetRepository(context).removePreset(it) }
                TimerScheduler(context).cancel(timerId)
                NotificationHelper.cancelTimerNotification(context, timerId)
                AlarmRingingService.stop(context)
                AlarmRingActivity.notifyRingingStopped(context)
                TimerViewModel.instance()?.syncFromStorage()
            }
        }
    }

    private fun handleAlarm(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val alarmId = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_ID) ?: run {
            pendingResult.finish()
            return
        }
        val isSnooze = intent.getBooleanExtra(AlarmScheduler.EXTRA_IS_SNOOZE, false)
        val prepared = prepareScheduledAlarm(context, alarmId) ?: run {
            pendingResult.finish()
            return
        }
        launchRingingActivity(context, alarmId.hashCode(), prepared.ringIntent)
        AlarmRingingService.startAlarm(
            context = context,
            alarmId = alarmId,
            hour = prepared.hour,
            minute = prepared.minute,
            label = prepared.label,
            vibrate = prepared.vibrate,
            readLabelAloud = prepared.readLabelAloud,
            snoozeAllowed = prepared.snoozeAllowed,
            snoozeMinutes = prepared.snoozeMinutes,
            soundUri = prepared.soundUri,
            isSnooze = isSnooze
        )
        pendingResult.finish()
    }

    private fun handleUpcomingAlarm(context: Context, intent: Intent) {
        val alarmId = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_ID) ?: return
        val alarm = AlarmRepository(context).loadAlarms()
            .find { it.id == alarmId && it.isEnabled } ?: run {
            AlarmScheduler(context).cancel(alarmId)
            NotificationHelper.cancelUpcomingNotification(context, alarmId)
            return
        }
        val settings = SettingsRepository(context).load()
        val leadMinutes = settings.upcomingAlarmLeadMinutes
        if (leadMinutes <= 0) {
            NotificationHelper.cancelUpcomingNotification(context, alarmId)
            return
        }
        val timeText = TimeUtils.formatTime(alarm.time, settings.use24HourFormat)
        val leadText = upcomingAlarmLeadLabel(leadMinutes)

        NotificationHelper.showUpcomingAlarmNotification(
            context = context,
            alarmId = alarmId,
            label = alarm.label,
            timeText = timeText,
            leadText = leadText,
            isRepeating = alarm.repeat.type != ca.sekhrit.alarmpro.data.RepeatType.ONCE
        )
    }

    private fun handleTimer(context: Context, intent: Intent) {
        val timerId = intent.getStringExtra(TimerScheduler.EXTRA_TIMER_ID) ?: return
        completeTimer(context, timerId)
    }

    companion object {
        private val timerCompletionLock = Any()

        private data class PreparedAlarm(
            val ringIntent: Intent,
            val hour: Int,
            val minute: Int,
            val label: String,
            val vibrate: Boolean,
            val readLabelAloud: Boolean,
            val snoozeAllowed: Boolean,
            val snoozeMinutes: Int,
            val soundUri: String
        )

        private fun prepareScheduledAlarm(
            context: Context,
            alarmId: String
        ): PreparedAlarm? {
        val settings = SettingsRepository(context).load()
        val alarms = AlarmRepository(context).loadAlarms()
        val alarm = alarms.find { it.id == alarmId }
        if (alarm == null || !alarm.isEnabled) {
            AlarmScheduler(context).cancel(alarmId)
            NotificationHelper.cancelAlarmNotification(context, alarmId)
            return null
        }
        val group = alarm.groupId?.let { groupId ->
            AlarmGroupRepository(context).loadGroups().find { it.id == groupId }
        }
        val members = alarm.groupId?.let { AlarmGrouping.membersOf(it, alarms) }.orEmpty()
        val spokenLabel = alarm.let {
            AlarmGrouping.effectiveLabel(
                it,
                group,
                AlarmGrouping.indexInGroup(it, members)
            )
        }
        val snoozeAllowed = alarm.isSnoozeAllowed(settings)
        val snoozeMinutes = alarm.resolveSnoozeMinutes(settings)
        val soundUri = AlarmSoundUtils.resolvePlaybackUri(context, alarm, settings).toString()

        NotificationHelper.cancelUpcomingNotification(context, alarmId)

        val ringIntent = Intent(context, AlarmRingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(AlarmRingActivity.EXTRA_RING_TYPE, AlarmRingActivity.TYPE_ALARM)
            putExtra(AlarmRingActivity.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmRingActivity.EXTRA_HOUR, alarm.time.hour)
            putExtra(AlarmRingActivity.EXTRA_MINUTE, alarm.time.minute)
            putExtra(AlarmRingActivity.EXTRA_LABEL, spokenLabel)
            putExtra(AlarmRingActivity.EXTRA_SNOOZE_ALLOWED, snoozeAllowed)
            putExtra(AlarmRingActivity.EXTRA_SNOOZE_MINUTES, snoozeMinutes)
        }
        return PreparedAlarm(
            ringIntent = ringIntent,
            hour = alarm.time.hour,
            minute = alarm.time.minute,
            label = spokenLabel,
            vibrate = alarm.vibrate,
            readLabelAloud = alarm.readLabelAloud,
            snoozeAllowed = snoozeAllowed,
            snoozeMinutes = snoozeMinutes,
            soundUri = soundUri
        )
    }

        /**
         * Claims a completed timer before starting its alert. Both the exact
         * alarm and the foreground countdown ticker use this path, so the
         * ticker can alert immediately without a delayed alarm firing twice.
         */
        fun completeTimer(context: Context, timerId: String) {
            synchronized(timerCompletionLock) {
                val repository = TimerRepository(context)
                val timer = repository.loadAll().find { it.id == timerId } ?: run {
                    TimerScheduler(context).cancel(timerId)
                    NotificationHelper.cancelTimerNotification(context, timerId)
                    return
                }
                val label = timer.label
                val totalSeconds = timer.totalSeconds
                val assistantPresetId = timer.presetId
                    ?.let { presetId ->
                        TimerPresetRepository(context).loadPresets()
                            .find { it.id == presetId && it.isAssistantCreated }
                            ?.id
                    }
                TimerScheduler(context).cancel(timerId)
                NotificationHelper.cancelTimerNotification(context, timerId)
                repository.removeTimer(timerId)
                TimerViewModel.instance()?.syncFromStorage()
                MainActivity.dismissPipIfActive()
                launchRingingActivity(
                    context = context,
                    ringType = AlarmRingActivity.TYPE_TIMER,
                    timerId = timerId,
                    label = label,
                    totalSeconds = totalSeconds,
                    assistantPresetId = assistantPresetId
                )
                AlarmRingingService.startTimer(context, timerId, label, totalSeconds, assistantPresetId)
            }
        }

        private fun launchRingingActivity(
            context: Context,
            ringType: String,
            alarmId: String = "",
            timerId: String = "",
            hour: Int = 0,
            minute: Int = 0,
            label: String = "",
            snoozeAllowed: Boolean = true,
            snoozeMinutes: Int = 10,
            totalSeconds: Int = 0,
            assistantPresetId: String? = null
        ) {
            val ringIntent = Intent(context, AlarmRingActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(AlarmRingActivity.EXTRA_RING_TYPE, ringType)
                putExtra(AlarmRingActivity.EXTRA_ALARM_ID, alarmId)
                putExtra(AlarmRingActivity.EXTRA_TIMER_ID, timerId)
                putExtra(AlarmRingActivity.EXTRA_HOUR, hour)
                putExtra(AlarmRingActivity.EXTRA_MINUTE, minute)
                putExtra(AlarmRingActivity.EXTRA_LABEL, label)
                putExtra(AlarmRingActivity.EXTRA_SNOOZE_ALLOWED, snoozeAllowed)
                putExtra(AlarmRingActivity.EXTRA_SNOOZE_MINUTES, snoozeMinutes)
                putExtra(AlarmRingActivity.EXTRA_TIMER_TOTAL_SECONDS, totalSeconds)
                assistantPresetId?.let {
                    putExtra(AlarmRingActivity.EXTRA_ASSISTANT_TIMER_PRESET_ID, it)
                }
            }
            val requestCode = if (alarmId.isNotBlank()) {
                alarmId.hashCode()
            } else {
                TimerScheduler.notificationIdFor(timerId)
            }
            launchRingingActivity(context, requestCode, ringIntent)
        }

        private fun launchRingingActivity(
            context: Context,
            requestCode: Int,
            intent: Intent
        ) {
            NotificationHelper.launchRingingActivity(context, requestCode, intent)
        }

        const val ACTION_DISMISS_ALARM = "ca.sekhrit.alarmpro.DISMISS_ALARM"
        const val ACTION_SNOOZE_ALARM = "ca.sekhrit.alarmpro.SNOOZE_ALARM"
        const val ACTION_CANCEL_ALARM = "ca.sekhrit.alarmpro.CANCEL_ALARM"
        const val ACTION_SKIP_ALARM = "ca.sekhrit.alarmpro.SKIP_ALARM"
        const val ACTION_DISMISS_TIMER = "ca.sekhrit.alarmpro.DISMISS_TIMER"
    }
}
