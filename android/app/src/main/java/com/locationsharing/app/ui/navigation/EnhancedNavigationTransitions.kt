package com.locationsharing.app.ui.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.locationsharing.app.ui.components.feedback.NavigationLoadingOverlay
import com.locationsharing.app.ui.components.feedback.VisualFeedbackManager
import com.locationsharing.app.ui.theme.FFinderAnimations
import kotlinx.coroutines.delay

/**
 * Enhanced navigation transitions with visual feedback and loading states
 */
object EnhancedNavigationTransitions {
    
    /**
     * Navigation transition with loading indicator
     */
    @Composable
    fun NavigationWithLoading(
        targetState: String,
        isNavigating: Boolean,
        visualFeedbackManager: VisualFeedbackManager,
        transitionType: NavigationTransitionType = NavigationTransitionType.FORWARD,
        reduceMotion: Boolean = false,
        content: @Composable (String) -> Unit
    ) {
        val feedbackState by visualFeedbackManager.feedbackState.collectAsState()
        
        Box(modifier = Modifier.fillMaxSize()) {
            // Main content with transitions
            AnimatedContent(
                targetState = targetState,
                transitionSpec = {
                    getEnhancedTransitionForNavigationType(
                        transitionType, 
                        reduceMotion,
                        isNavigating
                    )
                },
                label = "enhanced_navigation_transition"
            ) { state ->
                content(state)
            }
            
            // Loading overlay
            NavigationLoadingOverlay(isNavigating = isNavigating) { showOverlay, alpha ->
                if (showOverlay) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(alpha)
                            .background(Color.Black.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 4.dp
                        )
                    }
                }
            }
        }
    }
    
    /**
     * Enhanced transition with feedback integration
     */
    private fun getEnhancedTransitionForNavigationType(
        type: NavigationTransitionType,
        reduceMotion: Boolean,
        isNavigating: Boolean
    ): ContentTransform {
        val duration = if (isNavigating) 500 else 300
        
        return when (type) {
            NavigationTransitionType.FORWARD -> {
                if (reduceMotion) {
                    fadeIn(
                        animationSpec = tween(duration)
                    ) togetherWith fadeOut(
                        animationSpec = tween(duration / 2)
                    )
                } else {
                    slideInHorizontally(
                        initialOffsetX = { fullWidth -> fullWidth },
                        animationSpec = tween(duration, easing = FFinderAnimations.Easing.Decelerated)
                    ) + fadeIn(
                        animationSpec = tween(duration)
                    ) + scaleIn(
                        initialScale = 0.95f,
                        animationSpec = tween(duration)
                    ) togetherWith slideOutHorizontally(
                        targetOffsetX = { fullWidth -> -fullWidth / 3 },
                        animationSpec = tween(duration / 2, easing = FFinderAnimations.Easing.Accelerated)
                    ) + fadeOut(
                        animationSpec = tween(duration / 2)
                    ) + scaleOut(
                        targetScale = 1.05f,
                        animationSpec = tween(duration / 2)
                    )
                }
            }
            NavigationTransitionType.BACKWARD -> {
                if (reduceMotion) {
                    fadeIn(
                        animationSpec = tween(duration)
                    ) togetherWith fadeOut(
                        animationSpec = tween(duration / 2)
                    )
                } else {
                    slideInHorizontally(
                        initialOffsetX = { fullWidth -> -fullWidth / 3 },
                        animationSpec = tween(duration, easing = FFinderAnimations.Easing.Decelerated)
                    ) + fadeIn(
                        animationSpec = tween(duration)
                    ) + scaleIn(
                        initialScale = 1.05f,
                        animationSpec = tween(duration)
                    ) togetherWith slideOutHorizontally(
                        targetOffsetX = { fullWidth -> fullWidth },
                        animationSpec = tween(duration / 2, easing = FFinderAnimations.Easing.Accelerated)
                    ) + fadeOut(
                        animationSpec = tween(duration / 2)
                    ) + scaleOut(
                        targetScale = 0.95f,
                        animationSpec = tween(duration / 2)
                    )
                }
            }
            NavigationTransitionType.MODAL -> {
                if (reduceMotion) {
                    fadeIn(
                        animationSpec = tween(duration)
                    ) togetherWith fadeOut(
                        animationSpec = tween(duration)
                    )
                } else {
                    slideInVertically(
                        animationSpec = tween(duration, easing = FFinderAnimations.Easing.Decelerated),
                        initialOffsetY = { fullHeight -> fullHeight }
                    ) + fadeIn(
                        animationSpec = tween(duration)
                    ) togetherWith slideOutVertically(
                        animationSpec = tween(duration, easing = FFinderAnimations.Easing.Accelerated),
                        targetOffsetY = { fullHeight -> fullHeight }
                    ) + fadeOut(
                        animationSpec = tween(duration)
                    )
                }
            }
            NavigationTransitionType.REPLACE -> {
                fadeIn(
                    animationSpec = tween(duration)
                ) togetherWith fadeOut(
                    animationSpec = tween(duration)
                )
            }
            NavigationTransitionType.SHARED_ELEMENT -> {
                if (reduceMotion) {
                    fadeIn(
                        animationSpec = tween(duration)
                    ) togetherWith fadeOut(
                        animationSpec = tween(duration)
                    )
                } else {
                    scaleIn(
                        initialScale = 0.8f,
                        animationSpec = FFinderAnimations.Springs.Bouncy
                    ) + fadeIn(
                        animationSpec = tween(duration)
                    ) togetherWith scaleOut(
                        targetScale = 1.2f,
                        animationSpec = FFinderAnimations.Springs.Standard
                    ) + fadeOut(
                        animationSpec = tween(duration)
                    )
                }
            }
        }
    }
    
    /**
     * Staggered content animation for smooth list appearances
     */
    @Composable
    fun StaggeredContentAnimation(
        items: List<String>,
        delayBetweenItems: Long = 50L,
        content: @Composable (String, Boolean) -> Unit
    ) {
        items.forEachIndexed { index, item ->
            var shouldAnimate by remember { mutableStateOf(false) }
            
            LaunchedEffect(item) {
                delay(index * delayBetweenItems)
                shouldAnimate = true
            }
            
            content(item, shouldAnimate)
        }
    }
    
    /**
     * Smooth transition between loading and content states
     */
    @Composable
    fun LoadingToContentTransition(
        isLoading: Boolean,
        loadingContent: @Composable () -> Unit,
        content: @Composable () -> Unit
    ) {
        val contentAlpha by animateFloatAsState(
            targetValue = if (isLoading) 0f else 1f,
            animationSpec = FFinderAnimations.Transitions.screenEnter(),
            label = "content_alpha"
        )
        
        val loadingAlpha by animateFloatAsState(
            targetValue = if (isLoading) 1f else 0f,
            animationSpec = FFinderAnimations.Transitions.screenExit(),
            label = "loading_alpha"
        )
        
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(loadingAlpha)
            ) {
                if (isLoading) {
                    loadingContent()
                }
            }
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(contentAlpha)
            ) {
                if (!isLoading) {
                    content()
                }
            }
        }
    }
    
    /**
     * Error state transition with shake effect
     */
    @Composable
    fun ErrorStateTransition(
        hasError: Boolean,
        onErrorAnimationComplete: () -> Unit = {},
        content: @Composable (Boolean, Float) -> Unit
    ) {
        val shakeOffset = remember { androidx.compose.animation.core.Animatable(0f) }
        
        LaunchedEffect(hasError) {
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
}

/**
 * Extension functions for AnimatedContentTransitionScope
 */
fun AnimatedContentTransitionScope<String>.slideInVertically(
    animationSpec: androidx.compose.animation.core.FiniteAnimationSpec<androidx.compose.ui.unit.IntOffset>,
    initialOffsetY: (Int) -> Int
) = androidx.compose.animation.slideInVertically(animationSpec, initialOffsetY)

fun AnimatedContentTransitionScope<String>.slideOutVertically(
    animationSpec: androidx.compose.animation.core.FiniteAnimationSpec<androidx.compose.ui.unit.IntOffset>,
    targetOffsetY: (Int) -> Int
) = androidx.compose.animation.slideOutVertically(animationSpec, targetOffsetY)