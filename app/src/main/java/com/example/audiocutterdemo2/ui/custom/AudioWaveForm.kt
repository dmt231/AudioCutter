package com.example.audiocutterdemo2.ui.custom

import android.view.MotionEvent
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import com.linc.audiowaveform.model.AmplitudeType
import com.linc.audiowaveform.model.WaveformAlignment
import kotlin.math.ceil
import kotlin.math.roundToInt

private val MinSpikeWidthDp: Dp = 1.dp
private val MaxSpikeWidthDp: Dp = 24.dp
private val MinSpikePaddingDp: Dp = 0.dp
private val MaxSpikePaddingDp: Dp = 24.dp
private val MinSpikeRadiusDp: Dp = 0.dp
private val MaxSpikeRadiusDp: Dp = 12.dp

private const val MinProgress: Float = 0F
private const val MaxProgress: Float = 1F

private const val MinSpikeHeight: Float = 1F
private const val DefaultGraphicsLayerAlpha: Float = 0.99F

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AudioWaveform(
    modifier: Modifier = Modifier,
    style: DrawStyle = Fill,
    waveformBrush: Brush = SolidColor(Color.White),
    progressBrush: Brush = SolidColor(Color.Blue),
    waveformAlignment: WaveformAlignment = WaveformAlignment.Center,
    amplitudeType: AmplitudeType = AmplitudeType.Avg,
    onProgressChangeFinished: (() -> Unit)? = null,
    spikeAnimationSpec: AnimationSpec<Float> = tween(500),
    spikeWidth: Dp = 4.dp,
    spikeRadius: Dp = 2.dp,
    spikePadding: Dp = 1.dp,
    progress: Float = 0F,
    amplitudes: List<Int>,
    onProgressChange: (Float) -> Unit
) {
    val _progress = remember(progress) { progress.coerceIn(MinProgress, MaxProgress) }
    val _spikeWidth = remember(spikeWidth) { spikeWidth.coerceIn(MinSpikeWidthDp, MaxSpikeWidthDp) }
    val _spikePadding = remember(spikePadding) { spikePadding.coerceIn(MinSpikePaddingDp, MaxSpikePaddingDp) }
    val _spikeRadius = remember(spikeRadius) { spikeRadius.coerceIn(MinSpikeRadiusDp, MaxSpikeRadiusDp) }
    val _spikeTotalWidth = remember(spikeWidth, spikePadding) { _spikeWidth + _spikePadding }
    var canvasSize by remember { mutableStateOf(Size(0f, 0f)) }
    var spikes by remember { mutableStateOf(0F) }
    val spikesAmplitudes = remember(amplitudes, spikes, amplitudeType) {
        amplitudes.toDrawableAmplitudes(
            amplitudeType = amplitudeType,
            spikes = spikes.toInt(),
            minHeight = MinSpikeHeight,
            maxHeight = canvasSize.height.coerceAtLeast(MinSpikeHeight)
        )
    }.map { animateFloatAsState(it, spikeAnimationSpec).value }
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(alpha = DefaultGraphicsLayerAlpha)
            .pointerInteropFilter {
                return@pointerInteropFilter when (it.action) {
                    MotionEvent.ACTION_DOWN,
                    MotionEvent.ACTION_MOVE -> {
                        if (it.x in 0F..canvasSize.width) {
                            onProgressChange(it.x / canvasSize.width)
                            true
                        } else false
                    }
                    MotionEvent.ACTION_UP -> {
                        onProgressChangeFinished?.invoke()
                        true
                    }
                    else -> false
                }
            }
            .then(modifier)
    ) {
        canvasSize = size
        spikes = size.width / _spikeTotalWidth.toPx()
        spikesAmplitudes.forEachIndexed { index, amplitude ->
            drawRoundRect(
                brush = waveformBrush,
                topLeft = Offset(
                    x = index * _spikeTotalWidth.toPx(),
                    y = when(waveformAlignment) {
                        WaveformAlignment.Top -> 0F
                        WaveformAlignment.Bottom -> size.height - amplitude
                        WaveformAlignment.Center -> size.height / 2F - amplitude / 2F
                    }
                ),
                size = Size(
                    width = _spikeWidth.toPx(),
                    height = amplitude
                ),
                cornerRadius = CornerRadius(_spikeRadius.toPx(), _spikeRadius.toPx()),
                style = style
            )
            drawRect(
                brush = progressBrush,
                size = Size(
                    width = _progress * size.width,
                    height = size.height
                ),
                blendMode = BlendMode.SrcAtop
            )
        }
    }
}

private fun List<Int>.toDrawableAmplitudes(
    amplitudeType: AmplitudeType,
    spikes: Int,
    minHeight: Float,
    maxHeight: Float
): List<Float> {
    val amplitudes = map(Int::toFloat)
    if(amplitudes.isEmpty() || spikes == 0) {
        return List(spikes) { minHeight }
    }
    val transform = { data: List<Float> ->
        when(amplitudeType) {
            AmplitudeType.Avg -> data.average()
            AmplitudeType.Max -> data.max()
            AmplitudeType.Min -> data.min()
        }.toFloat().coerceIn(minHeight, maxHeight)
    }

    val raw = when {
        spikes > amplitudes.count() -> amplitudes.fillToSize(spikes, transform)
        else -> amplitudes.chunkToSize(spikes, transform)
    }

    val maxVal = raw.maxOrNull()?.takeIf { it > 0f } ?: return raw
    return raw.map { value ->
        (value / maxVal * maxHeight).coerceAtLeast(minHeight)
    }
}


internal fun <T> Iterable<T>.fillToSize(size: Int, transform: (List<T>) -> T): List<T> {
    val capacity = ceil(size.safeDiv(count())).roundToInt()
    return map { data -> List(capacity) { data } }.flatten().chunkToSize(size, transform)
}

internal fun <T> Iterable<T>.chunkToSize(size: Int, transform: (List<T>) -> T): List<T> {
    val chunkSize = count() / size
    val remainder = count() % size
    val remainderIndex = ceil(count().safeDiv(remainder)).roundToInt()
    val chunkIteration = filterIndexed { index, _ ->
        remainderIndex == 0 || index % remainderIndex != 0
    }.chunked(chunkSize, transform)
    return when (size) {
        chunkIteration.count() -> chunkIteration
        else -> chunkIteration.chunkToSize(size, transform)
    }
}

internal fun Iterable<Float>.normalize(min: Float, max: Float): List<Float> {
    return map { (max-min) * ((it - min()) safeDiv (max() - min())) + min }
}

private fun Int.safeDiv(value: Int): Float {
    return if(value == 0) return 0F else this / value.toFloat()
}

private infix fun Float.safeDiv(value: Float): Float {
    return if (value == 0f) return 0F else this / value
}