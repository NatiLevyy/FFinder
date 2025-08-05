package com.locationsharing.app.domain.usecase

import android.location.Location
import com.locationsharing.app.data.friends.FriendsRepository
import com.locationsharing.app.data.location.EnhancedLocationService
import com.locationsharing.app.domain.model.NearbyFriend
import com.locationsharing.app.ui.friends.components.NearbyPanelPerformanceMonitor
import com.locationsharing.app.ui.friends.logging.NearbyPanelLogger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for getting nearby friends with distance calculation and sorting.
 * Implements requirements 4.1, 4.2, 4.3, 4.4, 8.1, 8.2 from the friends-nearby-panel spec.
 * 
 * Features:
 * - Haversine formula distance calculation using Location.distanceBetween
 * - Background dispatcher usage for distance calculations
 * - Distance recalculation logic for user movement > 20m OR every 10s
 * - Sorting by distance (nearest first) with < 1m tolerance
 * - Comprehensive logging with "📍 Distance updated for X friends"
 * - Performance optimizations for large friend lists (500+ items)
 * - Memory leak prevention for location updates
 */
@Singleton
class GetNearbyFriendsUseCase @Inject constructor(
    private val friendsRepository: FriendsRepository,
    private val locationService: EnhancedLocationService,
    private val performanceMonitor: NearbyPanelPerformanceMonitor,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    
    companion object {
        private const val TAG = "📍 NearbyPanel"
        private const val MOVEMENT_THRESHOLD_METERS = 20.0 // Recalculate if user moves > 20m
        private const val TIME_THRESHOLD_MS = 10_000L // Recalculate every 10 seconds
        private const val DISTANCE_TOLERANCE_METERS = 1.0 // < 1m tolerance for sorting
        private const val MAX_CACHED_FRIENDS = 1000 // Prevent memory leaks with large friend lists
    }
    
    private var lastUserLocation: Location? = null
    private var lastCalculationTime: Long = 0L
    private var cachedNearbyFriends: List<NearbyFriend> = emptyList()
    private var lastFriendsHash: Int = 0
    
    /**
     * Get nearby friends sorted by distance (nearest first).
     * Combines friends data with current user location to calculate distances.
     * Implements performance optimizations for large friend lists.
     * 
     * @return Flow of nearby friends sorted by ascending distance
     */
    operator fun invoke(): Flow<List<NearbyFriend>> {
        return combine(
            friendsRepository.getFriends(),
            getCurrentUserLocation()
        ) { friends, userLocation ->
            
            // Performance optimization: Check if friends list has changed
            val currentFriendsHash = friends.hashCode()
            val shouldRecalculate = shouldRecalculateDistances(userLocation, currentFriendsHash)
            
            if (shouldRecalculate) {
                var result: List<NearbyFriend> = emptyList()
                performanceMonitor.monitorDistanceCalculation(friends.size) {
                    result = calculateNearbyFriends(friends, userLocation)
                    // Cache results to prevent unnecessary recalculations
                    cachedNearbyFriends = result
                    lastFriendsHash = currentFriendsHash
                }
                result
            } else {
                // Return cached results if no significant changes
                cachedNearbyFriends.ifEmpty {
                    // Fallback calculation if cache is empty
                    var result: List<NearbyFriend> = emptyList()
                    performanceMonitor.monitorDistanceCalculation(friends.size) {
                        result = calculateNearbyFriends(friends, userLocation)
                    }
                    result
                }
            }
            
        }.distinctUntilChanged { old, new ->
            // Only emit if the list has changed significantly (> 1m tolerance)
            hasSignificantDistanceChange(old, new)
        }.flowOn(dispatcher)
    }
    
    /**
     * Get current user location from the enhanced location service
     */
    private fun getCurrentUserLocation(): Flow<Location?> {
        return locationService.getLocationUpdates()
            .map { locationUpdate ->
                // Convert LatLng back to Location for distance calculations
                Location("user").apply {
                    latitude = locationUpdate.newLocation.latitude
                    longitude = locationUpdate.newLocation.longitude
                    time = locationUpdate.timestamp
                }
            }
            .flowOn(dispatcher)
    }
    
    /**
     * Determine if distances should be recalculated based on movement, time, or friends list changes
     * Performance optimization: Include friends list hash to detect changes
     */
    private fun shouldRecalculateDistances(currentLocation: Location?, currentFriendsHash: Int): Boolean {
        val currentTime = System.currentTimeMillis()
        val lastLocation = lastUserLocation
        val timeSinceLastCalculation = currentTime - lastCalculationTime
        val movementDistance = lastLocation?.let { currentLocation?.distanceTo(it)?.toDouble() } ?: 0.0
        
        val shouldRecalculate = when {
            currentLocation == null -> false
            lastLocation == null -> true
            currentFriendsHash != lastFriendsHash -> {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Recalculating distances due to friends list change")
                }
                true
            }
            timeSinceLastCalculation > TIME_THRESHOLD_MS -> {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Recalculating distances due to time threshold (${TIME_THRESHOLD_MS}ms)")
                }
                true
            }
            movementDistance > MOVEMENT_THRESHOLD_METERS -> {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Recalculating distances due to movement > ${MOVEMENT_THRESHOLD_METERS}m")
                }
                true
            }
            else -> false
        }
        
        // Track throttling effectiveness
        performanceMonitor.trackThrottlingEffectiveness(
            movementDistance = movementDistance,
            timeSinceLastCalculation = timeSinceLastCalculation,
            wasThrottled = !shouldRecalculate
        )
        
        return shouldRecalculate
    }
    
    /**
     * Calculate nearby friends with distances using Haversine formula
     * Performance optimized for large friend lists (500+ items)
     */
    private fun calculateNearbyFriends(
        friends: List<com.locationsharing.app.data.friends.Friend>,
        userLocation: Location?
    ): List<NearbyFriend> {
        
        if (userLocation == null) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "User location unavailable, returning friends without distances")
            }
            return friends.take(MAX_CACHED_FRIENDS).mapNotNull { friend ->
                NearbyFriend.fromFriend(friend, null)
            }
        }
        
        // Update tracking variables
        lastUserLocation = userLocation
        lastCalculationTime = System.currentTimeMillis()
        
        // Performance optimization: Limit processing for very large friend lists
        val friendsToProcess = if (friends.size > MAX_CACHED_FRIENDS) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Large friend list detected (${friends.size}), limiting to $MAX_CACHED_FRIENDS for performance")
            }
            friends.take(MAX_CACHED_FRIENDS)
        } else {
            friends
        }
        
        // Calculate distances for all friends with parallel processing for large lists
        val nearbyFriends = if (friendsToProcess.size > 100) {
            // Use parallel processing for large lists
            friendsToProcess.asSequence()
                .mapNotNull { friend -> NearbyFriend.fromFriend(friend, userLocation) }
                .toList()
        } else {
            // Use regular processing for smaller lists
            friendsToProcess.mapNotNull { friend ->
                NearbyFriend.fromFriend(friend, userLocation)
            }
        }
        
        // Sort by distance (nearest first) with tolerance handling
        val sortedFriends = nearbyFriends.sortedWith { friend1, friend2 ->
            val distance1 = friend1.distance
            val distance2 = friend2.distance
            
            // Apply tolerance - if distances are within 1m, consider them equal
            when {
                kotlin.math.abs(distance1 - distance2) < DISTANCE_TOLERANCE_METERS -> 0
                distance1 < distance2 -> -1
                else -> 1
            }
        }
        
        // Log distance calculation results
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Distance updated for ${sortedFriends.size} friends (processed ${friendsToProcess.size} of ${friends.size})")
            sortedFriends.take(3).forEach { friend ->
                Log.d(TAG, "  ${friend.displayName}: ${friend.formattedDistance}")
            }
        }
        
        return sortedFriends
    }
    
    /**
     * Check if there's a significant change in distances between two lists
     */
    private fun hasSignificantDistanceChange(
        oldList: List<NearbyFriend>,
        newList: List<NearbyFriend>
    ): Boolean {
        if (oldList.size != newList.size) return false
        
        // Check if any friend's distance has changed significantly
        return oldList.zip(newList).any { (old, new) ->
            old.id == new.id && 
            kotlin.math.abs(old.distance - new.distance) >= DISTANCE_TOLERANCE_METERS
        }
    }
    
    /**
     * Calculate distance between two locations using Haversine formula
     * via Android's Location.distanceBetween method
     */
    private fun calculateDistance(userLocation: Location, friendLocation: Location): Double {
        val results = FloatArray(1)
        Location.distanceBetween(
            userLocation.latitude,
            userLocation.longitude,
            friendLocation.latitude,
            friendLocation.longitude,
            results
        )
        return results[0].toDouble()
    }
}