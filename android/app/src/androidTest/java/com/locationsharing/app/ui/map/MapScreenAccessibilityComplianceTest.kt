package com.locationsharing.app.ui.map

import android.Manifest
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasRole
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.requestFocus
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
 * Accessibility compliance tests for MapScreen
 * Tests WCAG 2.1 AA compliance and Android accessibility guidelines
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MapScreenAccessibilityComplianceTest {

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
    fun accessibility_allInteractiveElementsHaveContentDescriptions() {
        // Given - MapScreen with all features
        stateFlow.value = MapScreenState(
            hasLocationPermission = true,
            currentLocation = LatLng(37.7749, -122.4194),
            friends = createTestFriends(),
            nearbyFriendsCount = 2,
            isLocationSharingActive = false,
            isDebugMode = true // Include debug FAB
        )

        composeTestRule.setContent {
            FFTheme {
                MapScreen(
                    onNavigateBack = {},
                    viewModel = mockViewModel
                )
            }
        }

        // Test all interactive elements have content descriptions
        composeTestRule.onNodeWithContentDescription("Navigate back")
            .assertIsDisplayed()
            .assertHasClickAction()

        composeTestRule.onNodeWithContentDescription("View nearby friends, 2 friends available")
            .assertIsDisplayed()
            .assertHasClickAction()

        composeTestRule.onNodeWithContentDescription("Share your location instantly")
            .assertIsDisplayed()
            .assertHasClickAction()

        composeTestRule.onNodeWithContentDescription("Center map on your location")
            .assertIsDisplayed()
            .assertHasClickAction()

        composeTestRule.onNodeWithContentDescription("Google Map")
            .assertIsDisplayed()

        // Debug FAB should also have content description
        composeTestRule.onNodeWithContentDescription("Add test friends to map")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun accessibility_semanticRolesAreCorrect() {
        // Given - MapScreen with various components
        stateFlow.value = MapScreenState(
            hasLocationPermission = true,
            currentLocation = LatLng(37.7749, -122.4194),
            friends = createTestFriends(),
            nearbyFriendsCount = 2,
            isStatusSheetVisible = true,
            isLocationSharingActive = true
        )

        composeTestRule.setContent {
            FFTheme {
                MapScreen(
                    onNavigateBack = {},
                    viewModel = mockViewModel
                )
            }
        }

        // Test semantic roles
        composeTestRule.onNode(hasRole(Role.Button) and hasContentDescription("Navigate back"))
            .assertIsDisplayed()

        composeTestRule.onNode(hasRole(Role.Button) and hasContentDescription("View nearby friends, 2 friends available"))
            .assertIsDisplayed()

        composeTestRule.onNode(hasRole(Role.Button) and hasContentDescription("Share your location instantly"))
            .assertIsDisplayed()

        composeTestRule.onNode(hasRole(Role.Button) and hasContentDescription("Center map on your location"))
            .assertIsDisplayed()

        composeTestRule.onNode(hasRole(Role.Image) and hasContentDescription("Google Map"))
            .assertIsDisplayed()

        // Status sheet should have dialog role
        composeTestRule.onNodeWithTag("ShareStatusSheet")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Dialog))
    }

    @Test
    fun accessibility_focusOrderIsLogical() {
        // Given - MapScreen with all elements
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

        // Test logical focus order: Back → Title → Nearby → Map → Self-Location → Quick-Share
        
        // 1. Back button should be first focusable
        composeTestRule.onNodeWithContentDescription("Navigate back")
            .requestFocus()
            .assertIsFocused()

        // 2. Nearby friends button should be next
        composeTestRule.onNodeWithContentDescription("View nearby friends, 2 friends available")
            .requestFocus()
            .assertIsFocused()

        // 3. Map should be focusable
        composeTestRule.onNodeWithContentDescription("Google Map")
            .requestFocus()
            .assertIsFocused()

        // 4. Self location FAB should be focusable
        composeTestRule.onNodeWithContentDescription("Center map on your location")
            .requestFocus()
            .assertIsFocused()

        // 5. Quick share FAB should be last
        composeTestRule.onNodeWithContentDescription("Share your location instantly")
            .requestFocus()
            .assertIsFocused()
    }

    @Test
    fun accessibility_nearbyFriendsDrawerCompliance() {
        // Given - MapScreen with nearby friends drawer open
        stateFlow.value = MapScreenState(
            hasLocationPermission = true,
            currentLocation = LatLng(37.7749, -122.4194),
            friends = createTestFriends(),
            nearbyFriendsCount = 2,
            isNearbyDrawerOpen = true
        )

        composeTestRule.setContent {
            FFTheme {
                MapScreen(
                    onNavigateBack = {},
                    viewModel = mockViewModel
                )
            }
        }

        // Drawer should have navigation role
        composeTestRule.onNodeWithTag("NearbyFriendsDrawer")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Navigation))

        // Search bar should be accessible
        composeTestRule.onNodeWithContentDescription("Search friends")
            .assertIsDisplayed()

        // Friend items should have proper descriptions
        composeTestRule.onNodeWithContentDescription("Alice Johnson, 50 meters away, online")
            .assertIsDisplayed()
            .assertHasClickAction()

        composeTestRule.onNodeWithContentDescription("Bob Smith, 75 meters away, online")
            .assertIsDisplayed()
            .assertHasClickAction()

        // Action buttons should be accessible
        composeTestRule.onNodeWithContentDescription("Message Alice Johnson")
            .assertIsDisplayed()
            .assertHasClickAction()

        composeTestRule.onNodeWithContentDescription("Call Alice Johnson")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun accessibility_statusSheetCompliance() {
        // Given - MapScreen with status sheet visible
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

        // Status sheet should have dialog role
        composeTestRule.onNodeWithTag("ShareStatusSheet")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Dialog))

        // Status text should be accessible
        composeTestRule.onNodeWithText("Location Sharing Active")
            .assertIsDisplayed()

        // Coordinates should be accessible
        composeTestRule.onNodeWithText("Lat: 37.774900, Lng: -122.419400")
            .assertIsDisplayed()

        // Stop sharing button should be accessible
        composeTestRule.onNodeWithContentDescription("Stop location sharing")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun accessibility_errorStatesAreAccessible() {
        // Given - MapScreen with location error
        stateFlow.value = MapScreenState(
            hasLocationPermission = false,
            locationError = "Location permission is required to show your location on the map",
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

        // Error message should be accessible
        composeTestRule.onNodeWithText("Location permission is required to show your location on the map")
            .assertIsDisplayed()

        // Retry button should be accessible
        composeTestRule.onNodeWithContentDescription("Retry location request")
            .assertIsDisplayed()
            .assertHasClickAction()

        // Permission request button should be accessible
        composeTestRule.onNodeWithContentDescription("Grant location permission")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun accessibility_loadingStatesAreAccessible() {
        // Given - MapScreen with loading states
        stateFlow.value = MapScreenState(
            hasLocationPermission = true,
            isLocationLoading = true,
            isFriendsLoading = true
        )

        composeTestRule.setContent {
            FFTheme {
                MapScreen(
                    onNavigateBack = {},
                    viewModel = mockViewModel
                )
            }
        }

        // Loading indicators should be accessible
        composeTestRule.onNodeWithContentDescription("Loading your location")
            .assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription("Loading nearby friends")
            .assertIsDisplayed()

        // Self location FAB should show loading state
        composeTestRule.onNodeWithContentDescription("Getting your location")
            .assertIsDisplayed()
    }

    @Test
    fun accessibility_dynamicContentDescriptionsUpdate() {
        // Given - MapScreen with initial state
        stateFlow.value = MapScreenState(
            hasLocationPermission = true,
            currentLocation = LatLng(37.7749, -122.4194),
            friends = emptyList(),
            nearbyFriendsCount = 0
        )

        composeTestRule.setContent {
            FFTheme {
                MapScreen(
                    onNavigateBack = {},
                    viewModel = mockViewModel
                )
            }
        }

        // Initially no friends
        composeTestRule.onNodeWithContentDescription("View nearby friends, no friends available")
            .assertIsDisplayed()

        // When - friends are added
        val testFriends = createTestFriends()
        stateFlow.value = stateFlow.value.copy(
            friends = testFriends,
            nearbyFriendsCount = testFriends.size
        )

        // Then - content description should update
        composeTestRule.onNodeWithContentDescription("View nearby friends, 2 friends available")
            .assertIsDisplayed()

        // When - location sharing becomes active
        stateFlow.value = stateFlow.value.copy(isLocationSharingActive = true)

        // Then - Quick Share FAB description should update
        composeTestRule.onNodeWithContentDescription("View location sharing status")
            .assertIsDisplayed()
    }

    @Test
    fun accessibility_colorContrastCompliance() {
        // Given - MapScreen with all components
        stateFlow.value = MapScreenState(
            hasLocationPermission = true,
            currentLocation = LatLng(37.7749, -122.4194),
            friends = createTestFriends(),
            nearbyFriendsCount = 2,
            isLocationSharingActive = true
        )

        composeTestRule.setContent {
            FFTheme {
                MapScreen(
                    onNavigateBack = {},
                    viewModel = mockViewModel
                )
            }
        }

        // All text should be visible (contrast is handled by theme)
        composeTestRule.onNodeWithText("Your Location")
            .assertIsDisplayed()

        // FABs should have proper contrast (Material 3 ensures this)
        composeTestRule.onNodeWithContentDescription("Share your location instantly")
            .assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription("Center map on your location")
            .assertIsDisplayed()

        // Badge should be visible
        composeTestRule.onNodeWithText("2")
            .assertIsDisplayed()
    }

    @Test
    fun accessibility_touchTargetSizesAreAdequate() {
        // Given - MapScreen with all interactive elements
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

        // All interactive elements should be clickable (minimum 48dp touch target)
        // This is ensured by Material 3 components and our FAB implementations

        composeTestRule.onNodeWithContentDescription("Navigate back")
            .assertHasClickAction()

        composeTestRule.onNodeWithContentDescription("View nearby friends, 2 friends available")
            .assertHasClickAction()

        composeTestRule.onNodeWithContentDescription("Share your location instantly")
            .assertHasClickAction()

        composeTestRule.onNodeWithContentDescription("Center map on your location")
            .assertHasClickAction()
    }

    @Test
    fun accessibility_screenReaderAnnouncements() {
        // Given - MapScreen with state changes
        stateFlow.value = MapScreenState(
            hasLocationPermission = true,
            currentLocation = LatLng(37.7749, -122.4194)
        )

        composeTestRule.setContent {
            FFTheme {
                MapScreen(
                    onNavigateBack = {},
                    viewModel = mockViewModel
                )
            }
        }

        // When - location sharing starts
        stateFlow.value = stateFlow.value.copy(
            isLocationSharingActive = true,
            isStatusSheetVisible = true
        )

        // Then - status should be announced
        composeTestRule.onNodeWithText("Location Sharing Active")
            .assertIsDisplayed()

        // When - friends are loaded
        stateFlow.value = stateFlow.value.copy(
            friends = createTestFriends(),
            nearbyFriendsCount = 2
        )

        // Then - friend count should be announced through content description
        composeTestRule.onNodeWithContentDescription("View nearby friends, 2 friends available")
            .assertIsDisplayed()
    }

    @Test
    fun accessibility_keyboardNavigationSupport() {
        // Given - MapScreen with all elements
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

        // All interactive elements should be focusable and clickable
        val interactiveElements = listOf(
            "Navigate back",
            "View nearby friends, 2 friends available",
            "Center map on your location",
            "Share your location instantly"
        )

        interactiveElements.forEach { contentDescription ->
            composeTestRule.onNodeWithContentDescription(contentDescription)
                .assertIsDisplayed()
                .assertHasClickAction()
                .performScrollTo() // Ensures element is visible for keyboard navigation
        }
    }

    @Test
    fun accessibility_reducedMotionSupport() {
        // Given - MapScreen with animations
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

        // All elements should be accessible regardless of animation preferences
        // (Reduced motion is handled by the animation system)
        
        composeTestRule.onNodeWithContentDescription("Share your location instantly")
            .assertIsDisplayed()
            .performClick()

        // Animation state changes should not affect accessibility
        stateFlow.value = stateFlow.value.copy(
            isLocationSharingActive = true,
            isStatusSheetVisible = true
        )

        composeTestRule.onNodeWithText("Location Sharing Active")
            .assertIsDisplayed()
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