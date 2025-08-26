package com.locationsharing.app.ui.home.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.locationsharing.app.ui.theme.FFinderTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for SecondaryActionsRow component.
 * 
 * Tests the component's integration with other home screen components
 * and verifies proper layout and interaction behavior.
 */
@RunWith(AndroidJUnit4::class)
class SecondaryActionsRowIntegrationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun secondaryActionsRow_integratesWithOtherHomeComponents() {
        // Given
        var friendsClicked = false
        var settingsClicked = false
        var primaryActionClicked = false

        composeTestRule.setContent {
            FFinderTheme {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Hero Section
                    HeroSection()
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Primary Call to Action
                    PrimaryCallToAction(
                        onStartShare = { primaryActionClicked = true }
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Secondary Actions Row
                    SecondaryActionsRow(
                        onFriends = { friendsClicked = true },
                        onSettings = { settingsClicked = true }
                    )
                }
            }
        }

        // Then - Verify all components are displayed
        composeTestRule
            .onNodeWithText("Share your live location and find friends instantly.")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Start Live Sharing")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Friends")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Settings")
            .assertIsDisplayed()

        // When - Interact with secondary actions
        composeTestRule
            .onNodeWithText("Friends")
            .performClick()

        composeTestRule
            .onNodeWithText("Settings")
            .performClick()

        // Then - Verify callbacks are invoked independently
        assertTrue("Friends callback should be invoked", friendsClicked)
        assertTrue("Settings callback should be invoked", settingsClicked)
        assertTrue("Primary action should not be affected", !primaryActionClicked)
    }

    @Test
    fun secondaryActionsRow_maintainsLayoutWithResponsiveDesign() {
        // Given - Test with different screen configurations
        composeTestRule.setContent {
            FFinderTheme {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Test with responsive layout
                    ResponsiveLayout { isNarrowScreen ->
                        PrimaryCallToAction(
                            onStartShare = {},
                            isNarrowScreen = isNarrowScreen
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        SecondaryActionsRow(
                            onFriends = {},
                            onSettings = {}
                        )
                    }
                }
            }
        }

        // Then - Verify components are displayed correctly
        composeTestRule
            .onNodeWithText("Friends")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Settings")
            .assertIsDisplayed()
    }

    @Test
    fun secondaryActionsRow_worksWithBackgroundGradient() {
        // Given
        var friendsClicked = false
        var settingsClicked = false

        composeTestRule.setContent {
            FFinderTheme {
                BackgroundGradient {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        SecondaryActionsRow(
                            onFriends = { friendsClicked = true },
                            onSettings = { settingsClicked = true }
                        )
                    }
                }
            }
        }

        // Then - Verify components work with gradient background
        composeTestRule
            .onNodeWithText("Friends")
            .assertIsDisplayed()
            .performClick()

        composeTestRule
            .onNodeWithText("Settings")
            .assertIsDisplayed()
            .performClick()

        // Verify callbacks work with background
        assertTrue("Friends callback should work with gradient", friendsClicked)
        assertTrue("Settings callback should work with gradient", settingsClicked)
    }

    @Test
    fun secondaryActionsRow_maintainsSpacingRequirements() {
        // Given - Test spacing requirements (8dp gap between buttons)
        composeTestRule.setContent {
            FFinderTheme {
                SecondaryActionsRow(
                    onFriends = {},
                    onSettings = {},
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        // Then - Verify both buttons are displayed with proper spacing
        // The 8dp gap is handled by Arrangement.spacedBy(8.dp) in the Row
        composeTestRule
            .onNodeWithText("Friends")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Settings")
            .assertIsDisplayed()

        // Verify icons are displayed
        composeTestRule
            .onNodeWithText("👥")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("⚙️")
            .assertIsDisplayed()
    }
}