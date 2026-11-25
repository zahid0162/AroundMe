package com.zahid.aroundme.core.platform

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraMoveStartedReason
import com.google.maps.android.compose.ComposeMapColorScheme
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import com.zahid.aroundme.core.geo.GeoUtils
import com.zahid.aroundme.domain.model.CameraCommand
import com.zahid.aroundme.domain.model.GeoPoint
import com.zahid.aroundme.domain.model.MapCameraRegion
import com.zahid.aroundme.domain.model.Place
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

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
    val initialTarget = cameraCommand?.target ?: GeoPoint(24.6949, 46.6852)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            LatLng(initialTarget.latitude, initialTarget.longitude),
            cameraCommand?.zoom ?: 14.2f
        )
    }

    var lastHandledCommandId by remember { mutableStateOf(-1L) }
    var lastMoveByGesture by remember { mutableStateOf(false) }

    LaunchedEffect(cameraCommand?.id) {
        if (cameraCommand != null && cameraCommand.id != lastHandledCommandId) {
            lastHandledCommandId = cameraCommand.id
            lastMoveByGesture = false
            val update = CameraUpdateFactory.newLatLngZoom(
                LatLng(cameraCommand.target.latitude, cameraCommand.target.longitude),
                cameraCommand.zoom
            )
            if (cameraCommand.animate) {
                cameraPositionState.animate(update, durationMs = 450)
            } else {
                cameraPositionState.move(update)
            }
        }
    }

    LaunchedEffect(cameraPositionState) {
        snapshotFlow { cameraPositionState.isMoving to cameraPositionState.cameraMoveStartedReason }
            .distinctUntilChanged()
            .collect { (isMoving, reason) ->
                if (isMoving && reason == CameraMoveStartedReason.GESTURE) {
                    lastMoveByGesture = true
                }
            }
    }

    LaunchedEffect(cameraPositionState) {
        snapshotFlow { cameraPositionState.isMoving }
            .filter { !it }
            .collect {
                if (lastMoveByGesture) {
                    lastMoveByGesture = false
                    val pos = cameraPositionState.position
                    val center = GeoPoint(pos.target.latitude, pos.target.longitude)
                    val bounds = cameraPositionState.projection?.visibleRegion?.latLngBounds
                    val radiusMeters = if (bounds != null) {
                        GeoUtils.haversineMeters(
                            center,
                            GeoPoint(bounds.northeast.latitude, bounds.northeast.longitude)
                        ).coerceIn(300.0, 50_000.0)
                    } else {
                        2500.0
                    }
                    onCameraIdle(
                        MapCameraRegion(
                            center = center,
                            zoom = pos.zoom,
                            visibleRadiusMeters = radiusMeters
                        )
                    )
                }
            }
    }

    val mapProperties = remember(isMyLocationEnabled) {
        MapProperties(
            isMyLocationEnabled = isMyLocationEnabled,
            isBuildingEnabled = true
        )
    }

    val mapUiSettings = remember {
        MapUiSettings(
            compassEnabled = true,
            myLocationButtonEnabled = false,
            zoomControlsEnabled = false,
            mapToolbarEnabled = false,
            rotationGesturesEnabled = true,
            tiltGesturesEnabled = true
        )
    }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        properties = mapProperties,
        uiSettings = mapUiSettings,
        mapColorScheme = if (isDarkTheme) ComposeMapColorScheme.DARK else ComposeMapColorScheme.LIGHT,
        contentPadding = PaddingValues(bottom = bottomContentPaddingDp.dp),
        onMapClick = { onMapTap() }
    ) {
        places.forEach { place ->
            val isSelected = place.id == selectedPlaceId
            val markerState = rememberUpdatedMarkerState(
                position = LatLng(place.location.latitude, place.location.longitude)
            )
            MarkerComposable(
                keys = arrayOf(place.id, isSelected, isDarkTheme),
                state = markerState,
                zIndex = if (isSelected) 10f else 1f,
                onClick = {
                    onSelectPlace(place.id)
                    true
                }
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    contentColor = if (isSelected) {
                        Color.White
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.outline
                    ),
                    shadowElevation = if (isSelected) 8.dp else 3.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Place,
                            contentDescription = null,
                            modifier = Modifier.size(if (isSelected) 18.dp else 15.dp),
                            tint = if (isSelected) Color.White else MaterialTheme.colorScheme.primary
                        )
                        if (isSelected) {
                            Text(
                                text = place.name,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
