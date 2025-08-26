package com.locationsharing.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * ViewModel for the FFinder Home Screen.
 * 
 * Manages the state of all home screen components including loading states,
 * permissions, location data, UI visibility states, and user interactions.
 * 
 * Handles:
 * - Location permission management
 * - Map preview state and location updates
 * - Animation preferences and accessibility settings
 * - Screen configuration changes (responsive design)
 * - What's New dialog state
 * - User interaction events and navigation
 */
class HomeScreenViewModel : ViewModel() {
    
    private val _state = MutableStateFlow(createInitialState())
    val state: StateFlow<HomeScreenState> = _state.asStateFlow()
    
    init {
        Timber.d("HomeScreenViewModel initialized")
        initializeState()
    }
    
    private fun createInitialState(): HomeScreenState {
        return HomeScreenState(
            isLoading = false,
            hasLocationPermission = false,
            isLocationSharing = false,
            isWaitingForLocationFix = false,
            showWhatsNewDialog = false,
            mapPreviewLocation = null,
            animationsEnabled = true,
            isNarrowScreen = false,
            mapLoadError = false,
            locationError = null,
            isNavigating = false,
            isNavigatingToMap = false,
            isNavigatingToFriends = false,
            isNavigatingToSettings = false,
            retryCount = null,
            timeoutDuration = null
        )
    }
    
    /**
     * Handles all HomeScreenEvents and updates state accordingly.
     * Enhanced with navigation state management for proper button responsiveness.
     * 
     * @param event The event to handle
     */
    fun onEvent(event: HomeScreenEvent) {
        when (event) {
            is HomeScreenEvent.StartSharing -> handleStartSharing()
            is HomeScreenEvent.NavigateToFriends -> handleNavigateToFriends()
            is HomeScreenEvent.NavigateToSettings -> handleNavigateToSettings()
            is HomeScreenEvent.ShowWhatsNew -> handleShowWhatsNew()
            is HomeScreenEvent.DismissWhatsNew -> handleDismissWhatsNew()
            is HomeScreenEvent.LocationPermissionGranted -> handleLocationPermissionGranted()
            is HomeScreenEvent.LocationPermissionDenied -> handleLocationPermissionDenied()
            is HomeScreenEvent.MapLoadError -> handleMapLoadError()
            is HomeScreenEvent.ScreenConfigurationChanged -> handleScreenConfigurationChanged(event.isNarrowScreen)
            is HomeScreenEvent.AnimationPreferencesChanged -> handleAnimationPreferencesChanged(event.animationsEnabled)
            is HomeScreenEvent.NavigationToMapStarted -> handleNavigationToMapStarted()
            is HomeScreenEvent.NavigationToMapCompleted -> handleNavigationToMapCompleted()
            is HomeScreenEvent.NavigationToFriendsStarted -> handleNavigationToFriendsStarted()
            is HomeScreenEvent.NavigationToFriendsCompleted -> handleNavigationToFriendsCompleted()
            is HomeScreenEvent.NavigationToSettingsStarted -> handleNavigationToSettingsStarted()
            is HomeScreenEvent.NavigationToSettingsCompleted -> handleNavigationToSettingsCompleted()
            is HomeScreenEvent.NavigationFailed -> handleNavigationFailed(event.error)
        }
    }
    
    /**
     * Initializes the home screen state with default values.
     */
    private fun initializeState() {
        viewModelScope.launch {
            try {
                // Set default location (San Francisco) for demo purposes
                // In a real app, this would come from location services
                val defaultLocation = LatLng(37.7749, -122.4194)
                
                _state.value = _state.value.copy(
                    isLoading = false,
                    mapPreviewLocation = defaultLocation,
                    animationsEnabled = true,
                    hasLocationPermission = false // Will be updated by permission check
                )
                
                Timber.d("HomeScreen state initialized with default location")
            } catch (e: Exception) {
                Timber.e(e, "Error initializing HomeScreen state")
                _state.value = _state.value.copy(
                    isLoading = false,
                    locationError = "Failed to initialize location services"
                )
            }
        }
    }
    
    /**
     * Handles the start sharing event with navigation state management.
     */
    private fun handleStartSharing() {
        viewModelScope.launch {
            try {
                Timber.d("Start sharing event triggered")
                
                if (!_state.value.hasLocationPermission) {
                    _state.value = _state.value.copy(
                        locationError = "Location permission required to start sharing"
                    )
                    return@launch
                }
                
                // Set navigation loading state
                _state.value = _state.value.copy(
                    isNavigatingToMap = true,
                    isNavigating = true,
                    isLocationSharing = true,
                    locationError = null
                )
                
                Timber.d("Location sharing started successfully, navigating to map")
            } catch (e: Exception) {
                Timber.e(e, "Error starting location sharing")
                _state.value = _state.value.copy(
                    locationError = "Failed to start location sharing",
                    isNavigatingToMap = false,
                    isNavigating = false
                )
            }
        }
    }
    
    /**
     * Handles navigation to friends screen with loading state.
     */
    private fun handleNavigateToFriends() {
        Timber.d("Navigate to friends event triggered")
        _state.value = _state.value.copy(
            isNavigatingToFriends = true,
            isNavigating = true
        )
    }
    
    /**
     * Handles navigation to settings screen with loading state.
     */
    private fun handleNavigateToSettings() {
        Timber.d("Navigate to settings event triggered")
        _state.value = _state.value.copy(
            isNavigatingToSettings = true,
            isNavigating = true
        )
    }
    
    /**
     * Handles showing the What's New dialog.
     */
    private fun handleShowWhatsNew() {
        Timber.d("Show What's New dialog event triggered")
        _state.value = _state.value.copy(showWhatsNewDialog = true)
    }
    
    /**
     * Handles dismissing the What's New dialog.
     */
    private fun handleDismissWhatsNew() {
        Timber.d("Dismiss What's New dialog event triggered")
        _state.value = _state.value.copy(showWhatsNewDialog = false)
    }
    
    /**
     * Handles location permission being granted.
     */
    private fun handleLocationPermissionGranted() {
        viewModelScope.launch {
            try {
                Timber.d("Location permission granted")
                
                _state.value = _state.value.copy(
                    hasLocationPermission = true,
                    locationError = null,
                    mapLoadError = false
                )
                
                // In a real app, this would trigger location updates
                // For now, we'll use the default location
                updateLocationPreview()
                
            } catch (e: Exception) {
                Timber.e(e, "Error handling location permission granted")
                _state.value = _state.value.copy(
                    locationError = "Failed to initialize location services"
                )
            }
        }
    }
    
    /**
     * Handles location permission being denied.
     */
    private fun handleLocationPermissionDenied() {
        Timber.w("Location permission denied")
        _state.value = _state.value.copy(
            hasLocationPermission = false,
            mapPreviewLocation = null,
            locationError = "Location permission is required for map preview and sharing"
        )
    }
    
    /**
     * Handles map loading errors.
     */
    private fun handleMapLoadError() {
        Timber.w("Map load error occurred")
        _state.value = _state.value.copy(
            mapLoadError = true,
            locationError = "Failed to load map preview"
        )
    }
    
    /**
     * Handles screen configuration changes for responsive design.
     * 
     * @param isNarrowScreen Whether the screen is narrow (< 360dp)
     */
    private fun handleScreenConfigurationChanged(isNarrowScreen: Boolean) {
        Timber.d("Screen configuration changed: isNarrowScreen = $isNarrowScreen")
        _state.value = _state.value.copy(isNarrowScreen = isNarrowScreen)
    }
    
    /**
     * Handles animation preferences changes for accessibility.
     * 
     * @param animationsEnabled Whether animations should be enabled
     */
    private fun handleAnimationPreferencesChanged(animationsEnabled: Boolean) {
        Timber.d("Animation preferences changed: animationsEnabled = $animationsEnabled")
        _state.value = _state.value.copy(animationsEnabled = animationsEnabled)
    }
    
    /**
     * Updates the map preview location.
     * In a real app, this would integrate with location services.
     */
    private fun updateLocationPreview() {
        viewModelScope.launch {
            try {
                // Simulate location update
                // In a real app, this would come from LocationManager or FusedLocationProviderClient
                val currentLocation = LatLng(37.7749, -122.4194) // San Francisco
                
                _state.value = _state.value.copy(
                    mapPreviewLocation = currentLocation,
                    mapLoadError = false
                )
                
                Timber.d("Map preview location updated: $currentLocation")
            } catch (e: Exception) {
                Timber.e(e, "Error updating location preview")
                _state.value = _state.value.copy(
                    mapLoadError = true,
                    locationError = "Failed to update location"
                )
            }
        }
    }
    
    /**
     * Stops location sharing.
     */
    fun stopLocationSharing() {
        viewModelScope.launch {
            try {
                Timber.d("Stopping location sharing")
                
                _state.value = _state.value.copy(
                    isLocationSharing = false,
                    locationError = null
                )
                
                Timber.d("Location sharing stopped successfully")
            } catch (e: Exception) {
                Timber.e(e, "Error stopping location sharing")
                _state.value = _state.value.copy(
                    locationError = "Failed to stop location sharing"
                )
            }
        }
    }
    
    /**
     * Refreshes the map preview.
     */
    fun refreshMapPreview() {
        if (_state.value.hasLocationPermission) {
            updateLocationPreview()
        }
    }
    
    /**
     * Handles navigation to map screen started.
     */
    private fun handleNavigationToMapStarted() {
        Timber.d("Navigation to map started")
        _state.value = _state.value.copy(
            isNavigatingToMap = true,
            isNavigating = true
        )
    }
    
    /**
     * Handles navigation to map screen completed.
     */
    private fun handleNavigationToMapCompleted() {
        Timber.d("Navigation to map completed")
        _state.value = _state.value.copy(
            isNavigatingToMap = false,
            isNavigating = false
        )
    }
    
    /**
     * Handles navigation to friends screen started.
     */
    private fun handleNavigationToFriendsStarted() {
        Timber.d("Navigation to friends started")
        _state.value = _state.value.copy(
            isNavigatingToFriends = true,
            isNavigating = true
        )
    }
    
    /**
     * Handles navigation to friends screen completed.
     */
    private fun handleNavigationToFriendsCompleted() {
        Timber.d("Navigation to friends completed")
        _state.value = _state.value.copy(
            isNavigatingToFriends = false,
            isNavigating = false
        )
    }
    
    /**
     * Handles navigation to settings screen started.
     */
    private fun handleNavigationToSettingsStarted() {
        Timber.d("Navigation to settings started")
        _state.value = _state.value.copy(
            isNavigatingToSettings = true,
            isNavigating = true
        )
    }
    
    /**
     * Handles navigation to settings screen completed.
     */
    private fun handleNavigationToSettingsCompleted() {
        Timber.d("Navigation to settings completed")
        _state.value = _state.value.copy(
            isNavigatingToSettings = false,
            isNavigating = false
        )
    }
    
    /**
     * Handles navigation failure.
     */
    private fun handleNavigationFailed(error: String) {
        Timber.e("Navigation failed: $error")
        _state.value = _state.value.copy(
            isNavigating = false,
            isNavigatingToMap = false,
            isNavigatingToFriends = false,
            isNavigatingToSettings = false,
            locationError = "Navigation failed: $error"
        )
    }
    
    /**
     * Clears any error messages.
     */
    fun clearError() {
        _state.value = _state.value.copy(locationError = null)
    }
    
    override fun onCleared() {
        super.onCleared()
        Timber.d("HomeScreenViewModel cleared")
    }
}