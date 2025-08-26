package com.locationsharing.app.ui.friends.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.unit.dp
import com.locationsharing.app.ui.map.MapScreenConstants
import com.locationsharing.app.ui.map.haptic.rememberMapHapticFeedbackManager
import timber.log.Timber

/**
 * Self-Location FAB component for centering the map on user's current location.
 * 
 * Enhanced with Material 3 styling, loading states, haptic feedback, and accessibility support.
 * Implements requirements 7.1, 7.2, 7.3, 9.2, 9.6 from the MapScreen redesign specification.
 * 
 * Features:
 * - Material 3 styling with proper elevation and colors
 * - Loading state indicator with circular progress
 * - Scale animation on press (1.0 → 0.9 → 1.0)
 * - Rotation animation during loading
 * - Haptic feedback on interaction
 * - Comprehensive accessibility support
 * - Positioned above Quick Share FAB
 * 
 * @param onClick Callback when the FAB is clicked to center map on user location
 * @param modifier Modifier for styling and positioning
 * @param isLoading Whether location is currently being fetched
 * @param hasLocationPermission Whether location permission is granted
 * @param enabled Whether the FAB is enabled for interaction
 */
@Composable
fun SelfLocationFAB(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    hasLocationPermission: Boolean = true,
    enabled: Boolean = true
) {
    val hapticManager = rememberMapHapticFeedbackManager()
    
    // Enhanced scale animation using MapMicroAnimations with loading support
    val infiniteTransition = rememberInfiniteTransition(label = "SelfLocationFAB infinite transition")
    val scale = com.locationsharing.app.ui.map.animations.MapMicroAnimations.FABScaleAnimation(
        isPressed = false, // Don't use pressed state for loading
        isLoading = isLoading,
        pressedScale = MapScreenConstants.Animations.FAB_PRESSED_SCALE,
        normalScale = MapScreenConstants.Animations.FAB_NORMAL_SCALE
    )
    
    // Add loading pulse animation
    val loadingPulse = if (isLoading) {
        com.locationsharing.app.ui.map.animations.MapMicroAnimations.FABLoadingPulse()
    } else 1f
    
    // Rotation animation during loading
    val rotationAngle by if (isLoading) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = MapScreenConstants.Animations.EMPHASIZED_DURATION * 2,
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Restart
            ),
            label = "Self location FAB loading rotation"
        )
    } else {
        animateFloatAsState(
            targetValue = 0f,
            animationSpec = tween(durationMillis = MapScreenConstants.Animations.QUICK_DURATION),
            label = "Self location FAB idle rotation"
        )
    }
    
    // Determine FAB colors based on permission state
    val colorScheme = MaterialTheme.colorScheme
    val containerColor = if (hasLocationPermission) {
        colorScheme.primary // Green from theme
    } else {
        colorScheme.error // Red for permission denied
    }
    
    val contentColor = if (hasLocationPermission) {
        colorScheme.onPrimary
    } else {
        colorScheme.onError
    }
    
    // Content description based on state
    val contentDesc = when {
        !hasLocationPermission -> "Request location permission to center map"
        isLoading -> "Centering map on your location..."
        else -> MapScreenConstants.Accessibility.SELF_LOCATION_FAB_DESC
    }
    
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        FloatingActionButton(
            onClick = {
                if (enabled && !isLoading) {
                    Timber.d("📍 SelfLocationFAB: User tapped to center map on their location (loading: $isLoading, permission: $hasLocationPermission)")
                    
                    // Provide enhanced haptic feedback for location action
                    hapticManager.performLocationAction()
                    
                    onClick()
                }
            },
            modifier = Modifier
                .size(MapScreenConstants.Dimensions.FAB_SIZE)
                .scale(scale * loadingPulse)
                .semantics {
                    contentDescription = contentDesc
                    role = Role.Button
                    testTag = "self_location_fab"
                    stateDescription = when {
                        !hasLocationPermission -> "Location permission required"
                        isLoading -> "Loading location"
                        else -> "Location available"
                    }
                    if (!enabled || isLoading) {
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
            if (isLoading) {
                // Show loading indicator
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = contentColor,
                    strokeWidth = 2.dp
                )
            } else {
                // Show location icon
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = null, // Content description is on the FAB itself
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(rotationAngle)
                )
            }
        }
    }
}