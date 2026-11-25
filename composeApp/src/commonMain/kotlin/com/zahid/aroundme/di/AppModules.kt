package com.zahid.aroundme.di

import com.zahid.aroundme.core.network.createAroundMeHttpClient
import com.zahid.aroundme.data.local.AroundMeDatabase
import com.zahid.aroundme.data.local.RoomRecentSearchStore
import com.zahid.aroundme.data.local.RoomSavedPlacesRepository
import com.zahid.aroundme.data.local.RoomUserPreferencesRepository
import com.zahid.aroundme.data.remote.PlacesApiClient
import com.zahid.aroundme.data.remote.PlacesRemoteDataSource
import com.zahid.aroundme.data.remote.PlacesRepositoryImpl
import com.zahid.aroundme.data.remote.RecentSearchStore
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
    single { createAroundMeHttpClient() }
    single { get<AroundMeDatabase>().savedPlacesDao() }
    single { get<AroundMeDatabase>().recentSearchDao() }
    single { get<AroundMeDatabase>().userPreferencesDao() }

    single<RecentSearchStore> { RoomRecentSearchStore(get()) }
    single<PlacesRemoteDataSource> { PlacesApiClient(get(), get()) }
    single<PlacesRepository> { PlacesRepositoryImpl(get(), get()) }
    single<SavedPlacesRepository> { RoomSavedPlacesRepository(get()) }
    single<UserPreferencesRepository> { RoomUserPreferencesRepository(get()) }
}

val viewModelModule = module {
    viewModel { ExploreViewModel(get(), get(), get(), get()) }
    viewModel { SearchViewModel(get(), get(), get(), get()) }
    viewModel { SavedViewModel(get(), get(), get(), get()) }
    viewModel { ProfileViewModel(get(), get()) }
    viewModel { params ->
        PlaceDetailsViewModel(
            placeId = params.get(),
            sessionToken = params.getOrNull(),
            placesRepository = get(),
            savedPlacesRepository = get(),
            userPreferencesRepository = get(),
            platformActions = get()
        )
    }
}

fun initKoin(config: KoinAppDeclaration? = null) {
    startKoin {
        config?.invoke(this)
        modules(dataModule, viewModelModule)
    }
}
