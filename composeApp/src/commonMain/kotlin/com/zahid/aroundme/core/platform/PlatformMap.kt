package com.zahid.aroundme.core.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.zahid.aroundme.domain.model.GeoPoint
import com.zahid.aroundme.domain.model.Place

/**
 * Platform-specific interactive map surface.
 * Phase 1 renders a stylized cartographic preview canvas;
 * Phase 2 replaces actuals with maps-compose (Android) and GMSMapView (iOS).
 */
@Composable
expect fun AroundMeMapSurface(
    center: GeoPoint,
    places: List<Place>,
    selectedPlaceId: String?,
    onSelectPlace: (String) -> Unit,
    onMapCameraMoved: (GeoPoint) -> Unit,
    modifier: Modifier = Modifier
)
