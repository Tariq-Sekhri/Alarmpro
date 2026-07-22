package ca.sekhrit.alarmpro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            AlarmProTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    RequestAppPermissions()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val mainTabs = setOf("alarm", "timer", "stopwatch", "clock")
    val showBottomBar = currentRoute in mainTabs
    val activity = LocalActivity.current as ComponentActivity
    val alarmViewModel: AlarmViewModel = viewModel(activity)

    fun openSettings() {
        navController.navigate("settings")
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                AppBottomBar(navController)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "alarm",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("alarm") {
                AlarmScreen(
                    onOpenSettings = { openSettings() },
                    onCreateAlarm = { navController.navigate("alarm/edit") },
                    onEditAlarm = { alarmId -> navController.navigate("alarm/edit/$alarmId") },
                    viewModel = alarmViewModel
                )
            }
            composable("timer") {
                TimerScreen(onOpenSettings = { openSettings() })
            }
            composable("stopwatch") {
                StopwatchScreen(onOpenSettings = { openSettings() })
            }
            composable("clock") {
                ClockScreen(onOpenSettings = { openSettings() }, viewModel = alarmViewModel)
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
