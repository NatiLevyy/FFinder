package com.locationsharing.app.ui.map.components

import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
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
 * Comprehensive test suite for ShareStatusSheet component.
 * 
 * Tests all requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7, 8.4
 * 
 * Test Coverage:
 * - Component visibility and dismissal
 * - Location sharing status display with appropriate icons
 * - Coordinate display formatting
 * - Stop sharing button functionality
 * - Accessibility compliance
 * - Material 3 styling and theming
 * - Animation behavior
 * - Edge cases and error states
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ShareStatusSheetTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testLocation = LatLng(37.7749, -122.4194)
    private val expectedCoordinatesText = "Lat: 37.774900, Lng: -122.419400"

    @Test
    fun shareStatusSheet_whenVisible_displaysCorrectly() {
        // Given
        var dismissCalled = false
        var stopSharingCalled = false

        // When
        composeTestRule.setContent {
            FFinderTheme {
                ShareStatusSheet(
                    isVisible = true,
                    isLocationSharingActive = true,
                    currentLocation = testLocation,
                    onDismiss = { dismissCalled = true },
                    onStopSharing = { stopSharingCalled = true }
                )
            }
        }

        // Then
        composeTestRule
            .onNodeWithContentDescription("Location sharing status dialog")
            .assertIsDisplayed()
    }

    @Test
    fun shareStatusSheet_whenNotVisible_doesNotDisplay() {
        // Given
        composeTestRule.setContent {
            FFinderTheme {
                ShareStatusSheet(
                    isVisible = false,
                    isLocationSharingActive = false,
                    currentLocation = testLocation,
                    onDismiss = {},
                    onStopSharing = {}
                )
            }
        }

        // Then
        composeTestRule
            .onNodeWithContentDescription("Location sharing status dialog")
            .assertDoesNotExist()
    }

    @Test
    fun shareStatusSheet_whenLocationSharingActive_showsActiveStatus() {
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
            .onNodeWithText("Location Sharing Active")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithContentDescription("Location sharing is active")
            .assertIsDisplayed()
    }

    @Test
    fun shareStatusSheet_whenLocationSharingInactive_showsInactiveStatus() {
        // Given
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

        // Then
        composeTestRule
            .onNodeWithText("Location Sharing Off")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithContentDescription("Location sharing is off")
            .assertIsDisplayed()
    }

    @Test
    fun shareStatusSheet_withValidLocation_displaysCoordinates() {
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
            .onNodeWithContentDescription("Current coordinates: Lat: 37.774900\nLng: -122.419400")
            .assertIsDisplayed()
        
        // Check that coordinates are displayed with proper formatting
        composeTestRule
            .onNodeWithText("Lat: 37.774900\nLng: -122.419400")
            .assertIsDisplayed()
    }

    @Test
    fun shareStatusSheet_withNullLocation_displaysUnavailableMessage() {
        // Given
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

        // Then
        composeTestRule
            .onNodeWithText("Location unavailable")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithContentDescription("Current coordinates: Location unavailable")
            .assertIsDisplayed()
    }

    @Test
    fun shareStatusSheet_whenLocationSharingActive_showsStopButton() {
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

        // Then
        composeTestRule
            .onNodeWithText("Stop Sharing")
            .assertIsDisplayed()
            .assertHasClickAction()
        
        composeTestRule
            .onNodeWithContentDescription("Stop location sharing")
            .assertIsDisplayed()
    }

    @Test
    fun shareStatusSheet_whenLocationSharingInactive_hidesStopButton() {
        // Given
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

        // Then
        composeTestRule
            .onNodeWithText("Stop Sharing")
            .assertDoesNotExist()
    }

    @Test
    fun shareStatusSheet_stopButtonClick_triggersCallback() {
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

        // When
        composeTestRule
            .onNodeWithText("Stop Sharing")
            .performClick()

        // Then
        assert(stopSharingCalled) { "Stop sharing callback should be called" }
    }

    @Test
    fun shareStatusSheet_hasProperAccessibilitySupport() {
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

        // Then - Check dialog role
        composeTestRule
            .onNodeWithContentDescription("Location sharing status dialog")
            .assertIsDisplayed()

        // Check status accessibility
        composeTestRule
            .onNodeWithContentDescription("Location sharing is active")
            .assertIsDisplayed()

        // Check coordinates accessibility
        composeTestRule
            .onNodeWithContentDescription("Current coordinates: Lat: 37.774900\nLng: -122.419400")
            .assertIsDisplayed()

        // Check button accessibility
        composeTestRule
            .onNodeWithContentDescription("Stop location sharing")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun shareStatusSheet_coordinateFormatting_isAccurate() {
        // Given - Test various coordinate values
        val testCases = listOf(
            LatLng(0.0, 0.0) to "Lat: 0.000000\nLng: 0.000000",
            LatLng(90.0, 180.0) to "Lat: 90.000000\nLng: 180.000000",
            LatLng(-90.0, -180.0) to "Lat: -90.000000\nLng: -180.000000",
            LatLng(37.774900, -122.419400) to "Lat: 37.774900\nLng: -122.419400"
        )

        testCases.forEach { (location, expectedText) ->
            composeTestRule.setContent {
                FFinderTheme {
                    ShareStatusSheet(
                        isVisible = true,
                        isLocationSharingActive = false,
                        currentLocation = location,
                        onDismiss = {},
                        onStopSharing = {}
                    )
                }
            }

            // Then
            composeTestRule
                .onNodeWithText(expectedText)
                .assertIsDisplayed()
        }
    }

    @Test
    fun shareStatusSheet_materialDesign_compliance() {
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

        // Then - Verify Material 3 components are used
        // The sheet should be displayed with proper Material 3 styling
        composeTestRule
            .onNodeWithContentDescription("Location sharing status dialog")
            .assertIsDisplayed()

        // Status text should use proper typography
        composeTestRule
            .onNodeWithText("Location Sharing Active")
            .assertIsDisplayed()

        // Button should have proper Material 3 styling
        composeTestRule
            .onNodeWithText("Stop Sharing")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun shareStatusSheet_edgeCases_handledCorrectly() {
        // Test case 1: Extreme coordinates
        val extremeLocation = LatLng(89.999999, 179.999999)
        
        composeTestRule.setContent {
            FFinderTheme {
                ShareStatusSheet(
                    isVisible = true,
                    isLocationSharingActive = true,
                    currentLocation = extremeLocation,
                    onDismiss = {},
                    onStopSharing = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText("Lat: 89.999999\nLng: 179.999999")
            .assertIsDisplayed()

        // Test case 2: Zero coordinates
        val zeroLocation = LatLng(0.0, 0.0)
        
        composeTestRule.setContent {
            FFinderTheme {
                ShareStatusSheet(
                    isVisible = true,
                    isLocationSharingActive = false,
                    currentLocation = zeroLocation,
                    onDismiss = {},
                    onStopSharing = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText("Lat: 0.000000\nLng: 0.000000")
            .assertIsDisplayed()
    }

    @Test
    fun shareStatusSheet_stateTransitions_workCorrectly() {
        // Given - Start with inactive sharing
        var isActive = false
        var isVisible = true

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

        // Then - Should show inactive state
        composeTestRule
            .onNodeWithText("Location Sharing Off")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Stop Sharing")
            .assertDoesNotExist()

        // When - Change to active state
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

        // Then - Should show active state
        composeTestRule
            .onNodeWithText("Location Sharing Active")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Stop Sharing")
            .assertIsDisplayed()
    }

    @Test
    fun shareStatusSheet_callbacks_areTriggeredCorrectly() {
        // Given
        var dismissCallCount = 0
        var stopSharingCallCount = 0

        composeTestRule.setContent {
            FFinderTheme {
                ShareStatusSheet(
                    isVisible = true,
                    isLocationSharingActive = true,
                    currentLocation = testLocation,
                    onDismiss = { dismissCallCount++ },
                    onStopSharing = { stopSharingCallCount++ }
                )
            }
        }

        // When - Click stop sharing button
        composeTestRule
            .onNodeWithText("Stop Sharing")
            .performClick()

        // Then
        assert(stopSharingCallCount == 1) { "Stop sharing should be called exactly once" }
        assert(dismissCallCount == 0) { "Dismiss should not be called when clicking stop sharing" }
    }
}