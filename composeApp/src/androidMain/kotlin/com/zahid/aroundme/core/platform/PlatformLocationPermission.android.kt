package com.zahid.aroundme.core.platform

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import com.zahid.aroundme.domain.model.LocationPermissionStatus
import com.zahid.aroundme.domain.repository.LocationRepository
import org.koin.compose.koinInject

@Composable
actual fun rememberLocationPermissionRequester(
    onPermissionResult: (LocationPermissionStatus) -> Unit
): () -> Unit {
    val context = LocalContext.current
    val locationRepo: LocationRepository = koinInject()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val fineGranted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        val status = when {
            fineGranted -> LocationPermissionStatus.GRANTED_PRECISE
            coarseGranted -> LocationPermissionStatus.GRANTED_APPROXIMATE
            else -> {
                val activity = context.findActivity()
                val canShowRationale = activity != null && (
                    ActivityCompat.shouldShowRequestPermissionRationale(
                        activity,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) || ActivityCompat.shouldShowRequestPermissionRationale(
                        activity,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
                if (canShowRationale) {
                    LocationPermissionStatus.DENIED
                } else {
                    LocationPermissionStatus.PERMANENTLY_DENIED
                }
            }
        }

        (locationRepo as? AndroidLocationRepository)?.updatePermissionStatus(status)
        locationRepo.refreshPermissionState()
        onPermissionResult(locationRepo.permissionStatus.value)
    }

    return remember(launcher) {
        {
            launcher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
