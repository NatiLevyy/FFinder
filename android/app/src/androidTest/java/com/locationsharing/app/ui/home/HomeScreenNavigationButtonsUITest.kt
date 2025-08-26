package com.locationsharing.app.ui.home

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.maps.model.LatLng
import com.locationsharing.app.navigation.NavigationManager
import com.locationsharing.app.ui.theme.FFinderTheme
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for HomeScreen navigation buttons functionality.
 * Tests the enhanced ResponsiveButton integration, loading states,
 * haptic feedback, and visual feedback.
 */
@RunWith(AndroidJUnit4::class)
class HomeScreenNavigationButtonsUITest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    private lateinit var mockNavigationManager: NavigationManager
    private var startShareClicked = false
    private var friendsClicked = false
    private var settingsClicked = false
    
    @Before
    fun setUp() {
        mockNavigationManager = mockk(relaxed = true)
        startShareClicked = false
        friendsClicked = false
        settingsClicked = false
    }
    
    @Test
    fun homeScreen_primaryCallToActionButton_isDisplayedAndClickable() {
        // Given
        val state = HomeScreenState(
            hasLocationPermission = true,
            mapPreviewLocation = LatLng(37.7749, -122.4194),
            animationsEnabled = true,
            isNarrowScreen = false
        )
        
        // When
        composeTestRule.setContent {
            FFinderTheme {
                HomeScreenContent(
                    state = state,
                    onEvent = { },
                    onStartShare = { startShareClicked = true },
                    onFriends = { friendsClicked = true },
                    onSettings = { settingsClicked = true },
                    navigationManager = mockNavigationManager
                )
            }
        }
        
        // Then
        composeTestRule
            .onNodeWithText("Start Live Sharing")
            .assertIsDisplayed()
            .assertIsEnabled()
            .assertHasClickAction()
    }
    
    @Test
    fun homeScreen_primaryCallToActionButton_clickTriggersNavigation() {
        // Given
        val state = HomeScreenState(
            hasLocationPermission = true,
            mapPreviewLocation = LatLng(37.7749, -122.4194),
            animationsEnabled = true,
            isNarrowScreen = false
        )
        
        // When
        composeTestRule.setContent {
            FFinderTheme {
                HomeScreenContent(
                    state = state,
                    onEvent = { },
                    onStartShare = { startShareClicked = true },
                    onFriends = { friendsClicked = true },
                    onSettings = { settingsClicked = true },
                    navigationManager = mockNavigationManager
                )
            }
        }
        
        // When
        composeTestRule
            .onNodeWithText("Start Live Sharing")
            .performClick()
        
        // Then
        composeTestRule.waitForIdle()
        verify { mockNavigationManager.navigateToMap() }
        assert(startShareClicked)
    }
    
    @Test
    fun homeScreen_primaryCallToActionButton_showsLoadingState() {
        // Given
        val state = HomeScreenState(
            hasLocationPermission = true,
            mapPreviewLocation = LatLng(37.7749, -122.4194),
            animationsEnabled = true,
            isNarrowScreen = false,
            isNavigatingToMap = true,
            isNavigating = true
        )
        
        // When
        composeTestRule.setContent {
            FFinderTheme {
                HomeScreenContent(
                    state = state,
                    onEvent = { },
                    onStartShare = { startShareClicked = true },
                    onFriends = { friendsClicked = true },
                    onSettings = { settingsClicked = true },
                    navigationManager = mockNavigationManager
                )
            }
        }
        
        // Then
        composeTestRule
            .onNodeWithContentDescription("Start Live Sharing button, loading")
            .assertIsDisplayed()
    }
    
    @Test
    fun homeScreen_friendsButton_isDisplayedAndClickable() {
        // Given
        val state = HomeScreenState(
            hasLocationPermission = true,
            mapPreviewLocation = LatLng(37.7749, -122.4194),
            animationsEnabled = true,
            isNarrowScreen = false
        )
        
        // When
        composeTestRule.setContent {
            FFinderTheme {
                HomeScreenContent(
                    state = state,
                    onEvent = { },
                    onStartShare = { startShareClicked = true },
                    onFriends = { friendsClicked = true },
                    onSettings = { settingsClicked = true },
                    navigationManager = mockNavigationManager
                )
            }
        }
        
        // Then
        composeTestRule
            .onNodeWithText("Friends")
            .assertIsDisplayed()
            .assertIsEnabled()
            .assertHasClickAction()
    }
    
    @Test
    fun homeScreen_friendsButton_clickTriggersNavigation() {
        // Given
        val state = HomeScreenState(
            hasLocationPermission = true,
            mapPreviewLocation = LatLng(37.7749, -122.4194),
            animationsEnabled = true,
            isNarrowScreen = false
        )
        
        // When
        composeTestRule.setContent {
            FFinderTheme {
                HomeScreenContent(
                    state = state,
                    onEvent = { },
                    onStartShare = { startShareClicked = true },
                    onFriends = { friendsClicked = true },
                    onSettings = { settingsClicked = true },
                    navigationManager = mockNavigationManager
                )
            }
        }
        
        // When
        composeTestRule
            .onNodeWithText("Friends")
            .performClick()
        
        // Then
        composeTestRule.waitForIdle()
        verify { mockNavigationManager.navigateToFriends() }
        assert(friendsClicked)
    }
    
    @Test
    fun homeScreen_friendsButton_showsLoadingState() {
        // Given
        val state = HomeScreenState(
            hasLocationPermission = true,
            mapPreviewLocation = LatLng(37.7749, -122.4194),
            animationsEnabled = true,
            isNarrowScreen = false,
            isNavigatingToFriends = true,
            isNavigating = true
        )
        
        // When
        composeTestRule.setContent {
            FFinderTheme {
                HomeScreenContent(
                    state = state,
                    onEvent = { },
                    onStartShare = { startShareClicked = true },
                    onFriends = { friendsClicked = true },
                    onSettings = { settingsClicked = true },
                    navigationManager = mockNavigationManager
                )
            }
        }
        
        // Then
        composeTestRule
            .onNodeWithContentDescription("Friends button, loading")
            .assertIsDisplayed()
    }
    
    @Test
    fun homeScreen_settingsButton_isDisplayedAndClickable() {
        // Given
        val state = HomeScreenState(
            hasLocationPermission = true,
            mapPreviewLocation = LatLng(37.7749, -122.4194),
            animationsEnabled = true,
            isNarrowScreen = false
        )
        
        // When
        composeTestRule.setContent {
            FFinderTheme {
                HomeScreenContent(
                    state = state,
                    onEvent = { },
                    onStartShare = { startShareClicked = true },
                    onFriends = { friendsClicked = true },
                    onSettings = { settingsClicked = true },
                    navigationManager = mockNavigationManager
                )
            }
        }
        
        // Then
        composeTestRule
            .onNodeWithText("Settings")
            .assertIsDisplayed()
            .assertIsEnabled()
            .assertHasClickAction()
    }
    
    @Test
    fun homeScreen_settingsButton_clickTriggersNavigation() {
        // Given
        val state = HomeScreenState(
            hasLocationPermission = true,
            mapPreviewLocation = LatLng(37.7749, -122.4194),
            animationsEnabled = true,
            isNarrowScreen = false
        )
        
        // When
        composeTestRule.setContent {
            FFinderTheme {
                HomeScreenContent(
                    state = state,
                    onEvent = { },
                    onStartShare = { startShareClicked = true },
                    onFriends = { friendsClicked = true },
                    onSettings = { settingsClicked = true },
                    navigationManager = mockNavigationManager
                )
            }
        }
        
        // When
        composeTestRule
            .onNodeWithText("Settings")
            .performClick()
        
        // Then
        composeTestRule.waitForIdle()
        verify { mockNavigationManager.navigateToSettings() }
        assert(settingsClicked)
    }
    
    @Test
    fun homeScreen_settingsButton_showsLoadingState() {
        // Given
        val state = HomeScreenState(
            hasLocationPermission = true,
            mapPreviewLocation = LatLng(37.7749, -122.4194),
            animationsEnabled = true,
            isNarrowScreen = false,
            isNavigatingToSettings = true,
            isNavigating = true
        )
        
        // When
        composeTestRule.setContent {
            FFinderTheme {
                HomeScreenContent(
                    state = state,
                    onEvent = { },
                    onStartShare = { startShareClicked = true },
                    onFriends = { friendsClicked = true },
                    onSettings = { settingsClicked = true },
                    navigationManager = mockNavigationManager
                )
            }
        }
        
        // Then
        composeTestRule
            .onNodeWithContentDescription("Settings button, loading")
            .assertIsDisplayed()
    }
    
    @Test
    fun homeScreen_buttonsDisabledWhenNavigating() {
        // Given
        val state = HomeScreenState(
            hasLocationPermission = true,
            mapPreviewLocation = LatLng(37.7749, -122.4194),
            animationsEnabled = true,
            isNarrowScreen = false,
            isNavigating = true,
            isNavigatingToMap = true
        )
        
        // When
        composeTestRule.setContent {
            FFinderTheme {
                HomeScreenContent(
                    state = state,
                    onEvent = { },
                    onStartShare = { startShareClicked = true },
                    onFriends = { friendsClicked = true },
                    onSettings = { settingsClicked = true },
                    navigationManager = mockNavigationManager
                )
            }
        }
        
        // Then - Friends and Settings buttons should be disabled when navigating
        composeTestRule
            .onNodeWithText("Friends")
            .assertIsNotEnabled()
        
        composeTestRule
            .onNodeWithText("Settings")
            .assertIsNotEnabled()
    }
    
    @Test
    fun homeScreen_narrowScreen_showsIconOnlyFAB() {
        // Given
        val state = HomeScreenState(
            hasLocationPermission = true,
            mapPreviewLocation = LatLng(37.7749, -122.4194),
            animationsEnabled = true,
            isNarrowScreen = true
        )
        
        // When
        composeTestRule.setContent {
            FFinderTheme {
                HomeScreenContent(
                    state = state,
                    onEvent = { },
                    onStartShare = { startShareClicked = true },
                    onFriends = { friendsClicked = true },
                    onSettings = { settingsClicked = true },
                    navigationManager = mockNavigationManager
                )
            }
        }
        
        // Then - Should show icon-only FAB with proper content description
        composeTestRule
            .onNodeWithContentDescription("Start Live Sharing. Tap to begin sharing your location with friends.")
            .assertIsDisplayed()
            .assertIsEnabled()
            .assertHasClickAction()
    }
    
    @Test
    fun homeScreen_accessibilityContentDescriptions_areCorrect() {
        // Given
        val state = HomeScreenState(
            hasLocationPermission = true,
            mapPreviewLocation = LatLng(37.7749, -122.4194),
            animationsEnabled = true,
            isNarrowScreen = false
        )
        
        // When
        composeTestRule.setContent {
            FFinderTheme {
                HomeScreenContent(
                    state = state,
                    onEvent = { },
                    onStartShare = { startShareClicked = true },
                    onFriends = { friendsClicked = true },
                    onSettings = { settingsClicked = true },
                    navigationManager = mockNavigationManager
                )
            }
        }
        
        // Then
        composeTestRule
            .onNodeWithContentDescription("Start Live Sharing button. Tap to begin sharing your location with friends.")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithContentDescription("Friends button. Navigate to friends list and management.")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithContentDescription("Settings button. Navigate to app settings and preferences.")
            .assertIsDisplayed()
    }
    
    @Test
    fun homeScreen_buttonTouchTargets_meetMinimumSize() {
        // Given
        val state = HomeScreenState(
            hasLocationPermission = true,
            mapPreviewLocation = LatLng(37.7749, -122.4194),
            animationsEnabled = true,
            isNarrowScreen = false
        )
        
        // When
        composeTestRule.setContent {
            FFinderTheme {
                HomeScreenContent(
                    state = state,
                    onEvent = { },
                    onStartShare = { startShareClicked = true },
                    onFriends = { friendsClicked = true },
                    onSettings = { settingsClicked = true },
                    navigationManager = mockNavigationManager
                )
            }
        }
        
        // Then - All buttons should have minimum touch target size (48dp)
        composeTestRule
            .onNodeWithText("Start Live Sharing")
            .assertTouchHeightIsEqualTo(composeTestRule.density.run { 48.dp })
        
        composeTestRule
            .onNodeWithText("Friends")
            .assertTouchHeightIsEqualTo(composeTestRule.density.run { 48.dp })
        
        composeTestRule
            .onNodeWithText("Settings")
            .assertTouchHeightIsEqualTo(composeTestRule.density.run { 48.dp })
    }
}