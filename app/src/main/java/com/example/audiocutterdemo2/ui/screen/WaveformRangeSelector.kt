package com.example.audiocutterdemo2.ui.screen

import android.util.Log
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.audiocutterdemo2.R
import com.example.audiocutterdemo2.core.file_manager.data.TrimMode
import com.linc.audiowaveform.AudioWaveform
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun WaveformRangeSelector(
    amplitudes: List<Int>,
    totalDurationMs: Long,
    handleLMs: Long,
    handleRMs: Long,
    trimMode: TrimMode,
    onHandleLChange: (Long) -> Unit,
    onHandleRChange: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (amplitudes.isEmpty() || totalDurationMs == 0L) return

    var zoomScale by remember { mutableFloatStateOf(1f) }
    val scrollState = rememberScrollState()
    var containerWidthPx by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current

    val keepStart = (handleLMs / totalDurationMs.toFloat()).coerceIn(0f, 1f)
    val keepEnd = (handleRMs / totalDurationMs.toFloat()).coerceIn(0f, 1f)

    val keepColor = Color(0xFFB89FFF)
    val dimColor = Color(0xFFB89FFF).copy(alpha = 0.1f)

    val waveformBrush = when (trimMode) {
        TrimMode.TRIM_SIDE -> Brush.horizontalGradient(
            0f to dimColor, keepStart to dimColor, keepStart to keepColor,
            keepEnd to keepColor, keepEnd to dimColor, 1f to dimColor
        )

        TrimMode.TRIM_MIDDLE -> Brush.horizontalGradient(
            0f to keepColor, keepStart to keepColor, keepStart to dimColor,
            keepEnd to dimColor, keepEnd to keepColor, 1f to keepColor
        )
    }

    val screenWidthDp =
        with(density) { if (containerWidthPx > 0f) containerWidthPx.toDp() else 300.dp }
    val minWidthDp = (totalDurationMs / 1000f * 15f).dp
    val baseWidthDp = maxOf(screenWidthDp, minWidthDp)
    val waveformWidthDp = baseWidthDp * zoomScale

    Column(modifier = modifier.fillMaxWidth()) {
        // --- Header Time
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTimeMs(handleLMs),
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Total: ${formatTimeMs(totalDurationMs)}",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = formatTimeMs(handleRMs),
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        // --- Khu vực Waveform ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .onSizeChanged { containerWidthPx = it.width.toFloat() }
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoom, _ ->
                        zoomScale = (zoomScale * zoom).coerceIn(1f, 8f)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(scrollState),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Spacer(modifier = Modifier.width(16.dp))

                Box(
                    modifier = Modifier
                        .width(waveformWidthDp)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    AudioWaveform(
                        modifier = Modifier.fillMaxSize(),
                        amplitudes = amplitudes,
                        progress = 0f,
                        onProgressChange = {},
                        waveformBrush = waveformBrush,
                        spikePadding = 3.dp,
                        spikeWidth = if (zoomScale > 2f) 4.dp else 2.dp,
                        spikeRadius = 1.dp,
                        spikeAnimationSpec = tween(durationMillis = 0)
                    )

                    RangeOverlay(
                        modifier = Modifier.fillMaxSize(),
                        totalDurationMs = totalDurationMs,
                        handleLMs = handleLMs,
                        handleRMs = handleRMs,
                        onHandleLChange = onHandleLChange,
                        onHandleRChange = onHandleRChange
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))
            }
        }
    }
}

private fun formatTimeMs(ms: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(ms)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
    val deciseconds = (ms % 1000) / 100
    return String.format(Locale.getDefault(), "%02d:%02d.%d", minutes, seconds, deciseconds)
}

@Composable
fun RangeOverlay(
    totalDurationMs: Long,
    handleLMs: Long,
    handleRMs: Long,
    onHandleLChange: (Long) -> Unit,
    onHandleRChange: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var totalWidthPx by remember { mutableFloatStateOf(0f) }
    val minGapMs = 500L

    val selectedOverlayColor = Color.Transparent
    val unselectedOverlayColor = Color(0xFFB89FFF).copy(alpha = 0.1f)

    Box(
        modifier = modifier.onSizeChanged { totalWidthPx = it.width.toFloat() }
    ) {
        if (totalWidthPx > 0f) {
            val leftPx = (handleLMs / totalDurationMs.toFloat()) * totalWidthPx
            val rightPx = (handleRMs / totalDurationMs.toFloat()) * totalWidthPx

            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .width(with(LocalDensity.current) { leftPx.toDp() })
                        .fillMaxHeight()
                        .background(unselectedOverlayColor)
                )

                Box(
                    modifier = Modifier
                        .width(with(LocalDensity.current) { (rightPx - leftPx).toDp() })
                        .fillMaxHeight()
                        .background(selectedOverlayColor)
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(unselectedOverlayColor)
                )
            }
        }

        HandleBar(
            positionMs = handleLMs,
            totalDurationMs = totalDurationMs,
            totalWidthPx = totalWidthPx,
            isLeft = true,
            onDrag = { deltaPx ->
                val newMs = (handleLMs + (deltaPx / totalWidthPx * totalDurationMs).toLong())
                    .coerceIn(0L, handleRMs - minGapMs)
                onHandleLChange(newMs)
            }
        )

        HandleBar(
            positionMs = handleRMs,
            totalDurationMs = totalDurationMs,
            totalWidthPx = totalWidthPx,
            isLeft = false,
            onDrag = { deltaPx ->
                val newMs = (handleRMs + (deltaPx / totalWidthPx * totalDurationMs).toLong())
                    .coerceIn(handleLMs + minGapMs, totalDurationMs)
                onHandleRChange(newMs)
            }
        )
    }
}

@Composable
fun HandleBar(
    positionMs: Long,
    totalDurationMs: Long,
    totalWidthPx: Float,
    isLeft: Boolean,
    onDrag: (deltaPx: Float) -> Unit,
) {
    val density = LocalDensity.current
    val handleTouchWidthDp = 20.dp

    val handleTouchWidthPx = with(density) { handleTouchWidthDp.toPx() }

    val offsetPx = if (totalWidthPx > 0f)
        (positionMs / totalDurationMs.toFloat() * totalWidthPx) else 0f

    val currentOnDrag by rememberUpdatedState(onDrag)

    Box(
        modifier = Modifier
            .offset { IntOffset((offsetPx - handleTouchWidthPx / 2).toInt(), 0) }
            .width(handleTouchWidthDp)
            .fillMaxHeight()
            .pointerInput(Unit) {
                detectDragGestures { _, dragAmount ->
                    currentOnDrag(dragAmount.x)
                }
            }
    ) {
        val resId = if (isLeft) R.drawable.ic_indicator_right else R.drawable.ic_indicator_left

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isLeft) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .width(1.5.dp)
                        .background(Color(0xFFB89FFF), RoundedCornerShape(2.dp))
                )
                Image(
                    painter = painterResource(resId),
                    contentDescription = null,
                    modifier = Modifier.wrapContentSize()
                )
            } else {
                Image(
                    painter = painterResource(resId),
                    contentDescription = null,
                    modifier = Modifier.wrapContentSize()
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .width(1.5.dp)
                        .background(Color(0xFFB89FFF), RoundedCornerShape(2.dp))
                )
            }
        }
    }
}