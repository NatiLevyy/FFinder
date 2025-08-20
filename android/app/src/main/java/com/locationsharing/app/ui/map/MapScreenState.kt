package com.locationsharing.app.ui.map

import com.google.android.gms.maps.model.LatLng
import com.locationsharing.app.data.friends.Friend

/**
 * State data class for MapScreen with all required fields
 * Implements requirements 2.2, 2.3, 7.1, 7.2, 7.3
 */
data class MapScreenState(
    // Location state
    val currentLocation: LatLng? = null,
    val isLocationLoading: Boolean = false,
    val locationError: String? = null,
    val hasLocationPermission: Boolean = false,
    val isLocationPermissionRequested: Boolean = false,
    
    // Friends state
    val friends: List<Friend> = emptyList(),
    val nearbyFriends: List<com.locationsharing.app.domain.model.NearbyFriend> = emptyList(),
    val nearbyFriendsCount: Int = 0,
    val selectedFriendId: String? = null,
    val isFriendsLoading: Boolean = false,
    val friendsError: String? = null,
    
    // Location sharing state
    val isLocationSharingActive: Boolean = false,
    val locationSharingError: String? = null,
    
    // UI state
    val isNearbyDrawerOpen: Boolean = false,
    val isStatusSheetVisible: Boolean = false,
    val isDebugMode: Boolean = false,
    val debugSnackbarMessage: String? = null,
    
    // Map state
    val mapZoom: Float = MapScreenConstants.Map.DEFAULT_ZOOM,
    val mapCenter: LatLng = LatLng(37.7749, -122.4194), // Default San Francisco
    val isMapLoading: Boolean = false,
    
    // Performance state
    val batteryLevel: Int = 100,
    val isHighAccuracyMode: Boolean = false,
    val locationUpdateInterval: Long = 5000L,
    
    // Error handling
    val generalError: String? = null,
    val isRetrying: Boolean = false,
    val retryCount: Int = 0,
    
    // Navigation state
    val shouldNavigateToSearchFriends: Boolean = false
) {
    /**
     * Check if the screen is in a loading state
     */
    val isLoading: Boolean
        get() = isLocationLoading || isFriendsLoading || isMapLoading
    
    /**
     * Check if there are any errors
     */
    val hasError: Boolean
        get() = locationError != null || friendsError != null || 
                locationSharingError != null || generalError != null
    
    /**
     * Get the primary error message to display
     */
    val primaryError: String?
        get() = locationError ?: friendsError ?: locationSharingError ?: generalError
    
    /**
     * Check if location services are ready
     */
    val isLocationReady: Boolean
        get() = hasLocationPermission && currentLocation != null && !isLocationLoading
    
    /**
     * Check if friends data is ready
     */
    val isFriendsReady: Boolean
        get() = !isFriendsLoading && friendsError == null
    
    /**
     * Get nearby friends within a certain radius (for display purposes)
     */
    fun getNearbyFriends(radiusKm: Double = 10.0): List<Friend> {
        val userLocation = currentLocation ?: return emptyList()
        
        return friends.filter { friend ->
            friend.getLatLng()?.let { friendLocation ->
                val distance = calculateDistance(userLocation, friendLocation)
                distance <= radiusKm * 1000 // Convert km to meters
            } ?: false
        }
    }
    
    /**
     * Calculate distance between two LatLng points in meters
     */
    private fun calculateDistance(point1: LatLng, point2: LatLng): Double {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(
            point1.latitude, point1.longitude,
            point2.latitude, point2.longitude,
            results
        )
        return results[0].toDouble()
    }
    
    /**
     * Get the selected friend object
     */
    fun getSelectedFriend(): Friend? {
        return selectedFriendId?.let { id ->
            friends.find { it.id == id }
        }
    }
    
    /**
     * Check if retry is available
     */
    val canRetry: Boolean
        get() = hasError && !isRetrying && retryCount < MapScreenConstants.States.MAX_RETRY_ATTEMPTS
    
    /**
     * Get status text for location sharing
     */
    val locationSharingStatusText: String
        get() = if (isLocationSharingActive) {
            "Location Sharing Active"
        } else {
            "Location Sharing Off"
        }
    
    /**
     * Get coordinates text for display
     */
    val coordinatesText: String
        get() = currentLocation?.let { location ->
            "Lat: ${"%.6f".format(location.latitude)}, Lng: ${"%.6f".format(location.longitude)}"
        } ?: "Location unavailable"
}