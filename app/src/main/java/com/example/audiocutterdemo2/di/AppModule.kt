package com.example.audiocutterdemo2.di

import com.example.audiocutterdemo2.core.amplituda.Amplituda
import com.example.audiocutterdemo2.core.decoder.PcmDecoder
import com.example.audiocutterdemo2.core.ffmpeg.DecoderManager
import org.koin.dsl.module


val appModule = module {
    single<Amplituda> { Amplituda(get()) }
    factory<DecoderManager> { DecoderManager(get()) }
    single<PcmDecoder> { PcmDecoder() }
}
