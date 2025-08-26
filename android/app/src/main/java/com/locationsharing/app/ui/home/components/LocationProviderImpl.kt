package com.locationsharing.app.ui.home.components

import com.google.android.gms.maps.model.LatLng
import com.locationsharing.app.data.location.EnhancedLocationService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of LocationProvider that uses EnhancedLocationService
 */
@Singleton
class LocationProviderImpl @Inject constructor(
    private val enhancedLocationService: EnhancedLocationService
) : LocationProvider {
    
    override suspend fun getLastLocation(): LatLng? {
        return try {
            enhancedLocationService.getCurrentLocation().first()
        } catch (e: Exception) {
            null
        }
    }
    
    override suspend fun requestSingleFix(timeoutMs: Long): LatLng? {
        return try {
            withTimeoutOrNull(timeoutMs) {
                enhancedLocationService.getCurrentLocation().first()
            }
        } catch (e: Exception) {
            null
        }
    }
}