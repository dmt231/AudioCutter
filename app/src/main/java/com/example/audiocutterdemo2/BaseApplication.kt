package com.example.audiocutterdemo2

import android.app.Application
import com.example.audiocutterdemo2.di.appModule
import com.example.audiocutterdemo.di.repositoryModule
import com.example.audiocutterdemo2.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin

class BaseApplication : Application(){
    companion object {
        lateinit var instance: BaseApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()

        instance = this

        startKoin {
            androidLogger()

            androidContext(this@BaseApplication)
            modules(appModule, repositoryModule, viewModelModule)
        }
    }
}