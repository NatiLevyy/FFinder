package com.locationsharing.app.ui.home.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Utility class for managing haptic feedback in the FFinder Home Screen.
 * 
 * Provides a centralized way to handle haptic feedback with consistent
 * patterns across all home screen components.
 */
class HapticFeedbackManager(private val hapticFeedback: HapticFeedback) {
    
    /**
     * Performs haptic feedback for primary actions (e.g., Start Sharing button).
     * Uses LongPress haptic feedback for important actions.
     */
    fun performPrimaryAction() {
        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
    }
    
    /**
     * Performs haptic feedback for secondary actions (e.g., Friends, Settings buttons).
     * Uses TextHandleMove haptic feedback for lighter interactions.
     */
    fun performSecondaryAction() {
        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }
    
    /**
     * Performs haptic feedback for card interactions (e.g., What's New teaser).
     * Uses TextHandleMove haptic feedback for card taps.
     */
    fun performCardInteraction() {
        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }
    
    /**
     * Performs haptic feedback for error states or failed actions.
     * Uses LongPress haptic feedback to indicate something went wrong.
     */
    fun performErrorFeedback() {
        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
    }
}

/**
 * Composable that provides a HapticFeedbackManager instance.
 * 
 * Creates a remembered instance of HapticFeedbackManager using the current
 * LocalHapticFeedback. This ensures consistent haptic feedback patterns
 * across all home screen components.
 * 
 * @return HapticFeedbackManager instance for the current composition
 */
@Composable
fun rememberHapticFeedbackManager(): HapticFeedbackManager {
    val hapticFeedback = LocalHapticFeedback.current
    return remember(hapticFeedback) {
        HapticFeedbackManager(hapticFeedback)
    }
}

/**
 * Extension functions for common haptic feedback patterns.
 */
object HapticPatterns {
    
    /**
     * Standard haptic feedback for button presses.
     */
    fun HapticFeedback.buttonPress() {
        performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }
    
    /**
     * Strong haptic feedback for important actions.
     */
    fun HapticFeedback.importantAction() {
        performHapticFeedback(HapticFeedbackType.LongPress)
    }
    
    /**
     * Light haptic feedback for subtle interactions.
     */
    fun HapticFeedback.lightTouch() {
        performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }
}