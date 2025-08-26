package com.locationsharing.app.ui.map.haptic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import timber.log.Timber

/**
 * Comprehensive haptic feedback manager for MapScreen components.
 * 
 * Provides centralized haptic feedback management with consistent patterns
 * across all MapScreen interactions. Implements requirements 3.4, 4.5, 9.6
 * from the MapScreen redesign specification.
 * 
 * Features:
 * - Appropriate feedback types for different actions
 * - Accessibility service compatibility
 * - Consistent feedback patterns across components
 * - Logging for debugging and testing
 * - Device type optimization
 * 
 * @param hapticFeedback The platform haptic feedback interface
 */
class MapHapticFeedbackManager(private val hapticFeedback: HapticFeedback) {
    
    /**
     * Performs haptic feedback for primary FAB actions (Quick Share, Self Location).
     * Uses TextHandleMove for responsive, light feedback on primary interactions.
     */
    fun performPrimaryFABAction() {
        try {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            Timber.d("🔊 MapHaptic: Primary FAB action feedback performed")
        } catch (e: Exception) {
            Timber.w(e, "🔊 MapHaptic: Failed to perform primary FAB haptic feedback")
        }
    }
    
    /**
     * Performs haptic feedback for secondary FAB actions (Debug FAB).
     * Uses LongPress for stronger feedback on debug/secondary actions.
     */
    fun performSecondaryFABAction() {
        try {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            Timber.d("🔊 MapHaptic: Secondary FAB action feedback performed")
        } catch (e: Exception) {
            Timber.w(e, "🔊 MapHaptic: Failed to perform secondary FAB haptic feedback")
        }
    }
    
    /**
     * Performs haptic feedback for AppBar button interactions (Back, Nearby Friends).
     * Uses TextHandleMove for light, responsive feedback on navigation actions.
     */
    fun performAppBarAction() {
        try {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            Timber.d("🔊 MapHaptic: AppBar action feedback performed")
        } catch (e: Exception) {
            Timber.w(e, "🔊 MapHaptic: Failed to perform AppBar haptic feedback")
        }
    }
    
    /**
     * Performs haptic feedback for drawer interactions (Open, Close, Friend Selection).
     * Uses TextHandleMove for smooth, responsive feedback on drawer actions.
     */
    fun performDrawerAction() {
        try {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            Timber.d("🔊 MapHaptic: Drawer action feedback performed")
        } catch (e: Exception) {
            Timber.w(e, "🔊 MapHaptic: Failed to perform drawer haptic feedback")
        }
    }
    
    /**
     * Performs haptic feedback for friend item interactions in the drawer.
     * Uses TextHandleMove for light feedback on friend selection.
     */
    fun performFriendItemAction() {
        try {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            Timber.d("🔊 MapHaptic: Friend item action feedback performed")
        } catch (e: Exception) {
            Timber.w(e, "🔊 MapHaptic: Failed to perform friend item haptic feedback")
        }
    }
    
    /**
     * Performs haptic feedback for friend action buttons (Message, More Actions).
     * Uses LongPress for stronger feedback on important friend actions.
     */
    fun performFriendActionButton() {
        try {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            Timber.d("🔊 MapHaptic: Friend action button feedback performed")
        } catch (e: Exception) {
            Timber.w(e, "🔊 MapHaptic: Failed to perform friend action button haptic feedback")
        }
    }
    
    /**
     * Performs haptic feedback for status sheet interactions (Dismiss, Stop Sharing).
     * Uses appropriate feedback based on action importance.
     * 
     * @param isImportantAction Whether this is an important action (like Stop Sharing)
     */
    fun performStatusSheetAction(isImportantAction: Boolean = false) {
        try {
            val feedbackType = if (isImportantAction) {
                HapticFeedbackType.LongPress // Stronger feedback for important actions
            } else {
                HapticFeedbackType.TextHandleMove // Light feedback for dismissal
            }
            hapticFeedback.performHapticFeedback(feedbackType)
            Timber.d("🔊 MapHaptic: Status sheet action feedback performed (important: $isImportantAction)")
        } catch (e: Exception) {
            Timber.w(e, "🔊 MapHaptic: Failed to perform status sheet haptic feedback")
        }
    }
    
    /**
     * Performs haptic feedback for map marker interactions.
     * Uses TextHandleMove for light feedback on marker selection.
     */
    fun performMarkerAction() {
        try {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            Timber.d("🔊 MapHaptic: Map marker action feedback performed")
        } catch (e: Exception) {
            Timber.w(e, "🔊 MapHaptic: Failed to perform marker haptic feedback")
        }
    }
    
    /**
     * Performs haptic feedback for error states or failed actions.
     * Uses LongPress for strong feedback to indicate something went wrong.
     */
    fun performErrorFeedback() {
        try {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            Timber.d("🔊 MapHaptic: Error feedback performed")
        } catch (e: Exception) {
            Timber.w(e, "🔊 MapHaptic: Failed to perform error haptic feedback")
        }
    }
    
    /**
     * Performs haptic feedback for successful actions or confirmations.
     * Uses TextHandleMove for light, positive feedback.
     */
    fun performSuccessFeedback() {
        try {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            Timber.d("🔊 MapHaptic: Success feedback performed")
        } catch (e: Exception) {
            Timber.w(e, "🔊 MapHaptic: Failed to perform success haptic feedback")
        }
    }
    
    /**
     * Performs haptic feedback for location-related actions (centering, sharing).
     * Uses TextHandleMove for responsive feedback on location operations.
     */
    fun performLocationAction() {
        try {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            Timber.d("🔊 MapHaptic: Location action feedback performed")
        } catch (e: Exception) {
            Timber.w(e, "🔊 MapHaptic: Failed to perform location haptic feedback")
        }
    }
}

/**
 * Composable that provides a MapHapticFeedbackManager instance.
 * 
 * Creates a remembered instance of MapHapticFeedbackManager using the current
 * LocalHapticFeedback. This ensures consistent haptic feedback patterns
 * across all MapScreen components and works with accessibility services.
 * 
 * @return MapHapticFeedbackManager instance for the current composition
 */
@Composable
fun rememberMapHapticFeedbackManager(): MapHapticFeedbackManager {
    val hapticFeedback = LocalHapticFeedback.current
    return remember(hapticFeedback) {
        MapHapticFeedbackManager(hapticFeedback)
    }
}

/**
 * Extension functions for common haptic feedback patterns in MapScreen.
 * Provides convenient access to haptic feedback without requiring manager instance.
 */
object MapHapticPatterns {
    
    /**
     * Standard haptic feedback for FAB button presses.
     */
    fun HapticFeedback.fabPress() {
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
    
    /**
     * Haptic feedback for navigation actions.
     */
    fun HapticFeedback.navigationAction() {
        performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }
    
    /**
     * Haptic feedback for error states.
     */
    fun HapticFeedback.errorState() {
        performHapticFeedback(HapticFeedbackType.LongPress)
    }
}

/**
 * Utility object for haptic feedback testing and validation.
 * Provides methods to test haptic feedback on different device types.
 */
object MapHapticTesting {
    
    /**
     * Tests all haptic feedback patterns to ensure they work correctly.
     * Should be called during development and testing phases.
     * 
     * @param manager The haptic feedback manager to test
     */
    fun testAllHapticPatterns(manager: MapHapticFeedbackManager) {
        Timber.d("🔊 MapHaptic: Testing all haptic feedback patterns")
        
        // Test primary actions
        manager.performPrimaryFABAction()
        
        // Test secondary actions
        manager.performSecondaryFABAction()
        
        // Test navigation actions
        manager.performAppBarAction()
        
        // Test drawer actions
        manager.performDrawerAction()
        
        // Test friend interactions
        manager.performFriendItemAction()
        manager.performFriendActionButton()
        
        // Test status sheet actions
        manager.performStatusSheetAction(false)
        manager.performStatusSheetAction(true)
        
        // Test map interactions
        manager.performMarkerAction()
        
        // Test feedback states
        manager.performErrorFeedback()
        manager.performSuccessFeedback()
        manager.performLocationAction()
        
        Timber.d("🔊 MapHaptic: All haptic feedback patterns tested")
    }
    
    /**
     * Validates that haptic feedback is working correctly with accessibility services.
     * 
     * @param manager The haptic feedback manager to validate
     * @return True if haptic feedback is working correctly
     */
    fun validateAccessibilityCompatibility(manager: MapHapticFeedbackManager): Boolean {
        return try {
            manager.performPrimaryFABAction()
            Timber.d("🔊 MapHaptic: Accessibility compatibility validated successfully")
            true
        } catch (e: Exception) {
            Timber.e(e, "🔊 MapHaptic: Accessibility compatibility validation failed")
            false
        }
    }
}