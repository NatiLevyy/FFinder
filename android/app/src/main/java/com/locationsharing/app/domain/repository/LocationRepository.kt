package com.locationsharing.app.domain.repository

import com.locationsharing.app.data.models.Location
import kotlinx.coroutines.flow.Flow

interface LocationRepository {
    suspend fun updateLocation(location: Location): Result<Unit>
    suspend fun getCurrentLocation(): Location?
    fun getLocationUpdates(): Flow<Location>
    suspend fun getFriendLocations(friendIds: List<String>): Result<Map<String, Location>>
    suspend fun startLocationSharing(): Result<Unit>
    suspend fun stopLocationSharing(): Result<Unit>
}