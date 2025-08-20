package com.locationsharing.app.ui.map

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.android.gms.maps.model.LatLng
import com.locationsharing.app.navigation.NavigationManager
import com.locationsharing.app.navigation.NavigationError
import com.locationsharing.app.ui.screens.MapScreen
import com.locationsharing.app.ui.theme.FFinderTheme
import com.locationsharing.app.ui.map.accessibility.MapAccessibilityConstants
import com.locationsharing.app.domain.model.NearbyFriend
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import io.mockk.just
import io.mockk.runs
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Android UI tests for MapScreen navigation functionality.
 * Tests requirements 2.1, 2.2, 2.4, 3.2, 4.1, 4.2 in a realistic UI environment.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class MapScreenNavigationUITest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createComposeRule()

    @MockK
    private lateinit var mockNavigationManager: NavigationManager

    @MockK
    private lateinit var mockViewModel: MapScreenViewModel

    private var backButtonClicked = false
    private var nearbyFriendsClicked = false

    @Before
    fun setUp() {
        hiltRule.inject()
        MockKAnnotations.init(this)
        
        // Setup mock navigation manager
        every { mockNavigationManager.navigateBack() } returns true
        every { mockNavigationManager.navigateToHome() } just runs
        every { mockNavigationManager.handleNavigationError(any()) } just runs
        
        // Setup mock view model
        every { mockViewModel.navigationManager } returns mockNavigationManager
        every { mockViewModel.cleanupOnNavigationAway() } just runs
        
        // Reset click flags
        backButtonClicked = false
        nearbyFriendsClicked = false
    }

    @Test
    fun mapScreen_navigationButtons_displayCorrectly() {
        // Given
        composeTestRule.setContent {
            FFinderTheme {
                MapScreen(
                    onBack = { backButtonClicked = true },
                    onNearbyFriends = { nearbyFriendsClicked = true },
                    viewModel = mockViewModel,
                    currentLocation = LatLng(37.7749, -122.4194),
                    hasLocationPermission = true,
                    nearbyFriendsCount = 3
                )
            }
        }

        // Then - verify all navigation buttons are displayed
        composeTestRule
            .onNodeWithTag(MapAccessibilityConstants.BACK_BUTTON_TEST_TAG)
            .assertIsDisplayed()
            .assertHasClickAction()

        composeTestRule
            .onNodeWithTag("home_navigation_button")
            .assertIsDisplayed()
            .assertHasClickAction()
            .assertContentDescriptionEquals("Navigate to home screen")

        composeTestRule
            .onNodeWithTag(MapAccessibilityConstants.NEARBY_FRIENDS_BUTTON_TEST_TAG)
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun backButton_performsNavigationWithFeedback() {
        // Given
        composeTestRule.setContent {
            FFinderTheme {
                MapScreen(
                    onBack = { backButtonClicked = true },
                    onNearbyFriends = { nearbyFriendsClicked = true },
                    viewModel = mockViewModel,
                    currentLocation = LatLng(37.7749, -122.4194),
                    hasLocationPermission = true
                )
            }
        }

        // When
        composeTestRule
            .onNodeWithTag(MapAccessibilityConstants.BACK_BUTTON_TEST_TAG)
            .performClick()

        // Then
        verify { mockNavigationManager.navigateBack() }
        
        // Wait for any animations or state changes
        composeTestRule.waitForIdle()
    }

    @Test
    fun homeButton_performsNavigationWithFeedback() {
        // Given
        composeTestRule.setContent {
            FFinderTheme {
                MapScreen(
                    onBack = { backButtonClicked = true },
                    onNearbyFriends = { nearbyFriendsClicked = true },
                    viewModel = mockViewModel,
                    currentLocation = LatLng(37.7749, -122.4194),
                    hasLocationPermission = true
                )
            }
        }

        // When
        composeTestRule
            .onNodeWithTag("home_navigation_button")
            .performClick()

        // Then
        verify { mockNavigationManager.navigateToHome() }
        
        // Wait for any animations or state changes
        composeTestRule.waitForIdle()
    }

    @Test
    fun nearbyFriendsButton_showsBadgeWhenFriendsPresent() {
        // Given
        composeTestRule.setContent {
            FFinderTheme {
                MapScreen(
                    onBack = { backButtonClicked = true },
                    onNearbyFriends = { nearbyFriendsClicked = true },
                    viewModel = mockViewModel,
                    currentLocation = LatLng(37.7749, -122.4194),
                    hasLocationPermission = true,
                    nearbyFriendsCount = 5,
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

        // Then
        composeTestRule
            .onNodeWithTag(MapAccessibilityConstants.NEARBY_FRIENDS_BUTTON_TEST_TAG)
            .assertIsDisplayed()

        // When
        composeTestRule
            .onNodeWithTag(MapAccessibilityConstants.NEARBY_FRIENDS_BUTTON_TEST_TAG)
            .performClick()

        // Then
        assert(nearbyFriendsClicked) { "Nearby friends callback should be triggered" }
    }

    @Test
    fun navigationButtons_handleErrorsGracefully() {
        // Given - navigation manager throws errors
        every { mockNavigationManager.navigateBack() } throws RuntimeException("Back navigation error")
        every { mockNavigationManager.navigateToHome() } throws RuntimeException("Home navigation error")
        
        composeTestRule.setContent {
            FFinderTheme {
                MapScreen(
                    onBack = { backButtonClicked = true },
                    onNearbyFriends = { nearbyFriendsClicked = true },
                    viewModel = mockViewModel,
                    currentLocation = LatLng(37.7749, -122.4194),
                    hasLocationPermission = true
                )
            }
        }

        // When - click back button
        composeTestRule
            .onNodeWithTag(MapAccessibilityConstants.BACK_BUTTON_TEST_TAG)
            .performClick()

        // Then - error should be handled and fallback called
        verify { mockNavigationManager.handleNavigationError(any<NavigationError.UnknownError>()) }
        assert(backButtonClicked) { "Fallback onBack should be called" }

        // When - click home button
        composeTestRule
            .onNodeWithTag("home_navigation_button")
            .performClick()

        // Then - error should be handled
        verify { mockNavigationManager.handleNavigationError(any<NavigationError.UnknownError>()) }
    }

    @Test
    fun mapScreen_maintainsAccessibilityDuringNavigation() {
        // Given
        composeTestRule.setContent {
            FFinderTheme {
                MapScreen(
                    onBack = { backButtonClicked = true },
                    onNearbyFriends = { nearbyFriendsClicked = true },
                    viewModel = mockViewModel,
                    currentLocation = LatLng(37.7749, -122.4194),
                    hasLocationPermission = true,
                    nearbyFriendsCount = 2
                )
            }
        }

        // When - perform navigation actions
        composeTestRule
            .onNodeWithTag(MapAccessibilityConstants.BACK_BUTTON_TEST_TAG)
            .performClick()

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag("home_navigation_button")
            .performClick()

        composeTestRule.waitForIdle()

        // Then - accessibility features should remain intact
        composeTestRule
            .onNodeWithTag(MapAccessibilityConstants.BACK_BUTTON_TEST_TAG)
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithTag("home_navigation_button")
            .assertIsDisplayed()
            .assertContentDescriptionEquals("Navigate to home screen")
    }

    @Test
    fun mapScreen_cleansUpResourcesOnDispose() {
        // Given
        var isMapScreenDisplayed = true
        
        composeTestRule.setContent {
            if (isMapScreenDisplayed) {
                FFinderTheme {
                    MapScreen(
                        onBack = { backButtonClicked = true },
                        onNearbyFriends = { nearbyFriendsClicked = true },
                        viewModel = mockViewModel,
                        currentLocation = LatLng(37.7749, -122.4194),
                        hasLocationPermission = true
                    )
                }
            }
        }

        // When - dispose the screen
        composeTestRule.setContent {
            isMapScreenDisplayed = false
        }

        composeTestRule.waitForIdle()

        // Then - cleanup should be called
        verify { mockViewModel.cleanupOnNavigationAway() }
    }
}