package com.zahid.aroundme.core.platform

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import com.zahid.aroundme.domain.model.CameraCommand
import com.zahid.aroundme.domain.model.GeoPoint
import com.zahid.aroundme.domain.model.MapCameraRegion
import com.zahid.aroundme.domain.model.Place
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIView

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun AroundMeMapSurface(
    cameraCommand: CameraCommand?,
    places: List<Place>,
    selectedPlaceId: String?,
    isDarkTheme: Boolean,
    isMyLocationEnabled: Boolean,
    bottomContentPaddingDp: Int,
    onSelectPlace: (String) -> Unit,
    onCameraIdle: (MapCameraRegion) -> Unit,
    onMapTap: () -> Unit,
    modifier: Modifier
) {
    val provider = remember { IosMapBridge.getProvider() }
    if (provider == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Google Maps iOS provider not registered",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    var mapHandle by remember { mutableStateOf<IosMapHandle?>(null) }
    var lastCommandId by remember { mutableStateOf(-1L) }

    val markerPayloads = remember(places, selectedPlaceId) {
        places.map { place ->
            IosMapMarkerPayload(
                id = place.id,
                title = place.name,
                latitude = place.location.latitude,
                longitude = place.location.longitude,
                isSelected = place.id == selectedPlaceId
            )
        }
    }

    UIKitView(
        modifier = modifier.fillMaxSize(),
        factory = {
            val initial = cameraCommand?.target ?: GeoPoint(24.6949, 46.6852)
            val callbacks = object : IosMapCallbacks {
                override fun onSelectPlace(placeId: String) {
                    onSelectPlace(placeId)
                }

                override fun onCameraIdle(
                    centerLat: Double,
                    centerLng: Double,
                    zoom: Float,
                    visibleRadiusMeters: Double
                ) {
                    onCameraIdle(
                        MapCameraRegion(
                            center = GeoPoint(centerLat, centerLng),
                            zoom = zoom,
                            visibleRadiusMeters = visibleRadiusMeters
                        )
                    )
                }

                override fun onMapTap() {
                    onMapTap()
                }
            }
            val handle = provider.createMap(
                initialLatitude = initial.latitude,
                initialLongitude = initial.longitude,
                initialZoom = cameraCommand?.zoom ?: 14.2f,
                callbacks = callbacks
            )
            mapHandle = handle
            handle.view
        },
        update = {
            val handle = mapHandle ?: return@UIKitView
            handle.updateAppearance(
                isDarkTheme = isDarkTheme,
                isMyLocationEnabled = isMyLocationEnabled,
                bottomPaddingPoints = bottomContentPaddingDp
            )
            handle.updateMarkers(markerPayloads)

            if (cameraCommand != null && cameraCommand.id != lastCommandId) {
                lastCommandId = cameraCommand.id
                handle.updateCamera(
                    latitude = cameraCommand.target.latitude,
                    longitude = cameraCommand.target.longitude,
                    zoom = cameraCommand.zoom,
                    animate = cameraCommand.animate
                )
            }
        }
    )
}
