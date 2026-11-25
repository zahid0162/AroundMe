package com.zahid.aroundme.domain.repository

import com.zahid.aroundme.domain.model.GeoPoint
import com.zahid.aroundme.domain.model.LocationPermissionStatus
import kotlinx.coroutines.flow.StateFlow

interface LocationRepository {
    val permissionStatus: StateFlow<LocationPermissionStatus>
    val currentLocation: StateFlow<GeoPoint?>

    fun refreshPermissionState()
    suspend fun refreshCurrentLocation(): Result<GeoPoint>
    fun openAppSettings()
    fun openLocationServicesSettings()
}
