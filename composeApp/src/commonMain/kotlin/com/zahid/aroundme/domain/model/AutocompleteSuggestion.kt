package com.zahid.aroundme.domain.model

sealed interface AutocompleteSuggestion {
    data class PlacePrediction(
        val placeId: String,
        val primaryText: String,
        val secondaryText: String,
        val distanceMeters: Int? = null,
        val types: List<String> = emptyList()
    ) : AutocompleteSuggestion

    data class QueryPrediction(
        val queryText: String
    ) : AutocompleteSuggestion
}

data class PlacesFeedResult(
    val places: List<Place>,
    val isLiveApi: Boolean,
    val errorMessage: String? = null
)
