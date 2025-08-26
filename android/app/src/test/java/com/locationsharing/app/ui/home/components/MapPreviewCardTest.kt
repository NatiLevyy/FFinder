package com.locationsharing.app.ui.home.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.google.android.gms.maps.model.LatLng
import com.locationsharing.app.ui.theme.FFinderTheme
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for MapPreviewCard component.
 * 
 * Tests cover:
 * - Component rendering with and without location permission
 * - Fallback UI display and interaction
 * - Accessibility content descriptions
 * - Animation behavior with accessibility preferences
 * - Error handling scenarios
 */
class MapPreviewCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun mapPreviewCard_withLocationPermission_displaysMapContent() {
        // Given
        val testLocation = LatLng(37.7749, -122.4194) // San Francisco
        
        // When
        composeTestRule.setContent {
            FFinderTheme {
                MapPreviewCard(
                    location = testLocation,
                    hasLocationPermission = true,
                    animationsEnabled = true
                )
            }
        }
        
        // Then
        composeTestRule
            .onNodeWithContentDescription("Your location pin")
            .assertIsDisplayed()
    }

    @Test
    fun mapPreviewCard_withoutLocationPermission_displaysFallbackUI() {
        // Given
        var permissionRequested = false
        
        // When
        composeTestRule.setContent {
            FFinderTheme {
                MapPreviewCard(
                    location = null,
                    hasLocationPermission = false,
                    animationsEnabled = true,
                    onPermissionRequest = { permissionRequested = true }
                )
            }
        }
        
        // Then
        composeTestRule
            .onNodeWithContentDescription("Location preview unavailable")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Map Preview")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Enable location to see your area")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Enable Location")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun mapPreviewCard_fallbackEnableLocationButton_triggersCallback() {
        // Given
        var permissionRequested = false
        
        composeTestRule.setContent {
            FFinderTheme {
                MapPreviewCard(
                    location = null,
                    hasLocationPermission = false,
                    animationsEnabled = true,
                    onPermissionRequest = { permissionRequested = true }
                )
            }
        }
        
        // When
        composeTestRule
            .onNodeWithText("Enable Location")
            .performClick()
        
        // Then
        assert(permissionRequested) { "Permission request callback should be triggered" }
    }

    @Test
    fun mapPreviewCard_withAnimationsDisabled_stillDisplaysContent() {
        // Given
        val testLocation = LatLng(37.7749, -122.4194)
        
        // When
        composeTestRule.setContent {
            FFinderTheme {
                MapPreviewCard(
                    location = testLocation,
                    hasLocationPermission = true,
                    animationsEnabled = false // Accessibility: animations disabled
                )
            }
        }
        
        // Then
        composeTestRule
            .onNodeWithContentDescription("Your location pin")
            .assertIsDisplayed()
    }

    @Test
    fun mapPreviewCard_hasProperAccessibilitySupport() {
        // Given
        val testLocation = LatLng(37.7749, -122.4194)
        
        // When
        composeTestRule.setContent {
            FFinderTheme {
                MapPreviewCard(
                    location = testLocation,
                    hasLocationPermission = true,
                    animationsEnabled = true
                )
            }
        }
        
        // Then - Check that pin has proper content description
        composeTestRule
            .onNodeWithContentDescription("Your location pin")
            .assertIsDisplayed()
    }

    @Test
    fun mapPreviewCard_fallback_hasProperAccessibilitySupport() {
        // When
        composeTestRule.setContent {
            FFinderTheme {
                MapPreviewCard(
                    location = null,
                    hasLocationPermission = false,
                    animationsEnabled = true
                )
            }
        }
        
        // Then - Check accessibility content descriptions
        composeTestRule
            .onNodeWithContentDescription("Location preview unavailable")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Enable Location")
            .assertHasClickAction()
    }

    @Test
    fun mapPreviewWithErrorHandling_displaysErrorState() {
        // When
        composeTestRule.setContent {
            FFinderTheme {
                MapPreviewWithErrorHandling(
                    location = null,
                    hasLocationPermission = false,
                    animationsEnabled = true
                )
            }
        }
        
        // Then - Should display fallback when no permission
        composeTestRule
            .onNodeWithText("Enable Location")
            .assertIsDisplayed()
    }

    @Test
    fun mapPreviewCard_cardProperties_areCorrect() {
        // Given
        val testLocation = LatLng(37.7749, -122.4194)
        
        // When
        composeTestRule.setContent {
            FFinderTheme {
                MapPreviewCard(
                    location = testLocation,
                    hasLocationPermission = true,
                    animationsEnabled = true
                )
            }
        }
        
        // Then - Card should be displayed (we can't directly test dimensions in Compose tests,
        // but we can verify the component renders without errors)
        composeTestRule
            .onNodeWithContentDescription("Your location pin")
            .assertIsDisplayed()
    }

    @Test
    fun mapPreviewCard_withNullLocation_butPermissionGranted_displaysFallback() {
        // Given - Permission granted but location is null (edge case)
        
        // When
        composeTestRule.setContent {
            FFinderTheme {
                MapPreviewCard(
                    location = null,
                    hasLocationPermission = true, // Permission granted but no location
                    animationsEnabled = true
                )
            }
        }
        
        // Then - Should display fallback UI
        composeTestRule
            .onNodeWithText("Enable Location")
            .assertIsDisplayed()
    }

    @Test
    fun mapPreviewCard_multipleInstances_renderIndependently() {
        // Given
        val location1 = LatLng(37.7749, -122.4194) // San Francisco
        val location2 = LatLng(40.7128, -74.0060)  // New York
        
        // When
        composeTestRule.setContent {
            FFinderTheme {
                androidx.compose.foundation.layout.Column {
                    MapPreviewCard(
                        location = location1,
                        hasLocationPermission = true,
                        animationsEnabled = true
                    )
                    MapPreviewCard(
                        location = null,
                        hasLocationPermission = false,
                        animationsEnabled = true
                    )
                }
            }
        }
        
        // Then - Both components should render
        composeTestRule
            .onAllNodesWithContentDescription("Your location pin")
            .assertCountEquals(1) // Only one has location
        
        composeTestRule
            .onNodeWithText("Enable Location")
            .assertIsDisplayed() // Fallback is shown for second instance
    }
}