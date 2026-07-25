package ca.sekhrit.alarmpro.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import androidx.core.content.ContextCompat
import ca.sekhrit.alarmpro.AlarmRingActivity
import ca.sekhrit.alarmpro.data.SettingsRepository
import ca.sekhrit.alarmpro.data.timerSpeechText
import ca.sekhrit.alarmpro.receiver.NotificationHelper
import ca.sekhrit.alarmpro.receiver.TimerScheduler
import ca.sekhrit.alarmpro.util.AlarmSoundUtils
import java.util.Locale

/**
 * Owns every effect that must continue even when Android chooses to show a
 * heads-up alarm notification instead of launching the full-screen activity.
 */
class AlarmRingingService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var textToSpeech: TextToSpeech? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        stopEffects()
        when (intent.action) {
            ACTION_START_ALARM -> startAlarm(intent)
            ACTION_START_TIMER -> startTimer(intent)
            else -> stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun startAlarm(intent: Intent) {
        val alarmId = intent.getStringExtra(AlarmRingActivity.EXTRA_ALARM_ID)
            ?.takeIf { it.isNotBlank() } ?: run {
            stopSelf()
            return
        }
        val hour = intent.getIntExtra(AlarmRingActivity.EXTRA_HOUR, 0)
        val minute = intent.getIntExtra(AlarmRingActivity.EXTRA_MINUTE, 0)
        val label = intent.getStringExtra(AlarmRingActivity.EXTRA_LABEL).orEmpty()
        val vibrate = intent.getBooleanExtra(AlarmRingActivity.EXTRA_VIBRATE, true)
        val readLabelAloud =
            intent.getBooleanExtra(AlarmRingActivity.EXTRA_READ_LABEL_ALOUD, false)
        val snoozeAllowed =
            intent.getBooleanExtra(AlarmRingActivity.EXTRA_SNOOZE_ALLOWED, true)
        val snoozeMinutes =
            intent.getIntExtra(AlarmRingActivity.EXTRA_SNOOZE_MINUTES, 10)

        val notification = NotificationHelper.buildAlarmNotification(
            context = this,
            alarmId = alarmId,
            hour = hour,
            minute = minute,
            label = label,
            snoozeAllowed = snoozeAllowed,
            snoozeMinutes = snoozeMinutes
        )
        startForeground(NotificationHelper.alarmNotificationId(alarmId), notification)
        acquireWakeLock()
        startAlarmSound(
            intent.getStringExtra(AlarmRingActivity.EXTRA_SOUND_URI)
                ?.takeIf { it.isNotBlank() }
                ?.let(Uri::parse)
                ?: AlarmSoundUtils.systemDefaultUri()
        )
        if (vibrate) startVibration()
        if (readLabelAloud && label.isNotBlank()) speakText(label)
    }

    private fun startTimer(intent: Intent) {
        val timerId = intent.getStringExtra(AlarmRingActivity.EXTRA_TIMER_ID)
            ?.takeIf { it.isNotBlank() } ?: run {
            stopSelf()
            return
        }
        val label = intent.getStringExtra(AlarmRingActivity.EXTRA_LABEL).orEmpty()
        val totalSeconds =
            intent.getIntExtra(AlarmRingActivity.EXTRA_TIMER_TOTAL_SECONDS, 0)

        val notification = NotificationHelper.buildTimerNotification(
            context = this,
            timerId = timerId,
            label = label,
            totalSeconds = totalSeconds
        )
        startForeground(TimerScheduler.notificationIdFor(timerId), notification)
        acquireWakeLock()
        startAlarmSound(AlarmSoundUtils.systemDefaultUri())
        startVibration()
        val settings = SettingsRepository(this).load()
        timerSpeechText(settings.timerSpeechFormat, label, totalSeconds)?.let(::speakText)
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "$packageName:alarm-ringing"
        ).apply {
            acquire(MAX_RING_DURATION_MILLIS)
        }
    }

    private fun startAlarmSound(uri: Uri) {
        fun createPlayer(playbackUri: Uri): MediaPlayer {
            return MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(this@AlarmRingingService, playbackUri)
                isLooping = true
                prepare()
                start()
            }
        }

        mediaPlayer = try {
            createPlayer(uri)
        } catch (_: Exception) {
            try {
                createPlayer(AlarmSoundUtils.systemDefaultUri())
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun startVibration() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
        vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 800, 800), 0))
    }

    private fun speakText(text: String) {
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.getDefault()
                textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "alarmpro_speech")
            }
        }
    }

    private fun stopEffects() {
        mediaPlayer?.runCatching { stop() }
        mediaPlayer?.release()
        mediaPlayer = null
        vibrator?.cancel()
        vibrator = null
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    override fun onDestroy() {
        stopEffects()
        super.onDestroy()
    }

    companion object {
        private const val ACTION_START_ALARM = "ca.sekhrit.alarmpro.START_RINGING_ALARM"
        private const val ACTION_START_TIMER = "ca.sekhrit.alarmpro.START_RINGING_TIMER"
        private const val MAX_RING_DURATION_MILLIS = 30 * 60 * 1000L

        fun startAlarm(
            context: Context,
            alarmId: String,
            hour: Int,
            minute: Int,
            label: String,
            vibrate: Boolean,
            readLabelAloud: Boolean,
            snoozeAllowed: Boolean,
            snoozeMinutes: Int,
            soundUri: String
        ) {
            val intent = Intent(context, AlarmRingingService::class.java).apply {
                action = ACTION_START_ALARM
                putExtra(AlarmRingActivity.EXTRA_RING_TYPE, AlarmRingActivity.TYPE_ALARM)
                putExtra(AlarmRingActivity.EXTRA_ALARM_ID, alarmId)
                putExtra(AlarmRingActivity.EXTRA_HOUR, hour)
                putExtra(AlarmRingActivity.EXTRA_MINUTE, minute)
                putExtra(AlarmRingActivity.EXTRA_LABEL, label)
                putExtra(AlarmRingActivity.EXTRA_VIBRATE, vibrate)
                putExtra(AlarmRingActivity.EXTRA_READ_LABEL_ALOUD, readLabelAloud)
                putExtra(AlarmRingActivity.EXTRA_SNOOZE_ALLOWED, snoozeAllowed)
                putExtra(AlarmRingActivity.EXTRA_SNOOZE_MINUTES, snoozeMinutes)
                putExtra(AlarmRingActivity.EXTRA_SOUND_URI, soundUri)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun startTimer(
            context: Context,
            timerId: String,
            label: String,
            totalSeconds: Int
        ) {
            val intent = Intent(context, AlarmRingingService::class.java).apply {
                action = ACTION_START_TIMER
                putExtra(AlarmRingActivity.EXTRA_RING_TYPE, AlarmRingActivity.TYPE_TIMER)
                putExtra(AlarmRingActivity.EXTRA_TIMER_ID, timerId)
                putExtra(AlarmRingActivity.EXTRA_LABEL, label)
                putExtra(AlarmRingActivity.EXTRA_TIMER_TOTAL_SECONDS, totalSeconds)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, AlarmRingingService::class.java))
        }
    }
}
