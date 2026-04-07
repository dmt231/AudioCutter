package com.example.audiocutterdemo2.di

import com.example.audiocutterdemo2.ui.main.MainViewModel
import org.koin.dsl.module

val viewModelModule = module {
    single { MainViewModel(get(), get()) }
}