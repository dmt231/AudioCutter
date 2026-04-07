package com.example.audiocutterdemo.core.ffmpeg.model

enum class AudioFormat(val value: String) {
    MP3(".mp3"), AAC(".m4a"), WAV(".wav"), FLAC(".flac")
}

enum class Effect(val time: Int) {
    OFF(0), AFTER_1S(1), AFTER_2S(2), AFTER_3S(3), AFTER_4S(4), AFTER_5S(5), AFTER_6S(6)
}

enum class BitRate(val value: Int) {
    BR_32KB(32), BR_64KB(64), BR_128KB(128), BR_192KB(192), BR_256KB(256), BR_320KB(320)
}

enum class FFMPEGState {
    IDLE, RUNNING, CANCEL, FAILED, SUCCESS
}

enum class MixSelector(val value: String) {
    SHORTEST("shortest"), LONGEST("longest")
}

enum class VoiceSelector(val value: Long) {
    NORMAL(44100),
    WOMEN(44100),
    MAN(44100),
    GIRL(44100),
    BOY(44100),
    BABY(44100),
    ROBOT(44100),
    MINIONS(44100),
    ALIENS(44100),
    MEGAPHONE(44100),
    ECHO(44100),
    CHIPMUNK(44100),
    SPEEDUP(44100),
    SLOWDOWN(44100),
    PARTY(44100),
    DEVIL(44100),
    MONSTER(44100),
    GIANT(44100),
}

enum class AudioEffect {
    SPEED_CHANGE, SOUND_CHANGE, VOICE_CHANGE, REVERSE_CHANGE, REMOVE_NOISE
}

data class AudioCut(val startPositionMs: Long = -1, val endPositionMs: Long = -1)

data class AudioMerge(val path: String, val duration: Long)

data class AudioMix(
    val path: String,
    val startPositionMs: Long,
    val volume: Float,
    val fadeInMs: Long,
    val fadeOutMs: Long,
    val duration: Long,
    val audioCut: AudioCut
)

val AudioMix.isCutEnable get() = audioCut.startPositionMs != -1L && audioCut.endPositionMs != -1L && audioCut.endPositionMs != 0L

fun String.convertAudioFormat(): AudioFormat {
    return when (this) {
        AudioFormat.AAC.value -> {
            AudioFormat.AAC
        }
        AudioFormat.FLAC.value -> {
            AudioFormat.FLAC
        }
        AudioFormat.MP3.value -> {
            AudioFormat.MP3
        }
        else -> {
            AudioFormat.WAV
        }
    }
}
