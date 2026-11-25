package com.zahid.aroundme.core.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.zahid.aroundme.domain.model.CameraCommand
import com.zahid.aroundme.domain.model.MapCameraRegion
import com.zahid.aroundme.domain.model.Place

/**
 * Platform-specific interactive Google Map surface.
 * Android actual: maps-compose GoogleMap.
 * iOS actual: UIKitView hosting GMSMapView via AroundMeGoogleMapProvider.
 */
@Composable
expect fun AroundMeMapSurface(
    cameraCommand: CameraCommand?,
    places: List<Place>,
    selectedPlaceId: String?,
    isDarkTheme: Boolean,
    isMyLocationEnabled: Boolean,
    bottomContentPaddingDp: Int,
    onSelectPlace: (String) -> Unit,
    onCameraIdle: (MapCameraRegion) -> Unit,
    onMapTap: () -> Unit,
    modifier: Modifier = Modifier
)
