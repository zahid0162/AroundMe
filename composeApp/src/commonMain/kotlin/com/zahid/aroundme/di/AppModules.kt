package com.zahid.aroundme.di

import com.zahid.aroundme.data.repository.InMemorySavedPlacesRepository
import com.zahid.aroundme.data.repository.InMemoryUserPreferencesRepository
import com.zahid.aroundme.data.repository.SamplePlacesRepository
import com.zahid.aroundme.domain.repository.PlacesRepository
import com.zahid.aroundme.domain.repository.SavedPlacesRepository
import com.zahid.aroundme.domain.repository.UserPreferencesRepository
import com.zahid.aroundme.feature.details.PlaceDetailsViewModel
import com.zahid.aroundme.feature.explore.ExploreViewModel
import com.zahid.aroundme.feature.profile.ProfileViewModel
import com.zahid.aroundme.feature.saved.SavedViewModel
import com.zahid.aroundme.feature.search.SearchViewModel
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

val dataModule = module {
    single<PlacesRepository> { SamplePlacesRepository() }
    single<SavedPlacesRepository> { InMemorySavedPlacesRepository() }
    single<UserPreferencesRepository> { InMemoryUserPreferencesRepository() }
}

val viewModelModule = module {
    viewModel { ExploreViewModel(get(), get(), get(), get()) }
    viewModel { SearchViewModel(get(), get(), get()) }
    viewModel { SavedViewModel(get(), get(), get()) }
    viewModel { ProfileViewModel(get(), get()) }
    viewModel { params -> PlaceDetailsViewModel(placeId = params.get(), get(), get(), get()) }
}

fun initKoin(config: KoinAppDeclaration? = null) {
    startKoin {
        config?.invoke(this)
        modules(dataModule, viewModelModule)
    }
}
