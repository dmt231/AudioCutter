package com.example.audiocutterdemo2.di

import com.example.audiocutterdemo2.core.amplituda.Amplituda
import com.example.audiocutterdemo2.core.ffmpeg.DecoderManager
import com.example.audiocutterdemo2.permission.PermissionChecker
import com.example.audiocutterdemo2.permission.PermissionCheckerImpl
import org.koin.dsl.module


val appModule = module {
    single<Amplituda> { Amplituda(get()) }
    factory<DecoderManager> { DecoderManager(get()) }
    single<PermissionChecker> { PermissionCheckerImpl(get()) }
}
