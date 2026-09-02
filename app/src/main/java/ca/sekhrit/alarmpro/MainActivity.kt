package ca.sekhrit.alarmpro

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.os.Bundle
import android.os.Build
import android.os.SystemClock
import android.content.res.Configuration
import android.util.Rational
import android.graphics.drawable.Icon
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.navigation.NavGraph.Companion.findStartDestination
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ca.sekhrit.alarmpro.ui.components.AppBottomBar
import ca.sekhrit.alarmpro.ui.RequestAppPermissions
import ca.sekhrit.alarmpro.ui.screens.AlarmEditScreen
import ca.sekhrit.alarmpro.ui.screens.AlarmScreen
import ca.sekhrit.alarmpro.ui.screens.ClockScreen
import ca.sekhrit.alarmpro.ui.screens.DefaultAlarmSettingsScreen
import ca.sekhrit.alarmpro.ui.screens.GeneralSettingsScreen
import ca.sekhrit.alarmpro.ui.screens.SettingsScreen
import ca.sekhrit.alarmpro.ui.screens.StopwatchScreen
import ca.sekhrit.alarmpro.ui.screens.StopwatchSettingsScreen
import ca.sekhrit.alarmpro.ui.screens.TimerScreen
import ca.sekhrit.alarmpro.ui.screens.TimerSettingsScreen
import ca.sekhrit.alarmpro.ui.screens.TimerSpeechFormatScreen
import ca.sekhrit.alarmpro.ui.screens.AlarmSpeechFormatScreen
import ca.sekhrit.alarmpro.ui.theme.AlarmProTheme
import ca.sekhrit.alarmpro.viewmodel.AlarmViewModel
import ca.sekhrit.alarmpro.viewmodel.TimerViewModel
import ca.sekhrit.alarmpro.data.StopwatchStateRepository
import ca.sekhrit.alarmpro.data.StopwatchRuntimeState
import ca.sekhrit.alarmpro.receiver.TimerActionReceiver
import ca.sekhrit.alarmpro.receiver.TimerScheduler
import ca.sekhrit.alarmpro.receiver.StopwatchActionReceiver
import ca.sekhrit.alarmpro.util.TimeUtils

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.channels.BufferOverflow
import android.content.Intent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import ca.sekhrit.alarmpro.ui.screens.NotesScreen

class MainActivity : ComponentActivity() {
    private val alarmViewModel: AlarmViewModel by viewModels()
    private val timerViewModel: TimerViewModel by viewModels()
    private val intentFlow = MutableSharedFlow<Intent>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private var pipContent by mutableStateOf<PipContent?>(null)
    private var isInPipMode by mutableStateOf(false)
    private var pipStopwatchStateJob: kotlinx.coroutines.Job? = null
    private var observedPipStopwatchId: String? = null
    private val stopwatchStateRepository by lazy { StopwatchStateRepository(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        
        intent?.let { intentFlow.tryEmit(it) }

        setStopwatchPipNotifier(::notifyStopwatchPipChanged)
        setTimerPipNotifier(::notifyTimerPipChanged)

        setContent {
            AlarmProTheme {
                MainScreen(
                    alarmViewModel = alarmViewModel,
                    timerViewModel = timerViewModel,
                    intentFlow = intentFlow,
                    isInPipMode = isInPipMode,
                    pipContent = pipContent,
                    onPipContentChanged = ::updatePipContent
                )
            }
        }
    }

    override fun onDestroy() {
        setStopwatchPipNotifier(null)
        setTimerPipNotifier(null)
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intentFlow.tryEmit(intent)
    }

    override fun onResume() {
        super.onResume()
        alarmViewModel.refreshFromStorage()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && pipContent != null) {
            enterPictureInPictureMode(buildPipParams(requireNotNull(pipContent)))
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)
        syncPipMode()
    }

    @Suppress("DEPRECATION")
    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        syncPipMode()
    }

    private fun updatePipContent(content: PipContent?, refreshObserver: Boolean = true) {
        if (content == null && (isInPipMode || isInPictureInPictureMode)) return
        if (pipContent == content) return
        pipContent = content
        if (refreshObserver) {
            observePipStopwatchState(content)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setPictureInPictureParams(content?.let(::buildPipParams) ?: PictureInPictureParams.Builder().build())
        }
    }

    private fun observePipStopwatchState(content: PipContent?) {
        val stopwatch = content as? PipContent.Stopwatch
        if (stopwatch == null) {
            pipStopwatchStateJob?.cancel()
            pipStopwatchStateJob = null
            observedPipStopwatchId = null
            return
        }
        if (observedPipStopwatchId == stopwatch.stopwatchId && pipStopwatchStateJob?.isActive == true) return
        observedPipStopwatchId = stopwatch.stopwatchId
        pipStopwatchStateJob?.cancel()
        pipStopwatchStateJob = lifecycleScope.launch {
            launch {
                StopwatchStateRepository.changes.collect { change ->
                    if (change.stopwatchId == stopwatch.stopwatchId) {
                        refreshPipStopwatch(change.stopwatchId, change.state)
                    }
                }
            }
            while (isActive) {
                val runtime = stopwatchStateRepository.load(stopwatch.stopwatchId)
                refreshPipStopwatch(stopwatch.stopwatchId, runtime)
                delay(if (runtime.isRunning) 200L else 1000L)
            }
        }
    }

    private fun refreshPipStopwatch(stopwatchId: String, runtime: StopwatchRuntimeState? = null) {
        val current = pipContent as? PipContent.Stopwatch ?: return
        if (current.stopwatchId != stopwatchId) return
        val state = runtime ?: stopwatchStateRepository.load(stopwatchId)
        runOnUiThread {
            updatePipContent(
                current.copy(
                    elapsedMs = state.liveElapsedMs(),
                    snapshotElapsedRealtime = SystemClock.elapsedRealtime(),
                    isRunning = state.isRunning
                ),
                refreshObserver = false
            )
        }
    }

    private fun notifyStopwatchPipChanged(stopwatchId: String, runtime: StopwatchRuntimeState? = null) {
        refreshPipStopwatch(stopwatchId, runtime)
    }

    private fun notifyTimerPipChanged(timerId: String) {
        val current = pipContent as? PipContent.Timer ?: return
        if (current.timerId != timerId) return
        val timer = timerViewModel.activeTimers.value.values.firstOrNull { it.id == timerId } ?: return
        val clock = timerViewModel.clockMillis.value
        runOnUiThread {
            updatePipContent(
                PipContent.Timer(
                    timerId = timer.id,
                    label = timer.label.ifBlank { "Timer" },
                    remainingSeconds = timer.liveRemainingSeconds(clock),
                    endTimeMillis = timer.endTimeMillis,
                    isRunning = timer.isRunning
                ),
                refreshObserver = false
            )
        }
    }

    private fun syncPipMode() {
        isInPipMode = isInPictureInPictureMode
    }

    private fun buildPipParams(content: PipContent): PictureInPictureParams {
        return PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
            .setActions(pipActions(content))
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    setAutoEnterEnabled(true)
                }
            }
            .build()
    }

    private fun pipActions(content: PipContent): List<RemoteAction> = when (content) {
        is PipContent.Timer -> listOf(
            remoteAction(
                if (content.isRunning) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (content.isRunning) "Pause" else "Resume",
                timerActionIntent(content.timerId, if (content.isRunning) TimerActionReceiver.ACTION_PAUSE else TimerActionReceiver.ACTION_RESUME, 1)
            ),
            remoteAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", timerActionIntent(content.timerId, TimerActionReceiver.ACTION_CLOSE, 2))
        )
        is PipContent.Stopwatch -> listOf(
            remoteAction(
                if (content.isRunning) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (content.isRunning) "Pause" else "Resume",
                stopwatchActionIntent(content.stopwatchId, StopwatchActionReceiver.ACTION_TOGGLE, 1)
            ),
            remoteAction(android.R.drawable.ic_input_add, "Lap", stopwatchActionIntent(content.stopwatchId, StopwatchActionReceiver.ACTION_ADD_LAP, 2)),
            remoteAction(android.R.drawable.ic_menu_close_clear_cancel, "Reset", stopwatchActionIntent(content.stopwatchId, StopwatchActionReceiver.ACTION_RESET, 3))
        )
    }

    private fun remoteAction(icon: Int, label: String, pendingIntent: PendingIntent) = RemoteAction(
        Icon.createWithResource(this, icon), label, label, pendingIntent
    )

    private fun timerActionIntent(timerId: String, action: String, offset: Int): PendingIntent =
        PendingIntent.getBroadcast(this, timerId.hashCode() + 70_000 + offset,
            Intent(this, TimerActionReceiver::class.java).apply {
                this.action = action
                putExtra(TimerScheduler.EXTRA_TIMER_ID, timerId)
            }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    private fun stopwatchActionIntent(stopwatchId: String, action: String, offset: Int): PendingIntent =
        PendingIntent.getBroadcast(this, stopwatchId.hashCode() + 80_000 + offset,
            Intent(this, StopwatchActionReceiver::class.java).apply {
                this.action = action
                putExtra(StopwatchActionReceiver.EXTRA_STOPWATCH_ID, stopwatchId)
            }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    companion object {
        const val EXTRA_TARGET_TAB = "extra_target_tab"
        const val EXTRA_STOPWATCH_COMMAND = "extra_stopwatch_command"

        private var stopwatchPipNotifier: ((String, StopwatchRuntimeState?) -> Unit)? = null
        private var timerPipNotifier: ((String) -> Unit)? = null

        internal fun setStopwatchPipNotifier(notifier: ((String, StopwatchRuntimeState?) -> Unit)?) {
            stopwatchPipNotifier = notifier
        }

        internal fun setTimerPipNotifier(notifier: ((String) -> Unit)?) {
            timerPipNotifier = notifier
        }

        internal fun notifyStopwatchPipChanged(stopwatchId: String, runtime: StopwatchRuntimeState? = null) {
            stopwatchPipNotifier?.invoke(stopwatchId, runtime)
        }

        internal fun notifyTimerPipChanged(timerId: String) {
            timerPipNotifier?.invoke(timerId)
        }
    }
}

sealed interface PipContent {
    data class Timer(
        val timerId: String,
        val label: String,
        val remainingSeconds: Int,
        val endTimeMillis: Long,
        val isRunning: Boolean
    ) : PipContent

    data class Stopwatch(
        val stopwatchId: String,
        val elapsedMs: Long,
        val snapshotElapsedRealtime: Long,
        val isRunning: Boolean
    ) : PipContent
}

@Composable
private fun PipDisplay(content: PipContent) {
    val displayContent = content
    val title = when (displayContent) {
        is PipContent.Timer -> displayContent.label
        is PipContent.Stopwatch -> "Stopwatch"
    }
    var displaySeconds by remember(displayContent) { mutableStateOf(pipDisplaySeconds(displayContent)) }
    val isRunning = when (displayContent) {
        is PipContent.Timer -> displayContent.isRunning
        is PipContent.Stopwatch -> displayContent.isRunning
    }
    LaunchedEffect(displayContent) {
        while (isRunning) {
            displaySeconds = pipDisplaySeconds(displayContent)
            delay(200)
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF101824)),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.Center) {
                androidx.compose.material3.Text(
                    title,
                    color = Color(0xFFB8C7D9),
                    style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
                androidx.compose.material3.Text(
                    text = TimeUtils.formatDuration(displaySeconds),
                    color = Color.White,
                    style = androidx.compose.material3.MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(10.dp))
            androidx.compose.material3.Text(
                text = if (isRunning) "RUNNING" else "PAUSED",
                color = if (isRunning) Color(0xFF4DE3C1) else Color(0xFFFFC857),
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(
                        color = if (isRunning) Color(0xFF173B3B) else Color(0xFF493813),
                        shape = RoundedCornerShape(50)
                    )
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            )
        }
    }
}

private fun pipDisplaySeconds(content: PipContent): Long = when (content) {
    is PipContent.Timer -> if (content.isRunning) {
        ((content.endTimeMillis - System.currentTimeMillis()) / 1000L).coerceAtLeast(0L)
    } else {
        content.remainingSeconds.toLong()
    }
    is PipContent.Stopwatch -> {
        val extra = if (content.isRunning) SystemClock.elapsedRealtime() - content.snapshotElapsedRealtime else 0L
        ((content.elapsedMs + extra) / 1000L).coerceAtLeast(0L)
    }
}

@Composable
fun MainScreen(
    alarmViewModel: AlarmViewModel,
    timerViewModel: TimerViewModel,
    intentFlow: SharedFlow<Intent>,
    isInPipMode: Boolean,
    pipContent: PipContent?,
    onPipContentChanged: (PipContent?) -> Unit
) {
    RequestAppPermissions()
    val navController = rememberNavController()
    
    val settings by alarmViewModel.settings.collectAsState()
    val tabs = listOf("alarm", "timer", "stopwatch", "clock") + if (settings.notesEnabled) listOf("notes") else emptyList()
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "home"
    val isHomeScreenVisible = currentRoute == "home"
    val activeTimers by timerViewModel.activeTimers.collectAsState()
    val timerClockMillis by timerViewModel.clockMillis.collectAsState()
    var stopwatchPipContent by remember { mutableStateOf<PipContent.Stopwatch?>(null) }

    LaunchedEffect(
        isHomeScreenVisible,
        pagerState.currentPage,
        activeTimers,
        timerClockMillis,
        stopwatchPipContent,
        isInPipMode
    ) {
        if (!isHomeScreenVisible) {
            onPipContentChanged(null)
            return@LaunchedEffect
        }
        when (pagerState.currentPage) {
            1 -> {
                val timersForPip = activeTimers.values.filter {
                    if (isInPipMode) it.remainingSeconds > 0 else it.isActive(timerClockMillis)
                }
                onPipContentChanged(
                    timersForPip.singleOrNull()?.let { timer ->
                        PipContent.Timer(
                            timerId = timer.id,
                            label = timer.label.ifBlank { "Timer" },
                            remainingSeconds = timer.liveRemainingSeconds(timerClockMillis),
                            endTimeMillis = timer.endTimeMillis,
                            isRunning = timer.isRunning
                        )
                    }
                )
            }
            2 -> if (!isInPipMode) onPipContentChanged(stopwatchPipContent)
            else -> onPipContentChanged(null)
        }
    }

    if (isInPipMode && pipContent != null) {
        PipDisplay(pipContent)
        return
    }
    
    LaunchedEffect(intentFlow) {
        intentFlow.collect { intent ->
            val targetTab = intent.getStringExtra(MainActivity.EXTRA_TARGET_TAB)
            if (targetTab != null) {
                val index = tabs.indexOf(targetTab)
                if (index >= 0) {
                    if (navController.currentDestination?.route != "home") {
                        navController.popBackStack("home", inclusive = false)
                    }
                    pagerState.animateScrollToPage(index)
                }
            }
        }
    }

    val showBottomBar = isHomeScreenVisible
    
    Scaffold(
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                AppBottomBar(
                    selectedTabIndex = pagerState.currentPage,
                    notesEnabled = settings.notesEnabled,
                    onTabSelected = { index ->
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") {
                HorizontalPager(state = pagerState) { page ->
                    when (page) {
                        0 -> AlarmScreen(
                            onOpenSettings = { navController.navigate("settings") },
                            onCreateAlarm = { navController.navigate("alarm/edit") },
                            onEditAlarm = { alarmId -> navController.navigate("alarm/edit/$alarmId") },
                            viewModel = alarmViewModel
                        )
                        1 -> TimerScreen(onOpenSettings = { navController.navigate("settings") }, viewModel = timerViewModel, settingsViewModel = alarmViewModel)
                        2 -> StopwatchScreen(
                            onOpenSettings = { navController.navigate("settings") },
                            intentFlow = intentFlow,
                            onSelectedStopwatchStateChanged = { stopwatchId, state ->
                                // Starting PiP still requires a running stopwatch. Once PiP is
                                // open, however, pause is a real state—not a signal to keep
                                // rendering the previous running snapshot.
                                stopwatchPipContent = state
                                    .takeIf { it.isRunning || isInPipMode }
                                    ?.let {
                                    PipContent.Stopwatch(
                                        stopwatchId = stopwatchId,
                                        elapsedMs = state.elapsedMs,
                                        snapshotElapsedRealtime = SystemClock.elapsedRealtime(),
                                        isRunning = state.isRunning
                                    )
                                }
                            }
                        )
                        3 -> ClockScreen(onOpenSettings = { navController.navigate("settings") }, viewModel = alarmViewModel)
                        4 -> NotesScreen(onOpenSettings = { navController.navigate("settings") }, viewModel = alarmViewModel)
                    }
                }
            }
            composable("alarm/edit") {
                AlarmEditScreen(
                    alarmId = null,
                    onBack = { navController.popBackStack() },
                    viewModel = alarmViewModel
                )
            }
            composable(
                route = "alarm/edit/{alarmId}",
                arguments = listOf(navArgument("alarmId") { type = NavType.StringType })
            ) { entry ->
                AlarmEditScreen(
                    alarmId = entry.arguments?.getString("alarmId"),
                    onBack = { navController.popBackStack() },
                    viewModel = alarmViewModel
                )
            }
            composable("settings") {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenGeneral = { navController.navigate("settings/general") },
                    onOpenDefaultAlarm = { navController.navigate("settings/default-alarm") },
                    onOpenTimer = { navController.navigate("settings/timer") },
                    onOpenStopwatch = { navController.navigate("settings/stopwatch") }
                )
            }
            composable("settings/general") {
                GeneralSettingsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenAlarmSpeechFormat = { navController.navigate("settings/alarm/speech") },
                    viewModel = alarmViewModel
                )
            }
            composable("settings/default-alarm") {
                DefaultAlarmSettingsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenSpeechFormat = { navController.navigate("settings/alarm/speech") },
                    viewModel = alarmViewModel
                )
            }
            composable("settings/alarm/speech") {
                AlarmSpeechFormatScreen(onBack = { navController.popBackStack() }, viewModel = alarmViewModel)
            }
            composable("settings/timer") {
                TimerSettingsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenSpeechFormat = { navController.navigate("settings/timer/speech") },
                    viewModel = alarmViewModel
                )
            }
            composable("settings/timer/speech") {
                TimerSpeechFormatScreen(
                    onBack = { navController.popBackStack() },
                    viewModel = alarmViewModel
                )
            }
            composable("settings/stopwatch") {
                StopwatchSettingsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
