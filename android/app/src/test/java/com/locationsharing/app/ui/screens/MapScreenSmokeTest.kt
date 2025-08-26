package com.locationsharing.app.ui.screens

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.maps.model.LatLng
import com.locationsharing.app.domain.model.NearbyFriend
import com.locationsharing.app.ui.theme.FFinderTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Smoke test for MapScreen redesign
 * Tests basic functionality without crashing
 */
@RunWith(AndroidJUnit4::class)
class MapScreenSmokeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun mapScreen_launchesSuccessfully() {
        // Arrange
        var quickShareClicked = false

        // Act
        composeTestRule.setContent {
            FFinderTheme {
                MapScreen(
                    onBack = {},
                    onNearbyFriends = {},
                    nearbyFriendsCount = 2,
                    currentLocation = LatLng(37.7749, -122.4194),
                    hasLocationPermission = true,
                    isLocationSharingActive = false,
                    onQuickShare = { quickShareClicked = true },
                    nearbyFriends = listOf(
                        NearbyFriend(
                            id = "1",
                            displayName = "Test Friend",
                            avatarUrl = null,
                            distance = 100.0,
                            isOnline = true,
                            lastUpdated = System.currentTimeMillis(),
                            latLng = LatLng(37.7749, -122.4194)
                        )
                    )
                )
            }
        }

        // Assert - No crash occurred and components are displayed
        composeTestRule.onNodeWithTag("map_content").assertExists()
        composeTestRule.onNodeWithTag("back_button").assertExists()
        composeTestRule.onNodeWithTag("nearby_friends_button").assertExists()
    }

    @Test
    fun quickShareFAB_clickable_noCrash() {
        // Arrange
        var quickShareClicked = false

        composeTestRule.setContent {
            FFinderTheme {
                MapScreen(
                    onBack = {},
                    onNearbyFriends = {},
                    currentLocation = LatLng(37.7749, -122.4194),
                    hasLocationPermission = true,
                    onQuickShare = { quickShareClicked = true }
                )
            }
        }

        // Act - Click the Quick Share FAB
        composeTestRule.onNodeWithTag("quick_share_fab").performClick()

        // Assert - No crash and callback triggered
        assert(quickShareClicked) { "Quick share callback should have been triggered" }
    }

    @Test
    fun mapScreen_withLocationError_displaysErrorHandler() {
        // Arrange
        val errorMessage = "Location permission required"

        composeTestRule.setContent {
            FFinderTheme {
                MapScreen(
                    onBack = {},
                    onNearbyFriends = {},
                    locationError = errorMessage,
                    hasLocationPermission = false
                )
            }
        }

        // Assert - Error handler is displayed
        composeTestRule.onNodeWithTag("location_error").assertExists()
    }

    @Test
    fun mapScreen_withShareStatusSheet_displaysSheet() {
        // Arrange
        composeTestRule.setContent {
            FFinderTheme {
                MapScreen(
                    onBack = {},
                    onNearbyFriends = {},
                    currentLocation = LatLng(37.7749, -122.4194),
                    isLocationSharingActive = true,
                    isStatusSheetVisible = true
                )
            }
        }

        // Assert - Status sheet is displayed
        composeTestRule.onNodeWithTag("share_status_sheet").assertExists()
    }

    @Test
    fun mapScreen_withNearbyDrawer_displaysDrawer() {
        // Arrange
        val nearbyFriends = listOf(
            NearbyFriend(
                id = "1",
                displayName = "Test Friend",
                avatarUrl = null,
                distance = 150.0,
                isOnline = true,
                lastUpdated = System.currentTimeMillis(),
                latLng = LatLng(37.7749, -122.4194)
            )
        )

        composeTestRule.setContent {
            FFinderTheme {
                MapScreen(
                    onBack = {},
                    onNearbyFriends = {},
                    isNearbyDrawerOpen = true,
                    nearbyFriends = nearbyFriends
                )
            }
        }

        // Assert - Drawer is displayed
        composeTestRule.onNodeWithTag("nearby_friends_drawer").assertExists()
    }
}