package com.locationsharing.app.data.location

import android.content.Context
import android.location.Location
import com.google.android.gms.maps.model.LatLng
import com.locationsharing.app.data.friends.FriendLocationUpdate
import com.locationsharing.app.data.friends.LocationUpdateType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Simple location service implementation for testing/demo purposes
 * Returns mock location data when dependency injection is unavailable
 */
class SimpleLocationService(context: Context) : EnhancedLocationService(context) {
    
    /**
     * Get mock location updates
     */
    override fun getLocationUpdates(): Flow<FriendLocationUpdate> {
        // Return a mock location update (San Francisco coordinates)
        val mockLocation = LatLng(37.7749, -122.4194)
        val mockUpdate = FriendLocationUpdate(
            friendId = "current_user",
            previousLocation = null,
            newLocation = mockLocation,
            timestamp = System.currentTimeMillis(),
            isOnline = true,
            updateType = LocationUpdateType.INITIAL_LOAD
        )
        return flowOf(mockUpdate)
    }
    
    /**
     * Get performance metrics (mock implementation)
     */
    override fun getPerformanceMetrics(): LocationPerformanceMetrics {
        return LocationPerformanceMetrics(
            batteryLevel = 80,
            isCharging = false,
            currentUpdateInterval = 2000L,
            isHighAccuracyMode = false,
            lastLocationAccuracy = 10.0f,
            isMoving = false
        )
    }
    
    /**
     * Enable high accuracy mode (no-op for simple implementation)
     */
    override fun enableHighAccuracyMode(enable: Boolean) {
        // No-op for simple implementation
    }
    
    /**
     * Stop location updates (no-op for simple implementation)
     */
    override fun stopLocationUpdates() {
        // No-op for simple implementation
    }
}