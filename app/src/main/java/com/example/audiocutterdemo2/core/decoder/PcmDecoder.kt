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
        } ?: run { extractor.release(); return@withContext emptyList() }

        extractor.selectTrack(trackIndex)
        val format = extractor.getTrackFormat(trackIndex)
        val mime = format.getString(MediaFormat.KEY_MIME)
            ?: run { extractor.release(); return@withContext emptyList() }

        val durationUs = format.getLong(MediaFormat.KEY_DURATION)

        val sampleRate = if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE))
            format.getInteger(MediaFormat.KEY_SAMPLE_RATE) else 44100
        val channelCount = if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT))
            format.getInteger(MediaFormat.KEY_CHANNEL_COUNT) else 1

        val totalSamplesEstimate = ((durationUs / 1_000_000f) * sampleRate).toLong()
        val chunkSize = ((totalSamplesEstimate / channelCount) / targetBars)
            .coerceAtLeast(1).toInt()

        val codec = MediaCodec.createDecoderByType(mime)
        codec.configure(format, null, null, 0)
        codec.start()

        val rawResult    = mutableListOf<Int>()
        val bufferInfo   = MediaCodec.BufferInfo()
        var inputDone    = false
        var outputDone   = false
        var chunkMax     = 0
        var chunkCount   = 0
        var chunkSum = 0L

        while (!outputDone) {
            if (!inputDone) {
                val inputIdx = codec.dequeueInputBuffer(10_000)
                if (inputIdx >= 0) {
                    val buf  = codec.getInputBuffer(inputIdx)!!
                    val size = extractor.readSampleData(buf, 0)
                    if (size < 0) {
                        codec.queueInputBuffer(inputIdx, 0, 0, 0,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        inputDone = true
                    } else {
                        codec.queueInputBuffer(inputIdx, 0, size,
                            extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }
            }

            val outputIdx = codec.dequeueOutputBuffer(bufferInfo, 10_000)
            if (outputIdx >= 0) {
                val buf = codec.getOutputBuffer(outputIdx)!!

                val shortBuffer = buf.order(java.nio.ByteOrder.nativeOrder()).asShortBuffer()

                while (shortBuffer.hasRemaining()) {
                    val sample = shortBuffer.get().toInt()

                    chunkSum += kotlin.math.abs(sample)
                    chunkCount++

                    if (chunkCount >= chunkSize) {
                        val average = (chunkSum / chunkSize).toInt()
                        rawResult.add(average)

                        chunkSum = 0L
                        chunkCount = 0
                    }
                }

                codec.releaseOutputBuffer(outputIdx, false)

                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                    if (chunkCount > 0) {
                        val average = (chunkSum / chunkCount).toInt()
                        rawResult.add(average)
                    }
                    outputDone = true
                }
            }
        }

        codec.stop()
        codec.release()
        extractor.release()

        return@withContext rawResult
    }
}