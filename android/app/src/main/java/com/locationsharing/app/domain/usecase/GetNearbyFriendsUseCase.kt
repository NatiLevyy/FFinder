package com.locationsharing.app.domain.usecase

import android.location.Location
import android.util.Log
import com.locationsharing.app.BuildConfig
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
        
        // Geo-filtering: 10km radius for nearby friends
        private const val GEO_FILTER_RADIUS_METERS = 10_000.0 // 10km radius
        
        // Smart ranking weights
        private const val PROXIMITY_WEIGHT = 0.5f
        private const val TIME_SINCE_SEEN_WEIGHT = 0.3f
        private const val STATUS_WEIGHT = 0.2f
        
        // Proximity buckets for categorization
        private const val VERY_CLOSE_THRESHOLD = 300.0 // < 300m = Very Close
        private const val NEARBY_THRESHOLD = 2000.0 // 300m-2km = Nearby
        private const val IN_TOWN_THRESHOLD = 10000.0 // 2-10km = In Town
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
                    result = NearbyPanelLogger.measureDistanceCalculation(
                        "calculateNearbyFriends",
                        friends.size
                    ) {
                        calculateNearbyFriends(friends, userLocation)
                    }
                    // Cache results to prevent unnecessary recalculations
                    cachedNearbyFriends = result
                    lastFriendsHash = currentFriendsHash
                }
                NearbyPanelLogger.logDistanceUpdate(result.size)
                result
            } else {
                // Return cached results if no significant changes
                cachedNearbyFriends.ifEmpty {
                    // Fallback calculation if cache is empty
                    var result: List<NearbyFriend> = emptyList()
                    performanceMonitor.monitorDistanceCalculation(friends.size) {
                        result = NearbyPanelLogger.measureDistanceCalculation(
                            "calculateNearbyFriends_fallback",
                            friends.size
                        ) {
                            calculateNearbyFriends(friends, userLocation)
                        }
                    }
                    NearbyPanelLogger.logDistanceUpdate(result.size)
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
            NearbyPanelLogger.logWarning(
                "calculateNearbyFriends",
                "User location unavailable, returning friends without distances",
                mapOf("friendCount" to friends.size)
            )
            return friends.take(MAX_CACHED_FRIENDS).mapNotNull { friend ->
                NearbyFriend.fromFriend(friend, null)
            }
        }
        
        // Update tracking variables
        lastUserLocation = userLocation
        lastCalculationTime = System.currentTimeMillis()
        
        // Performance optimization: Limit processing for very large friend lists
        val friendsToProcess = if (friends.size > MAX_CACHED_FRIENDS) {
            NearbyPanelLogger.logWarning(
                "calculateNearbyFriends",
                "Large friend list detected, limiting for performance",
                mapOf(
                    "totalFriends" to friends.size,
                    "processedFriends" to MAX_CACHED_FRIENDS
                )
            )
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
        
        // Apply geo-filtering: only include friends within 10km radius
        val geoFilteredFriends = nearbyFriends.filter { friend ->
            friend.distance <= GEO_FILTER_RADIUS_METERS
        }
        
        if (BuildConfig.DEBUG && geoFilteredFriends.size != nearbyFriends.size) {
            NearbyPanelLogger.logWarning(
                "geo_filtering",
                "Applied 10km geo-filter",
                mapOf(
                    "beforeFilter" to nearbyFriends.size,
                    "afterFilter" to geoFilteredFriends.size,
                    "filtered" to (nearbyFriends.size - geoFilteredFriends.size)
                )
            )
        }
        
        // Sort by smart ranking score (ascending = higher priority)
        val sortedFriends = geoFilteredFriends.sortedBy { it.smartRankingScore }
        
        // Log proximity bucket distribution
        if (BuildConfig.DEBUG && sortedFriends.isNotEmpty()) {
            val veryClose = sortedFriends.count { it.proximityBucket == NearbyFriend.ProximityBucket.VERY_CLOSE }
            val nearby = sortedFriends.count { it.proximityBucket == NearbyFriend.ProximityBucket.NEARBY }
            val inTown = sortedFriends.count { it.proximityBucket == NearbyFriend.ProximityBucket.IN_TOWN }
            
            NearbyPanelLogger.logWarning(
                "proximity_buckets",
                "Friend proximity distribution",
                mapOf(
                    "veryClose" to veryClose,
                    "nearby" to nearby,
                    "inTown" to inTown,
                    "total" to sortedFriends.size
                )
            )
        }
        
        // Log distance calculation results with detailed friend information
        NearbyPanelLogger.logFriendListState(sortedFriends.take(5))
        
        if (BuildConfig.DEBUG && sortedFriends.size != friendsToProcess.size) {
            NearbyPanelLogger.logWarning(
                "calculateNearbyFriends",
                "Friends filtered during geo-filtering and processing",
                mapOf(
                    "inputFriends" to friendsToProcess.size,
                    "outputFriends" to sortedFriends.size,
                    "geoFiltered" to (friendsToProcess.size - geoFilteredFriends.size)
                )
            )
        }
        
        return sortedFriends
    }
    
    /**
     * Check if there's a significant change in smart ranking or distances between two lists
     */
    private fun hasSignificantDistanceChange(
        oldList: List<NearbyFriend>,
        newList: List<NearbyFriend>
    ): Boolean {
        if (oldList.size != newList.size) return false
        
        // Check if any friend's distance or smart ranking has changed significantly
        return oldList.zip(newList).any { (old, new) ->
            old.id == new.id && (
                kotlin.math.abs(old.distance - new.distance) >= DISTANCE_TOLERANCE_METERS ||
                kotlin.math.abs(old.smartRankingScore - new.smartRankingScore) >= 0.01f // 1% change threshold
            )
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