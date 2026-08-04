package ca.sekhrit.alarmpro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

@Composable
fun DurationPickerDialog(
    title: String,
    initialTotalSeconds: Int = 0,
    showLabel: Boolean = true,
    showSeconds: Boolean = true,
    initialLabel: String = "",
    onDismiss: () -> Unit,
    onConfirm: (totalSeconds: Int, label: String) -> Unit
) {
    var hours by remember(initialTotalSeconds) { mutableIntStateOf(initialTotalSeconds / 3600) }
    var minutes by remember(initialTotalSeconds) {
        mutableIntStateOf((initialTotalSeconds % 3600) / 60)
    }
    var seconds by remember(initialTotalSeconds) { mutableIntStateOf(initialTotalSeconds % 60) }
    var label by remember(initialLabel) { mutableStateOf(initialLabel) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF293743),
            tonalElevation = 8.dp
        ) {
            Column {
                Text(
                    text = title,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFF465561), Color(0xFF2C3945))
                            )
                        )
                        .padding(horizontal = 14.dp, vertical = 11.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 15.dp, end = 15.dp, top = 17.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.Top
                ) {
                    DurationWheel(
                        label = "hour",
                        value = hours,
                        maxValue = 99,
                        onValueChange = { hours = it },
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = ":",
                        fontSize = 16.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .width(28.dp)
                            .padding(top = 93.dp)
                    )
                    DurationWheel(
                        label = "min",
                        value = minutes,
                        maxValue = 59,
                        onValueChange = { minutes = it },
                        modifier = Modifier.weight(1f)
                    )
                    if (showSeconds) {
                        Text(
                            text = ":",
                            fontSize = 16.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .width(28.dp)
                                .padding(top = 93.dp)
                        )
                        DurationWheel(
                            label = "sec",
                            value = seconds,
                            maxValue = 59,
                            onValueChange = { seconds = it },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (showLabel) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 19.dp, end = 19.dp, top = 9.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Label:",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        BasicTextField(
                            value = label,
                            onValueChange = { label = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            decorationBox = { innerTextField ->
                                Column {
                                    Box(modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) {
                                        if (label.isEmpty()) {
                                            Text(
                                                text = "none",
                                                color = Color(0xFFB9BEC2),
                                                maxLines = 1
                                            )
                                        }
                                        innerTextField()
                                    }
                                    HorizontalDivider(color = Color(0xFFFFB300), thickness = 2.dp)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 8.dp, top = 5.dp, bottom = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val buttonColors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismiss, colors = buttonColors) { Text("CANCEL") }
                    TextButton(
                        colors = buttonColors,
                        onClick = {
                            val total = hours * 3600 + minutes * 60 + if (showSeconds) seconds else 0
                            if (total > 0) onConfirm(total, label)
                        }
                    ) {
                        Text("OK")
                    }
                }
            }
        }
    }
}

@Composable
fun DurationWheel(
    label: String,
    value: Int,
    maxValue: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val valueCount = maxValue + 1
    val initialCenter = remember {
        val midpoint = 50_000
        midpoint - (midpoint % valueCount) + value
    }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialCenter - 1)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val centerIndex by remember {
        derivedStateOf { listState.firstVisibleItemIndex + 1 }
    }
    var editing by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf(value.toString()) }
    var fieldWasFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val scrollScope = rememberCoroutineScope()
    val currentValue by rememberUpdatedState(value)
    val isEditing by rememberUpdatedState(editing)

    fun commitInput() {
        if (!editing) return
        val enteredValue = inputText.toIntOrNull()?.coerceIn(0, maxValue) ?: value
        editing = false
        fieldWasFocused = false
        focusManager.clearFocus()
        onValueChange(enteredValue)
    }

    LaunchedEffect(listState) {
        androidx.compose.runtime.snapshotFlow { listState.isScrollInProgress }
            .filter { !it }
            .collect {
                val selectedValue = Math.floorMod(listState.firstVisibleItemIndex + 1, valueCount)
                if (!isEditing && selectedValue != currentValue) {
                    onValueChange(selectedValue)
                }
            }
    }

    LaunchedEffect(value, editing) {
        if (editing) return@LaunchedEffect
        val currentValue = Math.floorMod(listState.firstVisibleItemIndex + 1, valueCount)
        if (currentValue != value) {
            var distance = value - currentValue
            if (distance > valueCount / 2) distance -= valueCount
            if (distance < -(valueCount / 2)) distance += valueCount
            listState.animateScrollToItem(listState.firstVisibleItemIndex + distance)
        }
    }

    LaunchedEffect(editing) {
        if (editing) {
            inputText = value.toString()
            focusRequester.requestFocus()
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(132.dp)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                flingBehavior = flingBehavior,
                userScrollEnabled = !editing
            ) {
                items(count = 100_000, key = { it }) { index ->
                    val itemValue = index % valueCount
                    val isCenter = index == centerIndex
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clickable(enabled = !editing) {
                                if (isCenter) {
                                    editing = true
                                } else {
                                    scrollScope.launch {
                                        listState.animateScrollToItem(index - 1)
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCenter && editing) {
                            BasicTextField(
                                value = inputText,
                                onValueChange = { newText ->
                                    if (newText.length <= 2 && newText.all(Char::isDigit)) {
                                        inputText = newText
                                    }
                                },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                ),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(onDone = { commitInput() }),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester)
                                    .onFocusChanged { focusState ->
                                        if (focusState.isFocused) {
                                            fieldWasFocused = true
                                        } else if (fieldWasFocused) {
                                            commitInput()
                                        }
                                    }
                            )
                        } else {
                            Text(
                                text = itemValue.toString(),
                                fontSize = 14.sp,
                                color = if (isCenter) Color.White else Color(0xFF858E95),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
            HorizontalDivider(
                color = Color(0xFFB7BDC1),
                thickness = 2.dp,
                modifier = Modifier.padding(top = 43.dp)
            )
            HorizontalDivider(
                color = Color(0xFF9CA4A9),
                thickness = 2.dp,
                modifier = Modifier.padding(top = 87.dp)
            )
        }
    }
}
