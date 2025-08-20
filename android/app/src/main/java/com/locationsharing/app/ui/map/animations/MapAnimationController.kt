package com.locationsharing.app.ui.map.animations

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.locationsharing.app.data.friends.Friend
import com.locationsharing.app.ui.theme.FFinderAnimations
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Animation quality levels for performance scaling
 */
enum class AnimationQuality {
    LOW,    // Minimal animations, fast performance
    MEDIUM, // Balanced animations and performance
    HIGH    // Full animations, may impact performance on low-end devices
}

/**
 * Enhanced animation performance metrics
 */
data class AnimationPerformanceMetrics(
    val droppedFramePercentage: Float,
    val averageFrameTime: Float,
    val activeAnimations: Int,
    val peakAnimations: Int,
    val totalFrames: Int,
    val sessionDuration: Long
)

/**
 * Enhanced central animation controller for MapScreen micro-animations
 * Manages all animation states and coordinates between different animated components
 * 
 * Implements comprehensive animation management for requirements 8.1, 8.2, 8.3, 8.4, 8.5
 * 
 * New features:
 * - Performance monitoring and optimization
 * - Accessibility-aware animation controls
 * - Coordinated animation sequences
 * - Dynamic quality adjustment
 * - Memory-efficient state management
 */
class MapAnimationController {
    
    // Animation state tracking
    private val _animationStates = mutableStateMapOf<String, AnimationControllerState>()
    val animationStates: Map<String, AnimationControllerState> = _animationStates
    
    // Performance monitoring
    private var _isPerformanceOptimized by mutableStateOf(false)
    val isPerformanceOptimized: Boolean get() = _isPerformanceOptimized
    
    // Animation enablement flags
    private var _areAnimationsEnabled by mutableStateOf(true)
    val areAnimationsEnabled: Boolean get() = _areAnimationsEnabled
    
    // Accessibility and performance settings
    private var _isReducedMotionEnabled by mutableStateOf(false)
    val isReducedMotionEnabled: Boolean get() = _isReducedMotionEnabled
    
    private var _animationQuality by mutableStateOf(AnimationQuality.HIGH)
    val animationQuality: AnimationQuality get() = _animationQuality
    
    // Friend marker position tracking
    private val _friendPositions = mutableStateMapOf<String, Pair<Float, Float>>()
    val friendPositions: Map<String, Pair<Float, Float>> = _friendPositions
    
    // FAB press states
    private val _fabPressStates = mutableStateMapOf<String, Boolean>()
    val fabPressStates: Map<String, Boolean> = _fabPressStates
    
    /**
     * Initialize animation controller with lifecycle awareness
     */
    @Composable
    fun Initialize(lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current) {
        LaunchedEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_PAUSE -> {
                        Timber.d("🎬 MapAnimationController: Pausing animations for performance")
                        pauseAnimations()
                    }
                    Lifecycle.Event.ON_RESUME -> {
                        Timber.d("🎬 MapAnimationController: Resuming animations")
                        resumeAnimations()
                    }
                    Lifecycle.Event.ON_DESTROY -> {
                        Timber.d("🎬 MapAnimationController: Cleaning up animations")
                        cleanup()
                    }
                    else -> { /* No action needed */ }
                }
            }
            
            lifecycleOwner.lifecycle.addObserver(observer)
        }
    }
    
    /**
     * Register a new animation state
     */
    fun registerAnimation(key: String, state: AnimationControllerState) {
        _animationStates[key] = state
        Timber.d("🎬 MapAnimationController: Registered animation '$key'")
    }
    
    /**
     * Update animation state
     */
    fun updateAnimationState(key: String, state: AnimationControllerState) {
        _animationStates[key] = state
    }
    
    /**
     * Remove animation state
     */
    fun unregisterAnimation(key: String) {
        _animationStates.remove(key)
        Timber.d("🎬 MapAnimationController: Unregistered animation '$key'")
    }
    
    /**
     * Update friend marker position for interpolation
     */
    fun updateFriendPosition(friendId: String, x: Float, y: Float) {
        val oldPosition = _friendPositions[friendId]
        val newPosition = Pair(x, y)
        
        if (oldPosition != newPosition) {
            _friendPositions[friendId] = newPosition
            Timber.d("🎬 MapAnimationController: Updated position for friend '$friendId' to ($x, $y)")
        }
    }
    
    /**
     * Set FAB press state for animation
     */
    fun setFABPressed(fabId: String, isPressed: Boolean) {
        _fabPressStates[fabId] = isPressed
        Timber.d("🎬 MapAnimationController: FAB '$fabId' pressed state: $isPressed")
    }
    
    /**
     * Get FAB press state
     */
    fun isFABPressed(fabId: String): Boolean {
        return _fabPressStates[fabId] ?: false
    }
    
    /**
     * Enable or disable animations based on performance
     */
    fun setAnimationsEnabled(enabled: Boolean) {
        _areAnimationsEnabled = enabled
        Timber.d("🎬 MapAnimationController: Animations ${if (enabled) "enabled" else "disabled"}")
    }
    
    /**
     * Enable performance optimization mode
     */
    fun enablePerformanceMode(enabled: Boolean) {
        _isPerformanceOptimized = enabled
        if (enabled) {
            _animationQuality = AnimationQuality.MEDIUM
            Timber.d("🎬 MapAnimationController: Performance mode enabled - reducing animation complexity")
        } else {
            _animationQuality = AnimationQuality.HIGH
            Timber.d("🎬 MapAnimationController: Performance mode disabled - full animations restored")
        }
    }
    
    /**
     * Enable reduced motion for accessibility
     */
    fun enableReducedMotion(enabled: Boolean) {
        _isReducedMotionEnabled = enabled
        if (enabled) {
            _animationQuality = AnimationQuality.LOW
        }
        Timber.d("🎬 MapAnimationController: Reduced motion ${if (enabled) "enabled" else "disabled"}")
    }
    
    /**
     * Set animation quality level
     */
    fun setAnimationQuality(quality: AnimationQuality) {
        _animationQuality = quality
        Timber.d("🎬 MapAnimationController: Animation quality set to $quality")
    }
    
    /**
     * Auto-adjust animation quality based on performance
     */
    fun autoAdjustQuality() {
        val metrics = com.locationsharing.app.ui.map.animations.AnimationPerformanceMonitor.getPerformanceMetrics()
        
        val newQuality = when {
            metrics.droppedFramePercentage > 15f -> AnimationQuality.LOW
            metrics.droppedFramePercentage > 8f -> AnimationQuality.MEDIUM
            else -> AnimationQuality.HIGH
        }
        
        if (newQuality != _animationQuality) {
            setAnimationQuality(newQuality)
            Timber.d("🎬 MapAnimationController: Auto-adjusted quality to $newQuality (dropped frames: ${metrics.droppedFramePercentage}%)")
        }
    }
    
    /**
     * Pause all animations
     */
    private fun pauseAnimations() {
        _areAnimationsEnabled = false
        // Additional logic to pause ongoing animations could be added here
    }
    
    /**
     * Resume all animations
     */
    private fun resumeAnimations() {
        _areAnimationsEnabled = true
        // Additional logic to resume paused animations could be added here
    }
    
    /**
     * Cleanup all animation resources
     */
    private fun cleanup() {
        _animationStates.clear()
        _friendPositions.clear()
        _fabPressStates.clear()
        Timber.d("🎬 MapAnimationController: Cleanup completed")
    }
    
    /**
     * Get animation performance metrics
     */
    fun getPerformanceMetrics(): AnimationPerformanceMetrics {
        return AnimationPerformanceMetrics(
            droppedFramePercentage = 0f, // Will be updated by performance monitor
            averageFrameTime = 16.67f, // 60fps target
            activeAnimations = _animationStates.size,
            peakAnimations = _animationStates.size,
            totalFrames = 0,
            sessionDuration = 0L
        )
    }
    
    /**
     * Stagger friend marker animations for smooth appearance
     */
    suspend fun staggerFriendMarkerAnimations(friends: List<Friend>, delayMs: Long = 50L) {
        friends.forEachIndexed { index, friend ->
            delay(index * delayMs)
            updateAnimationState(
                key = "friend_${friend.id}",
                state = AnimationControllerState.APPEARING
            )
        }
    }
    
    /**
     * Animate friend marker removal with staggering
     */
    suspend fun staggerFriendMarkerRemoval(friendIds: List<String>, delayMs: Long = 30L) {
        friendIds.forEachIndexed { index, friendId ->
            delay(index * delayMs)
            updateAnimationState(
                key = "friend_$friendId",
                state = AnimationControllerState.DISAPPEARING
            )
        }
    }
    
    /**
     * Enhanced coordinate drawer and sheet animations to avoid conflicts
     */
    fun coordinateOverlayAnimations(
        isDrawerOpen: Boolean,
        isSheetVisible: Boolean
    ) {
        when {
            isDrawerOpen && isSheetVisible -> {
                // Prioritize drawer, fade out sheet with stagger
                com.locationsharing.app.ui.map.animations.AnimationCoordinator.startSequence(
                    id = "overlay_conflict_resolution",
                    sequence = com.locationsharing.app.ui.map.animations.AnimationSequence(
                        steps = listOf(
                            com.locationsharing.app.ui.map.animations.AnimationStep(delay = 0L, duration = 150L),
                            com.locationsharing.app.ui.map.animations.AnimationStep(delay = 100L, duration = 300L)
                        )
                    )
                )
                updateAnimationState("drawer", AnimationControllerState.ACTIVE)
                updateAnimationState("sheet", AnimationControllerState.FADING_OUT)
            }
            isDrawerOpen -> {
                updateAnimationState("drawer", AnimationControllerState.ACTIVE)
                updateAnimationState("sheet", AnimationControllerState.IDLE)
            }
            isSheetVisible -> {
                updateAnimationState("drawer", AnimationControllerState.IDLE)
                updateAnimationState("sheet", AnimationControllerState.ACTIVE)
            }
            else -> {
                updateAnimationState("drawer", AnimationControllerState.IDLE)
                updateAnimationState("sheet", AnimationControllerState.IDLE)
            }
        }
    }
    
    /**
     * Coordinate FAB animations to prevent conflicts
     */
    fun coordinateFABAnimations(
        activeFABs: List<String>,
        pressedFAB: String? = null
    ) {
        activeFABs.forEach { fabId ->
            val state = when {
                fabId == pressedFAB -> AnimationControllerState.SCALING
                pressedFAB != null -> AnimationControllerState.IDLE // Dim other FABs
                else -> AnimationControllerState.ACTIVE
            }
            updateAnimationState("fab_$fabId", state)
        }
    }
    
    /**
     * Coordinate friend marker animations for smooth group updates
     */
    suspend fun coordinateFriendMarkerUpdates(
        updates: Map<String, Pair<Float, Float>>,
        staggerDelay: Long = 30L
    ) {
        updates.entries.forEachIndexed { index, (friendId, position) ->
            delay(index * staggerDelay)
            updateFriendPosition(friendId, position.first, position.second)
            updateAnimationState("friend_${friendId}_move", AnimationControllerState.MOVING)
        }
    }
    
    /**
     * Start coordinated entrance animation sequence
     */
    suspend fun startEntranceSequence() {
        com.locationsharing.app.ui.map.animations.AnimationCoordinator.startSequence(
            id = "map_entrance",
            sequence = com.locationsharing.app.ui.map.animations.AnimationSequence(
                steps = listOf(
                    // Location marker appears first
                    com.locationsharing.app.ui.map.animations.AnimationStep(delay = 0L, duration = 300L),
                    // FABs appear with stagger
                    com.locationsharing.app.ui.map.animations.AnimationStep(delay = 200L, duration = 400L),
                    // Friend markers appear last
                    com.locationsharing.app.ui.map.animations.AnimationStep(delay = 400L, duration = 500L)
                )
            )
        )
        
        // Update states in sequence
        updateAnimationState("location_marker", AnimationControllerState.APPEARING)
        delay(200L)
        updateAnimationState("fabs", AnimationControllerState.APPEARING)
        delay(200L)
        updateAnimationState("friend_markers", AnimationControllerState.APPEARING)
    }
}

/**
 * Animation state enumeration for controller
 */
enum class AnimationControllerState {
    IDLE,
    ACTIVE,
    APPEARING,
    DISAPPEARING,
    FADING_IN,
    FADING_OUT,
    SCALING,
    MOVING,
    PULSING,
    BREATHING,
    ERROR
}

/**
 * Composable wrapper for MapAnimationController
 */
@Composable
fun rememberMapAnimationController(): MapAnimationController {
    val controller = remember { MapAnimationController() }
    controller.Initialize()
    return controller
}

/**
 * Animation configuration based on device performance
 */
object AnimationConfig {
    
    /**
     * Determine if device can handle full animations
     */
    fun shouldUseFullAnimations(): Boolean {
        // Simple heuristic - in a real app, you'd check device specs
        return Runtime.getRuntime().availableProcessors() >= 4
    }
    
    /**
     * Get animation duration multiplier based on performance
     */
    fun getDurationMultiplier(): Float {
        return if (shouldUseFullAnimations()) 1.0f else 0.7f
    }
    
    /**
     * Get maximum concurrent animations
     */
    fun getMaxConcurrentAnimations(): Int {
        return if (shouldUseFullAnimations()) 20 else 10
    }
}

/**
 * Extension functions for easy animation management
 */
fun MapAnimationController.animateFriendAppearance(friend: Friend) {
    registerAnimation(
        key = "friend_${friend.id}_appear",
        state = AnimationControllerState.APPEARING
    )
}

fun MapAnimationController.animateFriendDisappearance(friendId: String) {
    updateAnimationState(
        key = "friend_${friendId}_appear",
        state = AnimationControllerState.DISAPPEARING
    )
}

fun MapAnimationController.animateFriendMovement(friendId: String, fromX: Float, fromY: Float, toX: Float, toY: Float) {
    updateFriendPosition(friendId, toX, toY)
    updateAnimationState(
        key = "friend_${friendId}_move",
        state = AnimationControllerState.MOVING
    )
}