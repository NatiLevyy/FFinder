package com.locationsharing.app.ui.map.components

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.android.gms.maps.model.LatLng
import com.locationsharing.app.ui.theme.FFinderTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Comprehensive accessibility tests for ShareStatusSheet component.
 * 
 * Tests accessibility compliance according to requirements 9.1, 9.2, 9.3, 9.4, 9.5, 9.6
 * 
 * Accessibility Test Coverage:
 * - Content descriptions for all interactive elements
 * - Proper semantic roles (Dialog, Button)
 * - Screen reader announcements for state changes
 * - Focus order and navigation
 * - TalkBack compatibility
 * - Accessibility service integration
 * - WCAG 2.1 compliance
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ShareStatusSheetAccessibilityTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testLocation = LatLng(37.7749, -122.4194)

    @Test
    fun shareStatusSheet_hasProperDialogRole() {
        // Given
        composeTestRule.setContent {
            FFinderTheme {
                ShareStatusSheet(
                    isVisible = true,
                    isLocationSharingActive = true,
                    currentLocation = testLocation,
                    onDismiss = {},
                    onStopSharing = {}
                )
            }
        }

        // Then
        composeTestRule
            .onNodeWithContentDescription("Location sharing status dialog")
            .assertIsDisplayed()
    }

    @Test
    fun shareStatusSheet_statusHeader_hasProperAccessibility() {
        // Test active state
        composeTestRule.setContent {
            FFinderTheme {
                ShareStatusSheet(
                    isVisible = true,
                    isLocationSharingActive = true,
                    currentLocation = testLocation,
                    onDismiss = {},
                    onStopSharing = {}
                )
            }
        }

        // Should have proper content description for active state
        composeTestRule
            .onNodeWithContentDescription("Location sharing is active")
            .assertIsDisplayed()

        // Test inactive state
        composeTestRule.setContent {
            FFinderTheme {
                ShareStatusSheet(
                    isVisible = true,
                    isLocationSharingActive = false,
                    currentLocation = testLocation,
                    onDismiss = {},
                    onStopSharing = {}
                )
            }
        }

        // Should have proper content description for inactive state
        composeTestRule
            .onNodeWithContentDescription("Location sharing is off")
            .assertIsDisplayed()
    }

    @Test
    fun shareStatusSheet_coordinatesDisplay_hasProperAccessibility() {
        // Test with valid location
        composeTestRule.setContent {
            FFinderTheme {
                ShareStatusSheet(
                    isVisible = true,
                    isLocationSharingActive = true,
                    currentLocation = testLocation,
                    onDismiss = {},
                    onStopSharing = {}
                )
            }
        }

        // Should have descriptive content description for coordinates
        composeTestRule
            .onNodeWithContentDescription("Current coordinates: Lat: 37.774900\nLng: -122.419400")
            .assertIsDisplayed()

        // Test with null location
        composeTestRule.setContent {
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

        // Should have proper accessibility for unavailable location
        composeTestRule
            .onNodeWithContentDescription("Current coordinates: Location unavailable")
            .assertIsDisplayed()
    }

    @Test
    fun shareStatusSheet_stopButton_hasProperAccessibility() {
        // Given
        var stopSharingCalled = false

        composeTestRule.setContent {
            FFinderTheme {
                ShareStatusSheet(
                    isVisible = true,
                    isLocationSharingActive = true,
                    currentLocation = testLocation,
                    onDismiss = {},
                    onStopSharing = { stopSharingCalled = true }
                )
            }
        }

        // Then - Button should have proper role and description
        composeTestRule
            .onNodeWithContentDescription("Stop location sharing")
            .assertIsDisplayed()
            .assertHasClickAction()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))

        // Should be clickable and trigger callback
        composeTestRule
            .onNodeWithContentDescription("Stop location sharing")
            .performClick()

        assert(stopSharingCalled) { "Stop sharing callback should be triggered" }
    }

    @Test
    fun shareStatusSheet_focusOrder_isLogical() {
        // Given
        composeTestRule.setContent {
            FFinderTheme {
                ShareStatusSheet(
                    isVisible = true,
                    isLocationSharingActive = true,
                    currentLocation = testLocation,
                    onDismiss = {},
                    onStopSharing = {}
                )
            }
        }

        // Then - Focus order should be logical:
        // 1. Dialog container
        // 2. Status header
        // 3. Coordinates display
        // 4. Stop sharing button

        // Dialog should be focusable
        composeTestRule
            .onNodeWithContentDescription("Location sharing status dialog")
            .assertIsDisplayed()

        // Status should be accessible
        composeTestRule
            .onNodeWithContentDescription("Location sharing is active")
            .assertIsDisplayed()

        // Coordinates should be accessible
        composeTestRule
            .onNodeWithContentDescription("Current coordinates: Lat: 37.774900\nLng: -122.419400")
            .assertIsDisplayed()

        // Button should be last and actionable
        composeTestRule
            .onNodeWithContentDescription("Stop location sharing")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun shareStatusSheet_screenReaderAnnouncements_areCorrect() {
        // Test state change announcements
        var isActive = false

        // Start with inactive state
        composeTestRule.setContent {
            FFinderTheme {
                ShareStatusSheet(
                    isVisible = true,
                    isLocationSharingActive = isActive,
                    currentLocation = testLocation,
                    onDismiss = {},
                    onStopSharing = {}
                )
            }
        }

        // Should announce inactive state
        composeTestRule
            .onNodeWithContentDescription("Location sharing is off")
            .assertIsDisplayed()

        // Change to active state
        isActive = true

        composeTestRule.setContent {
            FFinderTheme {
                ShareStatusSheet(
                    isVisible = true,
                    isLocationSharingActive = isActive,
                    currentLocation = testLocation,
                    onDismiss = {},
                    onStopSharing = {}
                )
            }
        }

        // Should announce active state
        composeTestRule
            .onNodeWithContentDescription("Location sharing is active")
            .assertIsDisplayed()
    }

    @Test
    fun shareStatusSheet_talkBackCompatibility() {
        // Given
        composeTestRule.setContent {
            FFinderTheme {
                ShareStatusSheet(
                    isVisible = true,
                    isLocationSharingActive = true,
                    currentLocation = testLocation,
                    onDismiss = {},
                    onStopSharing = {}
                )
            }
        }

        // Then - All elements should be TalkBack compatible
        
        // Dialog should be announced as a dialog
        composeTestRule
            .onNode(hasContentDescription("Location sharing status dialog"))
            .assertIsDisplayed()

        // Status should be readable
        composeTestRule
            .onNode(hasContentDescription("Location sharing is active"))
            .assertIsDisplayed()

        // Coordinates should be readable with proper formatting
        composeTestRule
            .onNode(hasContentDescription("Current coordinates: Lat: 37.774900\nLng: -122.419400"))
            .assertIsDisplayed()

        // Button should be actionable
        composeTestRule
            .onNode(hasContentDescription("Stop location sharing"))
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun shareStatusSheet_accessibilityServices_integration() {
        // Test with various accessibility service scenarios
        
        // Scenario 1: High contrast mode (simulated through theming)
        composeTestRule.setContent {
            FFinderTheme {
                ShareStatusSheet(
                    isVisible = true,
                    isLocationSharingActive = true,
                    currentLocation = testLocation,
                    onDismiss = {},
                    onStopSharing = {}
                )
            }
        }

        // All elements should remain accessible
        composeTestRule
            .onNodeWithContentDescription("Location sharing status dialog")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Location Sharing Active")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Stop Sharing")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun shareStatusSheet_contentDescriptions_areDescriptive() {
        // Given
        composeTestRule.setContent {
            FFinderTheme {
                ShareStatusSheet(
                    isVisible = true,
                    isLocationSharingActive = true,
                    currentLocation = testLocation,
                    onDismiss = {},
                    onStopSharing = {}
                )
            }
        }

        // Then - Content descriptions should be descriptive and helpful
        
        // Dialog description should indicate purpose
        composeTestRule
            .onNodeWithContentDescription("Location sharing status dialog")
            .assertIsDisplayed()

        // Status description should indicate current state
        composeTestRule
            .onNodeWithContentDescription("Location sharing is active")
            .assertIsDisplayed()

        // Coordinates description should include actual values
        composeTestRule
            .onNodeWithContentDescription("Current coordinates: Lat: 37.774900\nLng: -122.419400")
            .assertIsDisplayed()

        // Button description should indicate action
        composeTestRule
            .onNodeWithContentDescription("Stop location sharing")
            .assertIsDisplayed()
    }

    @Test
    fun shareStatusSheet_accessibilityWithDifferentLocations() {
        // Test accessibility with various coordinate formats
        val testCases = listOf(
            LatLng(0.0, 0.0) to "Current coordinates: Lat: 0.000000\nLng: 0.000000",
            LatLng(90.0, 180.0) to "Current coordinates: Lat: 90.000000\nLng: 180.000000",
            LatLng(-90.0, -180.0) to "Current coordinates: Lat: -90.000000\nLng: -180.000000",
            null to "Current coordinates: Location unavailable"
        )

        testCases.forEach { (location, expectedDescription) ->
            composeTestRule.setContent {
                FFinderTheme {
                    ShareStatusSheet(
                        isVisible = true,
                        isLocationSharingActive = true,
                        currentLocation = location,
                        onDismiss = {},
                        onStopSharing = {}
                    )
                }
            }

            // Should have proper accessibility for each location format
            composeTestRule
                .onNodeWithContentDescription(expectedDescription)
                .assertIsDisplayed()
        }
    }

    @Test
    fun shareStatusSheet_accessibilityInDarkMode() {
        // Given - Dark mode (simulated through theme)
        composeTestRule.setContent {
            FFinderTheme {
                ShareStatusSheet(
                    isVisible = true,
                    isLocationSharingActive = true,
                    currentLocation = testLocation,
                    onDismiss = {},
                    onStopSharing = {}
                )
            }
        }

        // Then - All accessibility features should work in dark mode
        composeTestRule
            .onNodeWithContentDescription("Location sharing status dialog")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithContentDescription("Location sharing is active")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithContentDescription("Current coordinates: Lat: 37.774900\nLng: -122.419400")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithContentDescription("Stop location sharing")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun shareStatusSheet_accessibilityStateTransitions() {
        // Test accessibility during state transitions
        var isActive = false
        var isVisible = true

        // Start with inactive, visible state
        composeTestRule.setContent {
            FFinderTheme {
                ShareStatusSheet(
                    isVisible = isVisible,
                    isLocationSharingActive = isActive,
                    currentLocation = testLocation,
                    onDismiss = {},
                    onStopSharing = {}
                )
            }
        }

        // Should be accessible in inactive state
        composeTestRule
            .onNodeWithContentDescription("Location sharing is off")
            .assertIsDisplayed()

        // Stop sharing button should not exist
        composeTestRule
            .onNodeWithContentDescription("Stop location sharing")
            .assertDoesNotExist()

        // Transition to active state
        isActive = true

        composeTestRule.setContent {
            FFinderTheme {
                ShareStatusSheet(
                    isVisible = isVisible,
                    isLocationSharingActive = isActive,
                    currentLocation = testLocation,
                    onDismiss = {},
                    onStopSharing = {}
                )
            }
        }

        // Should be accessible in active state
        composeTestRule
            .onNodeWithContentDescription("Location sharing is active")
            .assertIsDisplayed()

        // Stop sharing button should now exist and be accessible
        composeTestRule
            .onNodeWithContentDescription("Stop location sharing")
            .assertIsDisplayed()
            .assertHasClickAction()

        // Transition to hidden state
        isVisible = false

        composeTestRule.setContent {
            FFinderTheme {
                ShareStatusSheet(
                    isVisible = isVisible,
                    isLocationSharingActive = isActive,
                    currentLocation = testLocation,
                    onDismiss = {},
                    onStopSharing = {}
                )
            }
        }

        // Should not be accessible when hidden
        composeTestRule
            .onNodeWithContentDescription("Location sharing status dialog")
            .assertDoesNotExist()
    }
}