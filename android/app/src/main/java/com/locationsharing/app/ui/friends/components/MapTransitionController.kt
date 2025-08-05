package com.locationsharing.app.ui.friends.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.locationsharing.app.data.friends.Friend
import com.locationsharing.app.ui.theme.FFinderAnimations
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Advanced map transition controller for smooth camera movements and marker animations
 * Provides cinematic transitions, focus animations, and coordinated marker management
 */
class MapTransitionController {
    
    private var isTransitioning by mutableStateOf(false)
    private var currentFocusedFriend: String? = null
    private val markerStates = mutableMapOf<String, MarkerTransitionState>()
    
    /**
     * Smooth camera transition to focus on a friend with cinematic movement
     */
    suspend fun focusOnFriend(
        friend: Friend,
        cameraPositionState: CameraPositionState,
        zoomLevel: Float = 16f,
        duration: Int = 1200
    ) {
        if (isTransitioning) return
        
        isTransitioning = true
        currentFocusedFriend = friend.id
        
        try {
            // Calculate optimal camera position with slight offset for better visibility
            val targetPosition = CameraPosition.Builder()
                .target(friend.getLatLng() ?: return)
                .zoom(zoomLevel)
                .bearing(calculateOptimalBearing(friend))
                .tilt(calculateOptimalTilt(zoomLevel))
                .build()
            
            // Animate camera with smooth easing
            cameraPositionState.animate(
                update = CameraUpdateFactory.newCameraPosition(targetPosition),
                durationMs = duration
            )
            
            // Trigger marker focus animation
            getMarkerState(friend.id).apply {
                isFocused = true
                shouldHighlight = true
            }
            
        } finally {
            isTransitioning = false
        }
    }
    
    /**
     * Smooth transition to show all friends on the map
     */
    suspend fun showAllFriends(
        friends: List<Friend>,
        cameraPositionState: CameraPositionState,
        padding: Int = 100
    ) {
        if (friends.isEmpty() || isTransitioning) return
        
        isTransitioning = true
        currentFocusedFriend = null
        
        try {
            val bounds = calculateBounds(friends.mapNotNull { it.getLatLng() })
            val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding)
            
            cameraPositionState.animate(
                update = cameraUpdate,
                durationMs = 1000
            )
            
            // Reset all marker focus states
            markerStates.values.forEach { state ->
                state.isFocused = false
                state.shouldHighlight = false
            }
            
        } finally {
            isTransitioning = false
        }
    }
    
    /**
     * Animated friend addition with staggered appearance
     */
    suspend fun addFriendWithAnimation(
        friend: Friend,
        delayMs: Long = 0
    ) {
        delay(delayMs)
        
        val state = getMarkerState(friend.id)
        state.apply {
            isVisible = true
            shouldShowAppearAnimation = true
            animationPhase = MarkerAnimationPhase.APPEARING
        }
        
        // Reset animation state after completion
        delay(800)
        state.apply {
            shouldShowAppearAnimation = false
            animationPhase = MarkerAnimationPhase.IDLE
        }
    }
    
    /**
     * Animated friend removal with fade out
     */
    suspend fun removeFriendWithAnimation(friendId: String) {
        val state = getMarkerState(friendId)
        state.apply {
            shouldShowDisappearAnimation = true
            animationPhase = MarkerAnimationPhase.DISAPPEARING
        }
        
        delay(500) // Wait for animation to complete
        
        state.apply {
            isVisible = false
            shouldShowDisappearAnimation = false
            animationPhase = MarkerAnimationPhase.HIDDEN
        }
        
        markerStates.remove(friendId)
    }
    
    /**
     * Smooth friend location update with movement trail
     */
    suspend fun updateFriendLocation(
        friendId: String,
        newLocation: LatLng,
        showMovementTrail: Boolean = true
    ) {
        val state = getMarkerState(friendId)
        
        if (showMovementTrail) {
            state.apply {
                shouldShowMovementTrail = true
                isMoving = true
                animationPhase = MarkerAnimationPhase.MOVING
            }
        }
        
        // Update location (this would be handled by the map component)
        // The actual location update is managed by the parent component
        
        if (showMovementTrail) {
            delay(1200) // Duration of movement animation
            state.apply {
                shouldShowMovementTrail = false
                isMoving = false
                animationPhase = MarkerAnimationPhase.IDLE
            }
        }
    }
    
    /**
     * Batch friend updates with coordinated animations
     */
    suspend fun batchUpdateFriends(
        updates: List<FriendLocationUpdate>,
        staggerDelayMs: Long = 50
    ) {
        updates.forEachIndexed { index, update ->
            delay(index * staggerDelayMs)
            
            when (update.type) {
                FriendUpdateType.ADDED -> addFriendWithAnimation(update.friend)
                FriendUpdateType.REMOVED -> removeFriendWithAnimation(update.friend.id)
                FriendUpdateType.MOVED -> updateFriendLocation(
                    update.friend.id, 
                    update.friend.getLatLng() ?: return@forEachIndexed,
                    showMovementTrail = true
                )
                FriendUpdateType.STATUS_CHANGED -> {
                    val state = getMarkerState(update.friend.id)
                    state.shouldPulse = update.friend.isOnline()
                }
            }
        }
    }
    
    /**
     * Get or create marker state for a friend
     */
    fun getMarkerState(friendId: String): MarkerTransitionState {
        return markerStates.getOrPut(friendId) { MarkerTransitionState() }
    }
    
    /**
     * Clear focus from current friend
     */
    fun clearFocus() {
        currentFocusedFriend = null
        markerStates.values.forEach { state ->
            state.isFocused = false
            state.shouldHighlight = false
        }
    }
    
    /**
     * Check if currently transitioning
     */
    fun isCurrentlyTransitioning(): Boolean = isTransitioning
    
    /**
     * Get currently focused friend ID
     */
    fun getCurrentFocusedFriend(): String? = currentFocusedFriend
    
    /**
     * Calculate optimal bearing for friend focus
     */
    private fun calculateOptimalBearing(friend: Friend): Float {
        // Add slight rotation for more dynamic view
        return if (friend.isMoving()) 15f else 0f
    }
    
    /**
     * Calculate optimal tilt based on zoom level
     */
    private fun calculateOptimalTilt(zoomLevel: Float): Float {
        return when {
            zoomLevel > 18f -> 45f
            zoomLevel > 15f -> 30f
            else -> 0f
        }
    }
    
    /**
     * Calculate bounds for multiple locations
     */
    private fun calculateBounds(locations: List<LatLng>): com.google.android.gms.maps.model.LatLngBounds {
        val builder = com.google.android.gms.maps.model.LatLngBounds.Builder()
        locations.forEach { builder.include(it) }
        return builder.build()
    }
}

/**
 * State for individual marker transitions
 */
data class MarkerTransitionState(
    var isVisible: Boolean = false,
    var isFocused: Boolean = false,
    var shouldHighlight: Boolean = false,
    var shouldShowAppearAnimation: Boolean = false,
    var shouldShowDisappearAnimation: Boolean = false,
    var shouldShowMovementTrail: Boolean = false,
    var shouldPulse: Boolean = true,
    var isMoving: Boolean = false,
    var animationPhase: MarkerAnimationPhase = MarkerAnimationPhase.HIDDEN
)

/**
 * Animation phases for markers
 */
enum class MarkerAnimationPhase {
    HIDDEN,
    APPEARING,
    IDLE,
    MOVING,
    FOCUSED,
    DISAPPEARING
}

/**
 * Friend update types for batch operations
 */
enum class FriendUpdateType {
    ADDED,
    REMOVED,
    MOVED,
    STATUS_CHANGED
}

/**
 * Friend location update data class
 */
data class FriendLocationUpdate(
    val friend: Friend,
    val type: FriendUpdateType
)

/**
 * Composable for smooth screen transitions
 */
@Composable
fun MapScreenTransition(
    isVisible: Boolean,
    content: @Composable () -> Unit
) {
    val transitionProgress by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = FFinderAnimations.Transitions.screenEnter(),
        label = "screen_transition"
    )
    
    Box(
        modifier = androidx.compose.ui.Modifier
            .alpha(transitionProgress)
            .scale(0.95f + (transitionProgress * 0.05f))
    ) {
        content()
    }
}

/**
 * Composable for coordinated marker animations
 */
@Composable
fun CoordinatedMarkerAnimation(
    friendId: String,
    controller: MapTransitionController,
    content: @Composable (MarkerTransitionState) -> Unit
) {
    val state = remember { controller.getMarkerState(friendId) }
    
    // Animate marker properties based on state
    val markerScale by animateFloatAsState(
        targetValue = when {
            state.isFocused -> 1.4f
            state.shouldHighlight -> 1.2f
            else -> 1.0f
        },
        animationSpec = FFinderAnimations.Springs.Bouncy,
        label = "marker_scale"
    )
    
    val markerAlpha by animateFloatAsState(
        targetValue = if (state.isVisible) 1f else 0f,
        animationSpec = when (state.animationPhase) {
            MarkerAnimationPhase.APPEARING -> FFinderAnimations.Transitions.screenEnter()
            MarkerAnimationPhase.DISAPPEARING -> FFinderAnimations.Transitions.screenExit()
            else -> FFinderAnimations.MicroInteractions.hover()
        },
        label = "marker_alpha"
    )
    
    // Apply transformations and render content
    Box(
        modifier = androidx.compose.ui.Modifier
            .scale(markerScale)
            .alpha(markerAlpha)
    ) {
        content(state)
    }
}

