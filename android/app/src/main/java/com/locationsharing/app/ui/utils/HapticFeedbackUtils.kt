package com.locationsharing.app.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Utility class for consistent haptic feedback across the MapScreen
 * Provides standardized haptic feedback patterns for different interactions
 */
object HapticFeedbackUtils {
    
    /**
     * Haptic feedback types for different MapScreen interactions
     */
    enum class FeedbackType {
        BUTTON_PRESS,
        FAB_PRESS,
        LONG_PRESS,
        SUCCESS,
        ERROR,
        SELECTION,
        NAVIGATION
    }
    
    /**
     * Performs haptic feedback based on the interaction type
     */
    fun performHapticFeedback(
        hapticFeedback: HapticFeedback,
        type: FeedbackType
    ) {
        val feedbackType = when (type) {
            FeedbackType.BUTTON_PRESS -> HapticFeedbackType.LongPress
            FeedbackType.FAB_PRESS -> HapticFeedbackType.LongPress
            FeedbackType.LONG_PRESS -> HapticFeedbackType.LongPress
            FeedbackType.SUCCESS -> HapticFeedbackType.LongPress
            FeedbackType.ERROR -> HapticFeedbackType.LongPress
            FeedbackType.SELECTION -> HapticFeedbackType.TextHandleMove
            FeedbackType.NAVIGATION -> HapticFeedbackType.TextHandleMove
        }
        
        hapticFeedback.performHapticFeedback(feedbackType)
    }
}

/**
 * Composable helper for easy haptic feedback access
 */
@Composable
fun rememberHapticFeedback(): HapticFeedback {
    return LocalHapticFeedback.current
}

/**
 * Extension function for easy haptic feedback on any composable
 */
@Composable
fun HapticFeedback.performFeedback(type: HapticFeedbackUtils.FeedbackType) {
    HapticFeedbackUtils.performHapticFeedback(this, type)
}