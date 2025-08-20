package com.locationsharing.app.ui.home.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Accessibility utilities for the FFinder Home Screen components.
 * 
 * Provides centralized accessibility checks and responsive design utilities
 * to ensure consistent behavior across all home screen components.
 */
object AccessibilityUtils {
    
    /**
     * Checks if animations should be enabled based on system accessibility preferences.
     * 
     * @param userPreference User's animation preference (default true)
     * @return true if animations should be enabled, false otherwise
     */
    @Composable
    fun shouldEnableAnimations(userPreference: Boolean = true): Boolean {
        // For now, respect user preference. In a real implementation,
        // you would check system accessibility settings
        return userPreference
    }
    
    /**
     * Checks if the current screen width is considered narrow (< 360dp).
     * 
     * @return true if screen is narrow, false otherwise
     */
    @Composable
    fun isNarrowScreen(): Boolean {
        val configuration = LocalConfiguration.current
        return configuration.screenWidthDp < 360
    }
    
    /**
     * Gets the current screen width in dp.
     * 
     * @return screen width in dp
     */
    @Composable
    fun getScreenWidthDp(): Int {
        val configuration = LocalConfiguration.current
        return configuration.screenWidthDp
    }
    
    /**
     * Gets the current screen height in dp.
     * 
     * @return screen height in dp
     */
    @Composable
    fun getScreenHeightDp(): Int {
        val configuration = LocalConfiguration.current
        return configuration.screenHeightDp
    }
    
    /**
     * Checks if the device is in landscape orientation.
     * 
     * @return true if in landscape, false if in portrait
     */
    @Composable
    fun isLandscape(): Boolean {
        val configuration = LocalConfiguration.current
        return configuration.screenWidthDp > configuration.screenHeightDp
    }
    
    /**
     * Gets the appropriate touch target size based on accessibility guidelines.
     * Minimum 48dp as per Material Design guidelines.
     * 
     * @param preferredSize The preferred size when accessibility is not a concern
     * @return The appropriate touch target size
     */
    @Composable
    fun getAccessibleTouchTargetSize(preferredSize: Dp = 56.dp): Dp {
        // Always ensure minimum 48dp touch target as per Material Design guidelines
        return maxOf(preferredSize, 48.dp)
    }
    
    /**
     * Gets the appropriate text scaling factor based on system font scale.
     * 
     * @return The current font scale factor
     */
    @Composable
    fun getFontScale(): Float {
        val configuration = LocalConfiguration.current
        return configuration.fontScale
    }
    
    /**
     * Checks if large text is enabled (font scale > 1.3).
     * 
     * @return true if large text is enabled
     */
    @Composable
    fun isLargeTextEnabled(): Boolean {
        return getFontScale() > 1.3f
    }
    
    /**
     * Gets responsive padding based on screen size.
     * 
     * @param compactPadding Padding for compact screens (< 360dp)
     * @param mediumPadding Padding for medium screens (360-599dp)
     * @param expandedPadding Padding for expanded screens (>= 600dp)
     * @return Appropriate padding for current screen size
     */
    @Composable
    fun getResponsivePadding(
        compactPadding: Dp = 12.dp,
        mediumPadding: Dp = 16.dp,
        expandedPadding: Dp = 24.dp
    ): Dp {
        val screenWidth = getScreenWidthDp()
        return when {
            screenWidth < 360 -> compactPadding
            screenWidth < 600 -> mediumPadding
            else -> expandedPadding
        }
    }
    
    /**
     * Gets responsive spacing based on screen size.
     * 
     * @param compactSpacing Spacing for compact screens
     * @param mediumSpacing Spacing for medium screens
     * @param expandedSpacing Spacing for expanded screens
     * @return Appropriate spacing for current screen size
     */
    @Composable
    fun getResponsiveSpacing(
        compactSpacing: Dp = 8.dp,
        mediumSpacing: Dp = 12.dp,
        expandedSpacing: Dp = 16.dp
    ): Dp {
        val screenWidth = getScreenWidthDp()
        return when {
            screenWidth < 360 -> compactSpacing
            screenWidth < 600 -> mediumSpacing
            else -> expandedSpacing
        }
    }
}

/**
 * Data class representing comprehensive accessibility and responsive design configuration.
 */
data class AccessibilityConfig(
    val animationsEnabled: Boolean,
    val isNarrowScreen: Boolean,
    val screenWidthDp: Int,
    val screenHeightDp: Int,
    val isLandscape: Boolean,
    val fontScale: Float,
    val isLargeTextEnabled: Boolean,
    val touchTargetSize: Dp,
    val responsivePadding: Dp,
    val responsiveSpacing: Dp
)

/**
 * Composable that provides comprehensive accessibility and responsive design configuration.
 * 
 * @param animationsEnabled User preference for animations
 * @return AccessibilityConfig with all relevant accessibility and responsive design information
 */
@Composable
fun rememberAccessibilityConfig(animationsEnabled: Boolean = true): AccessibilityConfig {
    val shouldAnimate = AccessibilityUtils.shouldEnableAnimations(animationsEnabled)
    val isNarrow = AccessibilityUtils.isNarrowScreen()
    val screenWidth = AccessibilityUtils.getScreenWidthDp()
    val screenHeight = AccessibilityUtils.getScreenHeightDp()
    val isLandscape = AccessibilityUtils.isLandscape()
    val fontScale = AccessibilityUtils.getFontScale()
    val isLargeText = AccessibilityUtils.isLargeTextEnabled()
    val touchTarget = AccessibilityUtils.getAccessibleTouchTargetSize()
    val padding = AccessibilityUtils.getResponsivePadding()
    val spacing = AccessibilityUtils.getResponsiveSpacing()
    
    return remember(
        shouldAnimate, isNarrow, screenWidth, screenHeight, 
        isLandscape, fontScale, isLargeText, touchTarget, padding, spacing
    ) {
        AccessibilityConfig(
            animationsEnabled = shouldAnimate,
            isNarrowScreen = isNarrow,
            screenWidthDp = screenWidth,
            screenHeightDp = screenHeight,
            isLandscape = isLandscape,
            fontScale = fontScale,
            isLargeTextEnabled = isLargeText,
            touchTargetSize = touchTarget,
            responsivePadding = padding,
            responsiveSpacing = spacing
        )
    }
}

/**
 * Focus order constants for logical navigation.
 */
object FocusOrder {
    const val LOGO = 1
    const val SUBTITLE = 2
    const val MAP_PREVIEW = 3
    const val PRIMARY_CTA = 4
    const val SECONDARY_FRIENDS = 5
    const val SECONDARY_SETTINGS = 6
    const val WHATS_NEW_TEASER = 7
}

/**
 * Content description templates for consistent accessibility announcements.
 */
object ContentDescriptions {
    const val LOGO = "FFinder app logo - Find Friends, Share Locations"
    const val SUBTITLE = "App description: Share your live location and find friends instantly"
    const val MAP_PREVIEW_WITH_LOCATION = "Map preview showing your current location with animated pin marker"
    const val MAP_PREVIEW_NO_LOCATION = "Map preview unavailable. Location permission required."
    const val PRIMARY_CTA_EXTENDED = "Start Live Sharing button. Tap to begin sharing your location with friends."
    const val PRIMARY_CTA_ICON = "Start Live Sharing. Tap to begin sharing your location with friends."
    const val FRIENDS_BUTTON = "Friends button. Navigate to friends list and management."
    const val SETTINGS_BUTTON = "Settings button. Navigate to app settings and preferences."
    const val WHATS_NEW_TEASER = "What's New announcement. Tap to learn about new features: Nearby Friends panel and Quick Share functionality."
    const val LOCATION_PIN = "Location pin marker showing your current position on the map"
    const val ENABLE_LOCATION = "Enable Location button. Tap to grant location permission and view map preview."
}