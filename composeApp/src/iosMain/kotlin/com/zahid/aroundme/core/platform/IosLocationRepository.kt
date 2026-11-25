package com.zahid.aroundme.core.platform

import com.zahid.aroundme.domain.model.GeoPoint
import com.zahid.aroundme.domain.model.LocationPermissionStatus
import com.zahid.aroundme.domain.repository.LocationRepository
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreLocation.CLAccuracyAuthorization
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusDenied
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLAuthorizationStatusRestricted
import platform.CoreLocation.kCLLocationAccuracyBest
import platform.CoreLocation.kCLLocationAccuracyReduced
import platform.Foundation.NSError
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.darwin.NSObject
import kotlin.coroutines.resume

@OptIn(ExperimentalForeignApi::class)
class IosLocationRepository : LocationRepository {

    private val locationManager = CLLocationManager()
    private var pendingLocationContinuation: CancellableContinuation<Result<GeoPoint>>? = null
    private var pendingPermissionCallback: ((LocationPermissionStatus) -> Unit)? = null

    private val _permissionStatus = MutableStateFlow(evaluateStatus())
    override val permissionStatus: StateFlow<LocationPermissionStatus> = _permissionStatus.asStateFlow()

    private val _currentLocation = MutableStateFlow<GeoPoint?>(null)
    override val currentLocation: StateFlow<GeoPoint?> = _currentLocation.asStateFlow()

    private val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
        override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
            val status = evaluateStatus()
            _permissionStatus.value = status
            pendingPermissionCallback?.invoke(status)
            pendingPermissionCallback = null
        }

        override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
            val latest = didUpdateLocations.lastOrNull() as? CLLocation ?: return
            val point = latest.coordinate.useContents {
                GeoPoint(latitude = latitude, longitude = longitude)
            }
            _currentLocation.value = point
            pendingLocationContinuation?.let { cont ->
                if (cont.isActive) cont.resume(Result.success(point))
                pendingLocationContinuation = null
            }
        }

        override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
            pendingLocationContinuation?.let { cont ->
                if (cont.isActive) {
                    cont.resume(Result.failure(IllegalStateException(didFailWithError.localizedDescription)))
                }
                pendingLocationContinuation = null
            }
        }
    }

    init {
        locationManager.delegate = delegate
    }

    override fun refreshPermissionState() {
        _permissionStatus.value = evaluateStatus()
    }

    fun requestWhenInUseAuthorization(onResult: (LocationPermissionStatus) -> Unit) {
        val current = evaluateStatus()
        if (current != LocationPermissionStatus.UNKNOWN) {
            _permissionStatus.value = current
            onResult(current)
            return
        }
        pendingPermissionCallback = onResult
        locationManager.requestWhenInUseAuthorization()
    }

    override suspend fun refreshCurrentLocation(): Result<GeoPoint> {
        val status = evaluateStatus()
        _permissionStatus.value = status
        if (status == LocationPermissionStatus.SERVICES_DISABLED) {
            return Result.failure(IllegalStateException("Location services are disabled."))
        }
        if (!status.isGranted) {
            return Result.failure(SecurityException("Location permission not granted."))
        }

        locationManager.desiredAccuracy = if (status == LocationPermissionStatus.GRANTED_PRECISE) {
            kCLLocationAccuracyBest
        } else {
            kCLLocationAccuracyReduced
        }

        return suspendCancellableCoroutine { cont ->
            pendingLocationContinuation?.cancel()
            pendingLocationContinuation = cont
            cont.invokeOnCancellation { pendingLocationContinuation = null }
            locationManager.requestLocation()
        }
    }

    override fun openAppSettings() {
        val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString) ?: return
        UIApplication.sharedApplication.openURL(url, emptyMap<Any?, Any?>(), null)
    }

    override fun openLocationServicesSettings() {
        openAppSettings()
    }

    private fun evaluateStatus(): LocationPermissionStatus {
        if (!CLLocationManager.locationServicesEnabled()) {
            return LocationPermissionStatus.SERVICES_DISABLED
        }

        return when (locationManager.authorizationStatus) {
            kCLAuthorizationStatusAuthorizedWhenInUse,
            kCLAuthorizationStatusAuthorizedAlways -> {
                if (locationManager.accuracyAuthorization == CLAccuracyAuthorization.CLAccuracyAuthorizationFullAccuracy) {
                    LocationPermissionStatus.GRANTED_PRECISE
                } else {
                    LocationPermissionStatus.GRANTED_APPROXIMATE
                }
            }
            kCLAuthorizationStatusDenied,
            kCLAuthorizationStatusRestricted -> LocationPermissionStatus.PERMANENTLY_DENIED
            kCLAuthorizationStatusNotDetermined -> LocationPermissionStatus.UNKNOWN
            else -> LocationPermissionStatus.DENIED
        }
    }
}
