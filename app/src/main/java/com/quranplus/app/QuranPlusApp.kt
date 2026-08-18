package com.quranplus.app

import android.app.Application
import com.quranplus.app.core.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class QuranPlusApp : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@QuranPlusApp)
            modules(appModule)
        }
    }
}
