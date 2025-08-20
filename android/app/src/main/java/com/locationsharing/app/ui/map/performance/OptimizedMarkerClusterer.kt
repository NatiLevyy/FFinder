package com.locationsharing.app.ui.map.performance

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.locationsharing.app.BuildConfig
import com.locationsharing.app.data.friends.Friend
import com.locationsharing.app.ui.friends.components.AnimatedFriendMarker
import com.locationsharing.app.ui.friends.components.SimpleFriendMarker
import com.locationsharing.app.ui.theme.FFinderAnimations
import kotlinx.coroutines.delay
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Optimized marker clusterer for efficient rendering of large friend lists
 * 
 * Features:
 * - Intelligent clustering based on zoom level and density
 * - Smooth animations for cluster expansion/collapse
 * - Memory-efficient marker management
 * - Performance monitoring and optimization
 */
@Singleton
class OptimizedMarkerClusterer @Inject constructor(
    private val performanceOptimizer: MapPerformanceOptimizer
) {
    
    companion object {
        private const val TAG = "OptimizedMarkerClusterer"
        private const val CLUSTER_ANIMATION_DURATION = 300
        private const val MARKER_STAGGER_DELAY = 50L
    }
    
    private val markerStates = mutableStateMapOf<String, MarkerState>()
    private val clusterStates = mutableStateMapOf<String, ClusterAnimationState>()
    
    /**
     * Render optimized markers with intelligent clustering
     */
    @Composable
    fun RenderOptimizedMarkers(
        friends: List<Friend>,
        selectedFriendId: String?,
        cameraPositionState: CameraPositionState,
        onMarkerClick: (String) -> Unit,
        onClusterClick: (List<Friend>) -> Unit
    ) {
        val currentZoom = cameraPositionState.position.zoom
        val currentCenter = cameraPositionState.position.target
        
        // Determine clustering strategy
        val shouldCluster = performanceOptimizer.shouldUseMarkerClustering(currentZoom, friends.size)
        
        if (BuildConfig.DEBUG) {
            Timber.d("$TAG: Rendering ${friends.size} friends, clustering: $shouldCluster, zoom: $currentZoom")
        }
        
        if (shouldCluster) {
            RenderClusteredMarkers(
                friends = friends,
                selectedFriendId = selectedFriendId,
                zoomLevel = currentZoom,
                cameraCenter = currentCenter,
                onMarkerClick = onMarkerClick,
                onClusterClick = onClusterClick
            )
        } else {
            RenderIndividualMarkers(
                friends = friends,
                selectedFriendId = selectedFriendId,
                onMarkerClick = onMarkerClick
            )
        }
        
        // Clean up removed markers
        LaunchedEffect(friends) {
            cleanupRemovedMarkers(friends.map { it.id }.toSet())
        }
    }
    
    /**
     * Render individual markers with optimized animations
     */
    @Composable
    private fun RenderIndividualMarkers(
        friends: List<Friend>,
        selectedFriendId: String?,
        onMarkerClick: (String) -> Unit
    ) {
        friends.forEachIndexed { index, friend ->
            val location = friend.getLatLng() ?: return@forEachIndexed
            
            val markerState = remember(friend.id) {
                markerStates.getOrPut(friend.id) { MarkerState(position = location) }
            }
            
            // Update marker position with throttling
            LaunchedEffect(location) {
                if (!performanceOptimizer.shouldThrottleLocationUpdate(location, markerState.position)) {
                    markerState.position = location
                }
            }
            
            // Staggered appearance animation
            var isVisible by remember { mutableStateOf(false) }
            LaunchedEffect(friend.id) {
                delay(index * MARKER_STAGGER_DELAY)
                isVisible = true
            }
            
            AnimatedVisibility(
                visible = isVisible,
                enter = scaleIn(
                    initialScale = 0.3f,
                    animationSpec = FFinderAnimations.Springs.Bouncy
                ) + fadeIn(),
                exit = scaleOut(targetScale = 0.3f) + fadeOut()
            ) {
                // Use simplified marker for better performance when many friends are visible
                if (friends.size > 15) {
                    SimpleFriendMarker(
                        friend = friend,
                        size = if (friend.id == selectedFriendId) 40 else 32,
                        onClick = { onMarkerClick(friend.id) },
                        showAnimation = friends.size <= 25
                    )
                } else {
                    AnimatedFriendMarker(
                        friend = friend,
                        isSelected = friend.id == selectedFriendId,
                        onClick = { onMarkerClick(friend.id) },
                        showAppearAnimation = true,
                        showMovementTrail = friend.isMoving() && friends.size <= 10
                    )
                }
            }
        }
    }
    
    /**
     * Render clustered markers for performance optimization
     */
    @Composable
    private fun RenderClusteredMarkers(
        friends: List<Friend>,
        selectedFriendId: String?,
        zoomLevel: Float,
        cameraCenter: LatLng,
        onMarkerClick: (String) -> Unit,
        onClusterClick: (List<Friend>) -> Unit
    ) {
        val clusters = remember(friends, zoomLevel, cameraCenter) {
            performanceOptimizer.createOptimizedClusters(friends, zoomLevel, cameraCenter)
        }
        
        clusters.forEach { cluster ->
            if (cluster.isCluster) {
                // Render cluster marker
                RenderClusterMarker(
                    cluster = cluster,
                    onClick = { onClusterClick(cluster.friends) }
                )
            } else {
                // Render individual friend marker
                val friend = cluster.friends.first()
                val location = friend.getLatLng() ?: return@forEach
                
                val markerState = remember(friend.id) {
                    markerStates.getOrPut(friend.id) { MarkerState(position = location) }
                }
                
                LaunchedEffect(location) {
                    if (!performanceOptimizer.shouldThrottleLocationUpdate(location, markerState.position)) {
                        markerState.position = location
                    }
                }
                
                SimpleFriendMarker(
                    friend = friend,
                    size = if (friend.id == selectedFriendId) 36 else 28,
                    onClick = { onMarkerClick(friend.id) },
                    showAnimation = false // Disable animations in cluster mode for performance
                )
            }
        }
    }
    
    /**
     * Render cluster marker with friend count
     */
    @Composable
    private fun RenderClusterMarker(
        cluster: FriendCluster,
        onClick: () -> Unit
    ) {
        val markerState = remember(cluster.id) {
            MarkerState(position = cluster.center)
        }
        
        val animationState = remember(cluster.id) {
            clusterStates.getOrPut(cluster.id) { ClusterAnimationState() }
        }
        
        val scale by animateFloatAsState(
            targetValue = if (animationState.isPressed) 0.9f else 1.0f,
            animationSpec = tween(150),
            label = "cluster_scale"
        )
        
        Marker(
            state = markerState,
            onClick = {
                animationState.isPressed = true
                onClick()
                true
            }
        )
    }
    
    /**
     * Cluster marker content composable
     */
    @Composable
    private fun ClusterMarkerContent(
        friendCount: Int,
        onlineFriendsCount: Int,
        scale: Float,
        onClick: () -> Unit
    ) {
        val density = LocalDensity.current
        val size = when {
            friendCount < 10 -> 48.dp
            friendCount < 50 -> 56.dp
            friendCount < 100 -> 64.dp
            else -> 72.dp
        }
        
        Box(
            modifier = Modifier
                .size(size)
                .scale(scale)
                .clickable { onClick() }
                .semantics {
                    contentDescription = "$friendCount friends, $onlineFriendsCount online"
                },
            contentAlignment = Alignment.Center
        ) {
            // Cluster background with gradient
            Box(
                modifier = Modifier
                    .size(size)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                            )
                        ),
                        shape = CircleShape
                    )
                    .border(
                        width = 3.dp,
                        color = Color.White,
                        shape = CircleShape
                    )
                    .clip(CircleShape)
            )
            
            // Friend count text
            Text(
                text = friendCount.toString(),
                color = Color.White,
                fontSize = when {
                    friendCount < 10 -> 14.sp
                    friendCount < 100 -> 12.sp
                    else -> 10.sp
                },
                fontWeight = FontWeight.Bold
            )
            
            // Online indicator
            if (onlineFriendsCount > 0) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            color = Color(0xFF4CAF50),
                            shape = CircleShape
                        )
                        .border(
                            width = 2.dp,
                            color = Color.White,
                            shape = CircleShape
                        )
                        .align(Alignment.TopEnd)
                )
            }
        }
    }
    
    /**
     * Clean up marker states for removed friends
     */
    private fun cleanupRemovedMarkers(currentFriendIds: Set<String>) {
        val removedIds = markerStates.keys - currentFriendIds
        removedIds.forEach { friendId ->
            markerStates.remove(friendId)
            clusterStates.remove(friendId)
        }
        
        if (BuildConfig.DEBUG && removedIds.isNotEmpty()) {
            Timber.d("$TAG: Cleaned up ${removedIds.size} removed markers")
        }
    }
    
    /**
     * Get marker state for friend (create if doesn't exist)
     */
    fun getMarkerState(friendId: String, initialPosition: LatLng): MarkerState {
        return markerStates.getOrPut(friendId) { MarkerState(position = initialPosition) }
    }
    
    /**
     * Update marker position with animation
     */
    suspend fun updateMarkerPosition(friendId: String, newPosition: LatLng) {
        markerStates[friendId]?.let { markerState ->
            if (!performanceOptimizer.shouldThrottleLocationUpdate(newPosition, markerState.position)) {
                markerState.position = newPosition
            }
        }
    }
    
    /**
     * Clear all marker states (for cleanup)
     */
    fun clearAllMarkers() {
        markerStates.clear()
        clusterStates.clear()
        
        if (BuildConfig.DEBUG) {
            Timber.d("$TAG: Cleared all marker states")
        }
    }
}

/**
 * Animation state for cluster markers
 */
data class ClusterAnimationState(
    var isPressed: Boolean = false,
    var isExpanding: Boolean = false,
    var lastUpdateTime: Long = 0L
)