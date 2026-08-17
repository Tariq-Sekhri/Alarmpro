package ca.sekhrit.alarmpro.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import ca.sekhrit.alarmpro.ui.theme.CardSurface
import ca.sekhrit.alarmpro.ui.theme.ElectricCyan

@Composable
fun AppBottomBar(
    selectedTabIndex: Int,
    notesEnabled: Boolean,
    onTabSelected: (Int) -> Unit
) {
    NavigationBar(containerColor = CardSurface) {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Alarm, contentDescription = "Alarm") },
            label = { AutoSizingMenuText("Alarm") },
            selected = selectedTabIndex == 0,
            colors = navColors(),
            onClick = { onTabSelected(0) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.HourglassEmpty, contentDescription = "Timer") },
            label = { AutoSizingMenuText("Timer") },
            selected = selectedTabIndex == 1,
            colors = navColors(),
            onClick = { onTabSelected(1) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Timer, contentDescription = "Stopwatch") },
            label = { AutoSizingMenuText("Stopwatch") },
            selected = selectedTabIndex == 2,
            colors = navColors(),
            onClick = { onTabSelected(2) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.AccessTime, contentDescription = "Clock") },
            label = { AutoSizingMenuText("Clock") },
            selected = selectedTabIndex == 3,
            colors = navColors(),
            onClick = { onTabSelected(3) }
        )
        if (notesEnabled) {
            NavigationBarItem(
                icon = { Icon(Icons.Default.NoteAlt, contentDescription = "Notes") },
                label = { AutoSizingMenuText("Notes") },
                selected = selectedTabIndex == 4,
                colors = navColors(),
                onClick = { onTabSelected(4) }
            )
        }
    }
}

@Composable
private fun AutoSizingMenuText(text: String) {
    BoxWithConstraints {
        val style = MaterialTheme.typography.labelMedium
        var fontSize by remember(text, maxWidth) { mutableStateOf(style.fontSize) }
        Text(
            text = text,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            style = style.copy(fontSize = fontSize),
            onTextLayout = { result ->
                if (result.hasVisualOverflow && fontSize > 9.sp) {
                    fontSize = (fontSize.value - 1f).coerceAtLeast(9f).sp
                }
            }
        )
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
