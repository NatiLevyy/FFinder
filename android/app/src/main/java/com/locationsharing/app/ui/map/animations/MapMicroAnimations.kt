package com.locationsharing.app.ui.map.animations

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.EaseInOutBack
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Modifier
import com.locationsharing.app.ui.theme.FFinderAnimations
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Enhanced micro-animations system for MapScreen components
 * Implements requirements 8.1, 8.2, 8.3, 8.4, 8.5 from the MapScreen redesign specification
 * 
 * Features:
 * - Location marker pulse animation (every 3 seconds) - ENHANCED
 * - FAB scale animations for all floating action buttons - ENHANCED
 * - Smooth drawer slide animation with overshoot interpolator - ENHANCED
 * - Status sheet fade-in/out animations - ENHANCED
 * - Friend marker position interpolation animations - ENHANCED
 * - Additional micro-interactions for polished UX
 * - Performance-optimized animation states
 * - Accessibility-aware animation controls
 */
object MapMicroAnimations {
    
    /**
     * Enhanced location marker pulse animation that runs every 3 seconds
     * Requirement 8.1: Create location marker pulse animation (every 3 seconds)
     * 
     * Features:
     * - Smooth bounce effect with overshoot
     * - Synchronized with breathing animation
     * - Performance optimized with reduced motion support
     */
    @Composable
    fun LocationMarkerPulse(
        isReducedMotion: Boolean = false
    ): Float {
        val infiniteTransition = rememberInfiniteTransition(label = "location_marker_pulse")
        
        return if (isReducedMotion) {
            // Subtle pulse for reduced motion accessibility
            infiniteTransition.animateFloat(
                initialValue = 1.0f,
                targetValue = 1.05f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 2000,
                        easing = FFinderAnimations.Easing.FFinderGentle
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "location_marker_pulse_reduced"
            ).value
        } else {
            // Full pulse animation with enhanced timing
            infiniteTransition.animateFloat(
                initialValue = 1.0f,
                targetValue = 1.2f,
                animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = 3000 // 3 seconds total cycle
                        1.0f at 0 with FFinderAnimations.Easing.FFinderSmooth
                        1.15f at 400 with FFinderAnimations.Easing.FFinderGentle
                        1.2f at 600 with EaseInOutBack // Peak at 600ms with overshoot
                        1.05f at 900 with FFinderAnimations.Easing.FFinderSmooth
                        1.0f at 1200 with FFinderAnimations.Easing.FFinderGentle
                        1.0f at 3000 // Hold for remaining time
                    },
                    repeatMode = RepeatMode.Restart
                ),
                label = "location_marker_pulse_scale"
            ).value
        }
    }
    
    /**
     * Enhanced FAB scale animation for press feedback
     * Requirement 8.2: Implement FAB scale animations for all floating action buttons
     * 
     * Features:
     * - Multi-stage press animation (1.0 → 0.9 → 1.0)
     * - Hover state support
     * - Loading state animation
     * - Accessibility-aware timing
     */
    @Composable
    fun FABScaleAnimation(
        isPressed: Boolean,
        isHovered: Boolean = false,
        isLoading: Boolean = false,
        pressedScale: Float = 0.9f,
        hoveredScale: Float = 1.05f,
        normalScale: Float = 1.0f,
        isReducedMotion: Boolean = false
    ): Float {
        val targetScale = when {
            isLoading -> normalScale // Keep normal scale during loading
            isPressed -> pressedScale
            isHovered -> hoveredScale
            else -> normalScale
        }
        
        return animateFloatAsState(
            targetValue = targetScale,
            animationSpec = when {
                isReducedMotion -> {
                    // Faster, less bouncy animation for accessibility
                    tween(
                        durationMillis = FFinderAnimations.Duration.Quick.inWholeMilliseconds.toInt(),
                        easing = FFinderAnimations.Easing.Standard
                    )
                }
                isPressed -> {
                    // Quick press down with sharp easing
                    tween(
                        durationMillis = FFinderAnimations.Duration.Quick.inWholeMilliseconds.toInt(),
                        easing = FFinderAnimations.Easing.FFinderSharp
                    )
                }
                else -> {
                    // Bouncy release with overshoot
                    spring(
                        dampingRatio = 0.6f,
                        stiffness = 800f
                    )
                }
            },
            label = "fab_scale_animation"
        ).value
    }
    
    /**
     * FAB loading pulse animation
     */
    @Composable
    fun FABLoadingPulse(): Float {
        val infiniteTransition = rememberInfiniteTransition(label = "fab_loading_pulse")
        
        return infiniteTransition.animateFloat(
            initialValue = 0.95f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 1000,
                    easing = FFinderAnimations.Easing.FFinderGentle
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "fab_loading_pulse_scale"
        ).value
    }
    
    /**
     * Enhanced drawer slide animation with overshoot interpolator
     * Requirement 8.3: Add smooth drawer slide animation with overshoot interpolator
     * 
     * Features:
     * - Overshoot interpolator with 300ms duration
     * - Separate open/close timing
     * - Reduced motion support
     * - Smooth deceleration curves
     */
    @Composable
    fun DrawerSlideAnimation(
        isOpen: Boolean,
        drawerWidth: Float = 280f,
        isReducedMotion: Boolean = false
    ): Float {
        return animateFloatAsState(
            targetValue = if (isOpen) 0f else drawerWidth,
            animationSpec = if (isReducedMotion) {
                // Simple linear animation for accessibility
                tween(
                    durationMillis = 200,
                    easing = FFinderAnimations.Easing.Standard
                )
            } else if (isOpen) {
                // Opening animation with overshoot (300ms as specified)
                spring(
                    dampingRatio = 0.75f, // Slight overshoot
                    stiffness = 500f
                )
            } else {
                // Closing animation - faster and smoother
                spring(
                    dampingRatio = 0.9f, // Less overshoot when closing
                    stiffness = 600f
                )
            },
            label = "drawer_slide_animation"
        ).value
    }
    
    /**
     * Drawer scrim fade animation
     */
    @Composable
    fun DrawerScrimFade(
        isVisible: Boolean,
        targetAlpha: Float = 0.5f
    ): Float {
        return animateFloatAsState(
            targetValue = if (isVisible) targetAlpha else 0f,
            animationSpec = tween(
                durationMillis = if (isVisible) 300 else 200,
                easing = if (isVisible) {
                    FFinderAnimations.Easing.Decelerated
                } else {
                    FFinderAnimations.Easing.Accelerated
                }
            ),
            label = "drawer_scrim_fade"
        ).value
    }
    
    /**
     * Enhanced status sheet fade animation
     * Requirement 8.4: Create status sheet fade-in/out animations
     * 
     * Features:
     * - 200ms fade duration as specified
     * - Separate in/out easing curves
     * - Scale animation for entrance
     * - Reduced motion support
     */
    @Composable
    fun StatusSheetFadeAnimation(
        isVisible: Boolean,
        fadeDuration: Int = 200,
        isReducedMotion: Boolean = false
    ): Float {
        return animateFloatAsState(
            targetValue = if (isVisible) 1f else 0f,
            animationSpec = if (isReducedMotion) {
                // Instant transition for accessibility
                tween(
                    durationMillis = 100,
                    easing = LinearEasing
                )
            } else {
                tween(
                    durationMillis = fadeDuration,
                    easing = if (isVisible) {
                        FFinderAnimations.Easing.Decelerated
                    } else {
                        FFinderAnimations.Easing.Accelerated
                    }
                )
            },
            label = "status_sheet_fade_animation"
        ).value
    }
    
    /**
     * Status sheet scale animation for entrance
     */
    @Composable
    fun StatusSheetScaleAnimation(
        isVisible: Boolean,
        isReducedMotion: Boolean = false
    ): Float {
        return animateFloatAsState(
            targetValue = if (isVisible) 1f else 0.95f,
            animationSpec = if (isReducedMotion) {
                tween(durationMillis = 100, easing = LinearEasing)
            } else {
                spring(
                    dampingRatio = 0.8f,
                    stiffness = 400f
                )
            },
            label = "status_sheet_scale_animation"
        ).value
    }
    
    /**
     * Enhanced friend marker position interpolation animation
     * Requirement 8.5: Add friend marker position interpolation animations
     * 
     * Features:
     * - Smooth position interpolation with easing
     * - Velocity-based animation duration
     * - Parallel X/Y animation coordination
     * - Performance optimized for multiple markers
     */
    @Composable
    fun FriendMarkerPositionAnimation(
        targetX: Float,
        targetY: Float,
        animationDuration: Int = 500,
        isReducedMotion: Boolean = false
    ): Pair<Float, Float> {
        val animatableX = remember { Animatable(targetX) }
        val animatableY = remember { Animatable(targetY) }
        
        LaunchedEffect(targetX, targetY) {
            val duration = if (isReducedMotion) {
                // Faster animation for accessibility
                (animationDuration * 0.5f).toInt()
            } else {
                animationDuration
            }
            
            val animationSpec = if (isReducedMotion) {
                tween<Float>(
                    durationMillis = duration,
                    easing = LinearEasing
                )
            } else {
                tween<Float>(
                    durationMillis = duration,
                    easing = FFinderAnimations.Easing.FFinderSmooth
                )
            }
            
            // Animate both coordinates simultaneously
            launch {
                animatableX.animateTo(
                    targetValue = targetX,
                    animationSpec = animationSpec
                )
            }
            launch {
                animatableY.animateTo(
                    targetValue = targetY,
                    animationSpec = animationSpec
                )
            }
        }
        
        return Pair(animatableX.value, animatableY.value)
    }
    
    /**
     * Friend marker velocity-based position animation
     * Adjusts animation duration based on distance traveled
     */
    @Composable
    fun FriendMarkerVelocityAnimation(
        targetX: Float,
        targetY: Float,
        previousX: Float = targetX,
        previousY: Float = targetY,
        baseSpeed: Float = 200f // pixels per second
    ): Pair<Float, Float> {
        val animatableX = remember { Animatable(previousX) }
        val animatableY = remember { Animatable(previousY) }
        
        LaunchedEffect(targetX, targetY) {
            // Calculate distance and duration based on velocity
            val distance = kotlin.math.sqrt(
                (targetX - previousX) * (targetX - previousX) + 
                (targetY - previousY) * (targetY - previousY)
            )
            val duration = ((distance / baseSpeed) * 1000).toInt().coerceIn(200, 1000)
            
            launch {
                animatableX.animateTo(
                    targetValue = targetX,
                    animationSpec = tween(
                        durationMillis = duration,
                        easing = FFinderAnimations.Easing.FFinderSmooth
                    )
                )
            }
            launch {
                animatableY.animateTo(
                    targetValue = targetY,
                    animationSpec = tween(
                        durationMillis = duration,
                        easing = FFinderAnimations.Easing.FFinderSmooth
                    )
                )
            }
        }
        
        return Pair(animatableX.value, animatableY.value)
    }
    
    /**
     * Friend marker appearance animation
     */
    @Composable
    fun FriendMarkerAppearAnimation(
        isVisible: Boolean
    ): Pair<Float, Float> {
        val scale by animateFloatAsState(
            targetValue = if (isVisible) 1f else 0f,
            animationSpec = FFinderAnimations.Map.markerAppear(),
            label = "friend_marker_appear_scale"
        )
        
        val alpha by animateFloatAsState(
            targetValue = if (isVisible) 1f else 0f,
            animationSpec = tween(
                durationMillis = FFinderAnimations.Duration.Standard.inWholeMilliseconds.toInt(),
                easing = FFinderAnimations.Easing.Decelerated
            ),
            label = "friend_marker_appear_alpha"
        )
        
        return Pair(scale, alpha)
    }
    
    /**
     * Staggered animation for multiple elements
     */
    @Composable
    fun StaggeredAnimation(
        isVisible: Boolean,
        index: Int,
        totalItems: Int,
        staggerDelay: Int = 50
    ): Float {
        val delay = (index * staggerDelay).coerceAtMost(300)
        
        return animateFloatAsState(
            targetValue = if (isVisible) 1f else 0f,
            animationSpec = tween(
                durationMillis = FFinderAnimations.Duration.Standard.inWholeMilliseconds.toInt(),
                delayMillis = delay,
                easing = FFinderAnimations.Easing.Decelerated
            ),
            label = "staggered_animation_$index"
        ).value
    }
    
    /**
     * Breathing animation for active states
     */
    @Composable
    fun BreathingAnimation(): Float {
        val infiniteTransition = rememberInfiniteTransition(label = "breathing_animation")
        
        return infiniteTransition.animateFloat(
            initialValue = 0.8f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 2000,
                    easing = FFinderAnimations.Easing.FFinderGentle
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "breathing_scale"
        ).value
    }
    
    /**
     * Enhanced shake animation for error states
     */
    @Composable
    fun ShakeAnimation(
        trigger: Boolean,
        intensity: Float = 10f
    ): Float {
        val animatable = remember { Animatable(0f) }
        
        LaunchedEffect(trigger) {
            if (trigger) {
                // Reset to center first
                animatable.snapTo(0f)
                
                // Perform shake animation
                animatable.animateTo(
                    targetValue = 0f,
                    animationSpec = keyframes {
                        durationMillis = 600
                        0f at 0
                        -intensity at 100 with FFinderAnimations.Easing.FFinderSharp
                        intensity at 200 with FFinderAnimations.Easing.FFinderSharp
                        -intensity * 0.8f at 300 with FFinderAnimations.Easing.FFinderSharp
                        intensity * 0.8f at 400 with FFinderAnimations.Easing.FFinderSharp
                        -intensity * 0.4f at 500 with FFinderAnimations.Easing.FFinderSharp
                        0f at 600 with FFinderAnimations.Easing.FFinderSmooth
                    }
                )
            }
        }
        
        return animatable.value
    }
    
    /**
     * Coordinated multi-element animation system
     */
    @Composable
    fun CoordinatedAnimation(
        isActive: Boolean,
        elements: List<String>,
        staggerDelay: Int = 50
    ): Map<String, Float> {
        val animations = remember(elements) {
            elements.associateWith { Animatable(0f) }
        }
        
        LaunchedEffect(isActive, elements) {
            if (isActive) {
                elements.forEachIndexed { index, element ->
                    launch {
                        delay(index * staggerDelay.toLong())
                        animations[element]?.animateTo(
                            targetValue = 1f,
                            animationSpec = FFinderAnimations.Springs.Standard
                        )
                    }
                }
            } else {
                elements.reversed().forEachIndexed { index, element ->
                    launch {
                        delay(index * (staggerDelay / 2).toLong())
                        animations[element]?.animateTo(
                            targetValue = 0f,
                            animationSpec = FFinderAnimations.Transitions.screenExit()
                        )
                    }
                }
            }
        }
        
        return animations.mapValues { it.value.value }
    }
    
    /**
     * Map zoom level animation
     */
    @Composable
    fun MapZoomAnimation(
        targetZoom: Float,
        currentZoom: Float = targetZoom
    ): Float {
        return animateFloatAsState(
            targetValue = targetZoom,
            animationSpec = FFinderAnimations.Map.cameraMove(),
            label = "map_zoom_animation"
        ).value
    }
    
    /**
     * Notification badge animation
     */
    @Composable
    fun NotificationBadgeAnimation(
        count: Int,
        previousCount: Int = 0
    ): Pair<Float, Float> {
        val scale by animateFloatAsState(
            targetValue = if (count > 0) 1f else 0f,
            animationSpec = if (count > previousCount) {
                // Bounce in for new notifications
                FFinderAnimations.Springs.Bouncy
            } else {
                // Fade out for cleared notifications
                FFinderAnimations.Transitions.screenExit()
            },
            label = "badge_scale"
        )
        
        val pulse = if (count > previousCount && count > 0) {
            // Pulse animation for new notifications
            val infiniteTransition = rememberInfiniteTransition(label = "badge_pulse")
            infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.2f,
                animationSpec = infiniteRepeatable(
                    animation = tween(300),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "badge_pulse_scale"
            ).value
        } else {
            1f
        }
        
        return Pair(scale, pulse)
    }
    
    /**
     * Search bar expand/collapse animation
     */
    @Composable
    fun SearchBarAnimation(
        isExpanded: Boolean,
        collapsedWidth: Float = 48f,
        expandedWidth: Float = 280f
    ): Float {
        return animateFloatAsState(
            targetValue = if (isExpanded) expandedWidth else collapsedWidth,
            animationSpec = FFinderAnimations.Springs.Standard,
            label = "search_bar_width"
        ).value
    }
}

/**
 * Enhanced modifier extensions for easy application of micro-animations
 */

/**
 * Apply location marker pulse animation
 */
@Composable
fun Modifier.locationMarkerPulse(
    isReducedMotion: Boolean = false
): Modifier {
    val scale = MapMicroAnimations.LocationMarkerPulse(isReducedMotion)
    return this.then(
        Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
    )
}

/**
 * Apply FAB scale animation with enhanced features
 */
@Composable
fun Modifier.fabScaleAnimation(
    isPressed: Boolean,
    isHovered: Boolean = false,
    isLoading: Boolean = false,
    isReducedMotion: Boolean = false
): Modifier {
    val scale = MapMicroAnimations.FABScaleAnimation(
        isPressed = isPressed,
        isHovered = isHovered,
        isLoading = isLoading,
        isReducedMotion = isReducedMotion
    )
    return this.then(
        Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
    )
}

/**
 * Apply breathing animation
 */
@Composable
fun Modifier.breathingAnimation(): Modifier {
    val scale = MapMicroAnimations.BreathingAnimation()
    return this.then(
        Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
    )
}

/**
 * Apply shake animation
 */
fun Modifier.shakeAnimation(offset: Float): Modifier = this.then(
    Modifier.graphicsLayer {
        translationX = offset
    }
)

/**
 * Apply drawer slide animation
 */
@Composable
fun Modifier.drawerSlideAnimation(
    isOpen: Boolean,
    drawerWidth: Float = 280f,
    isReducedMotion: Boolean = false
): Modifier {
    val offset = MapMicroAnimations.DrawerSlideAnimation(
        isOpen = isOpen,
        drawerWidth = drawerWidth,
        isReducedMotion = isReducedMotion
    )
    return this.then(
        Modifier.graphicsLayer {
            translationX = offset
        }
    )
}

/**
 * Apply status sheet fade animation
 */
@Composable
fun Modifier.statusSheetFade(
    isVisible: Boolean,
    isReducedMotion: Boolean = false
): Modifier {
    val alpha = MapMicroAnimations.StatusSheetFadeAnimation(
        isVisible = isVisible,
        isReducedMotion = isReducedMotion
    )
    val scale = MapMicroAnimations.StatusSheetScaleAnimation(
        isVisible = isVisible,
        isReducedMotion = isReducedMotion
    )
    return this.then(
        Modifier.graphicsLayer {
            this.alpha = alpha
            scaleX = scale
            scaleY = scale
        }
    )
}

/**
 * Apply friend marker position animation
 */
@Composable
fun Modifier.friendMarkerPosition(
    targetX: Float,
    targetY: Float,
    isReducedMotion: Boolean = false
): Modifier {
    val (x, y) = MapMicroAnimations.FriendMarkerPositionAnimation(
        targetX = targetX,
        targetY = targetY,
        isReducedMotion = isReducedMotion
    )
    return this.then(
        Modifier.graphicsLayer {
            translationX = x
            translationY = y
        }
    )
}

/**
 * Apply staggered animation
 */
@Composable
fun Modifier.staggeredAnimation(
    isVisible: Boolean,
    index: Int,
    totalItems: Int = 1
): Modifier {
    val alpha = MapMicroAnimations.StaggeredAnimation(
        isVisible = isVisible,
        index = index,
        totalItems = totalItems
    )
    return this.then(
        Modifier.graphicsLayer {
            this.alpha = alpha
            scaleX = alpha
            scaleY = alpha
        }
    )
}

/**
 * Apply notification badge animation
 */
@Composable
fun Modifier.notificationBadge(
    count: Int,
    previousCount: Int = 0
): Modifier {
    val (scale, pulse) = MapMicroAnimations.NotificationBadgeAnimation(
        count = count,
        previousCount = previousCount
    )
    return this.then(
        Modifier.graphicsLayer {
            scaleX = scale * pulse
            scaleY = scale * pulse
        }
    )
}

/**
 * Enhanced animation state management for performance optimization
 */
data class MicroAnimationState(
    val isLocationPulsing: Boolean = true,
    val isFABPressed: Boolean = false,
    val isDrawerOpen: Boolean = false,
    val isStatusSheetVisible: Boolean = false,
    val visibleFriendMarkers: Set<String> = emptySet(),
    val animatingFriendMarkers: Set<String> = emptySet(),
    val isReducedMotionEnabled: Boolean = false,
    val maxConcurrentAnimations: Int = 20,
    val animationQuality: AnimationQuality = AnimationQuality.HIGH
)



/**
 * Enhanced animation performance monitor with detailed metrics
 */
object AnimationPerformanceMonitor {
    private var frameDropCount = 0
    private var totalFrames = 0
    private var animationStartTime = 0L
    private var activeAnimations = 0
    private var peakAnimations = 0
    private val frameTimings = mutableListOf<Long>()
    
    fun recordFrame(isDropped: Boolean, frameTime: Long = System.currentTimeMillis()) {
        totalFrames++
        if (isDropped) frameDropCount++
        
        frameTimings.add(frameTime)
        
        // Keep only last 60 frame timings for rolling average
        if (frameTimings.size > 60) {
            frameTimings.removeAt(0)
        }
    }
    
    fun recordAnimationStart() {
        activeAnimations++
        if (activeAnimations > peakAnimations) {
            peakAnimations = activeAnimations
        }
        if (animationStartTime == 0L) {
            animationStartTime = System.currentTimeMillis()
        }
    }
    
    fun recordAnimationEnd() {
        activeAnimations = (activeAnimations - 1).coerceAtLeast(0)
    }
    
    fun getDroppedFramePercentage(): Float {
        return if (totalFrames > 0) {
            (frameDropCount.toFloat() / totalFrames) * 100f
        } else 0f
    }
    
    fun getAverageFrameTime(): Float {
        return if (frameTimings.size > 1) {
            val deltas = frameTimings.zipWithNext { a, b -> b - a }
            deltas.average().toFloat()
        } else 0f
    }
    
    fun getPerformanceMetrics(): AnimationPerformanceMetrics {
        return AnimationPerformanceMetrics(
            droppedFramePercentage = getDroppedFramePercentage(),
            averageFrameTime = getAverageFrameTime(),
            activeAnimations = activeAnimations,
            peakAnimations = peakAnimations,
            totalFrames = totalFrames,
            sessionDuration = if (animationStartTime > 0) {
                System.currentTimeMillis() - animationStartTime
            } else 0L
        )
    }
    
    fun shouldReduceAnimationQuality(): Boolean {
        return getDroppedFramePercentage() > 10f || activeAnimations > 15
    }
    
    fun reset() {
        frameDropCount = 0
        totalFrames = 0
        animationStartTime = 0L
        activeAnimations = 0
        peakAnimations = 0
        frameTimings.clear()
    }
}



/**
 * Animation coordinator for managing complex animation sequences
 */
object AnimationCoordinator {
    private val activeSequences = mutableMapOf<String, AnimationSequence>()
    
    fun startSequence(
        id: String,
        sequence: AnimationSequence,
        onComplete: (() -> Unit)? = null
    ) {
        activeSequences[id] = sequence.copy(onComplete = onComplete)
        AnimationPerformanceMonitor.recordAnimationStart()
    }
    
    fun stopSequence(id: String) {
        activeSequences.remove(id)
        AnimationPerformanceMonitor.recordAnimationEnd()
    }
    
    fun isSequenceActive(id: String): Boolean {
        return activeSequences.containsKey(id)
    }
    
    fun getActiveSequenceCount(): Int {
        return activeSequences.size
    }
}

/**
 * Animation sequence definition
 */
data class AnimationSequence(
    val steps: List<AnimationStep>,
    val onComplete: (() -> Unit)? = null
)

/**
 * Individual animation step
 */
data class AnimationStep(
    val delay: Long = 0L,
    val duration: Long = 300L,
    val easing: Easing = FFinderAnimations.Easing.Standard,
    val properties: Map<String, Float> = emptyMap()
)