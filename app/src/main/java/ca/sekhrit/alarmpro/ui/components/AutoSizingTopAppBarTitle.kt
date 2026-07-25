package ca.sekhrit.alarmpro.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

@Composable
fun AutoSizingTopAppBarTitle(
    text: String,
    modifier: Modifier = Modifier,
    minimumFontSize: TextUnit = 14.sp
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val titleStyle = MaterialTheme.typography.titleLarge
        var fontSize by remember(text, maxWidth) { mutableStateOf(titleStyle.fontSize) }

        Text(
            text = text,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            style = titleStyle.copy(fontSize = fontSize),
            onTextLayout = { result ->
                if (result.hasVisualOverflow && fontSize > minimumFontSize) {
                    fontSize = (fontSize.value - 1f)
                        .coerceAtLeast(minimumFontSize.value)
                        .sp
                }
            }
        )
    }
}
