package com.locationsharing.app.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.TransformOrigin
import com.locationsharing.app.ui.theme.FFinderAnimations

/**
 * Enhanced screen transitions for FFinder app navigation
 * Provides smooth, branded transitions between screens with accessibility support
 */
object FFinderScreenTransitions {
    
    /**
     * Standard horizontal slide transition for main navigation
     */
    fun horizontalSlide(): ContentTransform {
        return slideInHorizontally(
            initialOffsetX = { fullWidth -> fullWidth },
            animationSpec = tween(300)
        ) + fadeIn(
            animationSpec = tween(300)
        ) togetherWith slideOutHorizontally(
            targetOffsetX = { fullWidth -> -fullWidth / 3 },
            animationSpec = tween(150)
        ) + fadeOut(
            animationSpec = tween(150)
        )
    }
    
    /**
     * Vertical slide transition for modal screens
     */
    fun verticalSlide(): ContentTransform {
        return slideInVertically(
            initialOffsetY = { fullHeight -> fullHeight },
            animationSpec = tween(500)
        ) + fadeIn(
            animationSpec = tween(500)
        ) togetherWith slideOutVertically(
            targetOffsetY = { fullHeight -> fullHeight },
            animationSpec = tween(300)
        ) + fadeOut(
            animationSpec = tween(300)
        )
    }
    
    /**
     * Scale transition for focused content
     */
    fun scaleTransition(): ContentTransform {
        return scaleIn(
            initialScale = 0.8f,
            transformOrigin = TransformOrigin.Center,
            animationSpec = FFinderAnimations.Springs.Bouncy
        ) + fadeIn(
            animationSpec = FFinderAnimations.Transitions.screenEnter()
        ) togetherWith scaleOut(
            targetScale = 1.1f,
            transformOrigin = TransformOrigin.Center,
            animationSpec = FFinderAnimations.Springs.Standard
        ) + fadeOut(
            animationSpec = FFinderAnimations.Transitions.screenExit()
        )
    }
    
    /**
     * Fade transition for subtle changes
     */
    fun fadeTransition(): ContentTransform {
        return fadeIn(
            animationSpec = FFinderAnimations.Transitions.screenEnter()
        ) togetherWith fadeOut(
            animationSpec = FFinderAnimations.Transitions.screenExit()
        )
    }
    
    /**
     * Shared element transition for continuity
     */
    fun sharedElementTransition(): ContentTransform {
        return slideInHorizontally(
            initialOffsetX = { fullWidth -> fullWidth / 2 },
            animationSpec = tween(500)
        ) + scaleIn(
            initialScale = 0.9f,
            animationSpec = tween(500)
        ) + fadeIn(
            animationSpec = tween(500)
        ) togetherWith slideOutHorizontally(
            targetOffsetX = { fullWidth -> -fullWidth / 2 },
            animationSpec = tween(500)
        ) + scaleOut(
            targetScale = 1.1f,
            animationSpec = tween(500)
        ) + fadeOut(
            animationSpec = tween(500)
        )
    }
    
    /**
     * Accessibility-aware transition that respects reduced motion preferences
     */
    fun accessibleTransition(reduceMotion: Boolean = false): ContentTransform {
        return if (reduceMotion) {
            fadeIn(
                animationSpec = FFinderAnimations.Accessibility.reducedMotion()
            ) togetherWith fadeOut(
                animationSpec = FFinderAnimations.Accessibility.reducedMotion()
            )
        } else {
            horizontalSlide()
        }
    }
}

/**
 * Navigation transition types for different screen relationships
 */
enum class NavigationTransitionType {
    FORWARD,
    BACKWARD,
    MODAL,
    REPLACE,
    SHARED_ELEMENT
}

/**
 * Get appropriate transition based on navigation type
 */
fun getTransitionForNavigationType(
    type: NavigationTransitionType,
    reduceMotion: Boolean = false
): ContentTransform {
    return when (type) {
        NavigationTransitionType.FORWARD -> {
            if (reduceMotion) {
                FFinderScreenTransitions.accessibleTransition(true)
            } else {
                FFinderScreenTransitions.horizontalSlide()
            }
        }
        NavigationTransitionType.BACKWARD -> {
            if (reduceMotion) {
                FFinderScreenTransitions.accessibleTransition(true)
            } else {
                // Reverse horizontal slide for back navigation
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> -fullWidth / 3 },
                    animationSpec = tween(300)
                ) + fadeIn(
                    animationSpec = tween(300)
                ) togetherWith slideOutHorizontally(
                    targetOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(150)
                ) + fadeOut(
                    animationSpec = tween(150)
                )
            }
        }
        NavigationTransitionType.MODAL -> {
            if (reduceMotion) {
                FFinderScreenTransitions.fadeTransition()
            } else {
                FFinderScreenTransitions.verticalSlide()
            }
        }
        NavigationTransitionType.REPLACE -> {
            FFinderScreenTransitions.fadeTransition()
        }
        NavigationTransitionType.SHARED_ELEMENT -> {
            if (reduceMotion) {
                FFinderScreenTransitions.fadeTransition()
            } else {
                FFinderScreenTransitions.sharedElementTransition()
            }
        }
    }
}

/**
 * Enhanced navigation transition with gesture support
 */
@Composable
fun EnhancedNavigationTransition(
    targetState: String,
    transitionType: NavigationTransitionType = NavigationTransitionType.FORWARD,
    reduceMotion: Boolean = false,
    content: @Composable (String) -> Unit
) {
    androidx.compose.animation.AnimatedContent(
        targetState = targetState,
        transitionSpec = {
            getTransitionForNavigationType(transitionType, reduceMotion)
        },
        label = "navigation_transition"
    ) { state ->
        content(state)
    }
}

/**
 * Staggered content animation for lists and grids
 */
@Composable
fun StaggeredContentAnimation(
    items: List<String>,
    delayBetweenItems: Long = 50L,
    content: @Composable (String, Boolean) -> Unit
) {
    items.forEachIndexed { index, item ->
        val shouldAnimate = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
        
        androidx.compose.runtime.LaunchedEffect(item) {
            kotlinx.coroutines.delay(index * delayBetweenItems)
            shouldAnimate.value = true
        }
        
        content(item, shouldAnimate.value)
    }
}

/**
 * Parallax scroll transition effect
 */
@Composable
fun ParallaxScrollTransition(
    scrollOffset: Float,
    parallaxFactor: Float = 0.5f,
    content: @Composable (Float) -> Unit
) {
    val parallaxOffset = scrollOffset * parallaxFactor
    content(parallaxOffset)
}

/**
 * Morphing transition between different UI states
 */
@Composable
fun MorphingTransition(
    targetState: Boolean,
    content: @Composable (Boolean, Float) -> Unit
) {
    val morphProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (targetState) 1f else 0f,
        animationSpec = FFinderAnimations.Springs.Standard,
        label = "morph_progress"
    )
    
    content(targetState, morphProgress)
}

/**
 * Breathing animation for active states
 */
@Composable
fun BreathingAnimation(
    isActive: Boolean,
    content: @Composable (Float) -> Unit
) {
    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(
        label = "breathing_transition"
    )
    
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = if (isActive) {
            FFinderAnimations.Loading.breathing()
        } else {
            androidx.compose.animation.core.infiniteRepeatable(
                FFinderAnimations.Accessibility.reducedMotion()
            )
        },
        label = "breathing_scale"
    )
    
    content(if (isActive) breathingScale else 1f)
}

/**
 * Ripple effect animation for interactions
 */
@Composable
fun RippleEffectAnimation(
    isTriggered: Boolean,
    onAnimationComplete: () -> Unit = {},
    content: @Composable (Float, Float) -> Unit
) {
    val rippleScale = androidx.compose.runtime.remember { androidx.compose.animation.core.Animatable(0f) }
    val rippleAlpha = androidx.compose.runtime.remember { androidx.compose.animation.core.Animatable(0f) }
    
    androidx.compose.runtime.LaunchedEffect(isTriggered) {
        if (isTriggered) {
            rippleAlpha.animateTo(
                targetValue = 0.6f,
                animationSpec = FFinderAnimations.MicroInteractions.ripple()
            )
            rippleAlpha.animateTo(
                targetValue = 0f,
                animationSpec = FFinderAnimations.MicroInteractions.ripple()
            )
            
            rippleScale.animateTo(
                targetValue = 2f,
                animationSpec = FFinderAnimations.MicroInteractions.ripple()
            )
            rippleScale.snapTo(0f)
            onAnimationComplete()
        }
    }
    
    content(rippleScale.value, rippleAlpha.value)
}

/**
 * Loading state transition with skeleton animation
 */
@Composable
fun LoadingStateTransition(
    isLoading: Boolean,
    content: @Composable (Boolean, Float) -> Unit
) {
    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(
        label = "loading_transition"
    )
    
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = if (isLoading) {
            FFinderAnimations.Loading.shimmer()
        } else {
            androidx.compose.animation.core.infiniteRepeatable(
                FFinderAnimations.Accessibility.reducedMotion()
            )
        },
        label = "shimmer_offset"
    )
    
    content(isLoading, if (isLoading) shimmerOffset else 0f)
}

/**
 * Error state animation with shake effect
 */
@Composable
fun ErrorStateAnimation(
    hasError: Boolean,
    onErrorAnimationComplete: () -> Unit = {},
    content: @Composable (Boolean, Float) -> Unit
) {
    val shakeOffset = androidx.compose.runtime.remember { androidx.compose.animation.core.Animatable(0f) }
    
    androidx.compose.runtime.LaunchedEffect(hasError) {
        if (hasError) {
            shakeOffset.animateTo(
                targetValue = 0f,
                animationSpec = FFinderAnimations.Error.shake()
            )
            onErrorAnimationComplete()
        }
    }
    
    content(hasError, shakeOffset.value)
}