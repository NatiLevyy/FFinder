package com.locationsharing.app.ui.home.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.LatLng
import com.locationsharing.app.ui.theme.FFinderTheme

/**
 * Comprehensive accessibility and responsive design implementation for the FFinder Home Screen.
 * 
 * This composable demonstrates the complete implementation of Task 8 requirements:
 * - Meaningful contentDescription for all interactive elements
 * - Logical focus order: logo → subtitle → map → CTA → secondary → teaser
 * - Responsive Extended FAB that collapses to icon-only with tooltip on narrow screens
 * - All animations respect system accessibility preferences
 * - Proper dp scaling for different screen densities
 * 
 * @param onStartShare Callback for the primary "Start Live Sharing" action
 * @param onFriends Callback for navigating to friends screen
 * @param onSettings Callback for navigating to settings screen
 * @param onWhatsNew Callback for showing what's new dialog
 * @param userLocation Current user location (null if permission not granted)
 * @param hasLocationPermission Whether location permission is granted
 * @param onLocationPermissionRequest Callback for requesting location permission
 * @param animationsEnabled User preference for animations (will be overridden by system accessibility)
 * @param modifier Modifier for the root container
 */
@Composable
fun AccessibilityResponsiveHomeScreen(
    onStartShare: () -> Unit,
    onFriends: () -> Unit,
    onSettings: () -> Unit,
    onWhatsNew: () -> Unit,
    userLocation: LatLng? = null,
    hasLocationPermission: Boolean = false,
    onLocationPermissionRequest: () -> Unit = {},
    animationsEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    // Get comprehensive accessibility and responsive design configuration
    val accessibilityConfig = rememberAccessibilityConfig(animationsEnabled)
    
    // Responsive layout detection
    ResponsiveLayout { layoutConfig ->
        BackgroundGradient {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(accessibilityConfig.responsivePadding)
                    .semantics {
                        contentDescription = "FFinder Home Screen. Navigate through app features and start location sharing."
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(accessibilityConfig.responsiveSpacing)
            ) {
                
                // 1. Hero Section (Logo + Subtitle) - Focus Order: 1-2
                HeroSection(
                    modifier = Modifier.padding(top = accessibilityConfig.responsivePadding),
                    animationsEnabled = accessibilityConfig.animationsEnabled
                )
                
                Spacer(modifier = Modifier.height(accessibilityConfig.responsiveSpacing))
                
                // 2. Map Preview Card - Focus Order: 3
                MapPreviewCard(
                    location = userLocation,
                    hasLocationPermission = hasLocationPermission,
                    animationsEnabled = accessibilityConfig.animationsEnabled,
                    onPermissionRequest = onLocationPermissionRequest,
                    modifier = Modifier.padding(vertical = accessibilityConfig.responsiveSpacing)
                )
                
                Spacer(modifier = Modifier.height(accessibilityConfig.responsiveSpacing))
                
                // 3. Primary Call-to-Action - Focus Order: 4
                // Responsive: Extended FAB on normal screens, Icon-only FAB with tooltip on narrow screens
                PrimaryCallToAction(
                    onStartShare = onStartShare,
                    isNarrowScreen = layoutConfig.isNarrowScreen,
                    modifier = Modifier.padding(vertical = accessibilityConfig.responsiveSpacing)
                )
                
                Spacer(modifier = Modifier.height(accessibilityConfig.responsiveSpacing))
                
                // 4. Secondary Navigation Actions - Focus Order: 5-6
                SecondaryActionsRow(
                    onFriends = onFriends,
                    onSettings = onSettings,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = accessibilityConfig.responsivePadding)
                )
                
                Spacer(modifier = Modifier.height(accessibilityConfig.responsiveSpacing))
                
                // 5. What's New Feature Teaser - Focus Order: 7
                WhatsNewTeaser(
                    onTap = onWhatsNew,
                    isVisible = true,
                    animationsEnabled = accessibilityConfig.animationsEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = accessibilityConfig.responsivePadding)
                )
                
                // Bottom padding for scroll content
                Spacer(modifier = Modifier.height(accessibilityConfig.responsivePadding))
            }
        }
    }
}

/**
 * Preview demonstrating normal screen layout with all accessibility features.
 */
@Preview(
    name = "Normal Screen - Accessibility Enabled",
    showBackground = true,
    widthDp = 400,
    heightDp = 800
)
@Composable
fun AccessibilityResponsiveHomeScreenNormalPreview() {
    FFinderTheme {
        AccessibilityResponsiveHomeScreen(
            onStartShare = { /* Preview action */ },
            onFriends = { /* Preview action */ },
            onSettings = { /* Preview action */ },
            onWhatsNew = { /* Preview action */ },
            userLocation = LatLng(37.7749, -122.4194), // San Francisco
            hasLocationPermission = true,
            animationsEnabled = true
        )
    }
}

/**
 * Preview demonstrating narrow screen layout with responsive FAB.
 */
@Preview(
    name = "Narrow Screen - Responsive FAB",
    showBackground = true,
    widthDp = 320, // Narrow screen (< 360dp)
    heightDp = 600
)
@Composable
fun AccessibilityResponsiveHomeScreenNarrowPreview() {
    FFinderTheme {
        AccessibilityResponsiveHomeScreen(
            onStartShare = { /* Preview action */ },
            onFriends = { /* Preview action */ },
            onSettings = { /* Preview action */ },
            onWhatsNew = { /* Preview action */ },
            userLocation = LatLng(37.7749, -122.4194),
            hasLocationPermission = true,
            animationsEnabled = true
        )
    }
}

/**
 * Preview demonstrating accessibility mode with animations disabled.
 */
@Preview(
    name = "Accessibility Mode - No Animations",
    showBackground = true,
    widthDp = 400,
    heightDp = 800
)
@Composable
fun AccessibilityResponsiveHomeScreenNoAnimationsPreview() {
    FFinderTheme {
        AccessibilityResponsiveHomeScreen(
            onStartShare = { /* Preview action */ },
            onFriends = { /* Preview action */ },
            onSettings = { /* Preview action */ },
            onWhatsNew = { /* Preview action */ },
            userLocation = LatLng(37.7749, -122.4194),
            hasLocationPermission = true,
            animationsEnabled = false // Animations disabled for accessibility
        )
    }
}

/**
 * Preview demonstrating no location permission state.
 */
@Preview(
    name = "No Location Permission",
    showBackground = true,
    widthDp = 400,
    heightDp = 800
)
@Composable
fun AccessibilityResponsiveHomeScreenNoLocationPreview() {
    FFinderTheme {
        AccessibilityResponsiveHomeScreen(
            onStartShare = { /* Preview action */ },
            onFriends = { /* Preview action */ },
            onSettings = { /* Preview action */ },
            onWhatsNew = { /* Preview action */ },
            userLocation = null,
            hasLocationPermission = false,
            onLocationPermissionRequest = { /* Handle permission request */ },
            animationsEnabled = true
        )
    }
}

/**
 * Preview demonstrating dark theme with accessibility features.
 */
@Preview(
    name = "Dark Theme - Accessibility",
    showBackground = true,
    widthDp = 400,
    heightDp = 800
)
@Composable
fun AccessibilityResponsiveHomeScreenDarkPreview() {
    FFinderTheme(darkTheme = true) {
        AccessibilityResponsiveHomeScreen(
            onStartShare = { /* Preview action */ },
            onFriends = { /* Preview action */ },
            onSettings = { /* Preview action */ },
            onWhatsNew = { /* Preview action */ },
            userLocation = LatLng(37.7749, -122.4194),
            hasLocationPermission = true,
            animationsEnabled = true
        )
    }
}

/**
 * Preview demonstrating large screen layout.
 */
@Preview(
    name = "Large Screen - Expanded Layout",
    showBackground = true,
    widthDp = 600, // Large screen
    heightDp = 900
)
@Composable
fun AccessibilityResponsiveHomeScreenLargePreview() {
    FFinderTheme {
        AccessibilityResponsiveHomeScreen(
            onStartShare = { /* Preview action */ },
            onFriends = { /* Preview action */ },
            onSettings = { /* Preview action */ },
            onWhatsNew = { /* Preview action */ },
            userLocation = LatLng(37.7749, -122.4194),
            hasLocationPermission = true,
            animationsEnabled = true
        )
    }
}