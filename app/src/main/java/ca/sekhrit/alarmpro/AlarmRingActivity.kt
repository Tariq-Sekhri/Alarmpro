package ca.sekhrit.alarmpro

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ca.sekhrit.alarmpro.data.AlarmRepository
import ca.sekhrit.alarmpro.data.SettingsRepository
import ca.sekhrit.alarmpro.data.TimerRepository
import ca.sekhrit.alarmpro.domain.AlarmActions
import ca.sekhrit.alarmpro.receiver.NotificationHelper
import ca.sekhrit.alarmpro.receiver.TimerScheduler
import ca.sekhrit.alarmpro.service.AlarmRingingService
import ca.sekhrit.alarmpro.ui.theme.AlarmProTheme
import ca.sekhrit.alarmpro.util.AlarmSoundUtils
import ca.sekhrit.alarmpro.util.AlarmTimeParts
import ca.sekhrit.alarmpro.util.TimeUtils
import android.net.Uri
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class AlarmRingActivity : ComponentActivity() {
    private var mediaPlayer: MediaPlayer? = null
    private val ringingStoppedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_RINGING_STOPPED) finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
        )

        val ringType = intent.getStringExtra(EXTRA_RING_TYPE) ?: TYPE_ALARM
        val alarmId = intent.getStringExtra(EXTRA_ALARM_ID).orEmpty()
        val timerId = intent.getStringExtra(EXTRA_TIMER_ID).orEmpty()
        val hour = intent.getIntExtra(EXTRA_HOUR, 7)
        val minute = intent.getIntExtra(EXTRA_MINUTE, 0)
        val label = intent.getStringExtra(EXTRA_LABEL).orEmpty()
        val snoozeAllowed = intent.getBooleanExtra(EXTRA_SNOOZE_ALLOWED, true)
        val settings = SettingsRepository(this).load()

        // Alarm/timer effects live in AlarmRingingService so they remain reliable
        // when Android displays only a heads-up notification. Preview audio is
        // intentionally activity-owned.
        if (ringType == TYPE_PREVIEW) {
            val previewUri =
                intent.getStringExtra(EXTRA_SOUND_URI)?.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
                    ?: AlarmSoundUtils.resolvePlaybackUri(this, null, settings)
            startAlarmSound(previewUri)
        }

        val headline = when (ringType) {
            TYPE_PREVIEW -> "Preview"
            TYPE_TIMER -> if (label.isBlank()) "Timer" else label
            else -> "Alarm"
        }
        val timeParts = if (ringType == TYPE_ALARM || ringType == TYPE_PREVIEW) {
            TimeUtils.formatAlarmTimeParts(LocalTime.of(hour, minute), settings.use24HourFormat)
        } else {
            null
        }
        val dayLine = LocalDate.now()
            .format(DateTimeFormatter.ofPattern("EEEE", Locale.getDefault()))
            .uppercase(Locale.getDefault())

        setContent {
            AlarmProTheme {
                DefaultAlarmRingScreen(
                    headline = headline,
                    timeParts = timeParts,
                    dayLine = dayLine,
                    showSnooze = (ringType == TYPE_ALARM || ringType == TYPE_PREVIEW) && snoozeAllowed,
                    onSnooze = {
                        stopEffects()
                        if (ringType == TYPE_ALARM) {
                            AlarmActions.snooze(this@AlarmRingActivity, alarmId)
                        }
                        AlarmRingingService.stop(this@AlarmRingActivity)
                        finish()
                    },
                    onDismiss = {
                        stopEffects()
                        when (ringType) {
                            TYPE_ALARM -> AlarmActions.dismiss(this@AlarmRingActivity, alarmId)
                            TYPE_PREVIEW -> Unit
                            else -> {
                                if (timerId.isNotBlank()) {
                                    TimerRepository(this@AlarmRingActivity).removeTimer(timerId)
                                    TimerScheduler(this@AlarmRingActivity).cancel(timerId)
                                    NotificationHelper.cancelTimerNotification(this@AlarmRingActivity, timerId)
                                }
                            }
                        }
                        AlarmRingingService.stop(this@AlarmRingActivity)
                        finish()
                    }
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        ContextCompat.registerReceiver(
            this,
            ringingStoppedReceiver,
            IntentFilter(ACTION_RINGING_STOPPED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        recreate()
    }

    override fun onStop() {
        unregisterReceiver(ringingStoppedReceiver)
        super.onStop()
    }

    private fun startAlarmSound(uri: Uri) {
        mediaPlayer = MediaPlayer().apply {
            setDataSource(this@AlarmRingActivity, uri)
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            isLooping = true
            prepare()
            start()
        }
    }

    private fun stopEffects() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun onDestroy() {
        stopEffects()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_RING_TYPE = "RING_TYPE"
        const val EXTRA_ALARM_ID = "ALARM_ID"
        const val EXTRA_HOUR = "HOUR"
        const val EXTRA_MINUTE = "MINUTE"
        const val EXTRA_LABEL = "LABEL"
        const val EXTRA_VIBRATE = "VIBRATE"
        const val EXTRA_READ_LABEL_ALOUD = "READ_LABEL_ALOUD"
        const val EXTRA_SNOOZE_ALLOWED = "SNOOZE_ALLOWED"
        const val EXTRA_SNOOZE_MINUTES = "SNOOZE_MINUTES"
        const val EXTRA_SOUND_URI = "SOUND_URI"
        const val EXTRA_TIMER_ID = "TIMER_ID"
        const val EXTRA_TIMER_TOTAL_SECONDS = "TIMER_TOTAL_SECONDS"
        const val TYPE_ALARM = "alarm"
        const val TYPE_TIMER = "timer"
        const val TYPE_PREVIEW = "preview"
        private const val ACTION_RINGING_STOPPED =
            "ca.sekhrit.alarmpro.action.RINGING_STOPPED"

        fun notifyRingingStopped(context: Context) {
            context.sendBroadcast(
                Intent(ACTION_RINGING_STOPPED).setPackage(context.packageName)
            )
        }
    }
}

private val RingBackground = Color.Black
private val RingMuted = Color(0xFF8A8A8A)
private val RingButtonSurface = Color(0xFF1B2230)

@Composable
private fun DefaultAlarmRingScreen(
    headline: String,
    timeParts: AlarmTimeParts?,
    dayLine: String,
    showSnooze: Boolean,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RingBackground)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = 72.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = headline,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                color = RingMuted,
                textAlign = TextAlign.Center
            )

            if (timeParts != null) {
                Spacer(modifier = Modifier.height(28.dp))
                Text(
                    text = timeParts.time,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Thin,
                    fontSize = 96.sp,
                    letterSpacing = (-3).sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                timeParts.period?.let { period ->
                    Text(
                        text = period,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Normal,
                        fontSize = 20.sp,
                        color = RingMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(if (timeParts?.period != null) 10.dp else 14.dp))
            Text(
                text = dayLine,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                letterSpacing = 4.sp,
                color = RingMuted,
                textAlign = TextAlign.Center
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(if (showSnooze) 0.52f else 0.24f)
                .padding(horizontal = 16.dp)
                .padding(bottom = 20.dp)
                .navigationBarsPadding()
        ) {
            if (showSnooze) {
                RingActionButton(
                    text = "SNOOZE",
                    onClick = onSnooze,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                RingActionButton(
                    text = "DISMISS",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                )
            } else {
                RingActionButton(
                    text = "DISMISS",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun RingActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(RingButtonSurface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = 40.sp,
            letterSpacing = 0.5.sp,
            color = Color.White
        )
    }
}
