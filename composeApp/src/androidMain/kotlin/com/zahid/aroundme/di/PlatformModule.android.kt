package com.zahid.aroundme.di

import com.zahid.aroundme.core.platform.AndroidLocationRepository
import com.zahid.aroundme.core.platform.AndroidPlacesConfigProvider
import com.zahid.aroundme.core.platform.AndroidPlatformActions
import com.zahid.aroundme.core.platform.PlatformActions
import com.zahid.aroundme.core.platform.PlatformPlacesConfig
import com.zahid.aroundme.data.local.AroundMeDatabase
import com.zahid.aroundme.data.local.buildAroundMeDatabase
import com.zahid.aroundme.data.local.createAndroidDatabaseBuilder
import com.zahid.aroundme.domain.repository.LocationRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

actual val platformModule = module {
    single<LocationRepository> { AndroidLocationRepository(androidContext()) }
    single<PlatformPlacesConfig> { AndroidPlacesConfigProvider.load(androidContext()) }
    single<PlatformActions> { AndroidPlatformActions(androidContext()) }
    single<AroundMeDatabase> { buildAroundMeDatabase(createAndroidDatabaseBuilder(androidContext())) }
}
