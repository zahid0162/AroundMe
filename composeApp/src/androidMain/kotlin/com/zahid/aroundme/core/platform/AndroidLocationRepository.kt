package com.zahid.aroundme.core.platform

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.zahid.aroundme.domain.model.GeoPoint
import com.zahid.aroundme.domain.model.LocationPermissionStatus
import com.zahid.aroundme.domain.repository.LocationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class AndroidLocationRepository(
    private val context: Context
) : LocationRepository {

    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)

    private val _permissionStatus = MutableStateFlow(evaluatePermissionStatus())
    override val permissionStatus: StateFlow<LocationPermissionStatus> = _permissionStatus.asStateFlow()

    private val _currentLocation = MutableStateFlow<GeoPoint?>(null)
    override val currentLocation: StateFlow<GeoPoint?> = _currentLocation.asStateFlow()

    override fun refreshPermissionState() {
        _permissionStatus.value = evaluatePermissionStatus()
    }

    internal fun updatePermissionStatus(status: LocationPermissionStatus) {
        _permissionStatus.value = if (status.isGranted && !isLocationServicesEnabled()) {
            LocationPermissionStatus.SERVICES_DISABLED
        } else {
            status
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun refreshCurrentLocation(): Result<GeoPoint> {
        val status = evaluatePermissionStatus()
        _permissionStatus.value = status

        if (status == LocationPermissionStatus.SERVICES_DISABLED) {
            return Result.failure(IllegalStateException("Location services are disabled."))
        }
        if (!status.isGranted) {
            return Result.failure(SecurityException("Location permission not granted."))
        }

        val priority = if (status == LocationPermissionStatus.GRANTED_PRECISE) {
            Priority.PRIORITY_HIGH_ACCURACY
        } else {
            Priority.PRIORITY_BALANCED_POWER_ACCURACY
        }

        val fresh = fetchCurrentLocation(priority)
        if (fresh != null) {
            _currentLocation.value = fresh
            return Result.success(fresh)
        }

        val lastKnown = fetchLastKnownLocation()
        return if (lastKnown != null) {
            _currentLocation.value = lastKnown
            Result.success(lastKnown)
        } else {
            Result.failure(IllegalStateException("Unable to obtain device location."))
        }
    }

    override fun openAppSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    override fun openLocationServicesSettings() {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun evaluatePermissionStatus(): LocationPermissionStatus {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fineGranted && !coarseGranted) {
            return if (_permissionStatus.value == LocationPermissionStatus.PERMANENTLY_DENIED) {
                LocationPermissionStatus.PERMANENTLY_DENIED
            } else {
                LocationPermissionStatus.UNKNOWN
            }
        }

        if (!isLocationServicesEnabled()) {
            return LocationPermissionStatus.SERVICES_DISABLED
        }

        return if (fineGranted) {
            LocationPermissionStatus.GRANTED_PRECISE
        } else {
            LocationPermissionStatus.GRANTED_APPROXIMATE
        }
    }

    private fun isLocationServicesEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return false
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    @SuppressLint("MissingPermission")
    private suspend fun fetchCurrentLocation(priority: Int): GeoPoint? =
        suspendCancellableCoroutine { cont ->
            val cts = CancellationTokenSource()
            cont.invokeOnCancellation { cts.cancel() }

            fusedClient.getCurrentLocation(priority, cts.token)
                .addOnSuccessListener { location ->
                    if (cont.isActive) {
                        cont.resume(location?.let { GeoPoint(it.latitude, it.longitude) })
                    }
                }
                .addOnFailureListener {
                    if (cont.isActive) cont.resume(null)
                }
        }

    @SuppressLint("MissingPermission")
    private suspend fun fetchLastKnownLocation(): GeoPoint? =
        suspendCancellableCoroutine { cont ->
            fusedClient.lastLocation
                .addOnSuccessListener { location ->
                    if (cont.isActive) {
                        cont.resume(location?.let { GeoPoint(it.latitude, it.longitude) })
                    }
                }
                .addOnFailureListener {
                    if (cont.isActive) cont.resume(null)
                }
        }
}
