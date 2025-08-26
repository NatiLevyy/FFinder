package com.locationsharing.app.ui.components.feedback

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Types of haptic feedback for different navigation interactions
 */
enum class NavigationHapticType {
    BUTTON_PRESS,
    NAVIGATION_START,
    NAVIGATION_SUCCESS,
    NAVIGATION_ERROR,
    BACK_NAVIGATION,
    MODAL_OPEN,
    MODAL_CLOSE,
    SUCCESS_CONFIRMATION,
    ERROR_FEEDBACK,
    SELECTION_CHANGE
}

/**
 * Haptic feedback state for tracking active feedback
 */
data class HapticFeedbackState(
    val isActive: Boolean = false,
    val feedbackType: NavigationHapticType? = null,
    val intensity: Float = 1.0f
)

/**
 * Manager for coordinating haptic feedback across navigation interactions
 */
interface HapticFeedbackManager {
    val feedbackState: StateFlow<HapticFeedbackState>
    
    suspend fun triggerHapticFeedback(
        type: NavigationHapticType,
        intensity: Float = 1.0f
    )
    
    fun clearFeedback()
    
    fun isHapticEnabled(): Boolean
    fun setHapticEnabled(enabled: Boolean)
}

/**
 * Implementation of HapticFeedbackManager
 */
@Singleton
class HapticFeedbackManagerImpl @Inject constructor() : HapticFeedbackManager {
    
    private val _feedbackState = MutableStateFlow(HapticFeedbackState())
    override val feedbackState: StateFlow<HapticFeedbackState> = _feedbackState.asStateFlow()
    
    private var hapticEnabled = true
    
    override suspend fun triggerHapticFeedback(
        type: NavigationHapticType,
        intensity: Float
    ) {
        if (!hapticEnabled) return
        
        _feedbackState.value = HapticFeedbackState(
            isActive = true,
            feedbackType = type,
            intensity = intensity
        )
        
        // Auto-clear feedback after a short delay
        kotlinx.coroutines.delay(100)
        clearFeedback()
    }
    
    override fun clearFeedback() {
        _feedbackState.value = HapticFeedbackState()
    }
    
    override fun isHapticEnabled(): Boolean = hapticEnabled
    
    override fun setHapticEnabled(enabled: Boolean) {
        hapticEnabled = enabled
    }
}

/**
 * Composable for handling haptic feedback in navigation
 */
@Composable
fun NavigationHapticFeedbackHandler(
    hapticManager: HapticFeedbackManager,
    content: @Composable () -> Unit
) {
    val feedbackState by hapticManager.feedbackState.collectAsState()
    val hapticFeedback = LocalHapticFeedback.current
    
    // Handle haptic feedback based on navigation type
    LaunchedEffect(feedbackState.feedbackType) {
        if (feedbackState.isActive && hapticManager.isHapticEnabled()) {
            when (feedbackState.feedbackType) {
                NavigationHapticType.BUTTON_PRESS -> {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                NavigationHapticType.NAVIGATION_START -> {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                NavigationHapticType.NAVIGATION_SUCCESS -> {
                    // Double tap for success
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    kotlinx.coroutines.delay(100)
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                NavigationHapticType.NAVIGATION_ERROR -> {
                    // Triple tap for error
                    repeat(3) {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        kotlinx.coroutines.delay(100)
                    }
                }
                NavigationHapticType.BACK_NAVIGATION -> {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                NavigationHapticType.MODAL_OPEN -> {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                NavigationHapticType.MODAL_CLOSE -> {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                NavigationHapticType.SUCCESS_CONFIRMATION -> {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                NavigationHapticType.ERROR_FEEDBACK -> {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                NavigationHapticType.SELECTION_CHANGE -> {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                null -> { /* No feedback */ }
            }
        }
    }
    
    content()
}

/**
 * Extension functions for easy haptic feedback triggering
 */
suspend fun HapticFeedbackManager.triggerButtonPress() {
    triggerHapticFeedback(NavigationHapticType.BUTTON_PRESS)
}

suspend fun HapticFeedbackManager.triggerNavigationStart() {
    triggerHapticFeedback(NavigationHapticType.NAVIGATION_START)
}

suspend fun HapticFeedbackManager.triggerNavigationSuccess() {
    triggerHapticFeedback(NavigationHapticType.NAVIGATION_SUCCESS)
}

suspend fun HapticFeedbackManager.triggerNavigationError() {
    triggerHapticFeedback(NavigationHapticType.NAVIGATION_ERROR)
}

suspend fun HapticFeedbackManager.triggerBackNavigation() {
    triggerHapticFeedback(NavigationHapticType.BACK_NAVIGATION)
}

suspend fun HapticFeedbackManager.triggerModalOpen() {
    triggerHapticFeedback(NavigationHapticType.MODAL_OPEN)
}

suspend fun HapticFeedbackManager.triggerModalClose() {
    triggerHapticFeedback(NavigationHapticType.MODAL_CLOSE)
}

suspend fun HapticFeedbackManager.triggerSuccessConfirmation() {
    triggerHapticFeedback(NavigationHapticType.SUCCESS_CONFIRMATION)
}

suspend fun HapticFeedbackManager.triggerErrorFeedback() {
    triggerHapticFeedback(NavigationHapticType.ERROR_FEEDBACK)
}

suspend fun HapticFeedbackManager.triggerSelectionChange() {
    triggerHapticFeedback(NavigationHapticType.SELECTION_CHANGE)
}