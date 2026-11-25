package com.zahid.aroundme.di

import com.zahid.aroundme.core.platform.AndroidLocationRepository
import com.zahid.aroundme.domain.repository.LocationRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

actual val platformModule = module {
    single<LocationRepository> { AndroidLocationRepository(androidContext()) }
}
