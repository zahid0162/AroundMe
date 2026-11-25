package com.zahid.aroundme.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface AroundMeDestination {
    @Serializable
    data object Explore : AroundMeDestination

    @Serializable
    data object Search : AroundMeDestination

    @Serializable
    data object Saved : AroundMeDestination

    @Serializable
    data object Profile : AroundMeDestination

    @Serializable
    data class PlaceDetails(val placeId: String) : AroundMeDestination
}
