package com.locationsharing.app.ui.map

/**
 * Centralized import file for MapScreen components
 * Provides easy access to all MapScreen-related classes and utilities
 */

// Core MapScreen components
import com.locationsharing.app.ui.map.MapScreenConstants

// Theme and styling
import com.locationsharing.app.ui.theme.FFinderTheme

// Standard Compose imports for MapScreen
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp

// Google Maps imports
import com.google.maps.android.compose.*
import com.google.android.gms.maps.model.*

/**
 * Quick access object for all MapScreen imports
 * Use this to get easy access to commonly used components and utilities
 */
object MapScreenImports {
    
    // Constants
    val Constants = MapScreenConstants
    
    // Common composables shortcuts
    @Composable
    fun rememberHapticFeedback() = LocalHapticFeedback.current
    
    @Composable
    fun MaterialTheme() = androidx.compose.material3.MaterialTheme
    
    // Common modifiers
    val Modifier.standardPadding: Modifier
        get() = this.padding(MapScreenConstants.Dimensions.STANDARD_PADDING)
    
    val Modifier.largePadding: Modifier
        get() = this.padding(MapScreenConstants.Dimensions.LARGE_PADDING)
    
    val Modifier.smallPadding: Modifier
        get() = this.padding(MapScreenConstants.Dimensions.SMALL_PADDING)
    
    // FAB positioning modifiers
    val Modifier.quickShareFABPosition: Modifier
        get() = this.padding(
            bottom = MapScreenConstants.Layout.QUICK_SHARE_FAB_BOTTOM_MARGIN,
            end = MapScreenConstants.Layout.QUICK_SHARE_FAB_END_MARGIN
        )
    
    val Modifier.selfLocationFABPosition: Modifier
        get() = this.padding(
            bottom = MapScreenConstants.Layout.SELF_LOCATION_FAB_BOTTOM_MARGIN,
            end = MapScreenConstants.Layout.SELF_LOCATION_FAB_END_MARGIN
        )
    
    val Modifier.debugFABPosition: Modifier
        get() = this.padding(
            bottom = MapScreenConstants.Layout.DEBUG_FAB_BOTTOM_MARGIN,
            start = MapScreenConstants.Layout.DEBUG_FAB_START_MARGIN
        )
}

/**
 * Import shortcuts for MapScreen components
 */

// Animation type aliases
typealias AnimationSpec<T> = androidx.compose.animation.core.AnimationSpec<T>
typealias Easing = androidx.compose.animation.core.Easing
typealias SpringSpec<T> = androidx.compose.animation.core.SpringSpec<T>
typealias TweenSpec<T> = androidx.compose.animation.core.TweenSpec<T>

// Google Maps type aliases
typealias LatLng = com.google.android.gms.maps.model.LatLng
typealias CameraPosition = com.google.android.gms.maps.model.CameraPosition
typealias MarkerOptions = com.google.android.gms.maps.model.MarkerOptions
typealias GoogleMapOptions = com.google.android.gms.maps.GoogleMapOptions