package com.locationsharing.app.domain.service

import com.locationsharing.app.data.models.Location
import kotlinx.coroutines.flow.Flow

interface LocationService {
    fun startLocationUpdates(interval: Long = 5000)
    fun stopLocationUpdates()
    fun getCurrentLocation(): Location?
    fun getLocationUpdates(): Flow<Location>
    suspend fun requestPermissions(): PermissionStatus
    fun hasLocationPermission(): Boolean
}

enum class PermissionStatus {
    GRANTED, DENIED, SHOULD_SHOW_RATIONALE, NOT_REQUESTED
}