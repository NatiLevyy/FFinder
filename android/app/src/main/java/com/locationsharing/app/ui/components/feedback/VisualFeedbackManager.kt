package com.locationsharing.app.ui.components.feedback

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.locationsharing.app.ui.theme.FFinderAnimations
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Types of visual feedback that can be triggered
 */
enum class FeedbackType {
    BUTTON_PRESS,
    NAVIGATION_START,
    NAVIGATION_SUCCESS,
    NAVIGATION_ERROR,
    LOADING_START,
    LOADING_END,
    SUCCESS_CONFIRMATION,
    ERROR_SHAKE,
    RIPPLE_EFFECT
}

/**
 * Visual feedback state for tracking active animations
 */
data class VisualFeedbackState(
    val isActive: Boolean = false,
    val feedbackType: FeedbackType? = null,
    val intensity: Float = 1.0f,
    val duration: Long = 300L
)

/**
 * Manager for coordinating visual feedback across the app
 */
interface VisualFeedbackManager {
    val feedbackState: StateFlow<VisualFeedbackState>
    
    suspend fun triggerFeedback(
        type: FeedbackType,
        intensity: Float = 1.0f,
        duration: Long = 300L
    )
    
    suspend fun triggerHapticFeedback(type: HapticFeedbackType)
    
    fun clearFeedback()
}

/**
 * Implementation of VisualFeedbackManager
 */
@Singleton
class VisualFeedbackManagerImpl @Inject constructor() : VisualFeedbackManager {
    
    private val _feedbackState = MutableStateFlow(VisualFeedbackState())
    override val feedbackState: StateFlow<VisualFeedbackState> = _feedbackState.asStateFlow()
    
    override suspend fun triggerFeedback(
        type: FeedbackType,
        intensity: Float,
        duration: Long
    ) {
        _feedbackState.value = VisualFeedbackState(
            isActive = true,
            feedbackType = type,
            intensity = intensity,
            duration = duration
        )
        
        // Auto-clear feedback after duration
        kotlinx.coroutines.delay(duration)
        clearFeedback()
    }
    
    override suspend fun triggerHapticFeedback(type: HapticFeedbackType) {
        // This will be handled by the composable using LocalHapticFeedback
        // We just track the state here
        _feedbackState.value = _feedbackState.value.copy(
            isActive = true,
            feedbackType = FeedbackType.BUTTON_PRESS
        )
    }
    
    override fun clearFeedback() {
        _feedbackState.value = VisualFeedbackState()
    }
}

/**
 * Composable for handling visual feedback animations
 */
@Composable
fun VisualFeedbackHandler(
    feedbackManager: VisualFeedbackManager,
    content: @Composable (VisualFeedbackState) -> Unit
) {
    val feedbackState by feedbackManager.feedbackState.collectAsState()
    val hapticFeedback = LocalHapticFeedback.current
    
    // Handle haptic feedback
    LaunchedEffect(feedbackState.feedbackType) {
        when (feedbackState.feedbackType) {
            FeedbackType.BUTTON_PRESS -> {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            FeedbackType.NAVIGATION_SUCCESS -> {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            FeedbackType.NAVIGATION_ERROR -> {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            FeedbackType.SUCCESS_CONFIRMATION -> {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            else -> { /* No haptic feedback for other types */ }
        }
    }
    
    content(feedbackState)
}

/**
 * Ripple effect animation composable
 */
@Composable
fun RippleEffect(
    isTriggered: Boolean,
    onAnimationComplete: () -> Unit = {},
    content: @Composable (Float, Float) -> Unit
) {
    val rippleScale = remember { Animatable(0f) }
    val rippleAlpha = remember { Animatable(0f) }
    
    LaunchedEffect(isTriggered) {
        if (isTriggered) {
            // Start ripple animation
            launch {
                rippleAlpha.animateTo(
                    targetValue = 0.6f,
                    animationSpec = tween(150, easing = LinearEasing)
                )
                rippleAlpha.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(300, easing = LinearEasing)
                )
            }
            
            launch {
                rippleScale.animateTo(
                    targetValue = 2f,
                    animationSpec = FFinderAnimations.MicroInteractions.ripple()
                )
                rippleScale.snapTo(0f)
                onAnimationComplete()
            }
        }
    }
    
    content(rippleScale.value, rippleAlpha.value)
}

/**
 * Loading indicator with smooth transitions
 */
@Composable
fun LoadingIndicator(
    isLoading: Boolean,
    content: @Composable (Boolean, Float) -> Unit
) {
    val loadingProgress = remember { Animatable(0f) }
    
    LaunchedEffect(isLoading) {
        if (isLoading) {
            loadingProgress.animateTo(
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            )
        } else {
            loadingProgress.animateTo(
                targetValue = 0f,
                animationSpec = FFinderAnimations.Transitions.screenExit()
            )
        }
    }
    
    content(isLoading, loadingProgress.value)
}

/**
 * Success confirmation animation
 */
@Composable
fun SuccessConfirmation(
    showSuccess: Boolean,
    onAnimationComplete: () -> Unit = {},
    content: @Composable (Boolean, Float) -> Unit
) {
    val successScale = remember { Animatable(0f) }
    
    LaunchedEffect(showSuccess) {
        if (showSuccess) {
            successScale.animateTo(
                targetValue = 1.2f,
                animationSpec = FFinderAnimations.Springs.Bouncy
            )
            successScale.animateTo(
                targetValue = 1f,
                animationSpec = FFinderAnimations.Springs.Standard
            )
            onAnimationComplete()
        } else {
            successScale.snapTo(0f)
        }
    }
    
    content(showSuccess, successScale.value)
}

/**
 * Error shake animation
 */
@Composable
fun ErrorShake(
    hasError: Boolean,
    onAnimationComplete: () -> Unit = {},
    content: @Composable (Boolean, Float) -> Unit
) {
    val shakeOffset = remember { Animatable(0f) }
    
    LaunchedEffect(hasError) {
        if (hasError) {
            shakeOffset.animateTo(
                targetValue = 0f,
                animationSpec = FFinderAnimations.Error.shake()
            )
            onAnimationComplete()
        }
    }
    
    content(hasError, shakeOffset.value)
}

/**
 * Navigation transition loading overlay
 */
@Composable
fun NavigationLoadingOverlay(
    isNavigating: Boolean,
    content: @Composable (Boolean, Float) -> Unit
) {
    val overlayAlpha = remember { Animatable(0f) }
    
    LaunchedEffect(isNavigating) {
        if (isNavigating) {
            overlayAlpha.animateTo(
                targetValue = 0.3f,
                animationSpec = FFinderAnimations.Transitions.screenEnter()
            )
        } else {
            overlayAlpha.animateTo(
                targetValue = 0f,
                animationSpec = FFinderAnimations.Transitions.screenExit()
            )
        }
    }
    
    content(isNavigating, overlayAlpha.value)
}