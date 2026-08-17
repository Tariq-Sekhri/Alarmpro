package ca.sekhrit.alarmpro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.navigation.NavGraph.Companion.findStartDestination
import kotlinx.coroutines.launch
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
import ca.sekhrit.alarmpro.ui.theme.AlarmProTheme
import ca.sekhrit.alarmpro.viewmodel.AlarmViewModel

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
    private val intentFlow = MutableSharedFlow<Intent>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        
        intent?.let { intentFlow.tryEmit(it) }

        setContent {
            AlarmProTheme {
                MainScreen(alarmViewModel, intentFlow)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intentFlow.tryEmit(intent)
    }

    override fun onResume() {
        super.onResume()
        alarmViewModel.refreshFromStorage()
    }

    companion object {
        const val EXTRA_TARGET_TAB = "extra_target_tab"
    }
}

@Composable
fun MainScreen(alarmViewModel: AlarmViewModel, intentFlow: SharedFlow<Intent>) {
    RequestAppPermissions()
    val navController = rememberNavController()
    
    val settings by alarmViewModel.settings.collectAsState()
    val tabs = listOf("alarm", "timer", "stopwatch", "clock") + if (settings.notesEnabled) listOf("notes") else emptyList()
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()
    
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

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "home"
    val showBottomBar = currentRoute == "home"
    
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
                        1 -> TimerScreen(onOpenSettings = { navController.navigate("settings") }, settingsViewModel = alarmViewModel)
                        2 -> StopwatchScreen(onOpenSettings = { navController.navigate("settings") })
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
                GeneralSettingsScreen(onBack = { navController.popBackStack() }, viewModel = alarmViewModel)
            }
            composable("settings/default-alarm") {
                DefaultAlarmSettingsScreen(onBack = { navController.popBackStack() }, viewModel = alarmViewModel)
            }
            composable("settings/timer") {
                TimerSettingsScreen(onBack = { navController.popBackStack() }, viewModel = alarmViewModel)
            }
            composable("settings/stopwatch") {
                StopwatchSettingsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
