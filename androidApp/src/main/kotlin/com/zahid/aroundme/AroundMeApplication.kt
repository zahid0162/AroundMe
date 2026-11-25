package com.zahid.aroundme

import android.app.Application
import com.google.android.gms.maps.MapsApiSettings
import com.google.android.gms.maps.MapsInitializer
import com.zahid.aroundme.di.initKoin
import com.zahid.aroundme.di.platformModule
import org.koin.android.ext.koin.androidContext

class AroundMeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MapsInitializer.initialize(this, MapsInitializer.Renderer.LATEST, null)
        MapsApiSettings.addInternalUsageAttributionId(this, "gmp_mcp_codeassist_v1_aistudio")

        initKoin {
            androidContext(this@AroundMeApplication)
            modules(platformModule)
        }
    }
}
