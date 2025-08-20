package com.locationsharing.app.ui.map.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.LatLng
import com.locationsharing.app.R
import com.locationsharing.app.ui.components.AnimatedPin
import com.locationsharing.app.ui.map.MapScreenConstants
import com.locationsharing.app.ui.map.accessibility.MapAccessibilityConstants
import com.locationsharing.app.ui.map.haptic.rememberMapHapticFeedbackManager
import com.locationsharing.app.ui.theme.FFinderTheme
import timber.log.Timber

/**
 * ShareStatusSheet component for displaying location sharing status and controls.
 * 
 * Implements ModalBottomSheetScaffold for status display with Material 3 design.
 * Features swipe-to-dismiss, tap-outside-to-dismiss, and proper accessibility support.
 * 
 * Requirements implemented: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7, 8.4
 * 
 * Features:
 * - ModalBottomSheet with 16dp corner radius
 * - Location sharing status display with appropriate icons
 * - Current latitude and longitude coordinates display
 * - Swipe-to-dismiss and tap-outside-to-dismiss functionality
 * - "Stop Sharing" button with proper Material 3 styling
 * - Fade-in/out animations (200ms duration)
 * - Comprehensive accessibility support
 * - Haptic feedback for interactions
 * 
 * @param isVisible Whether the sheet should be visible
 * @param isLocationSharingActive Whether location sharing is currently active
 * @param currentLocation Current user location for coordinate display
 * @param onDismiss Callback when the sheet is dismissed
 * @param onStopSharing Callback when user taps "Stop Sharing" button
 * @param isAcquiringGPS Whether the system is currently acquiring GPS fix
 * @param modifier Modifier for styling and positioning
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareStatusSheet(
    isVisible: Boolean,
    isLocationSharingActive: Boolean,
    currentLocation: LatLng?,
    onDismiss: () -> Unit,
    onStopSharing: () -> Unit,
    isAcquiringGPS: Boolean = false,
    modifier: Modifier = Modifier
) {
    val hapticManager = rememberMapHapticFeedbackManager()
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false
    )
    
    // Enhanced fade animation using MapMicroAnimations with accessibility support
    val alpha = com.locationsharing.app.ui.map.animations.MapMicroAnimations.StatusSheetFadeAnimation(
        isVisible = isVisible,
        fadeDuration = 200, // 200ms as specified in design
        isReducedMotion = false // TODO: Get from accessibility settings
    )
    
    // Enhanced scale animation for entrance
    val scale = com.locationsharing.app.ui.map.animations.MapMicroAnimations.StatusSheetScaleAnimation(
        isVisible = isVisible,
        isReducedMotion = false // TODO: Get from accessibility settings
    )
    
    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = {
                Timber.d("📋 ShareStatusSheet: User dismissed sheet")
                hapticManager.performStatusSheetAction(false) // Light feedback for dismissal
                onDismiss()
            },
            sheetState = sheetState,
            modifier = modifier
                .alpha(alpha)
                .scale(scale)
                .semantics {
                    contentDescription = MapAccessibilityConstants.STATUS_SHEET
                    role = Role.Button
                    testTag = MapAccessibilityConstants.STATUS_SHEET_TEST_TAG
                    stateDescription = if (isLocationSharingActive) {
                        MapAccessibilityConstants.LOCATION_SHARING_ACTIVE
                    } else {
                        MapAccessibilityConstants.LOCATION_SHARING_INACTIVE
                    }
                },
            shape = RoundedCornerShape(
                topStart = MapScreenConstants.Dimensions.SHEET_CORNER_RADIUS,
                topEnd = MapScreenConstants.Dimensions.SHEET_CORNER_RADIUS
            ),
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f), // 95% opacity as specified
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = MapScreenConstants.Dimensions.SHEET_ELEVATION
        ) {
            ShareStatusSheetContent(
                isLocationSharingActive = isLocationSharingActive,
                currentLocation = currentLocation,
                isAcquiringGPS = isAcquiringGPS,
                onStopSharing = {
                    Timber.d("📋 ShareStatusSheet: User tapped stop sharing")
                    hapticManager.performStatusSheetAction(true) // Strong feedback for important action
                    onStopSharing()
                }
            )
        }
    }
}

/**
 * Content of the ShareStatusSheet with status display and controls
 */
@Composable
private fun ShareStatusSheetContent(
    isLocationSharingActive: Boolean,
    currentLocation: LatLng?,
    isAcquiringGPS: Boolean,
    onStopSharing: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(MapScreenConstants.Dimensions.LARGE_PADDING),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // GPS Animation when acquiring fix
        if (isAcquiringGPS) {
            AnimatedPin(
                modifier = Modifier.size(120.dp),
                tint = Color(0xFF6B4F8F), // Brand purple
                animated = true // Always animate when acquiring GPS
            )
            
            Text(
                text = "Acquiring GPS fix...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            // Status header with icon and text
            ShareStatusHeader(
                isLocationSharingActive = isLocationSharingActive
            )
        }
        
        // Coordinates display
        ShareCoordinatesDisplay(
            currentLocation = currentLocation
        )
        
        // Stop sharing button (only shown when sharing is active)
        if (isLocationSharingActive) {
            StopSharingButton(
                onClick = onStopSharing
            )
        }
        
        // Bottom spacing for gesture area
        Spacer(modifier = Modifier.height(8.dp))
    }
}

/**
 * Header section with status icon and text
 */
@Composable
private fun ShareStatusHeader(
    isLocationSharingActive: Boolean,
    modifier: Modifier = Modifier
) {
    val statusText = if (isLocationSharingActive) {
        "Location Sharing Active"
    } else {
        "Location Sharing Off"
    }
    
    val statusIcon = if (isLocationSharingActive) {
        Icons.Default.LocationOn
    } else {
        Icons.Default.LocationOff
    }
    
    val iconColor = if (isLocationSharingActive) {
        MaterialTheme.colorScheme.primary // Green when active
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant // Gray when inactive
    }
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = MapScreenConstants.Accessibility.locationSharingStatus(isLocationSharingActive)
                role = Role.Button
                heading()
            },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = statusIcon,
            contentDescription = null, // Description is on the Row
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = statusText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Coordinates display section
 */
@Composable
private fun ShareCoordinatesDisplay(
    currentLocation: LatLng?,
    modifier: Modifier = Modifier
) {
    val coordinatesText = currentLocation?.let { location ->
        "Lat: ${"%.6f".format(location.latitude)}\nLng: ${"%.6f".format(location.longitude)}"
    } ?: "Location unavailable"
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(16.dp)
            .semantics {
                contentDescription = "Current coordinates: $coordinatesText"
                role = Role.Button
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = coordinatesText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Normal
        )
    }
}

/**
 * Stop sharing button component
 */
@Composable
private fun StopSharingButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = MapAccessibilityConstants.STOP_SHARING_BUTTON
                role = Role.Button
                testTag = MapAccessibilityConstants.STOP_SHARING_BUTTON_TEST_TAG
            },
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Stop,
            contentDescription = null, // Description is on the button
            modifier = Modifier.size(18.dp)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = "Stop Sharing",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium
        )
    }
}

// Preview composables for development and testing

/**
 * Preview for ShareStatusSheet with active sharing
 */
@Preview(showBackground = true)
@Composable
fun ShareStatusSheetActivePreview() {
    FFinderTheme {
        ShareStatusSheet(
            isVisible = true,
            isLocationSharingActive = true,
            currentLocation = LatLng(37.7749, -122.4194),
            onDismiss = {},
            onStopSharing = {}
        )
    }
}

/**
 * Preview for ShareStatusSheet with inactive sharing
 */
@Preview(showBackground = true)
@Composable
fun ShareStatusSheetInactivePreview() {
    FFinderTheme {
        ShareStatusSheet(
            isVisible = true,
            isLocationSharingActive = false,
            currentLocation = LatLng(37.7749, -122.4194),
            onDismiss = {},
            onStopSharing = {}
        )
    }
}

/**
 * Preview for ShareStatusSheet with no location
 */
@Preview(showBackground = true)
@Composable
fun ShareStatusSheetNoLocationPreview() {
    FFinderTheme {
        ShareStatusSheet(
            isVisible = true,
            isLocationSharingActive = false,
            currentLocation = null,
            onDismiss = {},
            onStopSharing = {}
        )
    }
}

/**
 * Preview for ShareStatusSheet in dark theme
 */
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ShareStatusSheetDarkPreview() {
    FFinderTheme {
        ShareStatusSheet(
            isVisible = true,
            isLocationSharingActive = true,
            currentLocation = LatLng(37.7749, -122.4194),
            onDismiss = {},
            onStopSharing = {}
        )
    }
}

/**
 * Preview for ShareStatusSheetContent only
 */
@Preview(showBackground = true)
@Composable
fun ShareStatusSheetContentPreview() {
    FFinderTheme {
        ShareStatusSheetContent(
            isLocationSharingActive = true,
            currentLocation = LatLng(37.7749, -122.4194),
            isAcquiringGPS = false,
            onStopSharing = {}
        )
    }
}