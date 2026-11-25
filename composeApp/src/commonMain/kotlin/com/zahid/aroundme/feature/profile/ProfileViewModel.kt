package com.zahid.aroundme.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zahid.aroundme.domain.model.AppLanguage
import com.zahid.aroundme.domain.model.DistanceUnit
import com.zahid.aroundme.domain.model.LocationPermissionStatus
import com.zahid.aroundme.domain.model.ThemePreference
import com.zahid.aroundme.domain.model.UserPreferences
import com.zahid.aroundme.domain.repository.LocationRepository
import com.zahid.aroundme.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val locationRepository: LocationRepository
) : ViewModel() {

    val preferences: StateFlow<UserPreferences> = userPreferencesRepository.preferences
    val permissionStatus: StateFlow<LocationPermissionStatus> = locationRepository.permissionStatus

    fun setTheme(theme: ThemePreference) {
        viewModelScope.launch {
            userPreferencesRepository.updatePreferences { it.copy(themePreference = theme) }
        }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            userPreferencesRepository.updatePreferences { it.copy(language = language) }
        }
    }

    fun setDistanceUnit(unit: DistanceUnit) {
        viewModelScope.launch {
            userPreferencesRepository.updatePreferences { it.copy(distanceUnit = unit) }
        }
    }

    fun setDefaultRadius(meters: Double) {
        viewModelScope.launch {
            userPreferencesRepository.updatePreferences {
                it.copy(defaultRadiusMeters = meters.coerceIn(1000.0, 50_000.0))
            }
        }
    }

    fun refreshLocationStatus() {
        locationRepository.refreshPermissionState()
    }

    fun openAppSettings() {
        locationRepository.openAppSettings()
    }

    fun openLocationServicesSettings() {
        locationRepository.openLocationServicesSettings()
    }
}
