package org.wahid.attendancehub

import android.app.Application
import org.wahid.attendancehub.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class StudentApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize Koin for dependency injection
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@StudentApplication)
            modules(viewModelModule)
        }
    }
}

