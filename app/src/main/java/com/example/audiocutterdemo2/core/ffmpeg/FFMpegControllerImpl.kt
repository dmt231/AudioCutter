package com.example.audiocutterdemo.core.ffmpeg

import android.content.Context
import android.util.Log
import com.arthenica.ffmpegkit.*
import com.example.audiocutterdemo.core.ffmpeg.model.AudioCut
import com.example.audiocutterdemo.core.ffmpeg.model.AudioMerge
import com.example.audiocutterdemo.core.ffmpeg.model.AudioMix
import com.example.audiocutterdemo.core.ffmpeg.model.FFMPEGState
import com.example.audiocutterdemo.core.ffmpeg.model.isCutEnable
import com.example.audiocutterdemo.core.file_manager.IFileManager
import com.example.audiocutterdemo2.core.file_manager.getSampleRate
import com.example.audiomaster.core.ffmpeg.model.*

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONObject
import java.io.*
import java.util.*


class FFMpegControllerImpl(private val fileManager: IFileManager) : FFMpegController {

    private val cmdConvertVideoToAudio = " -y -i \'%s\' -ss %s -to %s -vn -q:a 0 -map a? \'%s\'"
    private val cmdConvertAudioToOtherFormat = " -y -i \'%s\' -c:v copy \'%s\'"
    private val cmdRecordingToOtherFormat = " -y -i \'%s\' -c:v copy -b:a %dk \'%s\'"
    private val cmdReverseAudio = " -y -i \'%s\' -af areverse \'%s\'"

    private val cmdInfoVideo = "-i \'%s\' -show_streams -select_streams a -of json -loglevel error"

    private val cmdChangeVolume = "-y -i \'%s\' -af \"volume=%s\" \'%s\'"

    private val cmdReduceNoise = "-y -i \'%s\' -af \"arnndn=m='%s'\" \'%s\'"

    /***
     *
     * Change Pitch
     * semitone vs octave (octave quãng tám, semitone quãng 12)
     * url : https://www.fengying.org/post/2021-04-23/cli-digital-audio/
     *
     * @param input1: input path
     * @param input2: asetrate (sample_rate after * speed) -> change speed(and pitch) audio (value speed 0.5 -> 2)
     * @param input3: atempo => change pitch (value range 0.5 -> 2)
     * @param input4: output path
     *
     * use asetrate + atempo => change tone keep length audio (UI Voice changer Screen), (value tone = -15 => asetrate = sample_rate * 1/2, tempo = 2)
     *
     * @example: tempo = 1.2, speed = 1.5, tone = 9.00, aresample = 44.1kHz
     * -y -ss 0.0 -i 'input.mp3' -af "atempo=0.705882,asetrate=112455" 'output.mp3'
     *
     */
    private val cmdVoiceChange = " -y -i \'%s\' -af \"asetrate=%s,atempo=%s,volume=%s\" \'%s\'"

    private val cmdCut =
        "-y -i \"%s\" -filter_complex \"%s%s,%svolume=%s[out]\" -map \"[out]\" -b:a %dk \"%s\""

    private val cmdMerge =
        "-y %s -filter_complex \"concat=n=%d:v=0:a=1[a]\" -map \"[a]\" -b:a %dk \'%s\'"
    private val cmdMixAudio =
        "-y %s -filter_complex \"%s:duration=longest:dropout_transition=0[0]\" -map \"[0]\" -q:a 0 '%s'"

    private val fadeOutVolume = "afade=t=out:st=%s:d=%s"
    private val fadeInVolume = "afade=t=in:st=%s:d=%s"

    private val formDelayAudio = "adelay=delays=%dms:all=1"
    private val formTrimAudio = "atrim=start=%s:end=%s"

    private val scope = MainScope()

    private val taskQueueStateFlow = MutableStateFlow<List<AudioTask>>(emptyList())
    private val taskQueueValue
        get() = taskQueueStateFlow.value

    override val taskQueue: Flow<List<AudioTask>> = taskQueueStateFlow.shareIn(
        scope, replay = 1, started = SharingStarted.WhileSubscribed()
    )

    init {
        //execute task one by one.
        FFmpegKitConfig.setAsyncConcurrencyLimit(1)

        FFmpegKitConfig.enableStatisticsCallback { static ->
            scope.launch {
                val taskItem = taskQueueValue.find { it.id == static.sessionId }

                if (taskItem != null && taskItem.isCancel) {
                    FFmpegKit.cancel(static.sessionId)

                    taskItem.onCancel(taskItem.outputPath)

                    taskQueueStateFlow.value = taskQueueValue.filter { it.id != taskItem.id }
                    return@launch
                }

                val result = taskQueueValue.map { task ->
                    if (task.id == static.sessionId) {
                        val currProgress = static.time / task.totalDuration.toFloat()

                        val progressValue = currProgress.toFloat()

                        task.updateAll(
                            state = FFMPEGState.RUNNING,
                            progress = if (progressValue.isNaN()) 0f else progressValue
                        )
                    } else {
                        task
                    }
                }
                taskQueueStateFlow.emit(result)
            }
        }

        FFmpegKitConfig.enableFFmpegSessionCompleteCallback { session ->
            scope.launch {

                val result = taskQueueValue.map { task ->

                    if (task.id == session.sessionId) {

                        val audioCache = fileManager.getCacheFile(
                            type = task.type.convertToTypeName(), path = task.outputPath
                        )

                        var uriString = ""

                        Log.d("MTHAI", "FFMPEG finish audioCache path: ${audioCache?.path}")

                        val isSuccess = if (audioCache != null) {
                            uriString = fileManager.insertAudioToMediaStore(audioCache)
                            uriString.isNotEmpty()
                        } else {
                            false
                        }

                        task.onDone(isSuccess, uriString)

                        task.copy(
                            state = if (session.state == SessionState.COMPLETED) FFMPEGState.SUCCESS else FFMPEGState.FAILED,
                            uriString = uriString
                        )
                    } else {
                        task
                    }
                }
                taskQueueStateFlow.emit(result)
            }
        }
    }

    override fun cut(
        audioPath: String,
        outputPath: String,
        splits: List<AudioCut>,
        bitrate: Int,
        fadeInMs: Long,
        fadeOutMs: Long,
        volume: Float,
        onDone: suspend (isSuccess: Boolean, uriString: String) -> Unit,
        onCancel: suspend (path: String) -> Unit
    ): Long {

        val totalDurationMs = splits.sumOf {
            it.endPositionMs - it.startPositionMs
        }
        val trimCommand = generateTrimCommand(splits = splits)
        val concatCommand = generateConcatCommand(splits = splits)
        val fadeCommand = generateFadeCommand(
            fadeInMs = fadeInMs, fadeOutMs = fadeOutMs, totalDurationMs = totalDurationMs
        )
        val volumeCommand = generateFloatToStr(value = volume)

        val cmd = String.format(
            cmdCut,
            audioPath,
            trimCommand,
            concatCommand,
            fadeCommand,
            volumeCommand,
            bitrate,
            outputPath
        )

        val session = execute(cmd)

        val audioTask = AudioTask(
            id = session.sessionId,
            outputPath = outputPath,
            state = FFMPEGState.IDLE,
            progress = 0f,
            totalDuration = totalDurationMs,
            type = AudioTaskType.CUT,
            onDone = onDone,
            onCancel = onCancel
        )

        addQueue(audioTask)

        return session.sessionId
    }

    override fun merger(
        merges: List<AudioMerge>,
        outputPath: String,
        onDone: suspend (isSuccess: Boolean, uriString: String) -> Unit,
        onCancel: suspend (path: String) -> Unit
    ): Long {

        val strInput = merges.map {
            it.path
        }.formatInput()

        val formatExecute =
            String.format(Locale.ENGLISH, cmdMerge, strInput, merges.size, 128, outputPath)

        val session = execute(strCommand = formatExecute)

        val audioTask = AudioTask(
            id = session.sessionId,
            outputPath = outputPath,
            state = FFMPEGState.IDLE,
            progress = 0f,
            type = AudioTaskType.MERGE,
            totalDuration = merges.sumOf { it.duration },
            onDone = onDone,
            onCancel = onCancel
        )
        addQueue(audioTask)
        return session.sessionId
    }

    override fun mix(
        mixes: List<AudioMix>,
        totalDurationMs: Long,
        outputPath: String,
        onDone: suspend (isSuccess: Boolean, uriString: String) -> Unit,
        onCancel: suspend (path: String) -> Unit
    ): Long {
        val strInput = mixes.formatInputMix()
        val strFilterComplex = mixes.formatFilterComplexMix()
        val cmdMixAudio = String.format(
            Locale.ENGLISH, cmdMixAudio, strInput, strFilterComplex, outputPath
        )

        val session = execute(strCommand = cmdMixAudio)

        val audioTask = AudioTask(
            id = session.sessionId,
            outputPath = outputPath,
            state = FFMPEGState.IDLE,
            progress = 0f,
            totalDuration = totalDurationMs,
            type = AudioTaskType.MIX,
            onDone = onDone,
            onCancel = onCancel
        )

        addQueue(audioTask)

        return session.sessionId
    }

    override fun checkAudioStreamInVideoFile(videoPath: String): Boolean {
        val cmdFFProbe = String.format(cmdInfoVideo, videoPath)
        val ffProbeSession = FFprobeKit.execute(cmdFFProbe)

        return ffProbeSession.output.audioStreamExist()
    }

    override fun convertVideoToAudio(
        videoPath: String,
        outputPath: String,
        startPositionMs: Long,
        endPositionMs: Long,
        onDone: suspend (isSuccess: Boolean, uriString: String) -> Unit,
        onCancel: suspend (path: String) -> Unit
    ): Long {

        val command = String.format(
            cmdConvertVideoToAudio,
            videoPath,
            startPositionMs.toFormTime(),
            endPositionMs.toFormTime(),
            outputPath
        )

        val session = execute(strCommand = command)

        val audioTask = AudioTask(
            id = session.sessionId,
            outputPath = outputPath,
            state = FFMPEGState.IDLE,
            progress = 0f,
            totalDuration = endPositionMs - startPositionMs,
            type = AudioTaskType.CONVERT_VIDEO_TO_AUDIO,
            onDone = onDone,
            onCancel = onCancel
        )

        addQueue(audioTask)

        return session.sessionId
    }

    override fun changedAudioEffect(
        context: Context,
        audioPath: String,
        outputPath: String,
        tone: Float,
        tempo: Float,
        speed: Float,
        volume: Float,
        totalDurationMs: Long,
        onDone: suspend (isSuccess: Boolean, uriString: String) -> Unit,
        onCancel: suspend (path: String) -> Unit
    ): Long {

        val sampleRate = context.getSampleRate(audioPath) ?: -1f
        val toneConvert =
            convertTone(currValue = tone, min1 = -15f, max1 = 15f, min2 = -2f, max2 = 2f)

        val distanceSampleRate = getDistanceSampleRateChanged(
            oldSampleRate = sampleRate, toneConvert = toneConvert
        )

        val newSampleRate = sampleRate + distanceSampleRate

        val duration = getNewDuration(
            oldDurationMs = totalDurationMs, speed = speed, tempo = tempo
        )

        val tempoConvert = convertNewTempo(
            oldTempo = tempo, toneConvert = convertTone(
                currValue = tone, min1 = -15f, max1 = 15f, min2 = 0.5f, max2 = 2f
            ), speed = speed
        )

        val command = String.format(
            cmdVoiceChange, audioPath, newSampleRate, tempoConvert, "${volume}dB", outputPath
        )

        val session = execute(strCommand = command)

        val audioTask = AudioTask(
            id = session.sessionId,
            outputPath = outputPath,
            state = FFMPEGState.IDLE,
            progress = 0f,
            totalDuration = duration,
            type = AudioTaskType.CHANGE_SOUND_EFFECT,
            onDone = onDone,
            onCancel = onCancel
        )

        addQueue(audioTask)
        return session.sessionId
    }

    override fun changedAudioEffect(
        audioPath: String,
        outputPath: String,
        sampleRate: Float,
        tempo: Float,
        volume: Float,
        totalDurationMs: Long,
        onDone: suspend (isSuccess: Boolean, uriString: String) -> Unit,
        onCancel: suspend (path: String) -> Unit
    ): Long {

        val command = String.format(
            cmdVoiceChange, audioPath, sampleRate, tempo, "${volume}dB", outputPath
        )

        val session = execute(strCommand = command)

        val audioTask = AudioTask(
            id = session.sessionId,
            outputPath = outputPath,
            state = FFMPEGState.IDLE,
            progress = 0f,
            totalDuration = totalDurationMs,
            type = AudioTaskType.CHANGE_SOUND_EFFECT,
            onDone = onDone,
            onCancel = onCancel
        )

        addQueue(audioTask)
        return session.sessionId
    }

    override fun changeFormat(
        audioPath: String,
        format: String,
        totalDurationMs: Long,
        onDone: suspend (isSuccess: Boolean, uriString: String) -> Unit,
        onCancel: suspend (path: String) -> Unit
    ): Long {
        val outputPath = audioPath.substringBeforeLast(".").plus(format)

        val command = String.format(
            cmdConvertAudioToOtherFormat, audioPath, outputPath
        )

        val session = execute(strCommand = command)

        val audioTask = AudioTask(
            id = session.sessionId,
            outputPath = outputPath,
            state = FFMPEGState.IDLE,
            progress = 0f,
            totalDuration = totalDurationMs,
            type = AudioTaskType.CHANGE_FORMAT,
            onDone = onDone,
            onCancel = onCancel
        )

        addQueue(audioTask)
        return session.sessionId
    }

    override fun changeVolume(
        audioPath: String,
        outputPath: String,
        volume: Float,
        totalDurationMs: Long,
        onDone: suspend (isSuccess: Boolean, uriString: String) -> Unit,
        onCancel: suspend (path: String) -> Unit
    ): Long {
        val command = String.format(cmdChangeVolume, audioPath, "${volume}dB", outputPath)

        val session = execute(strCommand = command)

        val audioTask = AudioTask(
            id = session.sessionId,
            outputPath = outputPath,
            state = FFMPEGState.IDLE,
            progress = 0f,
            totalDuration = totalDurationMs,
            type = AudioTaskType.CHANGE_VOLUME,
            onDone = onDone,
            onCancel = onCancel
        )

        addQueue(audioTask)
        return session.sessionId
    }

    override suspend fun reduceNoise(
        context: Context,
        audioPath: String,
        outputPath: String,
        totalDurationMs: Long,
        onDone: suspend (isSuccess: Boolean, uriString: String) -> Unit,
        onCancel: suspend (path: String) -> Unit
    ): Long {
        val shPath = getRnnoiseFilePath(context)

        val command = String.format(cmdReduceNoise, audioPath, shPath, outputPath)

        val session = execute(strCommand = command)

        val audioTask = AudioTask(
            id = session.sessionId,
            outputPath = outputPath,
            state = FFMPEGState.IDLE,
            progress = 0f,
            totalDuration = totalDurationMs,
            type = AudioTaskType.REDUCE_NOISE,
            onDone = onDone,
            onCancel = onCancel
        )

        addQueue(audioTask)

        return session.sessionId
    }

    override fun reverse(
        audioPath: String,
        outputPath: String,
        totalDurationMs: Long,
        onDone: suspend (isSuccess: Boolean, uriString: String) -> Unit,
        onCancel: suspend (path: String) -> Unit
    ): Long {
        val command = String.format(cmdReverseAudio, audioPath, outputPath)
        val session = execute(strCommand = command)

        val audioTask = AudioTask(
            id = session.sessionId,
            outputPath = outputPath,
            state = FFMPEGState.IDLE,
            progress = 0f,
            totalDuration = totalDurationMs,
            type = AudioTaskType.REVERSE,
            onDone = onDone,
            onCancel = onCancel
        )

        addQueue(audioTask)
        return session.sessionId
    }

    override fun changeFormatRecording(
        audioPath: String,
        format: String,
        bitrate: Int,
        totalDurationMs: Long,
        onDone: suspend (isSuccess: Boolean, uriString: String) -> Unit,
        onCancel: suspend (path: String) -> Unit
    ): Long {

        val outputPath = audioPath.substringBeforeLast(".").plus(format)

        val command = String.format(
            cmdRecordingToOtherFormat, audioPath, bitrate, outputPath
        )

        val session = execute(strCommand = command)

        val audioTask = AudioTask(
            id = session.sessionId,
            outputPath = outputPath,
            state = FFMPEGState.IDLE,
            progress = 0f,
            totalDuration = totalDurationMs,
            type = AudioTaskType.RECORDING,
            onDone = onDone,
            onCancel = onCancel
        )

        addQueue(audioTask)
        return session.sessionId
    }

    override fun cancel(id: Long) {
        scope.launch {
            val item = taskQueueValue.map {
                if (it.id == id) it.copy(isCancel = true)
                else it
            }
            taskQueueStateFlow.emit(item)
        }
    }

    private fun addQueue(audioTask: AudioTask) {
        scope.launch {
            val tasks = taskQueueValue.toMutableList().apply {
                add(audioTask)
            }

            taskQueueStateFlow.emit(tasks)
        }
    }

    private fun String.audioStreamExist(): Boolean {
        try {
            val jsonObj = JSONObject(this)
            val streams = jsonObj.optJSONArray("streams")

            if (streams != null && streams.length() != 0) {
                return true
            }

            return false
        } catch (e: Exception) {
            return false
        }

    }

    private fun execute(strCommand: String): FFmpegSession {
        return FFmpegKit.executeAsync(strCommand) {}
    }

    private fun List<AudioMix>.formatFilterComplexMix(): String {
        var strVolume = ""
        var allAudioStream = ""
        forEachIndexed { index, audioMix ->
            val strTrim = audioMix.generateTrimCmd()
            val strFade = audioMix.generateFadeCmd()
            val strDelay = audioMix.generateDelayCmd()
            strVolume =
                strVolume.plus("[${index}:0]${if (strTrim.isEmpty()) "" else "$strTrim,"}volume=${audioMix.volume},${if (strFade.isEmpty()) "" else "$strFade,"}${strDelay}[$index];")
            allAudioStream = allAudioStream.plus("[${index}]")
        }
        return strVolume.plus(allAudioStream).plus("amix=inputs=$size")
    }

    private fun AudioMix.generateTrimCmd(): String {
        return if (isCutEnable) String.format(
            Locale.ENGLISH,
            formTrimAudio,
            audioCut.startPositionMs.toFormTime(),
            audioCut.endPositionMs.toFormTime()
        )
        else ""
    }

    private fun AudioMix.generateDelayCmd(): String {
        return String.format(Locale.ENGLISH, formDelayAudio, startPositionMs)
    }

    private fun AudioMix.generateFadeCmd(): String {
        val isFadeVolume = fadeInMs != 0L || fadeOutMs != 0L
        val startFade = if (isCutEnable) audioCut.startPositionMs else 0
        val endFade = if (isCutEnable) audioCut.endPositionMs - fadeOutMs else duration

        return if (isFadeVolume) {
            val strFadeIn = if (fadeInMs != 0L) String.format(
                Locale.ENGLISH, fadeInVolume, startFade.toFormTime(), fadeInMs.toFormTime()
            )
            else ""
            val strFadeOut = if (fadeOutMs != 0L) String.format(
                Locale.ENGLISH, fadeOutVolume, endFade.toFormTime(), fadeOutMs.toFormTime()
            )
            else ""

            if (strFadeIn.isNotEmpty() && strFadeOut.isNotEmpty()) {
                return "${strFadeIn},${strFadeOut}"
            } else {
                return "${strFadeIn}${strFadeOut}"
            }
        } else {
            ""
        }
    }

    private fun List<AudioMix>.formatInputMix(): String {
        return this.map { it.path }.formatInput()
    }

    private fun List<String>.formatInput(): String {
        var str = ""
        this.forEach {
            str = str.plus("-i \"$it\" ")
        }
        return str
    }

    private fun Long.toFormTime(): String {
        return (this / 1000f).toString().replace(",", ".")
    }

    private fun generateTrimCommand(splits: List<AudioCut>): String {
        val cmdTrim = "[0:a]atrim=start=%s:end=%s,asetpts=PTS-STARTPTS[%d]"

        val trimCommand = StringBuilder("")
        splits.forEachIndexed { index, audioCut ->
            val cmd = String.format(
                cmdTrim,
                audioCut.startPositionMs.toFormTime(),
                audioCut.endPositionMs.toFormTime(),
                index
            )
            trimCommand.append("$cmd;")
        }

        return trimCommand.toString()
    }

    private fun generateConcatCommand(splits: List<AudioCut>): String {
        val cmdConcatTrim = "%sconcat=n=%d:v=0:a=1"
        val trimIndex = StringBuilder("")
        splits.forEachIndexed { index, _ ->
            trimIndex.append("[$index]")
        }

        return String.format(
            cmdConcatTrim, trimIndex, splits.size
        )
    }

    private fun generateFadeCommand(
        fadeInMs: Long, fadeOutMs: Long, totalDurationMs: Long
    ): String {
        val cmdFade = StringBuilder("")

        if (fadeInMs > 0L) {
            cmdFade.append(
                String.format(fadeInVolume, 0, fadeInMs.toFormTime())
            )
        }

        if (fadeOutMs > 0L) {
            if (cmdFade.isNotEmpty()) {
                cmdFade.append(",")
            }
            cmdFade.append(
                String.format(
                    fadeOutVolume,
                    (totalDurationMs - fadeOutMs).coerceAtLeast(0L).toFormTime(),
                    fadeOutMs.toFormTime()
                )
            )
        }

        if (cmdFade.isNotEmpty()) {
            cmdFade.append(",")
        }

        return cmdFade.toString()
    }

    private fun generateFloatToStr(value: Float): String {
        val valueStr = value.toString()
        return valueStr.replace(",", ".")
    }

    private suspend fun getRnnoiseFilePath(context: Context): String? =
        withContext(Dispatchers.IO) {
            val filesDir = context.filesDir.absolutePath
            val fileName = "sh.rnnn"

            val existFile = File(filesDir, fileName)

            if (existFile.exists()) return@withContext existFile.absolutePath

            var inputStream: InputStream? = null
            var outputStream: OutputStream? = null

            try {
                inputStream = context.assets.open(fileName)

                val outFile = File(filesDir, fileName)

                outputStream = FileOutputStream(outFile)
                inputStream.copyTo(outputStream)

                return@withContext outFile.absolutePath
            } catch (e: IOException) {
                e.printStackTrace()
                return@withContext null
            } finally {
                try {
                    inputStream?.close()
                    outputStream?.flush()
                    outputStream?.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
}