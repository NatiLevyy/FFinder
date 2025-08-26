package com.locationsharing.app.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.BatteryManager
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.locationsharing.app.data.friends.FriendLocationUpdate
import com.locationsharing.app.data.friends.LocationUpdateType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enhanced location service with battery optimization and performance monitoring
 * Implements intelligent update intervals based on battery level and movement patterns
 */
@Singleton
open class EnhancedLocationService @Inject constructor(
    @ApplicationContext 
     protected val context: Context
) {
    
    private val fusedLocationClient: FusedLocationProviderClient = 
        LocationServices.getFusedLocationProviderClient(context)
    
    private val batteryManager: BatteryManager = 
        context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    
    private var currentLocationCallback: LocationCallback? = null
    private var lastKnownLocation: Location? = null
    private var lastMovementTime: Long = 0L
    private var isHighAccuracyMode: Boolean = false
    private var currentUpdateInterval: Long = DEFAULT_UPDATE_INTERVAL
    private var lastIntervalAdjustment: Long = 0L
    
    companion object {
        private const val DEFAULT_UPDATE_INTERVAL = 2000L // 2 seconds
        private const val BATTERY_SAVER_INTERVAL = 10000L // 10 seconds
        private const val STATIONARY_INTERVAL = 5000L // 5 seconds
        private const val HIGH_SPEED_INTERVAL = 1000L // 1 second
        private const val MOVEMENT_THRESHOLD = 5.0f // meters
        private const val HIGH_SPEED_THRESHOLD = 10.0f // m/s (~36 km/h)
        private const val LOW_BATTERY_THRESHOLD = 20 // 20%
        private const val CRITICAL_BATTERY_THRESHOLD = 10 // 10%
    }
    
    /**
     * Get current location once (for immediate location requests)
     */
    fun getCurrentLocation(): Flow<LatLng?> = callbackFlow {
        if (!hasLocationPermission()) {
            Timber.w("Location permission not granted for current location")
            trySend(null)
            close()
            return@callbackFlow
        }
        
        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        trySend(LatLng(location.latitude, location.longitude))
                        Timber.d("Got current location: ${location.latitude}, ${location.longitude}")
                    } else {
                        // Request fresh location if last location is null
                        requestFreshLocation()
                    }
                    close()
                }
                .addOnFailureListener { exception ->
                    Timber.e(exception, "Failed to get current location")
                    trySend(null)
                    close(exception)
                }
        } catch (e: SecurityException) {
            Timber.e(e, "Security exception getting current location")
            trySend(null)
            close(e)
        }
        
        awaitClose { }
    }
    
    /**
     * Request a fresh location update (when last location is null or stale)
     */
    private fun requestFreshLocation() {
        if (!hasLocationPermission()) return
        
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setMaxUpdates(1)
            .build()
            
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    Timber.d("Got fresh location: ${location.latitude}, ${location.longitude}")
                }
            }
        }
        
        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, callback, Looper.getMainLooper())
        } catch (e: SecurityException) {
            Timber.e(e, "Security exception requesting fresh location")
        }
    }

    /**
     * Get optimized location updates with intelligent interval adjustment
     */
    open fun getLocationUpdates(): Flow<FriendLocationUpdate> = callbackFlow {
        if (!hasLocationPermission()) {
            Timber.w("Location permission not granted")
            close()
            return@callbackFlow
        }
        
        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    val update = processLocationUpdate(location)
                    trySend(update)
                    
                    // Adjust update interval based on movement and battery
                    adjustUpdateInterval(location)
                }
            }
        }
        
        currentLocationCallback = locationCallback
        
        val locationRequest = createOptimizedLocationRequest()
        
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            Timber.d("Enhanced location updates started")
        } catch (e: SecurityException) {
            Timber.e(e, "Security exception requesting location updates")
            close(e)
        }
        
        awaitClose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            currentLocationCallback = null
            Timber.d("Enhanced location updates stopped")
        }
    }.distinctUntilChanged { old, new ->
        // Only emit if location has changed significantly
        val oldLatLng = old.newLocation
        val newLatLng = new.newLocation
        // Convert LatLng to Location for distance calculation
        val oldLocation = Location("").apply {
            latitude = oldLatLng.latitude
            longitude = oldLatLng.longitude
        }
        val newLocation = Location("").apply {
            latitude = newLatLng.latitude
            longitude = newLatLng.longitude
        }
        oldLocation.distanceTo(newLocation) < MOVEMENT_THRESHOLD
    }
    
    /**
     * Process location update and determine update type
     */
    private fun processLocationUpdate(location: Location): FriendLocationUpdate {
        val updateType = determineUpdateType(location)
        val isMoving = isLocationMoving(location)
        
        lastKnownLocation = location
        if (isMoving) {
            lastMovementTime = System.currentTimeMillis()
        }
        
        return FriendLocationUpdate(
            friendId = "current_user", // This would be the current user's ID
            previousLocation = lastKnownLocation?.let { LatLng(it.latitude, it.longitude) },
            newLocation = LatLng(location.latitude, location.longitude),
            timestamp = System.currentTimeMillis(),
            isOnline = true, // Current user is always online
            updateType = updateType
        )
    }
    
    /**
     * Determine the type of location update
     */
    private fun determineUpdateType(location: Location): LocationUpdateType {
        val lastLocation = lastKnownLocation
        
        return when {
            lastLocation == null -> LocationUpdateType.INITIAL_LOAD
            isLocationMoving(location) -> LocationUpdateType.POSITION_CHANGE
            else -> LocationUpdateType.STATUS_CHANGE
        }
    }
    
    /**
     * Check if location indicates movement
     */
    private fun isLocationMoving(location: Location): Boolean {
        val lastLocation = lastKnownLocation ?: return false
        val distance = location.distanceTo(lastLocation)
        val timeDiff = location.time - lastLocation.time
        
        // Consider moving if distance > threshold or has significant speed
        return distance > MOVEMENT_THRESHOLD || 
               (location.hasSpeed() && location.speed > 1.0f) ||
               (timeDiff > 0 && distance / (timeDiff / 1000f) > 0.5f)
    }
    
    /**
     * Check if location indicates stationary state
     */
    private fun isLocationStationary(location: Location): Boolean {
        val lastLocation = lastKnownLocation ?: return false
        val distance = location.distanceTo(lastLocation)
        val timeSinceMovement = System.currentTimeMillis() - lastMovementTime
        
        return distance < MOVEMENT_THRESHOLD && 
               timeSinceMovement > 30000L && // 30 seconds without movement
               (!location.hasSpeed() || location.speed < 0.5f)
    }
    
    /**
     * Create optimized location request based on current conditions
     */
    private fun createOptimizedLocationRequest(): LocationRequest {
        val batteryLevel = getBatteryLevel()
        val interval = currentUpdateInterval
        
        val priority = when {
            batteryLevel < CRITICAL_BATTERY_THRESHOLD -> Priority.PRIORITY_LOW_POWER
            batteryLevel < LOW_BATTERY_THRESHOLD -> Priority.PRIORITY_BALANCED_POWER_ACCURACY
            isHighAccuracyMode -> Priority.PRIORITY_HIGH_ACCURACY
            else -> Priority.PRIORITY_BALANCED_POWER_ACCURACY
        }
        
        return LocationRequest.Builder(priority, interval)
            .setMinUpdateIntervalMillis(interval / 2)
            .setMaxUpdateDelayMillis(interval * 2)
            .setWaitForAccurateLocation(false)
            .build()
    }
    
    /**
     * Get optimal update interval based on battery and movement
     */
    private fun getOptimalUpdateInterval(batteryLevel: Int): Long {
        val lastLocation = lastKnownLocation
        
        return when {
            batteryLevel < CRITICAL_BATTERY_THRESHOLD -> BATTERY_SAVER_INTERVAL * 2
            batteryLevel < LOW_BATTERY_THRESHOLD -> BATTERY_SAVER_INTERVAL
            lastLocation?.hasSpeed() == true && lastLocation.speed > HIGH_SPEED_THRESHOLD -> HIGH_SPEED_INTERVAL
            isLocationStationary(lastLocation ?: return DEFAULT_UPDATE_INTERVAL) -> STATIONARY_INTERVAL
            else -> DEFAULT_UPDATE_INTERVAL
        }
    }
    
    /**
     * Dynamically adjust update interval based on current conditions
     */
    private fun adjustUpdateInterval(location: Location) {
        val currentTime = System.currentTimeMillis()
        
        // Prevent rapid re-adjustments (minimum 30 seconds between adjustments)
        if (currentTime - lastIntervalAdjustment < 30000L) {
            return
        }
        
        val batteryLevel = getBatteryLevel()
        val newInterval = getOptimalUpdateInterval(batteryLevel)
        
        // Only adjust if the interval has changed significantly (more than 2 seconds difference)
        if (kotlin.math.abs(newInterval - currentUpdateInterval) > 2000L) {
            currentUpdateInterval = newInterval
            lastIntervalAdjustment = currentTime
            
            // Restart location updates with new interval
            val currentCallback = currentLocationCallback
            if (currentCallback != null) {
                val newRequest = createOptimizedLocationRequest()
                
                try {
                    fusedLocationClient.removeLocationUpdates(currentCallback)
                    fusedLocationClient.requestLocationUpdates(newRequest, currentCallback, Looper.getMainLooper())
                    Timber.d("Location update interval adjusted to ${newInterval}ms (battery: $batteryLevel%)")
                } catch (e: SecurityException) {
                    Timber.e(e, "Error adjusting location update interval")
                }
            }
        }
    }
    
    /**
     * Get current battery level percentage
     */
    private fun getBatteryLevel(): Int {
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }
    
    /**
     * Check if device is charging
     */
    private fun isCharging(): Boolean {
        val status = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)
        return status == BatteryManager.BATTERY_STATUS_CHARGING ||
               status == BatteryManager.BATTERY_STATUS_FULL
    }
    
    /**
     * Enable high accuracy mode for critical situations
     */
    open fun enableHighAccuracyMode(enable: Boolean) {
        if (isHighAccuracyMode != enable) {
            isHighAccuracyMode = enable
            Timber.d("High accuracy mode ${if (enable) "enabled" else "disabled"}")
            
            // Restart location updates with new settings
            currentLocationCallback?.let { callback ->
                adjustUpdateInterval(lastKnownLocation ?: return)
            }
        }
    }
    
    /**
     * Get location performance metrics
     */
    open fun getPerformanceMetrics(): LocationPerformanceMetrics {
        val batteryLevel = getBatteryLevel()
        val isCharging = isCharging()
        val updateInterval = getOptimalUpdateInterval(batteryLevel)
        
        return LocationPerformanceMetrics(
            batteryLevel = batteryLevel,
            isCharging = isCharging,
            currentUpdateInterval = updateInterval,
            isHighAccuracyMode = isHighAccuracyMode,
            lastLocationAccuracy = lastKnownLocation?.accuracy ?: 0f,
            isMoving = lastKnownLocation?.let { isLocationMoving(it) } ?: false
        )
    }
    
    /**
     * Check if location permission is granted
     */
    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Stop location updates
     */
    open fun stopLocationUpdates() {
        currentLocationCallback?.let { callback ->
            fusedLocationClient.removeLocationUpdates(callback)
            currentLocationCallback = null
            Timber.d("Location updates stopped")
        }
    }
}

/**
 * Location performance metrics for monitoring and optimization
 */
data class LocationPerformanceMetrics(
    val batteryLevel: Int,
    val isCharging: Boolean,
    val currentUpdateInterval: Long,
    val isHighAccuracyMode: Boolean,
    val lastLocationAccuracy: Float,
    val isMoving: Boolean
)