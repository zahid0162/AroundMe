package com.zahid.aroundme

import android.app.Application
import com.zahid.aroundme.di.initKoin
import org.koin.android.ext.koin.androidContext

class AroundMeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@AroundMeApplication)
        }
    }
}
