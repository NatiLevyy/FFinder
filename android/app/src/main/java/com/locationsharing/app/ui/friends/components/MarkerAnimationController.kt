package com.locationsharing.app.ui.friends.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.locationsharing.app.data.friends.FriendLocationUpdate
import com.locationsharing.app.data.friends.LocationUpdateType
import com.locationsharing.app.ui.theme.FFinderAnimations
import kotlinx.coroutines.delay

/**
 * Controller for managing marker animations based on location updates
 * Handles bounce-in, movement, and disappearance animations
 */
class MarkerAnimationController {
    
    private val animationStates = mutableMapOf<String, MarkerAnimationState>()
    
    fun getAnimationState(friendId: String): MarkerAnimationState {
        return animationStates.getOrPut(friendId) { MarkerAnimationState() }
    }
    
    suspend fun processLocationUpdate(update: FriendLocationUpdate) {
        val state = getAnimationState(update.friendId)
        
        when (update.updateType) {
            LocationUpdateType.INITIAL_LOAD -> {
                state.shouldShowAppearAnimation = true
                state.isVisible = true
            }
            
            LocationUpdateType.FRIEND_APPEARED -> {
                state.shouldShowAppearAnimation = true
                state.shouldShowMovementTrail = false
                state.isVisible = true
            }
            
            LocationUpdateType.POSITION_CHANGE -> {
                state.shouldShowMovementTrail = true
                state.isMoving = true
                // Reset movement trail after animation
                delay(1000)
                state.shouldShowMovementTrail = false
                state.isMoving = false
            }
            
            LocationUpdateType.STATUS_CHANGE -> {
                state.shouldPulse = update.isOnline
            }
            
            LocationUpdateType.FRIEND_DISAPPEARED -> {
                state.shouldShowDisappearAnimation = true
                delay(300) // Wait for disappear animation
                state.isVisible = false
            }
        }
    }
    
    fun resetAnimationState(friendId: String) {
        animationStates[friendId] = MarkerAnimationState()
    }
    
    fun clearAllStates() {
        animationStates.clear()
    }
}

/**
 * Animation state for individual markers
 */
data class MarkerAnimationState(
    var isVisible: Boolean = false,
    var shouldShowAppearAnimation: Boolean = false,
    var shouldShowDisappearAnimation: Boolean = false,
    var shouldShowMovementTrail: Boolean = false,
    var shouldPulse: Boolean = true,
    var isMoving: Boolean = false,
    var isSelected: Boolean = false
)

/**
 * Composable wrapper for marker animations
 */
@Composable
fun AnimatedMarkerWrapper(
    friendId: String,
    controller: MarkerAnimationController,
    content: @Composable (MarkerAnimationState) -> Unit
) {
    val animationState = remember { controller.getAnimationState(friendId) }
    var localState by remember { mutableStateOf(animationState) }
    
    // Update local state when animation state changes
    LaunchedEffect(animationState) {
        localState = animationState
    }
    
    content(localState)
}

/**
 * Staggered appearance animation for multiple markers
 */
@Composable
fun StaggeredMarkerAppearance(
    friendIds: List<String>,
    delayBetweenMarkers: Long = 100L,
    onMarkerShouldAppear: (String) -> Unit
) {
    LaunchedEffect(friendIds) {
        friendIds.forEachIndexed { index, friendId ->
            delay(index * delayBetweenMarkers)
            onMarkerShouldAppear(friendId)
        }
    }
}

/**
 * Bounce animation for marker interactions
 */
@Composable
fun rememberBounceAnimation(): Animatable<Float, AnimationVector1D> {
    val bounceAnimation = remember { Animatable(1f) }
    
    return bounceAnimation
}

/**
 * Trigger bounce animation
 */
suspend fun Animatable<Float, AnimationVector1D>.triggerBounce() {
    animateTo(
        targetValue = 1.2f,
        animationSpec = FFinderAnimations.Springs.Bouncy
    )
    animateTo(
        targetValue = 1f,
        animationSpec = FFinderAnimations.Springs.Bouncy
    )
}

/**
 * Marker focus animation for accessibility
 */
@Composable
fun MarkerFocusAnimation(
    isFocused: Boolean,
    content: @Composable (scale: Float, alpha: Float) -> Unit
) {
    val focusScale = remember { Animatable(1f) }
    val focusAlpha = remember { Animatable(1f) }
    
    LaunchedEffect(isFocused) {
        if (isFocused) {
            focusScale.animateTo(
                targetValue = 1.1f,
                animationSpec = FFinderAnimations.MicroInteractions.focus()
            )
            focusAlpha.animateTo(
                targetValue = 0.8f,
                animationSpec = FFinderAnimations.MicroInteractions.focus()
            )
        } else {
            focusScale.animateTo(
                targetValue = 1f,
                animationSpec = FFinderAnimations.MicroInteractions.focus()
            )
            focusAlpha.animateTo(
                targetValue = 1f,
                animationSpec = FFinderAnimations.MicroInteractions.focus()
            )
        }
    }
    
    content(focusScale.value, focusAlpha.value)
}

/**
 * Marker shake animation for errors or attention
 */
@Composable
fun rememberShakeAnimation(): Animatable<Float, AnimationVector1D> {
    return remember { Animatable(0f) }
}

/**
 * Trigger shake animation
 */
suspend fun Animatable<Float, AnimationVector1D>.triggerShake() {
    animateTo(
        targetValue = 0f,
        animationSpec = FFinderAnimations.Error.shake()
    )
}