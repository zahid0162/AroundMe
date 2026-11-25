package com.zahid.aroundme.di

import com.zahid.aroundme.core.platform.IosLocationRepository
import com.zahid.aroundme.core.platform.IosPlacesConfigProvider
import com.zahid.aroundme.core.platform.IosPlatformActions
import com.zahid.aroundme.core.platform.PlatformActions
import com.zahid.aroundme.core.platform.PlatformPlacesConfig
import com.zahid.aroundme.data.local.AroundMeDatabase
import com.zahid.aroundme.data.local.buildAroundMeDatabase
import com.zahid.aroundme.data.local.createIosDatabaseBuilder
import com.zahid.aroundme.domain.repository.LocationRepository
import org.koin.dsl.module

actual val platformModule = module {
    single<LocationRepository> { IosLocationRepository() }
    single<PlatformPlacesConfig> { IosPlacesConfigProvider.load() }
    single<PlatformActions> { IosPlatformActions() }
    single<AroundMeDatabase> { buildAroundMeDatabase(createIosDatabaseBuilder()) }
}
