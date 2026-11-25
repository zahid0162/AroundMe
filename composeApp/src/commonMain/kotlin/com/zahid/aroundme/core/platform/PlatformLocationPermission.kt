package com.zahid.aroundme.core.platform

import androidx.compose.runtime.Composable
import com.zahid.aroundme.domain.model.LocationPermissionStatus

/**
 * Launches the native foreground location permission prompt (Fine + Coarse on Android,
 * When-In-Use on iOS) and reports the resulting LocationPermissionStatus.
 */
@Composable
expect fun rememberLocationPermissionRequester(
    onPermissionResult: (LocationPermissionStatus) -> Unit
): () -> Unit
