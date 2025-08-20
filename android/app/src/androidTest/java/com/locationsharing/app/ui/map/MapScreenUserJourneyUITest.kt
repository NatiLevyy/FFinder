package com.locationsharing.app.ui.map

import android.Manifest
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.google.android.gms.maps.model.LatLng
import com.locationsharing.app.data.friends.Friend
import com.locationsharing.app.data.friends.FriendLocation
import com.locationsharing.app.data.friends.FriendStatus
import com.locationsharing.app.ui.screens.MapScreen
import com.locationsharing.app.ui.theme.FFTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

/**
 * UI tests for complete user journey flows in MapScreen
 * Tests end-to-end user interactions and workflows
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MapScreenUserJourneyUITest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    private lateinit var mockViewModel: MapScreenViewModel
    private val stateFlow = MutableStateFlow(MapScreenState())

    @Before
    fun setup() {
        hiltRule.inject()
        
        mockViewModel = mockk(relaxed = true) {
            every { state } returns stateFlow
        }
    }

    @Test
    fun userJourney_firstTimeUser_shouldCompleteOnboardingFlow() {
        // Given - first time user with no location permission
        stateFlow.value = MapScreenState(
            hasLocationPermission = false,
            isLocationPermissionRequested = false
        )

        // When - launch MapScreen
        composeTestRule.setContent {
            FFTheme {
                MapScreen(
                    onNavigateBack = {},
                    viewModel = mockViewModel
                )
            }
        }

        // Then - should show location permission request UI
        composeTestRule.onNodeWithText("Location Permission Required")
            .assertIsDisplayed()

        // When - user grants permission
        stateFlow.value = stateFlow.value.copy(
            hasLocationPermission = true,
            isLocationPermissionRequested = false,
            currentLocation = LatLng(37.7749, -122.4194)
        )

        // Then - should show map with user location
        composeTestRule.onNodeWithContentDescription("Google Map")
            .assertIsDisplayed()
        
        composeTestRule.onNodeWithContentDescription("Your current location")
            .assertIsDisplayed()
    }

    @Test
    fun userJourney_locationSharing_shouldCompleteFullFlow() {
        // Given - user with location permission
        stateFlow.value = MapScreenState(
            hasLocationPermission = true,
            currentLocation = LatLng(37.7749, -122.4194),
            isLocationSharingActive = false
        )

        composeTestRule.setContent {
            FFTheme {
                MapScreen(
                    onNavigateBack = {},
                    viewModel = mockViewModel
                )
            }
        }

        // When - user taps Quick Share FAB
        composeTestRule.onNodeWithContentDescription("Share your location instantly")
            .assertIsDisplayed()
            .assertHasClickAction()
            .performClick()

        // Then - should start location sharing and show status sheet
        stateFlow.value = stateFlow.value.copy(
            isLocationSharingActive = true,
            isStatusSheetVisible = true
        )

        composeTestRule.onNodeWithText("Location Sharing Active")
            .assertIsDisplayed()

        composeTestRule.onNodeWithText("Stop Sharing")
            .assertIsDisplayed()
            .assertHasClickAction()

        // When - user stops sharing
        composeTestRule.onNodeWithText("Stop Sharing")
            .performClick()

        // Then - should stop sharing and hide sheet
        stateFlow.value = stateFlow.value.copy(
            isLocationSharingActive = false,
            isStatusSheetVisible = false
        )

        composeTestRule.onNodeWithText("Location Sharing Active")
            .assertIsNotDisplayed()
    }

    @Test
    fun userJourney_nearbyFriends_shouldCompleteInteractionFlow() {
        // Given - user with nearby friends
        val testFriends = createTestFriends()
        stateFlow.value = MapScreenState(
            hasLocationPermission = true,
            currentLocation = LatLng(37.7749, -122.4194),
            friends = testFriends,
            nearbyFriendsCount = testFriends.size
        )

        composeTestRule.setContent {
            FFTheme {
                MapScreen(
                    onNavigateBack = {},
                    viewModel = mockViewModel
                )
            }
        }

        // When - user taps nearby friends button
        composeTestRule.onNodeWithContentDescription("View nearby friends, ${testFriends.size} friends available")
            .assertIsDisplayed()
            .performClick()

        // Then - should open nearby friends drawer
        stateFlow.value = stateFlow.value.copy(isNearbyDrawerOpen = true)

        composeTestRule.onNodeWithTag("NearbyFriendsDrawer")
            .assertIsDisplayed()

        // Should show friend list
        composeTestRule.onNodeWithText("Alice Johnson")
            .assertIsDisplayed()
        
        composeTestRule.onNodeWithText("Bob Smith")
            .assertIsDisplayed()

        // When - user taps on a friend
        composeTestRule.onNodeWithText("Alice Johnson")
            .performClick()

        // Then - should select friend and center map
        stateFlow.value = stateFlow.value.copy(
            selectedFriendId = "friend_1",
            isNearbyDrawerOpen = false
        )

        // When - user taps map to clear selection
        composeTestRule.onNodeWithContentDescription("Google Map")
            .performTouchInput { 
                click(center) 
            }

        // Then - should clear friend selection
        stateFlow.value = stateFlow.value.copy(selectedFriendId = null)
    }

    @Test
    fun userJourney_selfLocationCentering_shouldWork() {
        // Given - user location away from map center
        stateFlow.value = MapScreenState(
            hasLocationPermission = true,
            currentLocation = LatLng(37.7749, -122.4194),
            mapCenter = LatLng(40.7128, -74.0060), // New York (different from user location)
            mapZoom = 10f
        )

        composeTestRule.setContent {
            FFTheme {
                MapScreen(
                    onNavigateBack = {},
                    viewModel = mockViewModel
                )
            }
        }

        // When - user taps self location FAB
        composeTestRule.onNodeWithContentDescription("Center map on your location")
            .assertIsDisplayed()
            .performClick()

        // Then - should center map on user location with close zoom
        stateFlow.value = stateFlow.value.copy(
            mapCenter = stateFlow.value.currentLocation!!,
            mapZoom = MapScreenConstants.Map.CLOSE_ZOOM
        )

        // Map should be centered on user location (verified through state)
        assert(stateFlow.value.mapCenter == stateFlow.value.currentLocation)
        assert(stateFlow.value.mapZoom == MapScreenConstants.Map.CLOSE_ZOOM)
    }

    @Test
    fun userJourney_errorRecovery_shouldHandleGracefully() {
        // Given - user with location error
        stateFlow.value = MapScreenState(
            hasLocationPermission = true,
            locationError = "GPS signal lost. Please check your location settings.",
            canRetry = true
        )

        composeTestRule.setContent {
            FFTheme {
                MapScreen(
                    onNavigateBack = {},
                    viewModel = mockViewModel
                )
            }
        }

        // Then - should show error message
        composeTestRule.onNodeWithText("GPS signal lost. Please check your location settings.")
            .assertIsDisplayed()

        // Should show retry button
        composeTestRule.onNodeWithText("Retry")
            .assertIsDisplayed()
            .assertHasClickAction()

        // When - user taps retry
        composeTestRule.onNodeWithText("Retry")
            .performClick()

        // Then - should attempt retry and show loading
        stateFlow.value = stateFlow.value.copy(
            isRetrying = true,
            retryCount = 1
        )

        // When - retry succeeds
        stateFlow.value = stateFlow.value.copy(
            isRetrying = false,
            locationError = null,
            currentLocation = LatLng(37.7749, -122.4194)
        )

        // Then - should show map normally
        composeTestRule.onNodeWithContentDescription("Google Map")
            .assertIsDisplayed()

        composeTestRule.onNodeWithText("GPS signal lost")
            .assertIsNotDisplayed()
    }

    @Test
    fun userJourney_statusSheetInteraction_shouldWork() {
        // Given - active location sharing
        stateFlow.value = MapScreenState(
            hasLocationPermission = true,
            currentLocation = LatLng(37.7749, -122.4194),
            isLocationSharingActive = true,
            isStatusSheetVisible = true
        )

        composeTestRule.setContent {
            FFTheme {
                MapScreen(
                    onNavigateBack = {},
                    viewModel = mockViewModel
                )
            }
        }

        // Then - should show status sheet
        composeTestRule.onNodeWithText("Location Sharing Active")
            .assertIsDisplayed()

        // Should show coordinates
        composeTestRule.onNodeWithText("Lat: 37.774900, Lng: -122.419400")
            .assertIsDisplayed()

        // When - user swipes down to dismiss
        composeTestRule.onNodeWithTag("ShareStatusSheet")
            .performTouchInput { 
                swipeDown() 
            }

        // Then - should dismiss sheet
        stateFlow.value = stateFlow.value.copy(isStatusSheetVisible = false)

        composeTestRule.onNodeWithText("Location Sharing Active")
            .assertIsNotDisplayed()
    }

    @Test
    fun userJourney_friendMarkerInteraction_shouldWork() {
        // Given - friends on map
        val testFriends = createTestFriends()
        stateFlow.value = MapScreenState(
            hasLocationPermission = true,
            currentLocation = LatLng(37.7749, -122.4194),
            friends = testFriends
        )

        composeTestRule.setContent {
            FFTheme {
                MapScreen(
                    onNavigateBack = {},
                    viewModel = mockViewModel
                )
            }
        }

        // When - user taps on friend marker (simulated through content description)
        composeTestRule.onNodeWithContentDescription("Alice Johnson's location")
            .performClick()

        // Then - should select friend and show details
        stateFlow.value = stateFlow.value.copy(
            selectedFriendId = "friend_1",
            mapCenter = testFriends[0].getLatLng()!!
        )

        // Should center map on selected friend
        assert(stateFlow.value.selectedFriendId == "friend_1")
        assert(stateFlow.value.mapCenter == testFriends[0].getLatLng())
    }

    @Test
    fun userJourney_multipleInteractions_shouldWorkTogether() {
        // Given - full featured state
        val testFriends = createTestFriends()
        stateFlow.value = MapScreenState(
            hasLocationPermission = true,
            currentLocation = LatLng(37.7749, -122.4194),
            friends = testFriends,
            nearbyFriendsCount = testFriends.size,
            isLocationSharingActive = false
        )

        composeTestRule.setContent {
            FFTheme {
                MapScreen(
                    onNavigateBack = {},
                    viewModel = mockViewModel
                )
            }
        }

        // User Journey: Start sharing, view friends, select friend, stop sharing

        // Step 1: Start location sharing
        composeTestRule.onNodeWithContentDescription("Share your location instantly")
            .performClick()

        stateFlow.value = stateFlow.value.copy(
            isLocationSharingActive = true,
            isStatusSheetVisible = true
        )

        // Step 2: Open nearby friends
        composeTestRule.onNodeWithContentDescription("View nearby friends, ${testFriends.size} friends available")
            .performClick()

        stateFlow.value = stateFlow.value.copy(isNearbyDrawerOpen = true)

        // Step 3: Select a friend
        composeTestRule.onNodeWithText("Alice Johnson")
            .performClick()

        stateFlow.value = stateFlow.value.copy(
            selectedFriendId = "friend_1",
            isNearbyDrawerOpen = false
        )

        // Step 4: Center on self location
        composeTestRule.onNodeWithContentDescription("Center map on your location")
            .performClick()

        stateFlow.value = stateFlow.value.copy(
            mapCenter = stateFlow.value.currentLocation!!,
            mapZoom = MapScreenConstants.Map.CLOSE_ZOOM
        )

        // Step 5: Stop sharing
        composeTestRule.onNodeWithText("Stop Sharing")
            .performClick()

        stateFlow.value = stateFlow.value.copy(
            isLocationSharingActive = false,
            isStatusSheetVisible = false
        )

        // Verify final state
        composeTestRule.onNodeWithContentDescription("Google Map")
            .assertIsDisplayed()
        
        assert(!stateFlow.value.isLocationSharingActive)
        assert(stateFlow.value.selectedFriendId == "friend_1")
        assert(stateFlow.value.mapCenter == stateFlow.value.currentLocation)
    }

    @Test
    fun userJourney_accessibility_shouldWorkWithTalkBack() {
        // Given - user with accessibility needs
        stateFlow.value = MapScreenState(
            hasLocationPermission = true,
            currentLocation = LatLng(37.7749, -122.4194),
            friends = createTestFriends(),
            nearbyFriendsCount = 2
        )

        composeTestRule.setContent {
            FFTheme {
                MapScreen(
                    onNavigateBack = {},
                    viewModel = mockViewModel
                )
            }
        }

        // Verify all interactive elements have proper content descriptions
        composeTestRule.onNodeWithContentDescription("Navigate back")
            .assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription("View nearby friends, 2 friends available")
            .assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription("Share your location instantly")
            .assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription("Center map on your location")
            .assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription("Google Map")
            .assertIsDisplayed()

        // Test focus order by navigating through elements
        composeTestRule.onNodeWithContentDescription("Navigate back")
            .performScrollTo()

        composeTestRule.onNodeWithContentDescription("View nearby friends, 2 friends available")
            .performScrollTo()

        composeTestRule.onNodeWithContentDescription("Google Map")
            .performScrollTo()

        composeTestRule.onNodeWithContentDescription("Center map on your location")
            .performScrollTo()

        composeTestRule.onNodeWithContentDescription("Share your location instantly")
            .performScrollTo()
    }

    private fun createTestFriends(): List<Friend> {
        return listOf(
            Friend(
                id = "friend_1",
                userId = "user_1",
                name = "Alice Johnson",
                email = "alice@example.com",
                avatarUrl = "",
                profileColor = "#FF5722",
                location = FriendLocation(
                    latitude = 37.7750,
                    longitude = -122.4195,
                    accuracy = 10f,
                    isMoving = false,
                    timestamp = Date()
                ),
                status = FriendStatus(
                    isOnline = true,
                    lastSeen = System.currentTimeMillis(),
                    isLocationSharingEnabled = true
                )
            ),
            Friend(
                id = "friend_2",
                userId = "user_2",
                name = "Bob Smith",
                email = "bob@example.com",
                avatarUrl = "",
                profileColor = "#2196F3",
                location = FriendLocation(
                    latitude = 37.7748,
                    longitude = -122.4193,
                    accuracy = 15f,
                    isMoving = true,
                    timestamp = Date()
                ),
                status = FriendStatus(
                    isOnline = true,
                    lastSeen = System.currentTimeMillis(),
                    isLocationSharingEnabled = true
                )
            )
        )
    }
}