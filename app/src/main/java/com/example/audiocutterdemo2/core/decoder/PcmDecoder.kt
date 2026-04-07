package com.example.audiocutterdemo2.core.decoder

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PcmDecoder {

    suspend fun decode(
        context: Context,
        uri: Uri,
        targetBars: Int = 1000
    ): List<Int> = withContext(Dispatchers.IO) {

        val extractor = MediaExtractor()
        extractor.setDataSource(context, uri, null)

        val trackIndex = (0 until extractor.trackCount).firstOrNull { index ->
            extractor.getTrackFormat(index)
                .getString(MediaFormat.KEY_MIME)
                ?.startsWith("audio/") == true
        } ?: run {
            extractor.release()
            return@withContext emptyList()
        }

        extractor.selectTrack(trackIndex)
        val format = extractor.getTrackFormat(trackIndex)
        val mime = format.getString(MediaFormat.KEY_MIME) ?: run {
            extractor.release()
            return@withContext emptyList()
        }

        val codec = MediaCodec.createDecoderByType(mime)
        codec.configure(format, null, null, 0)
        codec.start()

        val rawSamples = mutableListOf<Short>()
        val bufferInfo = MediaCodec.BufferInfo()
        var inputDone  = false
        var outputDone = false

        while (!outputDone) {
            if (!inputDone) {
                val inputIdx = codec.dequeueInputBuffer(10_000)
                if (inputIdx >= 0) {
                    val buf  = codec.getInputBuffer(inputIdx)!!
                    val size = extractor.readSampleData(buf, 0)
                    if (size < 0) {
                        codec.queueInputBuffer(
                            inputIdx, 0, 0, 0,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM
                        )
                        inputDone = true
                    } else {
                        codec.queueInputBuffer(
                            inputIdx, 0, size,
                            extractor.sampleTime, 0
                        )
                        extractor.advance()
                    }
                }
            }

            val outputIdx = codec.dequeueOutputBuffer(bufferInfo, 10_000)
            if (outputIdx >= 0) {
                val buf    = codec.getOutputBuffer(outputIdx)!!
                val shorts = ShortArray(buf.remaining() / 2)
                buf.asShortBuffer().get(shorts)
                rawSamples.addAll(shorts.toList())
                codec.releaseOutputBuffer(outputIdx, false)

                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                    outputDone = true
                }
            }
        }

        codec.stop()
        codec.release()
        extractor.release()

        if (rawSamples.isEmpty()) return@withContext emptyList()

        val chunkSize = (rawSamples.size / targetBars).coerceAtLeast(1)
        List(targetBars) { i ->
            val from = i * chunkSize
            val to   = minOf(from + chunkSize, rawSamples.size)
            if (from >= rawSamples.size) 0
            else rawSamples.subList(from, to).maxOf { kotlin.math.abs(it.toInt()) }
        }
    }
}