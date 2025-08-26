package com.locationsharing.app.ui.map.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.android.gms.maps.model.LatLng
import com.locationsharing.app.ui.map.MapScreenState
import com.locationsharing.app.ui.theme.FFinderTheme
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Integration tests for ShareStatusSheet component with MapScreen state management.
 * 
 * Tests integration scenarios:
 * - ShareStatusSheet with MapScreenState
 * - State transitions and UI updates
 * - Integration with location sharing workflow
 * - Performance under state changes
 * - Error handling integration
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ShareStatusSheetIntegrationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testLocation = LatLng(37.7749, -122.4194)

    @Test
    fun shareStatusSheet_integratesWithMapScreenState_correctly() = runTest {
        // Given
        val initialState = MapScreenState(
            currentLocation = testLocation,
            isLocationSharingActive = false,
            isStatusSheetVisible = false,
            hasLocationPermission = true
        )

        var currentState = initialState
        var stopSharingCalled = false
        var dismissCalled = false

        // When
        composeTestRule.setContent {
            FFinderTheme {
                ShareStatusSheet(
                    isVisible = currentState.isStatusSheetVisible,
                    isLocationSharingActive = currentState.isLocationSharingActive,
                    currentLocation = currentState.currentLocation,
                    onDismiss = { dismissCalled = true },
                    onStopSharing = { stopSharingCalled = true }
                )
            }
        }

        // Then - Initially not visible
        composeTestRule
            .onNodeWithContentDescription("Location sharing status dialog")
            .assertDoesNotExist()

        // When - Show sheet with active sharing
        currentState = currentState.copy(
            isStatusSheetVisible = true,
            isLocationSharingActive = true
        )

        composeTestRule.setContent {
            FFinderTheme {
                ShareStatusSheet(
                    isVisible = currentState.isStatusSheetVisible,
                    isLocationSharingActive = currentState.isLocationSharingActive,
                    currentLocation = currentState.currentLocation,
                    onDismiss = { dismissCalled = true },
                    onStopSharing = { stopSharingCalled = true }
                )
            }
        }

        // Then - Should be visible with active state
        composeTestRule
            .onNodeWithContentDescription("Location sharing status dialog")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Location Sharing Active")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Stop Sharing")
            .assertIsDisplayed()
    }

    @Test
    fun shareStatusSheet_locationSharingWorkflow_integration() = runTest {
        // Given - Simulate complete location sharing workflow
        var currentState = MapScreenState(
            currentLocation = testLocation,
            isLocationSharingActive = false,
            isStatusSheetVisible = false,
            hasLocationPermission = true
        )

        var stopSharingCalled = false
        var dismissCalled = false

        // Step 1: Start sharing (sheet not visible yet)
        currentState = currentState.copy(isLocationSharingActive = true)

        composeTestRule.setContent {
            FFinderTheme {
                ShareStatusSheet(
                    isVisible = currentState.isStatusSheetVisible,
                    isLocationSharingActive = currentState.isLocationSharingActive,
                    currentLocation = currentState.currentLocation,
                    onDismiss = { dismissCalled = true },
                    onStopSharing = { stopSharingCalled = true }
                )
            }
        }

        // Sheet should not be visible
        composeTestRule
            .onNodeWithContentDescription("Location sharing status dialog")
            .assertDoesNotExist()

        // Step 2: Show status sheet
        currentState = currentState.copy(isStatusSheetVisible = true)

        composeTestRule.setContent {
            FFinderTheme {
                ShareStatusSheet(
                    isVisible = currentState.isStatusSheetVisible,
                    isLocationSharingActive = currentState.isLocationSharingActive,
                    currentLocation = currentState.currentLocation,
                    onDismiss = { dismissCalled = true },
                    onStopSharing = { stopSharingCalled = true }
                )
            }
        }

        // Should show active sharing state
        composeTestRule
            .onNodeWithText("Location Sharing Active")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Stop Sharing")
            .assertIsDisplayed()

        // Step 3: User stops sharing
        composeTestRule
            .onNodeWithText("Stop Sharing")
            .performClick()

        // Callback should be triggered
        assert(stopSharingCalled) { "Stop sharing callback should be called" }

        // Step 4: Update state to reflect stopped sharing
        currentState = currentState.copy(
            isLocationSharingActive = false,
            isStatusSheetVisible = false
        )

        composeTestRule.setContent {
            FFinderTheme {
                ShareStatusSheet(
                    isVisible = currentState.isStatusSheetVisible,
                    isLocationSharingActive = currentState.isLocationSharingActive,
                    currentLocation = currentState.currentLocation,
                    onDismiss = { dismissCalled = true },
                    onStopSharing = { stopSharingCalled = true }
                )
            }
        }

        // Sheet should be hidden
        composeTestRule
            .onNodeWithContentDescription("Location sharing status dialog")
            .assertDoesNotExist()
    }

    @Test
    fun shareStatusSheet_locationUpdates_reflectedInUI() = runTest {
        // Given - Start with one location
        val initialLocation = LatLng(37.7749, -122.4194)
        var currentLocation = initialLocation

        composeTestRule.setContent {
            FFinderTheme {
                ShareStatusSheet(
                    isVisible = true,
                    isLocationSharingActive = true,
                    currentLocation = currentLocation,
                    onDismiss = {},
                    onStopSharing = {}
                )
            }
        }

        // Then - Should show initial coordinates
        composeTestRule
            .onNodeWithText("Lat: 37.774900\nLng: -122.419400")
            .assertIsDisplayed()

        // When - Location changes
        currentLocation = LatLng(40.7128, -74.0060) // New York

        composeTestRule.setContent {
            FFinderTheme {
                ShareStatusSheet(
                    isVisible = true,
                    isLocationSharingActive = true,
                    currentLocation = currentLocation,
                    onDismiss = {},
                    onStopSharing = {}
                )
            }
        }

        // Then - Should show updated coordinates
        composeTestRule
            .onNodeWithText("Lat: 40.712800\nLng: -74.006000")
            .assertIsDisplayed()

        // Previous coordinates should not be visible
        composeTestRule
            .onNodeWithText("Lat: 37.774900\nLng: -122.419400")
            .assertDoesNotExist()
    }

    @Test
    fun shareStatusSheet_errorStates_handledGracefully() = runTest {
        // Test case 1: No location permission
        var currentState = MapScreenState(
            currentLocation = null,
            isLocationSharingActive = false,
            isStatusSheetVisible = true,
            hasLocationPermission = false,
            locationError = "Location permission denied"
        )

        composeTestRule.setContent {
            FFinderTheme {
                ShareStatusSheet(
                    isVisible = currentState.isStatusSheetVisible,
                    isLocationSharingActive = currentState.isLocationSharingActive,
                    currentLocation = currentState.currentLocation,
                    onDismiss = {},
                    onStopSharing = {}
                )
            }
        }

        // Should show location unavailable
        composeTestRule
            .onNodeWithText("Location unavailable")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Location Sharing Off")
            .assertIsDisplayed()

        // Test case 2: Location sharing error
        currentState = currentState.copy(
            isLocationSharingActive = true,
            locationSharingError = "Failed to share location"
        )

        composeTestRule.setContent {
            FFinderTheme {
                ShareStatusSheet(
                    isVisible = currentState.isStatusSheetVisible,
                    isLocationSharingActive = currentState.isLocationSharingActive,
                    currentLocation = currentState.currentLocation,
                    onDismiss = {},
                    onStopSharing = {}
                )
            }
        }

        // Should still show active state (error handling is done at higher level)
        composeTestRule
            .onNodeWithText("Location Sharing Active")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Stop Sharing")
            .assertIsDisplayed()
    }

    @Test
    fun shareStatusSheet_performanceUnderStateChanges() = runTest {
        // Given - Rapid state changes to test performance
        var currentState = MapScreenState(
            currentLocation = testLocation,
            isLocationSharingActive = false,
            isStatusSheetVisible = false
        )

        val stateChanges = listOf(
            currentState.copy(isStatusSheetVisible = true),
            currentState.copy(isStatusSheetVisible = true, isLocationSharingActive = true),
            currentState.copy(isStatusSheetVisible = true, isLocationSharingActive = false),
            currentState.copy(isStatusSheetVisible = false, isLocationSharingActive = true),
            currentState.copy(isStatusSheetVisible = true, isLocationSharingActive = true)
        )

        // When - Apply rapid state changes
        stateChanges.forEach { state ->
            composeTestRule.setContent {
                FFinderTheme {
                    ShareStatusSheet(
                        isVisible = state.isStatusSheetVisible,
                        isLocationSharingActive = state.isLocationSharingActive,
                        currentLocation = state.currentLocation,
                        onDismiss = {},
                        onStopSharing = {}
                    )
                }
            }

            // Then - UI should update correctly for each state
            if (state.isStatusSheetVisible) {
                composeTestRule
                    .onNodeWithContentDescription("Location sharing status dialog")
                    .assertIsDisplayed()

                val expectedStatusText = if (state.isLocationSharingActive) {
                    "Location Sharing Active"
                } else {
                    "Location Sharing Off"
                }

                composeTestRule
                    .onNodeWithText(expectedStatusText)
                    .assertIsDisplayed()
            } else {
                composeTestRule
                    .onNodeWithContentDescription("Location sharing status dialog")
                    .assertDoesNotExist()
            }
        }
    }

    @Test
    fun shareStatusSheet_accessibilityIntegration_worksCorrectly() = runTest {
        // Given
        val state = MapScreenState(
            currentLocation = testLocation,
            isLocationSharingActive = true,
            isStatusSheetVisible = true,
            hasLocationPermission = true
        )

        composeTestRule.setContent {
            FFinderTheme {
                ShareStatusSheet(
                    isVisible = state.isStatusSheetVisible,
                    isLocationSharingActive = state.isLocationSharingActive,
                    currentLocation = state.currentLocation,
                    onDismiss = {},
                    onStopSharing = {}
                )
            }
        }

        // Then - All accessibility features should work
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
    }

    @Test
    fun shareStatusSheet_multipleLocationUpdates_performanceTest() = runTest {
        // Given - Multiple location updates to simulate real-world usage
        val locations = listOf(
            LatLng(37.7749, -122.4194), // San Francisco
            LatLng(40.7128, -74.0060),  // New York
            LatLng(51.5074, -0.1278),   // London
            LatLng(35.6762, 139.6503),  // Tokyo
            LatLng(-33.8688, 151.2093)  // Sydney
        )

        // When - Apply each location update
        locations.forEach { location ->
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

            // Then - Should display correct coordinates
            val expectedText = "Lat: ${"%.6f".format(location.latitude)}\nLng: ${"%.6f".format(location.longitude)}"
            
            composeTestRule
                .onNodeWithText(expectedText)
                .assertIsDisplayed()
        }
    }
}