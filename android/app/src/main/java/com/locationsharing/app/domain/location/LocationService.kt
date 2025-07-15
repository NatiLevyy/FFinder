package com.locationsharing.app.domain.location

import com.locationsharing.app.data.models.LocationData
import com.locationsharing.app.data.models.LocationPermissionStatus
import kotlinx.coroutines.flow.Flow

interface LocationService {
    fun startLocationUpdates()
    fun stopLocationUpdates()
    fun getLocationUpdates(): Flow<LocationData>
    suspend fun getCurrentLocation(): LocationData
    fun checkLocationPermission(): LocationPermissionStatus
    fun isLocationEnabled(): Boolean
}