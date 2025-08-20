package com.locationsharing.app.domain.usecase

import android.content.Context
import android.content.Intent
import com.google.android.gms.maps.model.LatLng
import com.locationsharing.app.data.location.EnhancedLocationService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for sharing user's current location via system share intent.
 * Handles location retrieval and creates appropriate share content.
 */
@Singleton
class ShareLocationUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val locationService: EnhancedLocationService
) {
    
    /**
     * Share the user's current location via system share intent.
     * @return Result indicating success or failure
     */
    suspend fun shareCurrentLocation(): Result<Unit> {
        return try {
            Timber.d("📍 ShareLocationUseCase: Starting location share")
            
            // Get current location
            val locationUpdate = locationService.getLocationUpdates().first()
            val currentLocation = locationUpdate.newLocation
            
            // Create share intent
            val shareIntent = createLocationShareIntent(currentLocation)
            
            // Start share activity
            context.startActivity(Intent.createChooser(shareIntent, "Share Location"))
            
            Timber.d("📍 ShareLocationUseCase: Location share intent launched successfully")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Timber.e(e, "📍 ShareLocationUseCase: Failed to share location")
            Result.failure(e)
        }
    }
    
    /**
     * Create a share intent with location information.
     */
    private fun createLocationShareIntent(location: LatLng): Intent {
        val locationText = "I'm sharing my location with you!\n\n" +
                "📍 Latitude: ${location.latitude}\n" +
                "📍 Longitude: ${location.longitude}\n\n" +
                "View on Google Maps: https://maps.google.com/?q=${location.latitude},${location.longitude}\n\n" +
                "Shared via FFinder"
        
        return Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, locationText)
            putExtra(Intent.EXTRA_SUBJECT, "My Current Location")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }
    
    /**
     * Share location with custom message.
     */
    suspend fun shareLocationWithMessage(message: String): Result<Unit> {
        return try {
            val locationUpdate = locationService.getLocationUpdates().first()
            val currentLocation = locationUpdate.newLocation
            
            val customLocationText = "$message\n\n" +
                    "📍 Latitude: ${currentLocation.latitude}\n" +
                    "📍 Longitude: ${currentLocation.longitude}\n\n" +
                    "View on Google Maps: https://maps.google.com/?q=${currentLocation.latitude},${currentLocation.longitude}\n\n" +
                    "Shared via FFinder"
            
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, customLocationText)
                putExtra(Intent.EXTRA_SUBJECT, "My Location")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            
            context.startActivity(Intent.createChooser(shareIntent, "Share Location"))
            
            Timber.d("📍 ShareLocationUseCase: Custom location share launched")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Timber.e(e, "📍 ShareLocationUseCase: Failed to share location with custom message")
            Result.failure(e)
        }
    }
}