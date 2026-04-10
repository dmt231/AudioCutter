package com.example.audiocutterdemo2.ui.custom

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

@Composable
fun PlaybackIndicator(
    positionMs: Long,
    totalDurationMs: Long,
    totalWidthPx: Float,
    onSeek: (Long) -> Unit = {}
) {
    val density = LocalDensity.current
    val indicatorWidthDp = 2.dp
    val indicatorWidthPx = with(density) { indicatorWidthDp.toPx() }
    val touchWidthDp = 24.dp // vùng touch rộng hơn để dễ kéo

    val offsetPx = if (totalWidthPx > 0f && totalDurationMs > 0L) {
        (positionMs / totalDurationMs.toFloat() * totalWidthPx)
    } else 0f

    val currentOnSeek by rememberUpdatedState(onSeek)
    val currentPositionMs by rememberUpdatedState(positionMs)

    Box(
        modifier = Modifier
            .offset { IntOffset((offsetPx - with(density) { touchWidthDp.toPx() } / 2).toInt(), 0) }
            .width(touchWidthDp)
            .fillMaxHeight()
            .pointerInput(totalWidthPx) {
                detectDragGestures { _, dragAmount ->
                    if (totalWidthPx <= 0f) return@detectDragGestures
                    val newMs = (currentPositionMs + (dragAmount.x / totalWidthPx * totalDurationMs).toLong())
                        .coerceIn(0L, totalDurationMs)
                    currentOnSeek(newMs)
                }
            }
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset((with(density) { touchWidthDp.toPx() } / 2 - indicatorWidthPx / 2).toInt(), 0) }
                .width(indicatorWidthDp)
                .fillMaxHeight()
                .background(Color.White)
        )
    }
}