package com.example.audiocutterdemo.core.ffmpeg

import android.content.Context
import com.example.audiocutterdemo.core.ffmpeg.model.AudioCut
import com.example.audiocutterdemo.core.ffmpeg.model.AudioMerge
import com.example.audiocutterdemo.core.ffmpeg.model.AudioMix
import com.example.audiomaster.core.ffmpeg.model.AudioTask
import kotlinx.coroutines.flow.Flow

interface FFMpegController {
    val taskQueue: Flow<List<AudioTask>>

    fun cut(
        audioPath: String,
        outputPath: String,
        splits: List<AudioCut>,
        bitrate: Int,
        fadeInMs: Long,
        fadeOutMs: Long,
        volume: Float,
        onDone: suspend (isSuccess: Boolean, uriString: String) -> Unit = { _, _ -> },
        onCancel: suspend (path: String) -> Unit = {}
    ): Long

    fun merger(
        merges: List<AudioMerge>, outputPath: String,
        onDone: suspend (isSuccess: Boolean, uriString: String) -> Unit = { _, _ -> },
        onCancel: suspend (path: String) -> Unit = {}
    ): Long

    fun mix(
        mixes: List<AudioMix>,
        totalDurationMs: Long,
        outputPath: String,
        onDone: suspend (isSuccess: Boolean, uriString: String) -> Unit = { _, _ -> },
        onCancel: suspend (path: String) -> Unit = {}
    ): Long

    fun checkAudioStreamInVideoFile(videoPath: String) : Boolean

    fun convertVideoToAudio(
        videoPath: String,
        outputPath: String,
        startPositionMs: Long,
        endPositionMs: Long,
        onDone: suspend (isSuccess: Boolean, uriString: String) -> Unit = { _, _ -> },
        onCancel: suspend (path: String) -> Unit = {}
    ): Long

    // tone range: -15 -> 15, tempo range: 0.5 -> 1.5, speed range: 0.5 -> 1.5, volume range: -24dB -> 24dB
    fun changedAudioEffect(
        context: Context,
        audioPath: String,
        outputPath: String,
        tone: Float,
        tempo: Float,
        speed: Float,
        volume: Float,
        totalDurationMs: Long,
        onDone: suspend (isSuccess: Boolean, uriString: String) -> Unit = { _, _ -> },
        onCancel: suspend (path: String) -> Unit = {}
    ): Long

    // tempo range: 0.5 -> 2, volume range: -24dB -> 24dB
    fun changedAudioEffect(
        audioPath: String,
        outputPath: String,
        sampleRate: Float,
        tempo: Float,
        volume: Float,
        totalDurationMs: Long,
        onDone: suspend (isSuccess: Boolean, uriString: String) -> Unit = { _, _ -> },
        onCancel: suspend (path: String) -> Unit = {}
    ): Long

    fun changeFormat(
        audioPath: String,
        format: String,
        totalDurationMs: Long,
        onDone: suspend (isSuccess: Boolean, uriString: String) -> Unit = { _, _ -> },
        onCancel: suspend (path: String) -> Unit = {}
    ): Long

    // value range -24dB -> 24dB
    fun changeVolume(
        audioPath: String,
        outputPath: String,
        volume: Float,
        totalDurationMs: Long,
        onDone: suspend (isSuccess: Boolean, uriString: String) -> Unit = { _, _ -> },
        onCancel: suspend (path: String) -> Unit = {}
    ): Long

    suspend fun reduceNoise(
        context: Context,
        audioPath: String,
        outputPath: String,
        totalDurationMs: Long,
        onDone: suspend (isSuccess: Boolean, uriString: String) -> Unit = { _, _ -> },
        onCancel: suspend (path: String) -> Unit = {}
    ): Long

    fun reverse(
        audioPath: String,
        outputPath: String,
        totalDurationMs: Long,
        onDone: suspend (isSuccess: Boolean, uriString: String) -> Unit = { _, _ -> },
        onCancel: suspend (path: String) -> Unit = {}
    ): Long

    fun changeFormatRecording(
        audioPath: String,
        format: String,
        bitrate: Int,
        totalDurationMs: Long,
        onDone: suspend (isSuccess: Boolean, uriString: String) -> Unit = { _, _ -> },
        onCancel: suspend (path: String) -> Unit = {}
    ): Long

    fun cancel(id: Long)
}