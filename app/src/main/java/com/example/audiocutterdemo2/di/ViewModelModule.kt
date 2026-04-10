package com.example.audiocutterdemo2.di

import com.example.audiocutterdemo2.ui.screen.main.MainViewModel
import org.koin.dsl.module

val viewModelModule = module {
    single { MainViewModel(
        application = get(),
        fileManager = get(),
        decoderManager = get(),
        permissionChecker = get(),
        ffMpegController = get()
    ) }
}