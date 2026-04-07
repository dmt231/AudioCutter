package com.example.audiomaster.core.ffmpeg.model

import com.example.audiocutterdemo.core.ffmpeg.model.FFMPEGState
import com.example.audiocutterdemo.core.file_manager.TypeName

const val CUT_TYPE = 111
const val MERGE_TYPE = 112
const val MIX_TYPE = 113
const val CONVERT_VIDEO_TO_AUDIO_TYPE = 114
const val CHANGE_SOUND_EFFECT_TYPE = 115
const val CHANGE_FORMAT_TYPE = 116
const val REVERSE_TYPE = 117
const val CHANGE_VOLUME_TYPE = 118
const val REDUCE_NOISE_TYPE = 119
const val RECORDING_TYPE = 120

enum class AudioTaskType(val type: Int) {
    CUT(CUT_TYPE),
    MERGE(MERGE_TYPE),
    MIX(MIX_TYPE),
    CONVERT_VIDEO_TO_AUDIO(CONVERT_VIDEO_TO_AUDIO_TYPE),
    CHANGE_SOUND_EFFECT(CHANGE_SOUND_EFFECT_TYPE),
    CHANGE_FORMAT(CHANGE_FORMAT_TYPE),
    REVERSE(REVERSE_TYPE),
    CHANGE_VOLUME(CHANGE_VOLUME_TYPE),
    REDUCE_NOISE(REDUCE_NOISE_TYPE),
    RECORDING(RECORDING_TYPE)
}

data class AudioTask(
    val id: Long,
    val type: AudioTaskType,
    val outputPath: String,
    val uriString: String = "",
    val state: FFMPEGState,
    val progress: Float,
    val totalDuration: Long,
    val isCancel: Boolean = false,
    val onDone: suspend (isSuccess: Boolean, uriString: String) -> Unit = { _, _ -> },
    val onCancel: suspend (path: String) -> Unit = {}
)

fun AudioTask.updateAll(state: FFMPEGState, progress: Float): AudioTask {
    val curProgress = if (progress > 1f) 1f else progress

    return this.copy(
        state = state, progress = curProgress
    )
}

val AudioTask.fileName: String get() = outputPath.fileName()

private fun String.fileName(): String {
    return if (this.indexOf("/") != -1) substring(lastIndexOf("/") + 1, lastIndexOf(".")) else this
}

fun AudioTaskType.convertToTypeName(): TypeName {
    return when (this) {
        AudioTaskType.CUT -> TypeName.CUTTER
        AudioTaskType.MERGE -> TypeName.MERGER
        AudioTaskType.MIX -> TypeName.MIXER
        AudioTaskType.CONVERT_VIDEO_TO_AUDIO -> TypeName.CONVERT_VIDEO_TO_AUDIO
        AudioTaskType.CHANGE_SOUND_EFFECT -> TypeName.AUDIO_EFFECT
        AudioTaskType.CHANGE_FORMAT -> TypeName.CHANGE_FORMAT
        AudioTaskType.REVERSE -> TypeName.REVERSE
        AudioTaskType.CHANGE_VOLUME -> TypeName.CHANGE_VOLUME
        AudioTaskType.REDUCE_NOISE -> TypeName.NOISE_REDUCE
        AudioTaskType.RECORDING -> TypeName.RECORD
    }
}
