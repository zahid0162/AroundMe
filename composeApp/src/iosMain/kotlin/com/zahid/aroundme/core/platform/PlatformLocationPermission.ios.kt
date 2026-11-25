package com.zahid.aroundme.core.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.zahid.aroundme.domain.model.LocationPermissionStatus
import com.zahid.aroundme.domain.repository.LocationRepository
import org.koin.compose.koinInject

@Composable
actual fun rememberLocationPermissionRequester(
    onPermissionResult: (LocationPermissionStatus) -> Unit
): () -> Unit {
    val locationRepo: LocationRepository = koinInject()

    return remember(locationRepo) {
        {
            val iosRepo = locationRepo as? IosLocationRepository
            if (iosRepo != null) {
                iosRepo.requestWhenInUseAuthorization(onPermissionResult)
            } else {
                locationRepo.refreshPermissionState()
                onPermissionResult(locationRepo.permissionStatus.value)
            }
        }
    }
}
