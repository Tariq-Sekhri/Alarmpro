package ca.sekhrit.alarmpro.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

@Composable
fun WheelTimePicker(
    hour: Int,
    minute: Int,
    is24Hour: Boolean,
    onTimeChange: (hour: Int, minute: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val displayHour = if (is24Hour) hour else if (hour % 12 == 0) 12 else hour % 12
    val amPm = if (hour < 12) 0 else 1

    Row(
        modifier = modifier.height(132.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WheelPicker(
            value = displayHour,
            range = if (is24Hour) 0..23 else 1..12,
            onValueChange = { newDisplayHour ->
                val newHour = if (is24Hour) {
                    newDisplayHour
                } else {
                    val isPm = hour >= 12
                    when {
                        newDisplayHour == 12 && !isPm -> 0
                        newDisplayHour == 12 && isPm -> 12
                        else -> newDisplayHour + (if (isPm) 12 else 0)
                    }
                }
                onTimeChange(newHour, minute)
            },
            modifier = Modifier.weight(1f)
        )

        Text(
            text = ":",
            fontSize = 24.sp,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        WheelPicker(
            value = minute,
            range = 0..59,
            onValueChange = { newMinute ->
                onTimeChange(hour, newMinute)
            },
            modifier = Modifier.weight(1f),
            formatValue = { it.toString().padStart(2, '0') }
        )

        if (!is24Hour) {
            Spacer(modifier = Modifier.width(8.dp))
            WheelPicker(
                value = amPm,
                range = 0..1,
                onValueChange = { newAmPm ->
                    val isPm = newAmPm == 1
                    val newHour = when {
                        hour < 12 && isPm -> hour + 12
                        hour >= 12 && !isPm -> hour - 12
                        else -> hour
                    }
                    onTimeChange(newHour, minute)
                },
                modifier = Modifier.weight(1f),
                isInfinite = false,
                formatValue = { if (it == 0) "a.m." else "p.m." }
            )
        }
    }
}

@Composable
internal fun WheelPicker(
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    isInfinite: Boolean = true,
    formatValue: (Int) -> String = { it.toString() }
) {
    val valueCount = range.last - range.first + 1
    val actualItemCount = if (isInfinite) 100_000 else valueCount + 2
    val initialCenter = remember {
        if (isInfinite) {
            val midpoint = 50_000
            midpoint - (midpoint % valueCount) + (value - range.first)
        } else {
            (value - range.first) + 1
        }
    }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialCenter - 1)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val centerIndex by remember {
        derivedStateOf { listState.firstVisibleItemIndex + 1 }
    }
    
    val scrollScope = rememberCoroutineScope()
    val currentValue by rememberUpdatedState(value)

    LaunchedEffect(listState) {
        androidx.compose.runtime.snapshotFlow { listState.isScrollInProgress }
            .filter { !it }
            .collect {
                if (isInfinite) {
                    val selectedValue = Math.floorMod(listState.firstVisibleItemIndex + 1, valueCount) + range.first
                    if (selectedValue != currentValue) {
                        onValueChange(selectedValue)
                    }
                } else {
                    val rawIndex = listState.firstVisibleItemIndex + 1
                    val clampedIndex = rawIndex.coerceIn(1, valueCount)
                    val selectedValue = (clampedIndex - 1) + range.first
                    if (selectedValue != currentValue) {
                        onValueChange(selectedValue)
                    }
                }
            }
    }

    LaunchedEffect(value) {
        if (isInfinite) {
            val currentVisibleValue = Math.floorMod(listState.firstVisibleItemIndex + 1, valueCount) + range.first
            if (currentVisibleValue != value) {
                var distance = (value - range.first) - (currentVisibleValue - range.first)
                if (distance > valueCount / 2) distance -= valueCount
                if (distance < -(valueCount / 2)) distance += valueCount
                listState.animateScrollToItem(listState.firstVisibleItemIndex + distance)
            }
        } else {
            val currentCenterIndex = listState.firstVisibleItemIndex + 1
            val targetCenterIndex = (value - range.first) + 1
            if (currentCenterIndex != targetCenterIndex) {
                listState.animateScrollToItem(targetCenterIndex - 1)
            }
        }
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            flingBehavior = flingBehavior
        ) {
            items(count = actualItemCount, key = { it }) { index ->
                val isCenter = index == centerIndex
                if (!isInfinite && (index == 0 || index == actualItemCount - 1)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    )
                } else {
                    val itemValue = if (isInfinite) {
                        (index % valueCount) + range.first
                    } else {
                        (index - 1) + range.first
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clickable {
                                if (!isCenter) {
                                    scrollScope.launch {
                                        listState.animateScrollToItem(index - 1)
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = formatValue(itemValue),
                            fontSize = 20.sp,
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
