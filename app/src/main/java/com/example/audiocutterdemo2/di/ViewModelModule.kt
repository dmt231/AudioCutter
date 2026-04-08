package com.example.audiocutterdemo2.di

import com.example.audiocutterdemo2.ui.main.MainViewModel
import org.koin.dsl.module

val viewModelModule = module {
    single { MainViewModel(fileManager = get(), decoderManager = get(), permissionChecker = get()) }
}