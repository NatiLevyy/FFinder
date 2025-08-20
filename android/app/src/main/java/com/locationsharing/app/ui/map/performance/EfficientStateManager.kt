package com.locationsharing.app.ui.map.performance

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import com.google.android.gms.maps.model.LatLng
import com.locationsharing.app.BuildConfig
import com.locationsharing.app.data.friends.Friend
import com.locationsharing.app.ui.map.MapScreenState
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * Efficient state manager for MapScreen to minimize recompositions and optimize performance
 * 
 * Features:
 * - State change debouncing
 * - Selective recomposition optimization
 * - Memory-efficient state updates
 * - Performance monitoring for state changes
 */
@Singleton
class EfficientStateManager @Inject constructor() {
    
    companion object {
        private const val TAG = "EfficientStateManager"
        private const val DEBOUNCE_DELAY_MS = 100L
        private const val LOCATION_UPDATE_THRESHOLD_METERS = 5.0
        private const val ZOOM_UPDATE_THRESHOLD = 0.1f
        private const val FRIENDS_UPDATE_BATCH_SIZE = 10
    }
    
    /**
     * Optimized state holder for map-related state
     */
    data class OptimizedMapState(
        val currentLocation: LatLng? = null,
        val friends: List<Friend> = emptyList(),
        val selectedFriendId: String? = null,
        val zoomLevel: Float = 15f,
        val cameraCenter: LatLng? = null,
        val isLocationLoading: Boolean = false,
        val isFriendsLoading: Boolean = false,
        val nearbyFriendsCount: Int = 0,
        val onlineFriendsCount: Int = 0,
        val lastUpdateTime: Long = System.currentTimeMillis()
    ) {
        /**
         * Check if location update is significant enough to trigger recomposition
         */
        fun shouldUpdateLocation(newLocation: LatLng?): Boolean {
            if (newLocation == null) return currentLocation != null
            if (currentLocation == null) return true
            
            return calculateDistance(currentLocation, newLocation) > LOCATION_UPDATE_THRESHOLD_METERS
        }
        
        /**
         * Check if zoom update is significant enough to trigger recomposition
         */
        fun shouldUpdateZoom(newZoom: Float): Boolean {
            return abs(newZoom - zoomLevel) > ZOOM_UPDATE_THRESHOLD
        }
        
        /**
         * Check if friends list update should trigger recomposition
         */
        fun shouldUpdateFriends(newFriends: List<Friend>): Boolean {
            if (newFriends.size != friends.size) return true
            
            // Check for significant changes in friend positions or status
            return newFriends.zip(friends).any { (new, old) ->
                new.id != old.id ||
                new.isOnline() != old.isOnline() ||
                new.getLatLng()?.let { newPos ->
                    old.getLatLng()?.let { oldPos ->
                        calculateDistance(newPos, oldPos) > LOCATION_UPDATE_THRESHOLD_METERS
                    } ?: true
                } ?: false
            }
        }
        
        private fun calculateDistance(point1: LatLng, point2: LatLng): Double {
            val earthRadius = 6371000.0
            val dLat = Math.toRadians(point2.latitude - point1.latitude)
            val dLng = Math.toRadians(point2.longitude - point1.longitude)
            
            val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
                    kotlin.math.cos(Math.toRadians(point1.latitude)) * kotlin.math.cos(Math.toRadians(point2.latitude)) *
                    kotlin.math.sin(dLng / 2) * kotlin.math.sin(dLng / 2)
            
            val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
            return earthRadius * c
        }
    }
    
    /**
     * Create optimized state from MapScreenState
     */
    fun createOptimizedState(mapScreenState: MapScreenState): OptimizedMapState {
        return OptimizedMapState(
            currentLocation = mapScreenState.currentLocation,
            friends = mapScreenState.friends,
            selectedFriendId = mapScreenState.selectedFriendId,
            zoomLevel = mapScreenState.mapZoom,
            cameraCenter = mapScreenState.mapCenter,
            isLocationLoading = mapScreenState.isLocationLoading,
            isFriendsLoading = mapScreenState.isFriendsLoading,
            nearbyFriendsCount = mapScreenState.nearbyFriendsCount,
            onlineFriendsCount = mapScreenState.friends.count { it.isOnline() }
        )
    }
    
    /**
     * Batch process friend updates for better performance
     */
    fun batchProcessFriendUpdates(
        currentFriends: List<Friend>,
        newFriends: List<Friend>
    ): List<Friend> {
        if (newFriends.size <= FRIENDS_UPDATE_BATCH_SIZE) {
            return newFriends
        }
        
        // Process in batches to avoid blocking the main thread
        val result = mutableListOf<Friend>()
        val currentFriendsMap = currentFriends.associateBy { it.id }
        
        newFriends.chunked(FRIENDS_UPDATE_BATCH_SIZE).forEach { batch ->
            batch.forEach { newFriend ->
                val currentFriend = currentFriendsMap[newFriend.id]
                
                // Only update if there are significant changes
                if (currentFriend == null || hasSignificantChanges(currentFriend, newFriend)) {
                    result.add(newFriend)
                } else {
                    result.add(currentFriend) // Keep existing friend to avoid unnecessary updates
                }
            }
        }
        
        return result
    }
    
    /**
     * Check if friend has significant changes that warrant an update
     */
    private fun hasSignificantChanges(current: Friend, new: Friend): Boolean {
        // Check online status change
        if (current.isOnline() != new.isOnline()) return true
        
        // Check location change
        val currentLocation = current.getLatLng()
        val newLocation = new.getLatLng()
        
        if (currentLocation == null && newLocation != null) return true
        if (currentLocation != null && newLocation == null) return true
        
        if (currentLocation != null && newLocation != null) {
            val distance = calculateDistance(currentLocation, newLocation)
            if (distance > LOCATION_UPDATE_THRESHOLD_METERS) return true
        }
        
        // Check other significant changes
        return current.name != new.name ||
               current.avatarUrl != new.avatarUrl ||
               current.profileColor != new.profileColor
    }
    
    /**
     * Calculate distance between two points
     */
    private fun calculateDistance(point1: LatLng, point2: LatLng): Double {
        val earthRadius = 6371000.0
        val dLat = Math.toRadians(point2.latitude - point1.latitude)
        val dLng = Math.toRadians(point2.longitude - point1.longitude)
        
        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
                kotlin.math.cos(Math.toRadians(point1.latitude)) * kotlin.math.cos(Math.toRadians(point2.latitude)) *
                kotlin.math.sin(dLng / 2) * kotlin.math.sin(dLng / 2)
        
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        return earthRadius * c
    }
    
    /**
     * Monitor state change performance
     */
    fun monitorStateChangePerformance(
        operationName: String,
        oldState: OptimizedMapState,
        newState: OptimizedMapState
    ) {
        if (!BuildConfig.DEBUG) return
        
        val changes = mutableListOf<String>()
        
        if (oldState.currentLocation != newState.currentLocation) {
            changes.add("location")
        }
        if (oldState.friends.size != newState.friends.size) {
            changes.add("friends_count")
        }
        if (oldState.selectedFriendId != newState.selectedFriendId) {
            changes.add("selection")
        }
        if (oldState.zoomLevel != newState.zoomLevel) {
            changes.add("zoom")
        }
        if (oldState.isLocationLoading != newState.isLocationLoading) {
            changes.add("location_loading")
        }
        if (oldState.isFriendsLoading != newState.isFriendsLoading) {
            changes.add("friends_loading")
        }
        
        if (changes.isNotEmpty()) {
            Timber.d("$TAG: State change in $operationName: ${changes.joinToString(", ")}")
        }
    }
}

/**
 * Composable for efficient state management with debouncing and optimization
 */
@OptIn(FlowPreview::class)
@Composable
fun rememberEfficientMapState(
    mapScreenState: MapScreenState,
    stateManager: EfficientStateManager
): EfficientStateManager.OptimizedMapState {
    
    var optimizedState by remember { 
        mutableStateOf(stateManager.createOptimizedState(mapScreenState))
    }
    
    // Debounced location updates
    LaunchedEffect(mapScreenState.currentLocation) {
        snapshotFlow { mapScreenState.currentLocation }
            .distinctUntilChanged()
            .debounce(100L) // 100ms debounce delay
            .filter { newLocation ->
                optimizedState.shouldUpdateLocation(newLocation)
            }
            .collect { newLocation ->
                val oldState = optimizedState
                optimizedState = optimizedState.copy(
                    currentLocation = newLocation,
                    lastUpdateTime = System.currentTimeMillis()
                )
                
                stateManager.monitorStateChangePerformance("location_update", oldState, optimizedState)
            }
    }
    
    // Debounced friends updates
    LaunchedEffect(mapScreenState.friends) {
        snapshotFlow { mapScreenState.friends }
            .distinctUntilChanged()
            .debounce(100L) // 100ms debounce delay
            .filter { newFriends ->
                optimizedState.shouldUpdateFriends(newFriends)
            }
            .collect { newFriends ->
                val oldState = optimizedState
                val processedFriends = stateManager.batchProcessFriendUpdates(
                    optimizedState.friends,
                    newFriends
                )
                
                optimizedState = optimizedState.copy(
                    friends = processedFriends,
                    nearbyFriendsCount = processedFriends.size,
                    onlineFriendsCount = processedFriends.count { it.isOnline() },
                    lastUpdateTime = System.currentTimeMillis()
                )
                
                stateManager.monitorStateChangePerformance("friends_update", oldState, optimizedState)
            }
    }
    
    // Debounced zoom updates
    LaunchedEffect(mapScreenState.mapZoom) {
        snapshotFlow { mapScreenState.mapZoom }
            .distinctUntilChanged()
            .debounce(50L) // 50ms debounce for zoom
            .filter { newZoom ->
                optimizedState.shouldUpdateZoom(newZoom)
            }
            .collect { newZoom ->
                val oldState = optimizedState
                optimizedState = optimizedState.copy(
                    zoomLevel = newZoom,
                    lastUpdateTime = System.currentTimeMillis()
                )
                
                stateManager.monitorStateChangePerformance("zoom_update", oldState, optimizedState)
            }
    }
    
    // Immediate updates for critical state changes
    LaunchedEffect(mapScreenState.selectedFriendId) {
        if (optimizedState.selectedFriendId != mapScreenState.selectedFriendId) {
            val oldState = optimizedState
            optimizedState = optimizedState.copy(
                selectedFriendId = mapScreenState.selectedFriendId,
                lastUpdateTime = System.currentTimeMillis()
            )
            
            stateManager.monitorStateChangePerformance("selection_update", oldState, optimizedState)
        }
    }
    
    // Loading state updates
    LaunchedEffect(mapScreenState.isLocationLoading, mapScreenState.isFriendsLoading) {
        val oldState = optimizedState
        optimizedState = optimizedState.copy(
            isLocationLoading = mapScreenState.isLocationLoading,
            isFriendsLoading = mapScreenState.isFriendsLoading,
            lastUpdateTime = System.currentTimeMillis()
        )
        
        stateManager.monitorStateChangePerformance("loading_update", oldState, optimizedState)
    }
    
    return optimizedState
}

/**
 * Derived state for expensive computations
 */
@Composable
fun rememberDerivedMapState(
    optimizedState: EfficientStateManager.OptimizedMapState
) = remember {
    derivedStateOf {
        DerivedMapState(
            hasLocation = optimizedState.currentLocation != null,
            hasOnlineFriends = optimizedState.onlineFriendsCount > 0,
            shouldShowClustering = optimizedState.friends.size > 20 || optimizedState.zoomLevel < 12f,
            friendsInView = optimizedState.friends.filter { friend ->
                // This would be calculated based on camera bounds in a real implementation
                friend.isOnline()
            }
        )
    }
}.value

/**
 * Derived state data class
 */
data class DerivedMapState(
    val hasLocation: Boolean,
    val hasOnlineFriends: Boolean,
    val shouldShowClustering: Boolean,
    val friendsInView: List<Friend>
)