package com.locationsharing.app.ui.map.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.unit.dp
import com.locationsharing.app.R
import com.locationsharing.app.ui.map.MapScreenConstants
import com.locationsharing.app.ui.map.haptic.rememberMapHapticFeedbackManager
import timber.log.Timber

/**
 * Quick Share FAB component for instantly sharing user's location with friends.
 * 
 * Enhanced with Material 3 styling, scale animation on press, haptic feedback, and accessibility support.
 * Implements requirements 3.1, 3.2, 3.3, 3.4, 3.5, 8.2, 9.6 from the MapScreen redesign specification.
 * 
 * Features:
 * - Material 3 styling with primary colors
 * - ic_pin_finder icon (white, 24dp)
 * - Scale animation on press (1.0 → 0.9 → 1.0)
 * - Haptic feedback on interaction
 * - Comprehensive accessibility support
 * - Visual feedback for active sharing state
 * - Bottom-right positioning with 16dp margins
 * 
 * @param onClick Callback when the FAB is clicked to trigger location sharing
 * @param modifier Modifier for styling and positioning
 * @param isLocationSharingActive Whether location sharing is currently active
 * @param enabled Whether the FAB is enabled for interaction
 * @param isPressed Whether the FAB is currently being pressed (for animation)
 */
@Composable
fun QuickShareFAB(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLocationSharingActive: Boolean = false,
    enabled: Boolean = true,
    isPressed: Boolean = false
) {
    val hapticManager = rememberMapHapticFeedbackManager()
    
    // Enhanced scale animation using MapMicroAnimations with loading state support
    val scale = com.locationsharing.app.ui.map.animations.MapMicroAnimations.FABScaleAnimation(
        isPressed = isPressed,
        isLoading = !enabled, // Show loading state when disabled
        pressedScale = MapScreenConstants.Animations.FAB_PRESSED_SCALE,
        normalScale = MapScreenConstants.Animations.FAB_NORMAL_SCALE
    )
    
    // Add loading pulse when location sharing is being processed
    val loadingPulse = if (!enabled) {
        com.locationsharing.app.ui.map.animations.MapMicroAnimations.FABLoadingPulse()
    } else 1f
    
    // Determine FAB colors based on sharing state
    val colorScheme = MaterialTheme.colorScheme
    val containerColor = if (isLocationSharingActive) {
        colorScheme.secondary // Purple when active
    } else {
        colorScheme.primary // Green when inactive
    }
    
    val contentColor = if (isLocationSharingActive) {
        colorScheme.onSecondary
    } else {
        colorScheme.onPrimary
    }
    
    // Content description based on sharing state
    val contentDesc = if (isLocationSharingActive) {
        "Location sharing is active, tap to manage"
    } else {
        MapScreenConstants.Accessibility.QUICK_SHARE_FAB_DESC
    }
    
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        FloatingActionButton(
            onClick = {
                if (enabled) {
                    Timber.d("📍 QuickShareFAB: User tapped to share location (currently active: $isLocationSharingActive)")
                    
                    // Provide enhanced haptic feedback for primary FAB action
                    hapticManager.performPrimaryFABAction()
                    
                    onClick()
                }
            },
            modifier = Modifier
                .size(MapScreenConstants.Dimensions.FAB_SIZE)
                .scale(scale * loadingPulse)
                .semantics {
                    contentDescription = contentDesc
                    role = Role.Button
                    testTag = "quick_share_fab"
                    stateDescription = if (isLocationSharingActive) {
                        "Location sharing is active"
                    } else {
                        "Location sharing is off"
                    }
                    if (!enabled) {
                        disabled()
                    }
                },
            shape = CircleShape,
            containerColor = containerColor,
            contentColor = contentColor,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = MapScreenConstants.Dimensions.FAB_ELEVATION,
                pressedElevation = MapScreenConstants.Dimensions.FAB_PRESSED_ELEVATION
            )
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_pin_finder),
                contentDescription = null, // Content description is on the FAB itself
                modifier = Modifier.size(24.dp),
                tint = contentColor // Apply white tint as specified
            )
        }
    }
}