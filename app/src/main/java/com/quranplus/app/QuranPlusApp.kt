package com.quranplus.app

import android.app.Application
import com.quranplus.app.core.di.appModule
import com.quranplus.app.core.database.ReferenceAssetSynchronizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class QuranPlusApp : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        val koinApplication = startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@QuranPlusApp)
            modules(appModule)
        }
        applicationScope.launch {
            koinApplication.koin.get<ReferenceAssetSynchronizer>().synchronize()
        }
    }
}
