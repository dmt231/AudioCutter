package com.example.audiocutterdemo.core.ffmpeg

import com.example.audiocutterdemo.utils.convertValue

fun convertTone(
    currValue: Float, min1: Float = -15f, max1: Float = 15f, min2: Float = -2f, max2: Float = 2f
): Float {
    val percentOfDistance = (currValue - min1) / (max1 - min1)
    return percentOfDistance * (max2 - min2) + min2
}

fun getDistanceSampleRateChanged(oldSampleRate: Float, toneConvert: Float): Float {
    val tone = if (toneConvert >= 0)
        toneConvert / 2
    else
        -convertValue(
            currValue = toneConvert,
            min1 = -2f,
            max1 = 0f,
            min2 = 0.5f,
            max2 = 0f
        )

    return oldSampleRate * tone
}

fun convertNewTempo(oldTempo: Float, toneConvert: Float, speed: Float): Float {
    return oldTempo / toneConvert * speed
}

fun getNewDuration(
    oldDurationMs: Long, speed: Float, tempo: Float
): Long {
    return (oldDurationMs / speed / tempo).toLong()
}