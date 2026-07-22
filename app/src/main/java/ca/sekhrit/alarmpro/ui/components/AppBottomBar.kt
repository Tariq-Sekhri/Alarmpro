package ca.sekhrit.alarmpro.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import ca.sekhrit.alarmpro.ui.theme.CardSurface
import ca.sekhrit.alarmpro.ui.theme.ElectricCyan

@Composable
fun AppBottomBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(containerColor = CardSurface) {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Alarm, contentDescription = "Alarm") },
            label = { Text("Alarm") },
            selected = currentRoute == "alarm",
            colors = navColors(),
            onClick = { navigateTab(navController, "alarm") }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.HourglassEmpty, contentDescription = "Timer") },
            label = { Text("Timer") },
            selected = currentRoute == "timer",
            colors = navColors(),
            onClick = { navigateTab(navController, "timer") }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Timer, contentDescription = "Stopwatch") },
            label = { Text("Stopwatch") },
            selected = currentRoute == "stopwatch",
            colors = navColors(),
            onClick = { navigateTab(navController, "stopwatch") }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.AccessTime, contentDescription = "Clock") },
            label = { Text("Clock") },
            selected = currentRoute == "clock",
            colors = navColors(),
            onClick = { navigateTab(navController, "clock") }
        )
    }
}

private fun navigateTab(navController: NavHostController, route: String) {
    navController.navigate(route) {
        popUpTo("alarm") { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun navColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = ElectricCyan,
    selectedTextColor = ElectricCyan,
    indicatorColor = ElectricCyan.copy(alpha = 0.15f),
    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
)
