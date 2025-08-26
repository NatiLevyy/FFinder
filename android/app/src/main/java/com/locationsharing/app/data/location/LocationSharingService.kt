package com.locationsharing.app.data.location

import android.content.Context
import com.google.android.gms.maps.model.LatLng
import com.locationsharing.app.data.friends.FriendsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service responsible for managing location sharing functionality
 * Implements requirements 3.3, 5.6, 5.7
 */
@Singleton
class LocationSharingService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val enhancedLocationService: EnhancedLocationService,
    private val friendsRepository: FriendsRepository,
    private val locationSharingRepository: LocationSharingRepository
) {
    
    private val _sharingState = MutableStateFlow(LocationSharingState())
    val sharingState: StateFlow<LocationSharingState> = _sharingState.asStateFlow()
    
    private val _notifications = MutableStateFlow<LocationSharingNotification?>(null)
    val notifications: StateFlow<LocationSharingNotification?> = _notifications.asStateFlow()
    
    companion object {
        private const val SHARING_TIMEOUT_MS = 30000L // 30 seconds
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val RETRY_DELAY_MS = 2000L // 2 seconds
    }
    
    /**
     * Start location sharing
     */
    suspend fun startLocationSharing(): Result<Unit> {
        return try {
            Timber.d("Starting location sharing")
            
            // Update state to starting
            _sharingState.value = _sharingState.value.copy(
                status = LocationSharingStatus.STARTING,
                error = null,
                retryCount = 0
            )
            
            // Enable high accuracy mode for sharing
            enhancedLocationService.enableHighAccuracyMode(true)
            
            // Get current location for sharing
            val location = enhancedLocationService.getCurrentLocation().first()
            val lat = location?.latitude
            val lng = location?.longitude
            
            // Start sharing with backend
            val result = locationSharingRepository.toggleSharing(true, lat, lng)
            
            if (result.isSuccess) {
                _sharingState.value = _sharingState.value.copy(
                    status = LocationSharingStatus.ACTIVE,
                    startTime = System.currentTimeMillis(),
                    error = null
                )
                
                // Send success notification
                _notifications.value = LocationSharingNotification(
                    type = NotificationType.SUCCESS,
                    message = "Location sharing started",
                    timestamp = System.currentTimeMillis()
                )
                
                Timber.d("Location sharing started successfully")
            } else {
                _sharingState.value = _sharingState.value.copy(
                    status = LocationSharingStatus.ERROR,
                    error = result.exceptionOrNull()?.message ?: "Failed to start sharing"
                )
                
                // Send error notification
                _notifications.value = LocationSharingNotification(
                    type = NotificationType.ERROR,
                    message = "Failed to start location sharing",
                    timestamp = System.currentTimeMillis()
                )
                
                Timber.e("Failed to start location sharing: ${result.exceptionOrNull()}")
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Exception starting location sharing")
            handleSharingError(e, "Failed to start location sharing")
            Result.failure(e)
        }
    }
    
    /**
     * Stop location sharing
     */
    suspend fun stopLocationSharing(): Result<Unit> {
        return try {
            Timber.d("Stopping location sharing")
            
            // Update state to stopping
            _sharingState.value = _sharingState.value.copy(
                status = LocationSharingStatus.STOPPING,
                error = null
            )
            
            // Stop sharing with backend
            val result = locationSharingRepository.toggleSharing(false, null, null)
            
            if (result.isSuccess) {
                _sharingState.value = _sharingState.value.copy(
                    status = LocationSharingStatus.INACTIVE,
                    startTime = null,
                    error = null,
                    retryCount = 0
                )
                
                // Disable high accuracy mode
                enhancedLocationService.enableHighAccuracyMode(false)
                
                // Send success notification
                _notifications.value = LocationSharingNotification(
                    type = NotificationType.SUCCESS,
                    message = "Location sharing stopped",
                    timestamp = System.currentTimeMillis()
                )
                
                Timber.d("Location sharing stopped successfully")
                Result.success(Unit)
            } else {
                val error = result.exceptionOrNull() ?: Exception("Unknown error stopping location sharing")
                handleSharingError(error, "Failed to stop location sharing")
                Result.failure(error)
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception stopping location sharing")
            handleSharingError(e, "Failed to stop location sharing")
            Result.failure(e)
        }
    }
    
    /**
     * Toggle location sharing on/off
     */
    suspend fun toggleLocationSharing(): Result<Unit> {
        return when (_sharingState.value.status) {
            LocationSharingStatus.INACTIVE -> startLocationSharing()
            LocationSharingStatus.ACTIVE -> stopLocationSharing()
            LocationSharingStatus.STARTING, LocationSharingStatus.STOPPING -> {
                // Already in transition, ignore toggle
                Timber.w("Location sharing already in transition, ignoring toggle")
                Result.success(Unit)
            }
            LocationSharingStatus.ERROR -> {
                // Retry starting if in error state
                startLocationSharing()
            }
        }
    }
    
    /**
     * Retry failed location sharing operation
     */
    suspend fun retryLocationSharing(): Result<Unit> {
        val currentState = _sharingState.value
        
        if (currentState.retryCount >= MAX_RETRY_ATTEMPTS) {
            val error = Exception("Maximum retry attempts exceeded")
            handleSharingError(error, "Failed to start location sharing after ${MAX_RETRY_ATTEMPTS} attempts")
            return Result.failure(error)
        }
        
        Timber.d("Retrying location sharing (attempt ${currentState.retryCount + 1})")
        
        _sharingState.value = currentState.copy(
            retryCount = currentState.retryCount + 1,
            error = null
        )
        
        // Add delay before retry
        kotlinx.coroutines.delay(RETRY_DELAY_MS)
        
        return startLocationSharing()
    }
    
    /**
     * Get current location sharing status text for UI
     */
    fun getStatusText(): String {
        return when (_sharingState.value.status) {
            LocationSharingStatus.INACTIVE -> "Location Sharing Off"
            LocationSharingStatus.STARTING -> "Starting Location Sharing..."
            LocationSharingStatus.ACTIVE -> "Location Sharing Active"
            LocationSharingStatus.STOPPING -> "Stopping Location Sharing..."
            LocationSharingStatus.ERROR -> "Location Sharing Error"
        }
    }
    
    /**
     * Get sharing duration if active
     */
    fun getSharingDuration(): Long? {
        val startTime = _sharingState.value.startTime
        return if (startTime != null && _sharingState.value.status == LocationSharingStatus.ACTIVE) {
            System.currentTimeMillis() - startTime
        } else null
    }
    
    /**
     * Check if location sharing is active
     */
    fun isLocationSharingActive(): Boolean {
        return _sharingState.value.status == LocationSharingStatus.ACTIVE
    }
    
    /**
     * Check if location sharing can be toggled
     */
    fun canToggleSharing(): Boolean {
        return _sharingState.value.status in listOf(
            LocationSharingStatus.INACTIVE,
            LocationSharingStatus.ACTIVE,
            LocationSharingStatus.ERROR
        )
    }
    
    /**
     * Get combined location and sharing status for UI
     */
    fun getCombinedStatus(): Flow<CombinedLocationStatus> {
        return combine(
            sharingState,
            enhancedLocationService.getLocationUpdates().map { it.newLocation }.distinctUntilChanged()
        ) { sharingState, currentLocation ->
            CombinedLocationStatus(
                currentLocation = currentLocation,
                sharingStatus = sharingState.status,
                statusText = getStatusText(),
                sharingDuration = getSharingDuration(),
                error = sharingState.error,
                canRetry = sharingState.retryCount < MAX_RETRY_ATTEMPTS && sharingState.status == LocationSharingStatus.ERROR
            )
        }.catch { exception ->
            Timber.e(exception, "Error in combined status flow")
            emit(
                CombinedLocationStatus(
                    currentLocation = null,
                    sharingStatus = LocationSharingStatus.ERROR,
                    statusText = "Error getting location status",
                    sharingDuration = null,
                    error = exception.message,
                    canRetry = true
                )
            )
        }
    }
    
    /**
     * Clear current notification
     */
    fun clearNotification() {
        _notifications.value = null
    }
    
    /**
     * Handle sharing errors with proper state updates
     */
    private fun handleSharingError(exception: Throwable, message: String) {
        Timber.e(exception, message)
        
        val errorMessage = when (exception) {
            is PermissionDeniedException -> "Permission denied: Please check Firestore security rules"
            else -> exception.message ?: message
        }
        
        _sharingState.value = _sharingState.value.copy(
            status = LocationSharingStatus.ERROR,
            error = errorMessage
        )
        
        // Send error notification with specific type for permission errors
        val notificationType = if (exception is PermissionDeniedException) {
            NotificationType.WARNING
        } else {
            NotificationType.ERROR
        }
        
        _notifications.value = LocationSharingNotification(
            type = notificationType,
            message = errorMessage,
            timestamp = System.currentTimeMillis()
        )
        
        // Disable high accuracy mode on error
        enhancedLocationService.enableHighAccuracyMode(false)
    }
    
    /**
     * Reset sharing state (for testing or cleanup)
     */
    fun resetSharingState() {
        _sharingState.value = LocationSharingState()
        _notifications.value = null
        enhancedLocationService.enableHighAccuracyMode(false)
        Timber.d("Location sharing state reset")
    }
}

/**
 * Location sharing state data class
 */
data class LocationSharingState(
    val status: LocationSharingStatus = LocationSharingStatus.INACTIVE,
    val startTime: Long? = null,
    val error: String? = null,
    val retryCount: Int = 0
)

/**
 * Location sharing status enum
 */
enum class LocationSharingStatus {
    INACTIVE,
    STARTING,
    ACTIVE,
    STOPPING,
    ERROR
}

/**
 * Location sharing notification data class
 */
data class LocationSharingNotification(
    val type: NotificationType,
    val message: String,
    val timestamp: Long
)

/**
 * Notification types for user feedback
 */
enum class NotificationType {
    SUCCESS,
    ERROR,
    WARNING,
    INFO
}

/**
 * Combined location and sharing status for UI
 */
data class CombinedLocationStatus(
    val currentLocation: LatLng?,
    val sharingStatus: LocationSharingStatus,
    val statusText: String,
    val sharingDuration: Long?,
    val error: String?,
    val canRetry: Boolean
)