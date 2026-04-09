package com.example.audiocutterdemo2.ui.custom

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.example.audiocutterdemo2.core.file_manager.data.TrimMode

@Composable
fun ColorOverlayCanvas(
    totalDurationMs: Long,
    handleLMs: Long,
    handleRMs: Long,
    trimMode: TrimMode,
    modifier: Modifier = Modifier
) {
    val keepColor  = Color(0xFFB89FFF).copy(alpha = 0f)
    val dimColor   = Color(0xFF000000).copy(alpha = 0.55f)

    Canvas(modifier = modifier.fillMaxSize()) {
        val leftPx  = handleLMs / totalDurationMs.toFloat() * size.width
        val rightPx = handleRMs / totalDurationMs.toFloat() * size.width

        when (trimMode) {
            TrimMode.TRIM_SIDE -> {
                drawRect(
                    color   = dimColor,
                    topLeft = Offset(0f, 0f),
                    size    = Size(leftPx, size.height)
                )
                drawRect(
                    color   = dimColor,
                    topLeft = Offset(rightPx, 0f),
                    size    = Size(size.width - rightPx, size.height)
                )
            }
            TrimMode.TRIM_MIDDLE -> {
                drawRect(
                    color   = dimColor,
                    topLeft = Offset(leftPx, 0f),
                    size    = Size(rightPx - leftPx, size.height)
                )
            }
        }
    }
}