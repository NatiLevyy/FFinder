package com.locationsharing.app.ui.components

/**
 * Central import file for all MapScreen components
 * Provides organized access to all UI components used in the MapScreen redesign
 */

// Core Compose imports for MapScreen
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

// Animation imports
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput

// Google Maps imports
import com.google.maps.android.compose.*
import com.google.android.gms.maps.model.*

// Theme imports
import com.locationsharing.app.ui.theme.FFinderAnimations
import com.locationsharing.app.ui.theme.FFinderTheme

/**
 * Component type definitions for MapScreen
 */
object MapScreenComponents {
    
    /**
     * FAB component types
     */
    enum class FABType {
        QUICK_SHARE,
        SELF_LOCATION,
        DEBUG
    }
    
    /**
     * Animation states for components
     */
    enum class AnimationState {
        IDLE,
        PRESSED,
        LOADING,
        SUCCESS,
        ERROR
    }
    
    /**
     * Drawer states
     */
    enum class DrawerState {
        CLOSED,
        OPENING,
        OPEN,
        CLOSING
    }
    
    /**
     * Sheet states
     */
    enum class SheetState {
        HIDDEN,
        SHOWING,
        VISIBLE,
        HIDING
    }
}

/**
 * Common component modifiers for MapScreen
 */
object MapScreenModifiers {
    
    /**
     * Standard FAB modifier with haptic feedback and accessibility
     */
    fun fabModifier(
        contentDescription: String,
        onClick: () -> Unit,
        hapticFeedback: androidx.compose.ui.hapticfeedback.HapticFeedback
    ): Modifier = Modifier
        .size(56.dp)
        .semantics {
            role = Role.Button
            this.contentDescription = contentDescription
        }
        .pointerInput(Unit) {
            detectTapGestures(
                onTap = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                }
            )
        }
    
    /**
     * Standard card modifier with elevation and shape
     */
    val cardModifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
    
    /**
     * Standard padding for components
     */
    val standardPadding = Modifier.padding(16.dp)
    
    /**
     * FAB positioning modifiers
     */
    val quickShareFABPosition = Modifier
        .padding(bottom = 16.dp, end = 16.dp)
    
    val selfLocationFABPosition = Modifier
        .padding(bottom = 88.dp, end = 16.dp)
    
    val debugFABPosition = Modifier
        .padding(bottom = 16.dp, start = 16.dp)
}

/**
 * Common component colors for MapScreen
 */
object MapScreenColors {
    // Color definitions for MapScreen components
    // FAB colors will be defined inline in components
}

/**
 * Common animations for MapScreen components
 */
object MapScreenAnimations {
    
    /**
     * FAB press animation
     */
    @Composable
    fun fabPressAnimation(pressed: Boolean): Float {
        val scale by animateFloatAsState(
            targetValue = if (pressed) 0.9f else 1.0f,
            animationSpec = if (pressed) {
                FFinderAnimations.MicroInteractions.buttonPress()
            } else {
                FFinderAnimations.MicroInteractions.buttonRelease()
            },
            label = "fab_press_animation"
        )
        return scale
    }
    
    /**
     * Drawer slide animation
     */
    @Composable
    fun drawerSlideAnimation(visible: Boolean): Float {
        val offset by animateFloatAsState(
            targetValue = if (visible) 0f else 280f,
            animationSpec = tween(
                durationMillis = 300,
                easing = if (visible) {
                    CubicBezierEasing(0.68f, -0.55f, 0.265f, 1.55f) // Overshoot
                } else {
                    FastOutSlowInEasing
                }
            ),
            label = "drawer_slide_animation"
        )
        return offset
    }
    
    /**
     * Sheet fade animation
     */
    @Composable
    fun sheetFadeAnimation(visible: Boolean): Float {
        val alpha by animateFloatAsState(
            targetValue = if (visible) 1f else 0f,
            animationSpec = tween(
                durationMillis = 200,
                easing = if (visible) LinearOutSlowInEasing else FastOutLinearInEasing
            ),
            label = "sheet_fade_animation"
        )
        return alpha
    }
    
    /**
     * Location marker pulse animation
     */
    @Composable
    fun locationPulseAnimation(): Float {
        val infiniteTransition = rememberInfiniteTransition(label = "location_pulse")
        val scale by infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 3000,
                    easing = EaseInOutBack
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "location_pulse_scale"
        )
        return scale
    }
}

/**
 * Accessibility helpers for MapScreen components
 */
object MapScreenAccessibility {
    
    /**
     * Standard content descriptions
     */
    object ContentDescriptions {
        const val BACK_BUTTON = "Navigate back"
        const val NEARBY_FRIENDS_BUTTON = "View nearby friends"
        const val QUICK_SHARE_FAB = "Share your location instantly"
        const val SELF_LOCATION_FAB = "Center map on your location"
        const val DEBUG_FAB = "Add test friends to map"
        const val MAP_CONTENT = "Map showing your location and nearby friends"
        const val NEARBY_DRAWER = "Nearby friends panel"
        const val STATUS_SHEET = "Location sharing status"
    }
    
    /**
     * Semantic roles for components
     */
    object Roles {
        val BUTTON = Role.Button
        val IMAGE = Role.Image
        val NAVIGATION = androidx.compose.ui.semantics.Role.Button // For navigation drawer
        val DIALOG = androidx.compose.ui.semantics.Role.Button // For bottom sheet
    }
    
    /**
     * Focus order for screen reader navigation
     */
    object FocusOrder {
        const val BACK_BUTTON = 1
        const val APP_TITLE = 2
        const val NEARBY_FRIENDS_BUTTON = 3
        const val MAP_CONTENT = 4
        const val SELF_LOCATION_FAB = 5
        const val QUICK_SHARE_FAB = 6
        const val DEBUG_FAB = 7
    }
}