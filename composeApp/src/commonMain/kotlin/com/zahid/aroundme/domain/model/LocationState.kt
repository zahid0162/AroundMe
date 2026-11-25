package com.zahid.aroundme.domain.model

enum class LocationPermissionStatus {
    UNKNOWN,
    GRANTED_PRECISE,
    GRANTED_APPROXIMATE,
    DENIED,
    PERMANENTLY_DENIED,
    SERVICES_DISABLED;

    val isGranted: Boolean
        get() = this == GRANTED_PRECISE || this == GRANTED_APPROXIMATE
}

data class MapCameraRegion(
    val center: GeoPoint,
    val zoom: Float,
    val visibleRadiusMeters: Double
)

data class CameraCommand(
    val id: Long,
    val target: GeoPoint,
    val zoom: Float = 14.5f,
    val animate: Boolean = true
)
