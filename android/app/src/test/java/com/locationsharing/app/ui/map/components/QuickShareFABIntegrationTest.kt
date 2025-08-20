package com.locationsharing.app.ui.map.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.locationsharing.app.ui.map.MapScreenConstants
import com.locationsharing.app.ui.map.MapScreenEvent
import com.locationsharing.app.ui.map.MapScreenState
import com.locationsharing.app.ui.theme.FFinderTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

/**
 * Integration tests for QuickShareFAB component with MapScreen state and events
 * Tests requirements 3.1, 3.2, 3.3, 3.4, 3.5, 8.2, 9.6
 */
@RunWith(AndroidJUnit4::class)
class QuickShareFABIntegrationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun quickShareFAB_integrationWithMapScreenState_inactiveSharing() {
        // Given
        val mockOnEvent = mock<(MapScreenEvent) -> Unit>()
        val state = MapScreenState(
            isLocationSharingActive = false,
            hasLocationPermission = true
        )

        // When
        composeTestRule.setContent {
            FFinderTheme {
                QuickShareFAB(
                    onClick = { mockOnEvent(MapScreenEvent.OnQuickShare) },
                    isLocationSharingActive = state.isLocationSharingActive
                )
            }
        }

        // Then
        composeTestRule
            .onNodeWithContentDescription(MapScreenConstants.Accessibility.QUICK_SHARE_FAB_DESC)
            .assertExists()
            .assertIsDisplayed()
            .performClick()

        verify(mockOnEvent).invoke(MapScreenEvent.OnQuickShare)
    }

    @Test
    fun quickShareFAB_integrationWithMapScreenState_activeSharing() {
        // Given
        val mockOnEvent = mock<(MapScreenEvent) -> Unit>()
        val state = MapScreenState(
            isLocationSharingActive = true,
            hasLocationPermission = true
        )

        // When
        composeTestRule.setContent {
            FFinderTheme {
                QuickShareFAB(
                    onClick = { mockOnEvent(MapScreenEvent.OnQuickShare) },
                    isLocationSharingActive = state.isLocationSharingActive
                )
            }
        }

        // Then
        composeTestRule
            .onNodeWithContentDescription("Location sharing is active, tap to manage")
            .assertExists()
            .assertIsDisplayed()
            .performClick()

        verify(mockOnEvent).invoke(MapScreenEvent.OnQuickShare)
    }

    @Test
    fun quickShareFAB_integrationWithMapScreenState_noLocationPermission() {
        // Given
        val mockOnEvent = mock<(MapScreenEvent) -> Unit>()
        val state = MapScreenState(
            isLocationSharingActive = false,
            hasLocationPermission = false
        )

        // When
        composeTestRule.setContent {
            FFinderTheme {
                QuickShareFAB(
                    onClick = { mockOnEvent(MapScreenEvent.OnQuickShare) },
                    isLocationSharingActive = state.isLocationSharingActive,
                    enabled = state.hasLocationPermission
                )
            }
        }

        // Then - FAB should still be displayed but clicking should not trigger event
        composeTestRule
            .onNodeWithContentDescription(MapScreenConstants.Accessibility.QUICK_SHARE_FAB_DESC)
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun quickShareFAB_integrationWithMapScreenState_loadingState() {
        // Given
        val mockOnEvent = mock<(MapScreenEvent) -> Unit>()
        val state = MapScreenState(
            isLocationSharingActive = false,
            hasLocationPermission = true,
            isLocationLoading = true
        )

        // When
        composeTestRule.setContent {
            FFinderTheme {
                QuickShareFAB(
                    onClick = { mockOnEvent(MapScreenEvent.OnQuickShare) },
                    isLocationSharingActive = state.isLocationSharingActive,
                    enabled = !state.isLocationLoading
                )
            }
        }

        // Then - FAB should be displayed but disabled during loading
        composeTestRule
            .onNodeWithContentDescription(MapScreenConstants.Accessibility.QUICK_SHARE_FAB_DESC)
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun quickShareFAB_integrationWithMapScreenState_errorState() {
        // Given
        val mockOnEvent = mock<(MapScreenEvent) -> Unit>()
        val state = MapScreenState(
            isLocationSharingActive = false,
            hasLocationPermission = true,
            locationSharingError = "Failed to share location"
        )

        // When
        composeTestRule.setContent {
            FFinderTheme {
                QuickShareFAB(
                    onClick = { mockOnEvent(MapScreenEvent.OnQuickShare) },
                    isLocationSharingActive = state.isLocationSharingActive,
                    enabled = state.locationSharingError == null
                )
            }
        }

        // Then - FAB should be displayed but disabled during error state
        composeTestRule
            .onNodeWithContentDescription(MapScreenConstants.Accessibility.QUICK_SHARE_FAB_DESC)
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun quickShareFAB_integrationWithMapScreenState_stateTransitions() {
        // Given
        val mockOnEvent = mock<(MapScreenEvent) -> Unit>()
        var state = MapScreenState(
            isLocationSharingActive = false,
            hasLocationPermission = true
        )

        // When - Initial state
        composeTestRule.setContent {
            FFinderTheme {
                QuickShareFAB(
                    onClick = { mockOnEvent(MapScreenEvent.OnQuickShare) },
                    isLocationSharingActive = state.isLocationSharingActive
                )
            }
        }

        // Then - Initial state verification
        composeTestRule
            .onNodeWithContentDescription(MapScreenConstants.Accessibility.QUICK_SHARE_FAB_DESC)
            .assertExists()

        // When - State changes to active
        state = state.copy(isLocationSharingActive = true)
        composeTestRule.setContent {
            FFinderTheme {
                QuickShareFAB(
                    onClick = { mockOnEvent(MapScreenEvent.OnQuickShare) },
                    isLocationSharingActive = state.isLocationSharingActive
                )
            }
        }

        // Then - Active state verification
        composeTestRule
            .onNodeWithContentDescription("Location sharing is active, tap to manage")
            .assertExists()
    }

    @Test
    fun quickShareFAB_integrationWithMapScreenEvents_properEventHandling() {
        // Given
        val mockOnEvent = mock<(MapScreenEvent) -> Unit>()
        val state = MapScreenState(
            isLocationSharingActive = false,
            hasLocationPermission = true
        )

        // When
        composeTestRule.setContent {
            FFinderTheme {
                QuickShareFAB(
                    onClick = { 
                        // Simulate proper event handling
                        mockOnEvent(MapScreenEvent.OnQuickShare)
                    },
                    isLocationSharingActive = state.isLocationSharingActive
                )
            }
        }

        // Then - Multiple clicks should trigger multiple events
        composeTestRule
            .onNodeWithContentDescription(MapScreenConstants.Accessibility.QUICK_SHARE_FAB_DESC)
            .performClick()
            .performClick()

        verify(mockOnEvent, org.mockito.kotlin.times(2)).invoke(MapScreenEvent.OnQuickShare)
    }
}