package com.zahid.aroundme.di

import com.zahid.aroundme.core.platform.IosLocationRepository
import com.zahid.aroundme.domain.repository.LocationRepository
import org.koin.dsl.module

actual val platformModule = module {
    single<LocationRepository> { IosLocationRepository() }
}
