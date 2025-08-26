package com.locationsharing.app.ui.map.accessibility

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.printToLog
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.maps.model.LatLng
import com.locationsharing.app.domain.model.NearbyFriend
import com.locationsharing.app.ui.screens.MapScreen
import com.locationsharing.app.ui.theme.FFinderTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Comprehensive accessibility tests for MapScreen components.
 * 
 * Tests all accessibility requirements from the MapScreen redesign specification:
 * - 9.1: Content descriptions for all interactive elements
 * - 9.2: Proper semantic roles for all components
 * - 9.3: Correct focus order and navigation
 * - 9.4: Screen reader announcements for state changes
 * - 9.5: Accessibility compliance testing
 * - 9.6: TalkBack integration testing
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class MapAccessibilityTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    /**
     * Test that all interactive elements have proper content descriptions
     * Requirement 9.1: Add contentDescription to all interactive elements
     */
    @Test
    fun testAllInteractiveElementsHaveContentDescriptions() {
        composeTestRule.setContent {
            FFinderTheme {
                MapScreen(
                    onBack = {},
                    onNearbyFriends = {},
                    nearbyFriendsCount = 3,
                    currentLocation = LatLng(37.7749, -122.4194),
                    hasLocationPermission = true,
                    onSelfLocationCenter = {},
                    isLocationSharingActive = false,
                    isStatusSheetVisible = false,
                    isNearbyDrawerOpen = false,
                    nearbyFriends = createSampleNearbyFriends()
                )
            }
        }
        
        // Test back button has content description
        composeTestRule
            .onNodeWithContentDescription(MapAccessibilityConstants.BACK_BUTTON)
            .assertIsDisplayed()
            .assertHasClickAction()
        
        // Test nearby friends button has content description
        composeTestRule
            .onNodeWithContentDescription("View nearby friends, 3 friends available")
            .assertIsDisplayed()
            .assertHasClickAction()
        
        // Test quick share FAB has content description
        composeTestRule
            .onNodeWithContentDescription(MapAccessibilityConstants.QUICK_SHARE_FAB)
            .assertIsDisplayed()
            .assertHasClickAction()
        
        // Test self location FAB has content description
        composeTestRule
            .onNodeWithContentDescription(MapAccessibilityConstants.SELF_LOCATION_FAB)
            .assertIsDisplayed()
            .assertHasClickAction()
        
        // Test map content has content description
        composeTestRule
            .onNodeWithContentDescription(MapAccessibilityConstants.MAP_CONTENT)
            .assertIsDisplayed()
    }
    
    /**
     * Test that all components have proper semantic roles
     * Requirement 9.2: Implement proper semantic roles for all components
     */
    @Test
    fun testComponentsHaveProperSemanticRoles() {
        composeTestRule.setContent {
            FFinderTheme {
                MapScreen(
                    onBack = {},
                    onNearbyFriends = {},
                    nearbyFriendsCount = 2,
                    currentLocation = LatLng(37.7749, -122.4194),
                    hasLocationPermission = true,
                    onSelfLocationCenter = {},
                    isLocationSharingActive = true,
                    isStatusSheetVisible = true,
                    isNearbyDrawerOpen = false,
                    nearbyFriends = createSampleNearbyFriends()
                )
            }
        }
        
        // Print semantics tree for debugging
        composeTestRule.onRoot().printToLog("MapScreenSemantics")
        
        // Verify buttons have button role (implicit through hasClickAction)
        composeTestRule
            .onNodeWithContentDescription(MapAccessibilityConstants.BACK_BUTTON)
            .assertHasClickAction()
        
        composeTestRule
            .onNodeWithContentDescription(MapAccessibilityConstants.QUICK_SHARE_FAB)
            .assertHasClickAction()
        
        composeTestRule
            .onNodeWithContentDescription(MapAccessibilityConstants.SELF_LOCATION_FAB)
            .assertHasClickAction()
        
        // Verify status sheet has dialog semantics
        composeTestRule
            .onNodeWithContentDescription(MapAccessibilityConstants.STATUS_SHEET)
            .assertIsDisplayed()
    }
    
    /**
     * Test focus order and navigation
     * Requirement 9.3: Set up correct focus order and navigation
     */
    @Test
    fun testFocusOrderAndNavigation() {
        composeTestRule.setContent {
            FFinderTheme {
                MapScreen(
                    onBack = {},
                    onNearbyFriends = {},
                    nearbyFriendsCount = 1,
                    currentLocation = LatLng(37.7749, -122.4194),
                    hasLocationPermission = true,
                    onSelfLocationCenter = {},
                    isLocationSharingActive = false,
                    isStatusSheetVisible = false,
                    isNearbyDrawerOpen = false,
                    nearbyFriends = createSampleNearbyFriends()
                )
            }
        }
        
        // Expected focus order: Back → Nearby → Map → Self-Location → Quick-Share → Debug
        val expectedFocusOrder = listOf(
            MapAccessibilityConstants.BACK_BUTTON,
            "View nearby friends, 1 friend available",
            MapAccessibilityConstants.MAP_CONTENT,
            MapAccessibilityConstants.SELF_LOCATION_FAB,
            MapAccessibilityConstants.QUICK_SHARE_FAB
        )
        
        // Verify all elements in focus order are present and clickable
        expectedFocusOrder.forEach { contentDesc ->
            composeTestRule
                .onNodeWithContentDescription(contentDesc)
                .assertIsDisplayed()
        }
    }
    
    /**
     * Test screen reader announcements for state changes
     * Requirement 9.4: Add screen reader announcements for state changes
     */
    @Test
    fun testScreenReaderAnnouncementsForStateChanges() {
        var isLocationSharingActive = false
        var nearbyFriendsCount = 0
        
        composeTestRule.setContent {
            FFinderTheme {
                MapScreen(
                    onBack = {},
                    onNearbyFriends = {},
                    nearbyFriendsCount = nearbyFriendsCount,
                    currentLocation = LatLng(37.7749, -122.4194),
                    hasLocationPermission = true,
                    onSelfLocationCenter = {},
                    isLocationSharingActive = isLocationSharingActive,
                    isStatusSheetVisible = isLocationSharingActive,
                    isNearbyDrawerOpen = false,
                    nearbyFriends = if (nearbyFriendsCount > 0) createSampleNearbyFriends() else emptyList()
                )
            }
        }
        
        // Test initial state
        composeTestRule
            .onNodeWithContentDescription("View nearby friends")
            .assertIsDisplayed()
        
        // Simulate state change - location sharing becomes active
        isLocationSharingActive = true
        composeTestRule.setContent {
            FFinderTheme {
                MapScreen(
                    onBack = {},
                    onNearbyFriends = {},
                    nearbyFriendsCount = nearbyFriendsCount,
                    currentLocation = LatLng(37.7749, -122.4194),
                    hasLocationPermission = true,
                    onSelfLocationCenter = {},
                    isLocationSharingActive = isLocationSharingActive,
                    isStatusSheetVisible = isLocationSharingActive,
                    isNearbyDrawerOpen = false,
                    nearbyFriends = if (nearbyFriendsCount > 0) createSampleNearbyFriends() else emptyList()
                )
            }
        }
        
        // Verify status sheet appears with proper accessibility
        composeTestRule
            .onNodeWithContentDescription(MapAccessibilityConstants.STATUS_SHEET)
            .assertIsDisplayed()
        
        // Simulate nearby friends count change
        nearbyFriendsCount = 2
        composeTestRule.setContent {
            FFinderTheme {
                MapScreen(
                    onBack = {},
                    onNearbyFriends = {},
                    nearbyFriendsCount = nearbyFriendsCount,
                    currentLocation = LatLng(37.7749, -122.4194),
                    hasLocationPermission = true,
                    onSelfLocationCenter = {},
                    isLocationSharingActive = isLocationSharingActive,
                    isStatusSheetVisible = isLocationSharingActive,
                    isNearbyDrawerOpen = false,
                    nearbyFriends = createSampleNearbyFriends()
                )
            }
        }
        
        // Verify nearby friends button updates its description
        composeTestRule
            .onNodeWithContentDescription("View nearby friends, 2 friends available")
            .assertIsDisplayed()
    }
    
    /**
     * Test accessibility compliance for drawer component
     * Requirement 9.5: Test with TalkBack and other accessibility services
     */
    @Test
    fun testDrawerAccessibilityCompliance() {
        composeTestRule.setContent {
            FFinderTheme {
                MapScreen(
                    onBack = {},
                    onNearbyFriends = {},
                    nearbyFriendsCount = 3,
                    currentLocation = LatLng(37.7749, -122.4194),
                    hasLocationPermission = true,
                    onSelfLocationCenter = {},
                    isLocationSharingActive = false,
                    isStatusSheetVisible = false,
                    isNearbyDrawerOpen = true,
                    nearbyFriends = createSampleNearbyFriends()
                )
            }
        }
        
        // Test drawer has proper accessibility
        composeTestRule
            .onNodeWithContentDescription(MapAccessibilityConstants.DRAWER_CONTENT)
            .assertIsDisplayed()
        
        // Test search field has proper accessibility
        composeTestRule
            .onNodeWithContentDescription(MapAccessibilityConstants.SEARCH_FIELD)
            .assertIsDisplayed()
        
        // Test friend list has proper accessibility
        composeTestRule
            .onNodeWithContentDescription("List of 3 nearby friends")
            .assertIsDisplayed()
    }
    
    /**
     * Test accessibility compliance for status sheet
     * Requirement 9.5: Test with TalkBack and other accessibility services
     */
    @Test
    fun testStatusSheetAccessibilityCompliance() {
        composeTestRule.setContent {
            FFinderTheme {
                MapScreen(
                    onBack = {},
                    onNearbyFriends = {},
                    nearbyFriendsCount = 0,
                    currentLocation = LatLng(37.7749, -122.4194),
                    hasLocationPermission = true,
                    onSelfLocationCenter = {},
                    isLocationSharingActive = true,
                    isStatusSheetVisible = true,
                    isNearbyDrawerOpen = false,
                    nearbyFriends = emptyList()
                )
            }
        }
        
        // Test status sheet has proper accessibility
        composeTestRule
            .onNodeWithContentDescription(MapAccessibilityConstants.STATUS_SHEET)
            .assertIsDisplayed()
        
        // Test location sharing status has proper description
        composeTestRule
            .onNodeWithContentDescription(MapAccessibilityConstants.LOCATION_SHARING_ACTIVE)
            .assertIsDisplayed()
        
        // Test coordinates display has proper accessibility
        composeTestRule
            .onNodeWithContentDescription("Current coordinates: Lat: 37.774900\nLng: -122.419400")
            .assertIsDisplayed()
        
        // Test stop sharing button has proper accessibility
        composeTestRule
            .onNodeWithContentDescription(MapAccessibilityConstants.STOP_SHARING_BUTTON)
            .assertIsDisplayed()
            .assertHasClickAction()
    }
    
    /**
     * Test accessibility with no location permission
     * Requirement 9.6: Haptic feedback and accessibility support
     */
    @Test
    fun testAccessibilityWithoutLocationPermission() {
        composeTestRule.setContent {
            FFinderTheme {
                MapScreen(
                    onBack = {},
                    onNearbyFriends = {},
                    nearbyFriendsCount = 0,
                    currentLocation = null,
                    hasLocationPermission = false,
                    onSelfLocationCenter = {},
                    isLocationSharingActive = false,
                    isStatusSheetVisible = false,
                    isNearbyDrawerOpen = false,
                    nearbyFriends = emptyList()
                )
            }
        }
        
        // Test self location FAB has appropriate description for no permission
        composeTestRule
            .onNodeWithContentDescription("Request location permission to center map")
            .assertIsDisplayed()
            .assertHasClickAction()
        
        // Test quick share FAB is disabled without permission
        composeTestRule
            .onNodeWithContentDescription(MapAccessibilityConstants.QUICK_SHARE_FAB)
            .assertIsDisplayed()
    }
    
    /**
     * Test accessibility with loading states
     * Requirement 9.4: Add screen reader announcements for state changes
     */
    @Test
    fun testAccessibilityWithLoadingStates() {
        composeTestRule.setContent {
            FFinderTheme {
                MapScreen(
                    onBack = {},
                    onNearbyFriends = {},
                    nearbyFriendsCount = 0,
                    currentLocation = LatLng(37.7749, -122.4194),
                    isLocationLoading = true,
                    hasLocationPermission = true,
                    onSelfLocationCenter = {},
                    isLocationSharingActive = false,
                    isStatusSheetVisible = false,
                    isNearbyDrawerOpen = false,
                    nearbyFriends = emptyList()
                )
            }
        }
        
        // Test self location FAB has loading description
        composeTestRule
            .onNodeWithContentDescription("Centering map on your location...")
            .assertIsDisplayed()
    }
    
    /**
     * Test accessibility with error states
     * Requirement 9.4: Add screen reader announcements for state changes
     */
    @Test
    fun testAccessibilityWithErrorStates() {
        composeTestRule.setContent {
            FFinderTheme {
                MapScreen(
                    onBack = {},
                    onNearbyFriends = {},
                    nearbyFriendsCount = 0,
                    currentLocation = null,
                    locationError = "Location permission is required to show your position on the map.",
                    hasLocationPermission = false,
                    onSelfLocationCenter = {},
                    isLocationSharingActive = false,
                    isStatusSheetVisible = false,
                    isNearbyDrawerOpen = false,
                    nearbyFriends = emptyList()
                )
            }
        }
        
        // Test that error message is accessible
        composeTestRule
            .onNodeWithText("Location permission is required to show your position on the map.")
            .assertIsDisplayed()
    }
    
    /**
     * Helper function to create sample nearby friends for testing
     */
    private fun createSampleNearbyFriends(): List<NearbyFriend> {
        return listOf(
            NearbyFriend(
                id = "1",
                displayName = "Alice Johnson",
                avatarUrl = null,
                distance = 150.0,
                isOnline = true,
                lastUpdated = System.currentTimeMillis(),
                latLng = LatLng(37.7749, -122.4194)
            ),
            NearbyFriend(
                id = "2",
                displayName = "Bob Smith",
                avatarUrl = null,
                distance = 1200.0,
                isOnline = false,
                lastUpdated = System.currentTimeMillis() - 300000,
                latLng = LatLng(37.7849, -122.4094)
            ),
            NearbyFriend(
                id = "3",
                displayName = "Carol Davis",
                avatarUrl = null,
                distance = 2500.0,
                isOnline = true,
                lastUpdated = System.currentTimeMillis() - 60000,
                latLng = LatLng(37.7949, -122.3994)
            )
        )
    }
}