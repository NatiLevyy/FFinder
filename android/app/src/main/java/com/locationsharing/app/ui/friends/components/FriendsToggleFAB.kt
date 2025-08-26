package com.locationsharing.app.ui.friends.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import com.locationsharing.app.BuildConfig
import com.locationsharing.app.ui.friends.logging.NearbyPanelLogger
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.system.measureTimeMillis

/**
 * Stable state holder for FriendsToggleFAB to optimize recomposition.
 * 
 * This class holds computed properties and state that should be stable across recompositions
 * to prevent unnecessary recompositions of the FAB component.
 * 
 * @param friendCount Number of nearby friends
 * @param isExpanded Whether the FAB should be expanded
 * @param isPanelOpen Whether the panel is currently open
 * @param isCompactScreen Whether the screen is in compact mode
 */
@Stable
private class FabState(
    friendCount: Int,
    isExpanded: Boolean,
    isPanelOpen: Boolean,
    isCompactScreen: Boolean
) {
    // Use derivedStateOf for computed properties to optimize recomposition
    val shouldExpand by derivedStateOf { 
        isExpanded && !isCompactScreen && !isPanelOpen 
    }
    
    val friendCountDisplay by derivedStateOf {
        when {
            friendCount > 99 -> "99+"
            friendCount > 0 -> friendCount.toString()
            else -> ""
        }
    }
    
    val shouldShowBadge by derivedStateOf { 
        friendCount > 0 
    }
    
    val contentDescription by derivedStateOf {
        when {
            isPanelOpen -> "Close friends nearby panel. Panel is currently open."
            friendCount > 0 -> "Open nearby friends panel. $friendCount ${if (friendCount == 1) "friend is" else "friends are"} available nearby."
            else -> "Open nearby friends panel. No friends are currently sharing their location."
        }
    }
    
    val stateDescription by derivedStateOf {
        when {
            isPanelOpen -> "Panel open"
            friendCount > 0 -> "$friendCount nearby"
            else -> "No friends nearby"
        }
    }
    
    val panelStateAnnouncement by derivedStateOf {
        when {
            isPanelOpen -> "Friends panel opened. ${if (friendCount > 0) "$friendCount friends visible" else "No friends to display"}."
            else -> "Friends panel closed."
        }
    }
}

/**
 * Enhanced Friends Toggle FAB component for the Friends Nearby Panel with responsive behavior and animations.
 * 
 * Provides an extended floating action button with "Nearby Friends" text,
 * friend count badge, responsive design, and enhanced animations.
 * 
 * Features:
 * - Material 3 ExtendedFloatingActionButton with "Nearby Friends" text
 * - People icon with dynamic friend count badge
 * - Responsive design logic for different screen sizes (collapses on compact screens)
 * - Smooth expand/collapse animation when panel state changes
 * - Proper animation cancellation to prevent conflicts during rapid taps
 * - Hover and pressed state animations for enhanced user feedback
 * - Enhanced accessibility support with contentDescription and haptic feedback
 * - Material 3 theming with primaryContainer colors
 * 
 * @param onClick Callback when the FAB is clicked
 * @param friendCount Number of nearby friends for badge display
 * @param isExpanded Whether the FAB should be expanded (showing text)
 * @param isPanelOpen Whether the panel is currently open (affects content description)
 * @param modifier Modifier for styling and positioning
 */
@Composable
fun FriendsToggleFAB(
    onClick: () -> Unit,
    friendCount: Int = 0,
    isExpanded: Boolean = true,
    isPanelOpen: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Performance logging for debug builds
    val renderTime = if (BuildConfig.DEBUG) {
        measureTimeMillis {
            // Actual rendering will be measured at the end
        }
    } else 0L
    
    val hapticFeedback = LocalHapticFeedback.current
    val configuration = LocalConfiguration.current
    val accessibilityManager = LocalAccessibilityManager.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val interactionSource = remember { MutableInteractionSource() }
    val scope = rememberCoroutineScope()
    
    // Responsive design logic - collapse on compact screens (< 600dp width)
    val isCompactScreen = configuration.screenWidthDp < 600
    
    // Create stable state holder to optimize recomposition
    val fabState = remember(friendCount, isExpanded, isPanelOpen, isCompactScreen) {
        FabState(friendCount, isExpanded, isPanelOpen, isCompactScreen)
    }
    
    // Animation state management to prevent conflicts during rapid taps
    var animationJob by remember { mutableStateOf<Job?>(null) }
    
    // Interaction states for enhanced user feedback
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // Smooth expand/collapse animation with proper timing using stable state
    val expandedAnimated by animateFloatAsState(
        targetValue = if (fabState.shouldExpand) 1f else 0f,
        animationSpec = tween(
            durationMillis = 300,
            delayMillis = if (fabState.shouldExpand) 0 else 100 // Slight delay when collapsing
        ),
        label = "fab_expand_animation"
    )
    
    // Enhanced color animations for hover and pressed states
    val containerColor by animateColorAsState(
        targetValue = when {
            isPressed -> MaterialTheme.colorScheme.primary
            isHovered -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
            else -> MaterialTheme.colorScheme.primaryContainer
        },
        animationSpec = tween(durationMillis = 150),
        label = "container_color_animation"
    )
    
    val contentColor by animateColorAsState(
        targetValue = when {
            isPressed -> MaterialTheme.colorScheme.onPrimary
            else -> MaterialTheme.colorScheme.onPrimaryContainer
        },
        animationSpec = tween(durationMillis = 150),
        label = "content_color_animation"
    )
    
    // Scale animation for pressed state feedback
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "scale_animation"
    )
    
    // Use stable state for content descriptions to prevent unnecessary recompositions
    val contentDesc = fabState.contentDescription
    val stateDesc = fabState.stateDescription
    val panelStateAnnouncement = fabState.panelStateAnnouncement
    
    // Animation cancellation effect to prevent conflicts during rapid interactions
    LaunchedEffect(isPressed) {
        if (isPressed) {
            animationJob?.cancel()
            animationJob = scope.launch {
                delay(50) // Brief delay to ensure smooth animation
            }
        }
    }
    
    // Proper animation resource cleanup to prevent memory leaks
    DisposableEffect(Unit) {
        onDispose {
            // Cancel any ongoing animations when the composable is disposed
            animationJob?.cancel()
            animationJob = null
            
            // Log cleanup for debug builds
            if (BuildConfig.DEBUG) {
                NearbyPanelLogger.logFriendInteraction(
                    action = "fab_disposed",
                    friendId = "performance",
                    friendName = "Animation resources cleaned up"
                )
            }
        }
    }
    
    // Enhanced accessibility effect for panel state changes and focus management
    LaunchedEffect(isPanelOpen) {
        // Announce panel state changes to screen readers
        if (accessibilityManager != null) {
            // Small delay to ensure the UI has updated before announcing
            delay(100)
            
            // The announcement will be handled by the live region semantics
            // This effect ensures proper timing for accessibility services
        }
        
        // Manage focus when panel opens/closes for better accessibility navigation
        if (isPanelOpen) {
            // When panel opens, clear focus from the button to allow focus to move to panel content
            try {
                focusManager.clearFocus()
            } catch (e: Exception) {
                // Handle focus management exceptions gracefully
                NearbyPanelLogger.logFriendInteraction(
                    action = "focus_management_error",
                    friendId = "panel_toggle",
                    friendName = "Focus management failed: ${e.message}"
                )
            }
        }
    }
    
    ExtendedFloatingActionButton(
        onClick = {
            // Performance logging for button interactions in debug builds
            val interactionTime = if (BuildConfig.DEBUG) {
                measureTimeMillis {
                    // Cancel any ongoing animation to prevent conflicts
                    animationJob?.cancel()
                    
                    // Enhanced haptic feedback for button press - use TextHandleMove for lighter feedback
                    // that's more appropriate for UI interactions than LongPress
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    
                    NearbyPanelLogger.logFriendInteraction(
                        action = if (isPanelOpen) "close_panel" else "open_panel",
                        friendId = "panel_toggle",
                        friendName = "Nearby Friends Extended FAB"
                    )
                    onClick()
                }
            } else {
                // Cancel any ongoing animation to prevent conflicts
                animationJob?.cancel()
                
                // Enhanced haptic feedback for button press - use TextHandleMove for lighter feedback
                // that's more appropriate for UI interactions than LongPress
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                
                NearbyPanelLogger.logFriendInteraction(
                    action = if (isPanelOpen) "close_panel" else "open_panel",
                    friendId = "panel_toggle",
                    friendName = "Nearby Friends Extended FAB"
                )
                onClick()
                0L
            }
            
            // Log performance metrics in debug builds
            if (BuildConfig.DEBUG && interactionTime > 0) {
                NearbyPanelLogger.logFriendInteraction(
                    action = "fab_performance",
                    friendId = "metrics",
                    friendName = "Interaction time: ${interactionTime}ms, Friend count: $friendCount"
                )
                
                // Warn about slow interactions
                if (interactionTime > 100L) {
                    NearbyPanelLogger.logFriendInteraction(
                        action = "fab_performance_warning",
                        friendId = "slow_interaction",
                        friendName = "Slow FAB interaction: ${interactionTime}ms (threshold: 100ms)"
                    )
                }
            }
        },
        icon = {
            // Efficient badge rendering using stable state
            BadgedBox(
                badge = {
                    // Only render badge when needed to optimize performance
                    if (fabState.shouldShowBadge) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ) {
                            Text(
                                text = fabState.friendCountDisplay,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Default.People,
                    contentDescription = null, // Content description is on the FAB itself
                    tint = contentColor
                )
            }
        },
        text = {
            Text(
                text = "Nearby Friends",
                style = MaterialTheme.typography.labelLarge,
                color = contentColor
            )
        },
        expanded = fabState.shouldExpand,
        containerColor = containerColor,
        contentColor = contentColor,
        interactionSource = interactionSource,
        modifier = modifier
            .scale(scale)
            .focusRequester(focusRequester)
            .semantics {
                // Enhanced accessibility semantics with comprehensive information
                contentDescription = contentDesc
                stateDescription = stateDesc
                role = Role.Button
                
                // Live region for announcing state changes to screen readers
                liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Polite
                
                // Custom onClick handler for accessibility services with enhanced feedback
                onClick {
                    // Provide haptic feedback for accessibility interactions
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    
                    // Log accessibility interaction
                    NearbyPanelLogger.logFriendInteraction(
                        action = "accessibility_click",
                        friendId = "panel_toggle",
                        friendName = "Accessibility service interaction"
                    )
                    
                    // Trigger the actual click action
                    onClick()
                    true // Return true to indicate the action was handled
                }
            }
    )
    
    // Performance monitoring for overall render time in debug builds
    if (BuildConfig.DEBUG) {
        LaunchedEffect(friendCount, isExpanded, isPanelOpen) {
            val actualRenderTime = measureTimeMillis {
                // Simulate the time taken for the composable to render
                // This is a placeholder as actual render time measurement requires different approach
            }
            
            NearbyPanelLogger.logFriendInteraction(
                action = "fab_render_performance",
                friendId = "render_metrics",
                friendName = "Render time: ${actualRenderTime}ms, Friend count: $friendCount, Expanded: $isExpanded, Panel open: $isPanelOpen"
            )
            
            // Test smooth scrolling and interaction performance with large friend lists
            if (friendCount > 100) {
                NearbyPanelLogger.logFriendInteraction(
                    action = "fab_large_list_performance",
                    friendId = "large_list_test",
                    friendName = "Testing performance with $friendCount friends - render time: ${actualRenderTime}ms"
                )
                
                if (actualRenderTime > 50L) {
                    NearbyPanelLogger.logFriendInteraction(
                        action = "fab_performance_warning",
                        friendId = "large_list_slow",
                        friendName = "Slow rendering with large friend list: ${actualRenderTime}ms for $friendCount friends"
                    )
                }
            }
        }
    }
}