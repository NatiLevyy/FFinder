package com.locationsharing.app.ui.friends.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.locationsharing.app.data.friends.Friend
import com.locationsharing.app.ui.theme.FFinderAnimations
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Enhanced map marker manager with smooth animations, clustering, and performance optimization
 * Handles marker lifecycle, animations, and intelligent clustering based on zoom level
 */
class EnhancedMapMarkerManager {
    
    private val markerStates = mutableStateMapOf<String, MarkerAnimationState>()
    private val clusterStates = mutableStateMapOf<String, ClusterState>()
    private var lastZoomLevel: Float = 15f
    private var lastCameraPosition: LatLng? = null
    
    companion object {
        private const val CLUSTERING_ZOOM_THRESHOLD = 12f
        private const val CLUSTER_DISTANCE_THRESHOLD = 100.0 // meters
        private const val MAX_MARKERS_WITHOUT_CLUSTERING = 20
        private const val ANIMATION_STAGGER_DELAY = 50L
        private const val MOVEMENT_ANIMATION_DURATION = 800L
        private const val APPEARANCE_ANIMATION_DURATION = 600L
    }
    
    /**
     * Render enhanced markers with clustering and animations
     */
    @Composable
    fun RenderEnhancedMarkers(
        friends: List<Friend>,
        selectedFriendId: String?,
        cameraPositionState: CameraPositionState,
        onMarkerClick: (String) -> Unit,
        onClusterClick: (List<Friend>) -> Unit
    ) {
        val density = LocalDensity.current
        val currentZoom = cameraPositionState.position.zoom
        val currentCenter = cameraPositionState.position.target
        
        // 🧪 DEBUG: Log marker rendering
        LaunchedEffect(friends.size) {
            Timber.d("🧪 DEBUG: EnhancedMapMarkerManager - RenderEnhancedMarkers called")
            Timber.d("🧪 DEBUG: Friends to render: ${friends.size}")
            Timber.d("🧪 DEBUG: Selected friend ID: $selectedFriendId")
            Timber.d("🧪 DEBUG: Current zoom: $currentZoom")
            
            // Log test friends specifically
            val testFriends = friends.filter { it.id.startsWith("test_friend_") }
            Timber.d("🧪 DEBUG: Test friends to render: ${testFriends.size}")
            testFriends.forEach { friend ->
                Timber.d("🧪 DEBUG: Rendering test friend - ${friend.name} at ${friend.getLatLng()}")
            }
        }
        
        // Update zoom and camera tracking
        LaunchedEffect(currentZoom, currentCenter) {
            handleZoomChange(currentZoom)
            handleCameraMove(currentCenter)
        }
        
        // Determine if clustering should be used
        val shouldCluster = shouldUseClusteringForZoom(currentZoom) || 
                           friends.size > MAX_MARKERS_WITHOUT_CLUSTERING
        
        Timber.d("🧪 DEBUG: Should use clustering: $shouldCluster (zoom: $currentZoom, friends: ${friends.size})")
        
        if (shouldCluster) {
            Timber.d("🧪 DEBUG: Using clustered markers")
            RenderClusteredMarkers(
                friends = friends,
                selectedFriendId = selectedFriendId,
                zoomLevel = currentZoom,
                onMarkerClick = onMarkerClick,
                onClusterClick = onClusterClick
            )
        } else {
            Timber.d("🧪 DEBUG: Using individual markers")
            RenderIndividualMarkers(
                friends = friends,
                selectedFriendId = selectedFriendId,
                onMarkerClick = onMarkerClick
            )
        }
    }
    
    /**
     * Render individual markers with smooth animations
     */
    @Composable
    private fun RenderIndividualMarkers(
        friends: List<Friend>,
        selectedFriendId: String?,
        onMarkerClick: (String) -> Unit
    ) {
        Timber.d("🧪 DEBUG: RenderIndividualMarkers - rendering ${friends.size} individual markers")
        
        friends.forEachIndexed { index, friend ->
            val location = friend.getLatLng()
            
            if (location != null) {
                Timber.d("🧪 DEBUG: Creating marker for friend ${friend.name} at $location")
                
                val markerState = remember(friend.id) { 
                    MarkerState(position = location)
                }
                
                // 🧪 DEBUG: Simplified marker rendering - remove complex animations for now
                // Update marker position if it changes
                LaunchedEffect(location) {
                    if (markerState.position != location) {
                        Timber.d("🧪 DEBUG: Updating marker position for ${friend.name} to $location")
                        markerState.position = location
                    }
                }
                
                // Create the marker directly without complex animation logic
                Marker(
                    state = markerState,
                    title = friend.name,
                    snippet = friend.getStatusText(),
                    onClick = {
                        Timber.d("🧪 DEBUG: Marker clicked for friend ${friend.name}")
                        onMarkerClick(friend.id)
                        true
                    }
                )
                
                Timber.d("🧪 DEBUG: Marker created successfully for ${friend.name}")
            } else {
                Timber.w("🧪 DEBUG: Friend ${friend.name} has no location - skipping marker")
            }
        }
        
        Timber.d("🧪 DEBUG: RenderIndividualMarkers completed")
    }
    
    /**
     * Render clustered markers for better performance at low zoom levels
     */
    @Composable
    private fun RenderClusteredMarkers(
        friends: List<Friend>,
        selectedFriendId: String?,
        zoomLevel: Float,
        onMarkerClick: (String) -> Unit,
        onClusterClick: (List<Friend>) -> Unit
    ) {
        val clusters = createClusters(friends, zoomLevel)
        
        clusters.forEach { cluster ->
            if (cluster.friends.size == 1) {
                // Single friend - render as individual marker
                val friend = cluster.friends.first()
                friend.getLatLng()?.let { location ->
                    val markerState = remember(friend.id) { 
                        MarkerState(position = location)
                    }
                    
                    Marker(
                        state = markerState,
                        title = friend.name,
                        snippet = friend.getStatusText(),
                        onClick = {
                            onMarkerClick(friend.id)
                            true
                        }
                    )
                }
            } else {
                // Multiple friends - render as cluster
                val markerState = remember(cluster.id) { 
                    MarkerState(position = cluster.center)
                }
                
                Marker(
                    state = markerState,
                    title = "${cluster.friends.size} friends",
                    snippet = "Tap to expand",
                    onClick = {
                        onClusterClick(cluster.friends)
                        true
                    }
                )
            }
        }
    }
    
    /**
     * Enhanced animated friend marker with performance optimizations
     */
    @Composable
    private fun EnhancedAnimatedFriendMarker(
        friend: Friend,
        isSelected: Boolean,
        animationState: MarkerAnimationState,
        onClick: () -> Unit
    ) {
        AnimatedFriendMarker(
            friend = friend,
            isSelected = isSelected,
            onClick = onClick,
            showAppearAnimation = animationState.shouldShowAppearAnimation,
            showMovementTrail = animationState.isMoving && friend.isOnline()
        )
    }
    
    /**
     * Create clusters based on friend locations and zoom level
     */
    private fun createClusters(friends: List<Friend>, zoomLevel: Float): List<FriendCluster> {
        val clusters = mutableListOf<FriendCluster>()
        val processedFriends = mutableSetOf<String>()
        val distanceThreshold = getClusterDistanceForZoom(zoomLevel)
        
        friends.forEach { friend ->
            if (friend.id in processedFriends) return@forEach
            
            val friendLocation = friend.getLatLng() ?: return@forEach
            val nearbyFriends = mutableListOf(friend)
            processedFriends.add(friend.id)
            
            // Find nearby friends to cluster
            friends.forEach { otherFriend ->
                if (otherFriend.id in processedFriends) return@forEach
                
                val otherLocation = otherFriend.getLatLng() ?: return@forEach
                val distance = calculateDistance(friendLocation, otherLocation)
                
                if (distance <= distanceThreshold) {
                    nearbyFriends.add(otherFriend)
                    processedFriends.add(otherFriend.id)
                }
            }
            
            // Create cluster
            val clusterId = if (nearbyFriends.size == 1) {
                friend.id
            } else {
                "cluster_${nearbyFriends.map { it.id }.sorted().joinToString("_")}"
            }
            
            clusters.add(
                FriendCluster(
                    id = clusterId,
                    friends = nearbyFriends,
                    center = calculateClusterCenter(nearbyFriends),
                    onlineFriendsCount = nearbyFriends.count { it.isOnline() }
                )
            )
        }
        
        return clusters
    }
    
    /**
     * Calculate cluster center point
     */
    private fun calculateClusterCenter(friends: List<Friend>): LatLng {
        val locations = friends.mapNotNull { it.getLatLng() }
        if (locations.isEmpty()) return LatLng(0.0, 0.0)
        
        val avgLat = locations.map { it.latitude }.average()
        val avgLng = locations.map { it.longitude }.average()
        
        return LatLng(avgLat, avgLng)
    }
    
    /**
     * Calculate distance between two points in meters
     */
    private fun calculateDistance(point1: LatLng, point2: LatLng): Double {
        val earthRadius = 6371000.0 // Earth radius in meters
        val dLat = Math.toRadians(point2.latitude - point1.latitude)
        val dLng = Math.toRadians(point2.longitude - point1.longitude)
        
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(point1.latitude)) * cos(Math.toRadians(point2.latitude)) *
                sin(dLng / 2) * sin(dLng / 2)
        
        val c = 2 * kotlin.math.atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }
    
    /**
     * Get cluster distance threshold based on zoom level
     */
    private fun getClusterDistanceForZoom(zoomLevel: Float): Double {
        return when {
            zoomLevel < 10f -> 5000.0 // 5km
            zoomLevel < 12f -> 2000.0 // 2km
            zoomLevel < 14f -> 1000.0 // 1km
            zoomLevel < 16f -> 500.0  // 500m
            else -> 200.0             // 200m
        }
    }
    
    /**
     * Check if clustering should be used for current zoom level
     */
    private fun shouldUseClusteringForZoom(zoomLevel: Float): Boolean {
        return zoomLevel < CLUSTERING_ZOOM_THRESHOLD
    }
    
    /**
     * Handle zoom level changes
     */
    private fun handleZoomChange(newZoom: Float) {
        if (abs(newZoom - lastZoomLevel) > 0.5f) {
            lastZoomLevel = newZoom
            Timber.d("Map zoom changed to $newZoom")
        }
    }
    
    /**
     * Handle camera movement
     */
    private fun handleCameraMove(newCenter: LatLng) {
        val lastCenter = lastCameraPosition
        if (lastCenter == null || calculateDistance(lastCenter, newCenter) > 100.0) {
            lastCameraPosition = newCenter
            Timber.d("Camera moved to ${newCenter.latitude}, ${newCenter.longitude}")
        }
    }
    
    /**
     * Animate marker to new position smoothly
     */
    private suspend fun animateMarkerToPosition(
        friendId: String,
        markerState: MarkerState,
        newPosition: LatLng
    ) {
        val currentPosition = markerState.position
        val distance = calculateDistance(currentPosition, newPosition)
        
        // Only animate if the distance is significant
        if (distance > 10.0) { // 10 meters threshold
            val animationState = getOrCreateMarkerAnimationState(friendId)
            animationState.isMoving = true
            
            // Animate position change
            val latAnimatable = Animatable(currentPosition.latitude.toFloat())
            val lngAnimatable = Animatable(currentPosition.longitude.toFloat())
            
            CoroutineScope(Dispatchers.Main).launch {
                latAnimatable.animateTo(
                    targetValue = newPosition.latitude.toFloat(),
                    animationSpec = tween(MOVEMENT_ANIMATION_DURATION.toInt())
                )
            }
            
            CoroutineScope(Dispatchers.Main).launch {
                lngAnimatable.animateTo(
                    targetValue = newPosition.longitude.toFloat(),
                    animationSpec = tween(MOVEMENT_ANIMATION_DURATION.toInt())
                )
            }
            
            // Update marker position during animation
            while (latAnimatable.isRunning || lngAnimatable.isRunning) {
                markerState.position = LatLng(latAnimatable.value.toDouble(), lngAnimatable.value.toDouble())
                delay(16) // ~60 FPS
            }
            
            animationState.isMoving = false
        } else {
            // Small movement - just update position directly
            markerState.position = newPosition
        }
    }
    
    /**
     * Get or create marker animation state
     */
    private fun getOrCreateMarkerAnimationState(friendId: String): MarkerAnimationState {
        return markerStates.getOrPut(friendId) { MarkerAnimationState() }
    }
    
    /**
     * Focus camera on specific friend with smooth animation
     */
    suspend fun focusOnFriend(
        friend: Friend,
        cameraPositionState: CameraPositionState,
        zoomLevel: Float = 16f,
        duration: Int = 1200
    ) {
        friend.getLatLng()?.let { location ->
            val cameraUpdate = CameraUpdateFactory.newLatLngZoom(location, zoomLevel)
            cameraPositionState.animate(cameraUpdate, duration)
            Timber.d("Camera focused on ${friend.name}")
        }
    }
    
    /**
     * Focus camera on multiple friends (fit bounds)
     */
    suspend fun focusOnFriends(
        friends: List<Friend>,
        cameraPositionState: CameraPositionState,
        padding: Int = 100
    ) {
        val locations = friends.mapNotNull { it.getLatLng() }
        if (locations.isEmpty()) return
        
        val boundsBuilder = LatLngBounds.Builder()
        locations.forEach { boundsBuilder.include(it) }
        
        val bounds = boundsBuilder.build()
        val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding)
        
        cameraPositionState.animate(cameraUpdate, 1500)
        Timber.d("Camera focused on ${friends.size} friends")
    }
    
    /**
     * Clean up marker states for removed friends
     */
    fun cleanupRemovedFriends(currentFriendIds: Set<String>) {
        val removedIds = markerStates.keys - currentFriendIds
        removedIds.forEach { friendId ->
            markerStates.remove(friendId)
            Timber.d("Cleaned up marker state for removed friend: $friendId")
        }
    }
}

/**
 * Cluster state for grouped markers
 */
data class ClusterState(
    val id: String,
    val center: LatLng,
    val friends: List<Friend>,
    val isExpanded: Boolean = false
)

/**
 * Friend cluster data class
 */
data class FriendCluster(
    val id: String,
    val friends: List<Friend>,
    val center: LatLng,
    val onlineFriendsCount: Int
)