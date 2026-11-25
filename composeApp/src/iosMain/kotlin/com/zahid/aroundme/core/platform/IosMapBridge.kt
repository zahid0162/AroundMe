package com.zahid.aroundme.core.platform

import platform.UIKit.UIView

data class IosMapMarkerPayload(
    val id: String,
    val title: String,
    val latitude: Double,
    val longitude: Double,
    val isSelected: Boolean
)

interface IosMapHandle {
    val view: UIView
    fun updateCamera(latitude: Double, longitude: Double, zoom: Float, animate: Boolean)
    fun updateMarkers(markers: List<IosMapMarkerPayload>)
    fun updateAppearance(isDarkTheme: Boolean, isMyLocationEnabled: Boolean, bottomPaddingPoints: Int)
}

interface IosMapCallbacks {
    fun onSelectPlace(placeId: String)
    fun onCameraIdle(centerLat: Double, centerLng: Double, zoom: Float, visibleRadiusMeters: Double)
    fun onMapTap()
}

interface IosMapViewProvider {
    fun createMap(
        initialLatitude: Double,
        initialLongitude: Double,
        initialZoom: Float,
        callbacks: IosMapCallbacks
    ): IosMapHandle
}

object IosMapBridge {
    private var provider: IosMapViewProvider? = null

    fun register(provider: IosMapViewProvider) {
        this.provider = provider
    }

    internal fun getProvider(): IosMapViewProvider? = provider
}
