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
import ca.sekhrit.alarmpro.data.TimerRepository
import ca.sekhrit.alarmpro.domain.AlarmActions
import ca.sekhrit.alarmpro.data.upcomingAlarmLeadLabel
import ca.sekhrit.alarmpro.util.AlarmGrouping
import ca.sekhrit.alarmpro.util.AlarmSoundUtils
import ca.sekhrit.alarmpro.util.TimeUtils
import java.time.LocalTime

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            AlarmScheduler.ACTION_ALARM -> handleAlarm(context, intent)
            AlarmScheduler.ACTION_UPCOMING_ALARM -> handleUpcomingAlarm(context, intent)
            TimerScheduler.ACTION_TIMER -> handleTimer(context, intent)
            ACTION_DISMISS_ALARM -> {
                val alarmId = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_ID) ?: return
                AlarmActions.dismiss(context, alarmId)
            }
            ACTION_SNOOZE_ALARM -> {
                val alarmId = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_ID) ?: return
                AlarmActions.snooze(context, alarmId)
            }
            ACTION_DISMISS_TIMER -> {
                val timerId = intent.getStringExtra(TimerScheduler.EXTRA_TIMER_ID) ?: return
                TimerRepository(context).removeTimer(timerId)
                TimerScheduler(context).cancel(timerId)
                NotificationHelper.cancelTimerNotification(context, timerId)
            }
        }
    }

    private fun handleAlarm(context: Context, intent: Intent) {
        val alarmId = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_ID) ?: return
        val hour = intent.getIntExtra(AlarmScheduler.EXTRA_HOUR, 0)
        val minute = intent.getIntExtra(AlarmScheduler.EXTRA_MINUTE, 0)
        val label = intent.getStringExtra(AlarmScheduler.EXTRA_LABEL).orEmpty()
        val vibrate = intent.getBooleanExtra(AlarmScheduler.EXTRA_VIBRATE, true)
        val readLabelAloud = intent.getBooleanExtra(AlarmScheduler.EXTRA_READ_LABEL_ALOUD, false)

        val settings = SettingsRepository(context).load()
        val alarms = AlarmRepository(context).loadAlarms()
        val alarm = alarms.find { it.id == alarmId }
        val group = alarm?.groupId?.let { groupId ->
            AlarmGroupRepository(context).loadGroups().find { it.id == groupId }
        }
        val members = alarm?.groupId?.let { AlarmGrouping.membersOf(it, alarms) }.orEmpty()
        val spokenLabel = alarm?.let {
            AlarmGrouping.effectiveLabel(
                it,
                group,
                AlarmGrouping.indexInGroup(it, members)
            )
        } ?: label
        val snoozeAllowed = alarm?.isSnoozeAllowed(settings) ?: settings.defaultSnoozeEnabled
        val snoozeMinutes = alarm?.resolveSnoozeMinutes(settings) ?: settings.defaultSnoozeMinutes
        val soundUri = AlarmSoundUtils.resolvePlaybackUri(context, alarm, settings).toString()

        NotificationHelper.cancelUpcomingNotification(context, alarmId)

        NotificationHelper.showAlarmNotification(
            context,
            alarmId,
            hour,
            minute,
            label,
            vibrate,
            snoozeAllowed,
            snoozeMinutes
        )

        val ringIntent = Intent(context, AlarmRingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(AlarmRingActivity.EXTRA_RING_TYPE, AlarmRingActivity.TYPE_ALARM)
            putExtra(AlarmRingActivity.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmRingActivity.EXTRA_HOUR, hour)
            putExtra(AlarmRingActivity.EXTRA_MINUTE, minute)
            putExtra(AlarmRingActivity.EXTRA_LABEL, spokenLabel)
            putExtra(AlarmRingActivity.EXTRA_VIBRATE, vibrate)
            putExtra(AlarmRingActivity.EXTRA_READ_LABEL_ALOUD, readLabelAloud)
            putExtra(AlarmRingActivity.EXTRA_SNOOZE_ALLOWED, snoozeAllowed)
            putExtra(AlarmRingActivity.EXTRA_SNOOZE_MINUTES, snoozeMinutes)
            putExtra(AlarmRingActivity.EXTRA_SOUND_URI, soundUri)
        }
        context.startActivity(ringIntent)
    }

    private fun handleUpcomingAlarm(context: Context, intent: Intent) {
        val alarmId = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_ID) ?: return
        val hour = intent.getIntExtra(AlarmScheduler.EXTRA_HOUR, 0)
        val minute = intent.getIntExtra(AlarmScheduler.EXTRA_MINUTE, 0)
        val label = intent.getStringExtra(AlarmScheduler.EXTRA_LABEL).orEmpty()
        val leadMinutes = intent.getIntExtra(AlarmScheduler.EXTRA_LEAD_MINUTES, 60)
        val use24Hour = intent.getBooleanExtra(AlarmScheduler.EXTRA_USE_24H, false)
        val timeText = TimeUtils.formatTime(LocalTime.of(hour, minute), use24Hour)
        val leadText = upcomingAlarmLeadLabel(leadMinutes)

        NotificationHelper.showUpcomingAlarmNotification(
            context = context,
            alarmId = alarmId,
            label = label,
            timeText = timeText,
            leadText = leadText
        )
    }

    private fun handleTimer(context: Context, intent: Intent) {
        val timerId = intent.getStringExtra(TimerScheduler.EXTRA_TIMER_ID) ?: return
        val label = intent.getStringExtra(TimerScheduler.EXTRA_TIMER_LABEL).orEmpty()
        val totalSeconds = intent.getIntExtra(TimerScheduler.EXTRA_TIMER_TOTAL_SECONDS, 0)
        TimerRepository(context).removeTimer(timerId)
        NotificationHelper.showTimerNotification(context, timerId, label, totalSeconds)

        val ringIntent = Intent(context, AlarmRingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(AlarmRingActivity.EXTRA_RING_TYPE, AlarmRingActivity.TYPE_TIMER)
            putExtra(AlarmRingActivity.EXTRA_TIMER_ID, timerId)
            putExtra(AlarmRingActivity.EXTRA_LABEL, label)
            putExtra(AlarmRingActivity.EXTRA_TIMER_TOTAL_SECONDS, totalSeconds)
        }
        context.startActivity(ringIntent)
    }

    companion object {
        const val ACTION_DISMISS_ALARM = "ca.sekhrit.alarmpro.DISMISS_ALARM"
        const val ACTION_SNOOZE_ALARM = "ca.sekhrit.alarmpro.SNOOZE_ALARM"
        const val ACTION_DISMISS_TIMER = "ca.sekhrit.alarmpro.DISMISS_TIMER"
    }
}
