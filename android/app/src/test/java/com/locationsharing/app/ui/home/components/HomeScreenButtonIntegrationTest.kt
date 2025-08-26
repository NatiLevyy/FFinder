package com.locationsharing.app.ui.home.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit4.runners.AndroidJUnit4
import com.locationsharing.app.navigation.NavigationManager
import com.locationsharing.app.ui.components.button.ButtonResponseManager
import com.locationsharing.app.ui.components.button.ButtonState
import com.locationsharing.app.ui.theme.FFinderTheme
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for HomeScreen button components with NavigationManager
 * and ButtonResponseManager integration.
 */
@RunWith(AndroidJUnit4::class)
class HomeScreenButtonIntegrationTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    private lateinit var mockNavigationManager: NavigationManager
    private lateinit var mockButtonResponseManager: ButtonResponseManager
    private var onStartShareCalled = false
    private var onFriendsCalled = false
    private var onSettingsCalled = false
    
    @Before
    fun setUp() {
        mockNavigationManager = mockk(relaxed = true)
        mockButtonResponseManager = mockk(relaxed = true)
        onStartShareCalled = false
        onFriendsCalled = false
        onSettingsCalled = false
        
        // Setup default button state
        every { mockButtonResponseManager.getButtonState(any()) } returns 
            MutableStateFlow(ButtonState())
    }
    
    @Test
    fun primaryCallToAction_integratesWithNavigationManager() {
        // Given
        composeTestRule.setContent {
            FFinderTheme {
                PrimaryCallToAction(
                    onStartShare = { onStartShareCalled = true },
                    navigationManager = mockNavigationManager,
                    isNarrowScreen = false,
                    loading = false,
                    enabled = true
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
        assert(onStartShareCalled)
    }
    
    @Test
    fun primaryCallToAction_showsLoadingStateCorrectly() {
        // Given
        composeTestRule.setContent {
            FFinderTheme {
                PrimaryCallToAction(
                    onStartShare = { onStartShareCalled = true },
                    navigationManager = mockNavigationManager,
                    isNarrowScreen = false,
                    loading = true,
                    enabled = true
                )
            }
        }
        
        // Then
        composeTestRule
            .onNodeWithContentDescription("Start Live Sharing button, loading")
            .assertIsDisplayed()
    }
    
    @Test
    fun primaryCallToAction_narrowScreen_showsIconOnly() {
        // Given
        composeTestRule.setContent {
            FFinderTheme {
                PrimaryCallToAction(
                    onStartShare = { onStartShareCalled = true },
                    navigationManager = mockNavigationManager,
                    isNarrowScreen = true,
                    loading = false,
                    enabled = true
                )
            }
        }
        
        // Then
        composeTestRule
            .onNodeWithContentDescription("Start Live Sharing. Tap to begin sharing your location with friends.")
            .assertIsDisplayed()
    }
    
    @Test
    fun primaryCallToAction_narrowScreen_showsLoadingState() {
        // Given
        composeTestRule.setContent {
            FFinderTheme {
                PrimaryCallToAction(
                    onStartShare = { onStartShareCalled = true },
                    navigationManager = mockNavigationManager,
                    isNarrowScreen = true,
                    loading = true,
                    enabled = true
                )
            }
        }
        
        // Then
        composeTestRule
            .onNodeWithContentDescription("Start Live Sharing, loading")
            .assertIsDisplayed()
    }
    
    @Test
    fun secondaryActionsRow_integratesWithNavigationManager() {
        // Given
        composeTestRule.setContent {
            FFinderTheme {
                SecondaryActionsRow(
                    onFriends = { onFriendsCalled = true },
                    onSettings = { onSettingsCalled = true },
                    navigationManager = mockNavigationManager,
                    friendsLoading = false,
                    settingsLoading = false,
                    friendsEnabled = true,
                    settingsEnabled = true
                )
            }
        }
        
        // When - Click Friends button
        composeTestRule
            .onNodeWithText("Friends")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Then
        verify { mockNavigationManager.navigateToFriends() }
        assert(onFriendsCalled)
        
        // When - Click Settings button
        composeTestRule
            .onNodeWithText("Settings")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Then
        verify { mockNavigationManager.navigateToSettings() }
        assert(onSettingsCalled)
    }
    
    @Test
    fun secondaryActionsRow_showsFriendsLoadingState() {
        // Given
        composeTestRule.setContent {
            FFinderTheme {
                SecondaryActionsRow(
                    onFriends = { onFriendsCalled = true },
                    onSettings = { onSettingsCalled = true },
                    navigationManager = mockNavigationManager,
                    friendsLoading = true,
                    settingsLoading = false,
                    friendsEnabled = true,
                    settingsEnabled = true
                )
            }
        }
        
        // Then
        composeTestRule
            .onNodeWithContentDescription("Friends button, loading")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Settings")
            .assertIsDisplayed()
            .assertIsEnabled()
    }
    
    @Test
    fun secondaryActionsRow_showsSettingsLoadingState() {
        // Given
        composeTestRule.setContent {
            FFinderTheme {
                SecondaryActionsRow(
                    onFriends = { onFriendsCalled = true },
                    onSettings = { onSettingsCalled = true },
                    navigationManager = mockNavigationManager,
                    friendsLoading = false,
                    settingsLoading = true,
                    friendsEnabled = true,
                    settingsEnabled = true
                )
            }
        }
        
        // Then
        composeTestRule
            .onNodeWithText("Friends")
            .assertIsDisplayed()
            .assertIsEnabled()
        
        composeTestRule
            .onNodeWithContentDescription("Settings button, loading")
            .assertIsDisplayed()
    }
    
    @Test
    fun secondaryActionsRow_disablesButtonsWhenNotEnabled() {
        // Given
        composeTestRule.setContent {
            FFinderTheme {
                SecondaryActionsRow(
                    onFriends = { onFriendsCalled = true },
                    onSettings = { onSettingsCalled = true },
                    navigationManager = mockNavigationManager,
                    friendsLoading = false,
                    settingsLoading = false,
                    friendsEnabled = false,
                    settingsEnabled = false
                )
            }
        }
        
        // Then
        composeTestRule
            .onNodeWithText("Friends")
            .assertIsDisplayed()
            .assertIsNotEnabled()
        
        composeTestRule
            .onNodeWithText("Settings")
            .assertIsDisplayed()
            .assertIsNotEnabled()
    }
    
    @Test
    fun primaryCallToAction_disabledWhenNotEnabled() {
        // Given
        composeTestRule.setContent {
            FFinderTheme {
                PrimaryCallToAction(
                    onStartShare = { onStartShareCalled = true },
                    navigationManager = mockNavigationManager,
                    isNarrowScreen = false,
                    loading = false,
                    enabled = false
                )
            }
        }
        
        // When - Try to click disabled button
        composeTestRule
            .onNodeWithText("Start Live Sharing")
            .performClick()
        
        // Then - Should not trigger navigation or callback
        composeTestRule.waitForIdle()
        verify(exactly = 0) { mockNavigationManager.navigateToMap() }
        assert(!onStartShareCalled)
    }
    
    @Test
    fun buttons_haveProperAccessibilityLabels() {
        // Given
        composeTestRule.setContent {
            FFinderTheme {
                PrimaryCallToAction(
                    onStartShare = { onStartShareCalled = true },
                    navigationManager = mockNavigationManager,
                    isNarrowScreen = false,
                    loading = false,
                    enabled = true
                )
                
                SecondaryActionsRow(
                    onFriends = { onFriendsCalled = true },
                    onSettings = { onSettingsCalled = true },
                    navigationManager = mockNavigationManager,
                    friendsLoading = false,
                    settingsLoading = false,
                    friendsEnabled = true,
                    settingsEnabled = true
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
    fun buttons_maintainMinimumTouchTargetSize() {
        // Given
        composeTestRule.setContent {
            FFinderTheme {
                PrimaryCallToAction(
                    onStartShare = { onStartShareCalled = true },
                    navigationManager = mockNavigationManager,
                    isNarrowScreen = true, // Test narrow screen FAB
                    loading = false,
                    enabled = true
                )
                
                SecondaryActionsRow(
                    onFriends = { onFriendsCalled = true },
                    onSettings = { onSettingsCalled = true },
                    navigationManager = mockNavigationManager,
                    friendsLoading = false,
                    settingsLoading = false,
                    friendsEnabled = true,
                    settingsEnabled = true
                )
            }
        }
        
        // Then - All buttons should meet minimum 48dp touch target
        composeTestRule
            .onNodeWithContentDescription("Start Live Sharing. Tap to begin sharing your location with friends.")
            .assertTouchHeightIsEqualTo(composeTestRule.density.run { 56.dp }) // FAB is 56dp
        
        composeTestRule
            .onNodeWithText("Friends")
            .assertTouchHeightIsEqualTo(composeTestRule.density.run { 48.dp })
        
        composeTestRule
            .onNodeWithText("Settings")
            .assertTouchHeightIsEqualTo(composeTestRule.density.run { 48.dp })
    }
}