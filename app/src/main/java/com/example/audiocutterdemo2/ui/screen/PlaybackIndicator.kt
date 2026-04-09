package com.example.audiocutterdemo2.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

@Composable
fun PlaybackIndicator(
    positionMs: Long,
    totalDurationMs: Long,
    totalWidthPx: Float
) {
    val density = LocalDensity.current
    val indicatorWidthDp = 2.dp
    val indicatorWidthPx = with(density) { indicatorWidthDp.toPx() }

    val offsetPx = if (totalWidthPx > 0f && totalDurationMs > 0L) {
        (positionMs / totalDurationMs.toFloat() * totalWidthPx)
    } else 0f

    Box(
        modifier = Modifier
            .offset { IntOffset((offsetPx - indicatorWidthPx / 2).toInt(), 0) }
            .width(indicatorWidthDp)
            .fillMaxHeight()
            .background(Color.White)
    )
}