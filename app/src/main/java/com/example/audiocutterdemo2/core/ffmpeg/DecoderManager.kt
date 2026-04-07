package com.example.audiocutterdemo2.core.ffmpeg

import android.util.Log
import com.example.audiocutterdemo2.BaseApplication
import com.example.audiocutterdemo2.core.amplituda.Amplituda
import com.example.audiocutterdemo2.core.amplituda.AmplitudaProgressListener
import com.example.audiocutterdemo2.core.amplituda.ProgressOperation
import com.example.audiocutterdemo2.core.amplituda.callback.AmplitudaErrorListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.InputStream

const val DEFAULT_VALUE_FRAME = -1f

data class FrameData(val value: String = "", val totalFrame: Int = 0, val progress: Int = -1)

class DecoderManager( private val amplituda: Amplituda) {

    private val _frames = MutableStateFlow<List<Float>?>(null)
    val frames: StateFlow<List<Float>?> = _frames.asStateFlow()

    suspend fun loadFramesWithInputStream(
        uriString: String,
        inputStream: InputStream
    ) = withContext(Dispatchers.IO) {
        _frames.value = null

        amplituda.processAudio(
            inputStream,
            uriString,
            object : AmplitudaProgressListener() {
                override fun onProgress(
                    operation: ProgressOperation?,
                    path: String,
                    progress: Int,
                    data: String,
                    totalFrame: Int
                ) {
                    val itemFrames = data.amplitudesAsList()
                    val temps = mutableListOf<Float>()
                    val endPoint = itemFrames.size.coerceAtLeast(totalFrame)

                    if (totalFrame > 0 || itemFrames.isNotEmpty()) {
                        for (i in 0 until endPoint) {
                            temps.add(if (i < itemFrames.size) itemFrames[i] else DEFAULT_VALUE_FRAME)
                        }
                    }

                    _frames.value = temps
                }
            }
        ).get(
            AmplitudaErrorListener {
                _frames.value = null
            }
        )
    }
}

fun String?.amplitudesAsList(): List<Float> {
    if (this == null || this.isEmpty()) return emptyList()
    val log: Array<String> = this.split("\n").toTypedArray()
    val amplitudes: MutableList<Int> = ArrayList()
    for (amplitude in log) {
        if (amplitude.isEmpty()) {
            break
        }
        amplitudes.add(Integer.valueOf(amplitude))
    }
    return amplitudes.map(Int::toFloat)
}