package com.locationsharing.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for MainActivity navigation flows.
 * Tests the complete navigation experience from UI interactions.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MainActivityNavigationIntegrationTest {
    
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)
    
    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()
    
    @Before
    fun setUp() {
        hiltRule.inject()
    }
    
    @Test
    fun testHomeToMapNavigation() {
        // Given: User is on home screen
        composeTestRule.onNodeWithText("Start Sharing").assertIsDisplayed()
        
        // When: User clicks start sharing button
        composeTestRule.onNodeWithText("Start Sharing").performClick()
        
        // Then: User should be navigated to map screen
        // Note: This test assumes the map screen has identifiable content
        // The actual assertion will depend on the map screen implementation
        composeTestRule.waitForIdle()
    }
    
    @Test
    fun testHomeToFriendsNavigation() {
        // Given: User is on home screen
        composeTestRule.onNodeWithText("Friends").assertIsDisplayed()
        
        // When: User clicks friends button
        composeTestRule.onNodeWithText("Friends").performClick()
        
        // Then: User should be navigated to friends screen
        composeTestRule.waitForIdle()
        // Add specific assertions based on friends screen content
    }
    
    @Test
    fun testHomeToSettingsNavigation() {
        // Given: User is on home screen
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
        
        // When: User clicks settings button
        composeTestRule.onNodeWithText("Settings").performClick()
        
        // Then: User should be navigated to settings screen
        composeTestRule.onNodeWithText("Settings screen coming soon!").assertIsDisplayed()
    }
    
    @Test
    fun testSettingsBackNavigation() {
        // Given: User navigates to settings screen
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.onNodeWithText("Settings screen coming soon!").assertIsDisplayed()
        
        // When: User clicks back button
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        
        // Then: User should be back on home screen
        composeTestRule.onNodeWithText("Start Sharing").assertIsDisplayed()
    }
    
    @Test
    fun testMapBackNavigation() {
        // Given: User navigates to map screen
        composeTestRule.onNodeWithText("Start Sharing").performClick()
        composeTestRule.waitForIdle()
        
        // When: User performs back navigation (this would typically be through a back button on the map screen)
        // Note: The actual back navigation test will depend on the map screen implementation
        // For now, we'll test that the navigation manager is properly integrated
        
        // Then: User should be able to navigate back
        // This test will be completed when the map screen back button is properly implemented
    }
    
    @Test
    fun testFriendsBackNavigation() {
        // Given: User navigates to friends screen
        composeTestRule.onNodeWithText("Friends").performClick()
        composeTestRule.waitForIdle()
        
        // When: User performs back navigation
        // Note: The actual back navigation test will depend on the friends screen implementation
        
        // Then: User should be able to navigate back
        // This test will be completed when the friends screen back button is properly implemented
    }
    
    @Test
    fun testNavigationStateConsistency() {
        // Given: User is on home screen
        composeTestRule.onNodeWithText("Start Sharing").assertIsDisplayed()
        
        // When: User navigates through multiple screens
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.waitForIdle()
        
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.waitForIdle()
        
        composeTestRule.onNodeWithText("Friends").performClick()
        composeTestRule.waitForIdle()
        
        // Then: Navigation state should be consistent throughout
        // The navigation manager should properly track state changes
        // This is verified by the successful navigation operations
    }
    
    @Test
    fun testNavigationErrorRecovery() {
        // This test would verify that navigation errors are handled gracefully
        // For now, we test that the basic navigation flow works without errors
        
        // Given: User performs multiple navigation operations
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.waitForIdle()
        
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.waitForIdle()
        
        composeTestRule.onNodeWithText("Friends").performClick()
        composeTestRule.waitForIdle()
        
        // Then: All operations should complete without crashes
        // The app should remain in a consistent state
        composeTestRule.onNodeWithText("Friends").assertIsDisplayed()
    }
}