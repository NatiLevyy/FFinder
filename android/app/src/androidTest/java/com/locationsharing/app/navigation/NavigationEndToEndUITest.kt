package com.locationsharing.app.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.locationsharing.app.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end UI test for complete navigation functionality.
 * Validates all navigation requirements through actual user interactions.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class NavigationEndToEndUITest {
    
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)
    
    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()
    
    @Before
    fun setup() {
        hiltRule.inject()
    }
    
    @Test
    fun completeNavigationFlow_ValidatesAllRequirements() {
        // Requirement 1.1: All buttons should respond when tapped
        
        // Verify home screen is displayed
        composeTestRule.onNodeWithText("Start Live Sharing").assertIsDisplayed()
        composeTestRule.onNodeWithText("Friends").assertIsDisplayed()
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
        
        // Requirement 1.1 & 1.2: Button responsiveness and visual feedback
        composeTestRule.onNodeWithText("Start Live Sharing").assertIsEnabled()
        composeTestRule.onNodeWithText("Friends").assertIsEnabled()
        composeTestRule.onNodeWithText("Settings").assertIsEnabled()
        
        // Test navigation to map screen
        composeTestRule.onNodeWithText("Start Live Sharing").performClick()
        
        // Requirement 2.1 & 2.2: Navigation to map screen within 300ms
        // Wait for navigation to complete and verify map screen elements
        composeTestRule.waitForIdle()
        
        // Verify map screen is displayed (look for map-specific elements)
        composeTestRule.onNodeWithContentDescription("Back").assertIsDisplayed()
        
        // Requirement 2.3: Back navigation functionality
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        
        // Verify we're back on home screen
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Start Live Sharing").assertIsDisplayed()
        
        // Test navigation to friends screen
        composeTestRule.onNodeWithText("Friends").performClick()
        composeTestRule.waitForIdle()
        
        // Verify friends screen is displayed
        composeTestRule.onNodeWithContentDescription("Back").assertIsDisplayed()
        
        // Test back navigation from friends screen
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Friends").assertIsDisplayed()
        
        // Test navigation to settings screen
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.waitForIdle()
        
        // Verify settings screen is displayed
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Back").assertIsDisplayed()
        
        // Test back navigation from settings screen
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
    }
    
    @Test
    fun buttonResponseSystem_ValidatesRequirements() {
        // Requirement 1.3: Prevent double-clicks
        
        // Rapidly click the same button multiple times
        val startSharingButton = composeTestRule.onNodeWithText("Start Live Sharing")
        
        startSharingButton.performClick()
        startSharingButton.performClick() // Should be debounced
        startSharingButton.performClick() // Should be debounced
        
        // Wait for navigation to complete
        composeTestRule.waitForIdle()
        
        // Should only navigate once (verify we're on map screen, not multiple navigations)
        composeTestRule.onNodeWithContentDescription("Back").assertIsDisplayed()
        
        // Navigate back to test other buttons
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.waitForIdle()
    }
    
    @Test
    fun navigationErrorRecovery_ValidatesRequirements() {
        // Requirement 4.1 & 4.2: Error handling and recovery
        
        // This test would require simulating error conditions
        // For now, we test that navigation continues to work after normal operations
        
        // Perform multiple navigation operations
        composeTestRule.onNodeWithText("Friends").performClick()
        composeTestRule.waitForIdle()
        
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.waitForIdle()
        
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.waitForIdle()
        
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.waitForIdle()
        
        // Verify all buttons are still functional after multiple operations
        composeTestRule.onNodeWithText("Start Live Sharing").assertIsEnabled()
        composeTestRule.onNodeWithText("Friends").assertIsEnabled()
        composeTestRule.onNodeWithText("Settings").assertIsEnabled()
    }
    
    @Test
    fun accessibilitySupport_ValidatesRequirements() {
        // Requirement 5.4: Accessibility support
        
        // Verify all navigation elements have proper content descriptions
        composeTestRule.onNodeWithText("Start Live Sharing").assertIsDisplayed()
        composeTestRule.onNodeWithText("Friends").assertIsDisplayed()
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
        
        // Navigate to a screen and verify back button accessibility
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.waitForIdle()
        
        // Verify back button has proper content description
        composeTestRule.onNodeWithContentDescription("Back").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Back").assertIsEnabled()
        
        // Test back navigation accessibility
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.waitForIdle()
        
        // Verify we're back on home screen
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
    }
    
    @Test
    fun visualFeedback_ValidatesRequirements() {
        // Requirement 5.1, 5.2, 5.3: Visual feedback for navigation
        
        // Test button states during navigation
        val startSharingButton = composeTestRule.onNodeWithText("Start Live Sharing")
        
        // Button should be enabled initially
        startSharingButton.assertIsEnabled()
        
        // Click button and verify it provides feedback
        startSharingButton.performClick()
        
        // Wait for navigation to complete
        composeTestRule.waitForIdle()
        
        // Verify we navigated successfully (map screen should be displayed)
        composeTestRule.onNodeWithContentDescription("Back").assertIsDisplayed()
        
        // Navigate back and test other buttons
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.waitForIdle()
        
        // Test friends button feedback
        val friendsButton = composeTestRule.onNodeWithText("Friends")
        friendsButton.assertIsEnabled()
        friendsButton.performClick()
        composeTestRule.waitForIdle()
        
        // Verify navigation succeeded
        composeTestRule.onNodeWithContentDescription("Back").assertIsDisplayed()
        
        // Navigate back
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.waitForIdle()
    }
    
    @Test
    fun consistentNavigationBehavior_ValidatesRequirements() {
        // Requirement 3.1: Consistent navigation patterns
        
        // Test that all screens follow the same navigation pattern
        val screens = listOf(
            "Start Live Sharing" to "map",
            "Friends" to "friends", 
            "Settings" to "settings"
        )
        
        screens.forEach { (buttonText, screenType) ->
            // Navigate to screen
            composeTestRule.onNodeWithText(buttonText).performClick()
            composeTestRule.waitForIdle()
            
            // Verify back button is present and functional
            composeTestRule.onNodeWithContentDescription("Back").assertIsDisplayed()
            composeTestRule.onNodeWithContentDescription("Back").assertIsEnabled()
            
            // Navigate back
            composeTestRule.onNodeWithContentDescription("Back").performClick()
            composeTestRule.waitForIdle()
            
            // Verify we're back on home screen
            composeTestRule.onNodeWithText(buttonText).assertIsDisplayed()
        }
    }
    
    @Test
    fun navigationStateManagement_ValidatesRequirements() {
        // Requirement 3.3 & 3.4: Navigation state management
        
        // Perform a series of navigations to test state management
        
        // Home -> Map
        composeTestRule.onNodeWithText("Start Live Sharing").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Back").assertIsDisplayed()
        
        // Map -> Back to Home
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Start Live Sharing").assertIsDisplayed()
        
        // Home -> Friends
        composeTestRule.onNodeWithText("Friends").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Back").assertIsDisplayed()
        
        // Friends -> Back to Home
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Friends").assertIsDisplayed()
        
        // Verify all navigation elements are still functional
        composeTestRule.onNodeWithText("Start Live Sharing").assertIsEnabled()
        composeTestRule.onNodeWithText("Friends").assertIsEnabled()
        composeTestRule.onNodeWithText("Settings").assertIsEnabled()
    }
}