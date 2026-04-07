package com.example.audiocutterdemo.di

import com.example.audiocutterdemo.core.ffmpeg.FFMpegController
import com.example.audiocutterdemo.core.ffmpeg.FFMpegControllerImpl
import com.example.audiocutterdemo.core.file_manager.FileManagerImpl
import com.example.audiocutterdemo.core.file_manager.IFileManager
import org.koin.dsl.module

val repositoryModule = module {
    single<FFMpegController> { FFMpegControllerImpl(fileManager = get()) }
    single<IFileManager>{ FileManagerImpl(get()) }
}
