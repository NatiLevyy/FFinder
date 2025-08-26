package com.locationsharing.app.ui.home.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for MapPreviewCard with location refresh capabilities.
 * Works on emulator by attempting to get last known location and retrying.
 */
@HiltViewModel
class MapPreviewViewModel @Inject constructor(
    private val locationProvider: LocationProvider
) : ViewModel() {
    
    private val _lastLocation = MutableStateFlow<LatLng?>(null)
    val lastLocation: StateFlow<LatLng?> = _lastLocation.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _hasLocationPermission = MutableStateFlow(false)
    val hasLocationPermission: StateFlow<Boolean> = _hasLocationPermission.asStateFlow()
    
    init {
        refreshPreview()
    }
    
    fun refreshPreview() = viewModelScope.launch {
        _isLoading.value = true
        try {
            // First try to get last known location
            val lastKnown = locationProvider.getLastLocation()
            if (lastKnown != null) {
                _lastLocation.value = lastKnown
                _hasLocationPermission.value = true
                Timber.d("MapPreview: Got last known location: $lastKnown")
            } else {
                // Try to request a single location fix with timeout
                Timber.d("MapPreview: No last known location, requesting single fix")
                val singleFix = locationProvider.requestSingleFix(timeoutMs = 6000)
                if (singleFix != null) {
                    _lastLocation.value = singleFix
                    _hasLocationPermission.value = true
                    Timber.d("MapPreview: Got single fix location: $singleFix")
                } else {
                    Timber.d("MapPreview: No location available")
                    _hasLocationPermission.value = false
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "MapPreview: Error getting location")
            _hasLocationPermission.value = false
        } finally {
            _isLoading.value = false
        }
    }
}

/**
 * Location provider abstraction for dependency injection and testing.
 */
interface LocationProvider {
    suspend fun getLastLocation(): LatLng?
    suspend fun requestSingleFix(timeoutMs: Long): LatLng?
}