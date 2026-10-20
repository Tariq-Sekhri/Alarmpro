package ca.sekhrit.alarmpro.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.*
import ca.sekhrit.alarmpro.ui.theme.LocalLargerControls

/** Enlarge the visible control and its hit area without scaling cards, spacing or screen text. */
@Composable
private fun ControlSize(content: @Composable () -> Unit) {
    val density = LocalDensity.current
    val scale = if (LocalLargerControls.current) 1.5f else 1f
    CompositionLocalProvider(
        LocalDensity provides Density(density.density * scale, density.fontScale),
        LocalLargerControls provides false,
        content = content
    )
}

@Composable
fun IconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) = ControlSize {
    androidx.compose.material3.IconButton(onClick = onClick, modifier = modifier, enabled = enabled, content = content)
}

@Composable
fun Switch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: SwitchColors = SwitchDefaults.colors()
) = ControlSize {
    androidx.compose.material3.Switch(checked = checked, onCheckedChange = onCheckedChange,
        modifier = modifier, enabled = enabled, colors = colors)
}

@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit
) = ControlSize {
    androidx.compose.material3.Button(onClick = onClick, modifier = modifier, enabled = enabled,
        colors = colors, contentPadding = contentPadding, content = content)
}

@Composable
fun TextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
    contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding,
    content: @Composable RowScope.() -> Unit
) = ControlSize {
    androidx.compose.material3.TextButton(onClick = onClick, modifier = modifier, enabled = enabled,
        colors = colors, contentPadding = contentPadding, content = content)
}

@Composable
fun OutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit
) = ControlSize {
    androidx.compose.material3.OutlinedButton(onClick = onClick, modifier = modifier, enabled = enabled,
        colors = colors, contentPadding = contentPadding, content = content)
}

@Composable
fun FilledTonalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.filledTonalButtonColors(),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit
) = ControlSize {
    androidx.compose.material3.FilledTonalButton(onClick = onClick, modifier = modifier, enabled = enabled,
        colors = colors, contentPadding = contentPadding, content = content)
}
