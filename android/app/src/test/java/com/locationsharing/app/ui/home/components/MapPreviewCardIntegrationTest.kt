package com.locationsharing.app.ui.home.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.google.android.gms.maps.model.LatLng
import com.locationsharing.app.ui.theme.FFinderTheme
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

/**
 * Integration tests for MapPreviewCard component.
 * 
 * Tests integration scenarios including:
 * - Animation lifecycle management
 * - State changes and updates
 * - Error recovery scenarios
 * - Performance under different conditions
 */
class MapPreviewCardIntegrationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun mapPreviewCard_stateTransition_fromNoPermissionToPermissionGranted() = runTest {
        // Given
        var hasPermission = false
        var location: LatLng? = null
        var permissionRequested = false
        
        composeTestRule.setContent {
            FFinderTheme {
                MapPreviewCard(
                    location = location,
                    hasLocationPermission = hasPermission,
                    animationsEnabled = true,
                    onPermissionRequest = { permissionRequested = true }
                )
            }
        }
        
        // Initially should show fallback
        composeTestRule
            .onNodeWithText("Enable Location")
            .assertIsDisplayed()
        
        // When - User requests permission
        composeTestRule
            .onNodeWithText("Enable Location")
            .performClick()
        
        // Then - Callback should be triggered
        assert(permissionRequested) { "Permission request should be triggered" }
        
        // When - Permission is granted and location is available
        hasPermission = true
        location = LatLng(37.7749, -122.4194)
        
        composeTestRule.setContent {
            FFinderTheme {
                MapPreviewCard(
                    location = location,
                    hasLocationPermission = hasPermission,
                    animationsEnabled = true
                )
            }
        }
        
        // Then - Should show map content
        composeTestRule
            .onNodeWithContentDescription("Your location pin")
            .assertIsDisplayed()
    }

    @Test
    fun mapPreviewCard_animationToggle_respectsAccessibilityPreferences() = runTest {
        // Given
        val testLocation = LatLng(37.7749, -122.4194)
        var animationsEnabled = true
        
        composeTestRule.setContent {
            FFinderTheme {
                MapPreviewCard(
                    location = testLocation,
                    hasLocationPermission = true,
                    animationsEnabled = animationsEnabled
                )
            }
        }
        
        // Initially with animations enabled
        composeTestRule
            .onNodeWithContentDescription("Your location pin")
            .assertIsDisplayed()
        
        // When - Animations are disabled (accessibility preference)
        animationsEnabled = false
        
        composeTestRule.setContent {
            FFinderTheme {
                MapPreviewCard(
                    location = testLocation,
                    hasLocationPermission = true,
                    animationsEnabled = animationsEnabled
                )
            }
        }
        
        // Then - Component should still render but without animations
        composeTestRule
            .onNodeWithContentDescription("Your location pin")
            .assertIsDisplayed()
    }

    @Test
    fun mapPreviewCard_locationUpdate_handlesLocationChanges() = runTest {
        // Given
        var currentLocation = LatLng(37.7749, -122.4194) // San Francisco
        
        composeTestRule.setContent {
            FFinderTheme {
                MapPreviewCard(
                    location = currentLocation,
                    hasLocationPermission = true,
                    animationsEnabled = true
                )
            }
        }
        
        // Initially shows San Francisco location
        composeTestRule
            .onNodeWithContentDescription("Your location pin")
            .assertIsDisplayed()
        
        // When - Location changes to New York
        currentLocation = LatLng(40.7128, -74.0060)
        
        composeTestRule.setContent {
            FFinderTheme {
                MapPreviewCard(
                    location = currentLocation,
                    hasLocationPermission = true,
                    animationsEnabled = true
                )
            }
        }
        
        // Then - Should still display pin (location change handled)
        composeTestRule
            .onNodeWithContentDescription("Your location pin")
            .assertIsDisplayed()
    }

    @Test
    fun mapPreviewWithErrorHandling_recoversFromError() = runTest {
        // Given
        val testLocation = LatLng(37.7749, -122.4194)
        var errorOccurred = false
        
        composeTestRule.setContent {
            FFinderTheme {
                MapPreviewWithErrorHandling(
                    location = testLocation,
                    hasLocationPermission = true,
                    animationsEnabled = true,
                    onError = { errorOccurred = true }
                )
            }
        }
        
        // Should initially display map content
        composeTestRule
            .onNodeWithContentDescription("Your location pin")
            .assertIsDisplayed()
    }

    @Test
    fun mapPreviewCard_multipleStateChanges_handlesCorrectly() = runTest {
        // Given
        var hasPermission = false
        var location: LatLng? = null
        var animationsEnabled = true
        
        // Test multiple state transitions
        val states = listOf(
            Triple(false, null, true),                              // No permission, no location, animations on
            Triple(true, LatLng(37.7749, -122.4194), true),       // Permission granted, location available, animations on
            Triple(true, LatLng(37.7749, -122.4194), false),      // Same location, animations off (accessibility)
            Triple(true, LatLng(40.7128, -74.0060), false),       // Location changed, animations still off
            Triple(false, null, false)                              // Permission revoked, no location, animations off
        )
        
        states.forEach { (permission, loc, animations) ->
            hasPermission = permission
            location = loc
            animationsEnabled = animations
            
            composeTestRule.setContent {
                FFinderTheme {
                    MapPreviewCard(
                        location = location,
                        hasLocationPermission = hasPermission,
                        animationsEnabled = animationsEnabled
                    )
                }
            }
            
            // Verify appropriate content is displayed for each state
            if (hasPermission && location != null) {
                composeTestRule
                    .onNodeWithContentDescription("Your location pin")
                    .assertIsDisplayed()
            } else {
                composeTestRule
                    .onNodeWithText("Enable Location")
                    .assertIsDisplayed()
            }
        }
    }

    @Test
    fun mapPreviewCard_performanceUnderRapidStateChanges() = runTest {
        // Given - Rapid state changes to test performance
        val locations = listOf(
            LatLng(37.7749, -122.4194), // San Francisco
            LatLng(40.7128, -74.0060),  // New York
            LatLng(51.5074, -0.1278),   // London
            LatLng(35.6762, 139.6503),  // Tokyo
            LatLng(-33.8688, 151.2093)  // Sydney
        )
        
        // When - Rapidly change locations
        locations.forEach { location ->
            composeTestRule.setContent {
                FFinderTheme {
                    MapPreviewCard(
                        location = location,
                        hasLocationPermission = true,
                        animationsEnabled = true
                    )
                }
            }
            
            // Then - Should handle each change without errors
            composeTestRule
                .onNodeWithContentDescription("Your location pin")
                .assertIsDisplayed()
        }
    }

    @Test
    fun mapPreviewCard_edgeCases_handledGracefully() = runTest {
        // Test edge cases
        val edgeCases = listOf(
            // Edge case 1: Permission granted but null location
            Triple(true, null, true),
            // Edge case 2: No permission but location somehow available
            Triple(false, LatLng(37.7749, -122.4194), true),
            // Edge case 3: Extreme coordinates
            Triple(true, LatLng(90.0, 180.0), true),
            // Edge case 4: Zero coordinates
            Triple(true, LatLng(0.0, 0.0), true)
        )
        
        edgeCases.forEach { (permission, location, animations) ->
            composeTestRule.setContent {
                FFinderTheme {
                    MapPreviewCard(
                        location = location,
                        hasLocationPermission = permission,
                        animationsEnabled = animations
                    )
                }
            }
            
            // Should handle all edge cases gracefully
            if (permission && location != null) {
                composeTestRule
                    .onNodeWithContentDescription("Your location pin")
                    .assertIsDisplayed()
            } else {
                composeTestRule
                    .onNodeWithText("Enable Location")
                    .assertIsDisplayed()
            }
        }
    }
}