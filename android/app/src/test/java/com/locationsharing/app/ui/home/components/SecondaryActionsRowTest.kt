package com.locationsharing.app.ui.home.components

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.locationsharing.app.ui.theme.FFinderTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Unit tests for SecondaryActionsRow component.
 * 
 * Tests component rendering, user interactions, and callback invocations
 * according to requirements 6.1, 6.2, 6.3, 6.4, and 6.5.
 */
@RunWith(AndroidJUnit4::class)
class SecondaryActionsRowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun secondaryActionsRow_displaysCorrectly() {
        // Given
        composeTestRule.setContent {
            FFinderTheme {
                SecondaryActionsRow(
                    onFriends = {},
                    onSettings = {}
                )
            }
        }

        // Then - Verify both buttons are displayed (Requirement 6.1)
        composeTestRule
            .onNodeWithText("Friends")
            .assertIsDisplayed()
            .assertHasClickAction()

        composeTestRule
            .onNodeWithText("Settings")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun secondaryActionsRow_displaysCorrectIcons() {
        // Given
        composeTestRule.setContent {
            FFinderTheme {
                SecondaryActionsRow(
                    onFriends = {},
                    onSettings = {}
                )
            }
        }

        // Then - Verify correct icons are displayed (Requirements 6.3, 6.4)
        composeTestRule
            .onNodeWithText("👥")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("⚙️")
            .assertIsDisplayed()
    }

    @Test
    fun friendsButton_invokesCallback() {
        // Given
        var friendsClicked = false
        
        composeTestRule.setContent {
            FFinderTheme {
                SecondaryActionsRow(
                    onFriends = { friendsClicked = true },
                    onSettings = {}
                )
            }
        }

        // When - Click Friends button
        composeTestRule
            .onNodeWithText("Friends")
            .performClick()

        // Then - Verify callback is invoked (Requirement 6.3)
        assertTrue("Friends callback should be invoked", friendsClicked)
    }

    @Test
    fun settingsButton_invokesCallback() {
        // Given
        var settingsClicked = false
        
        composeTestRule.setContent {
            FFinderTheme {
                SecondaryActionsRow(
                    onFriends = {},
                    onSettings = { settingsClicked = true }
                )
            }
        }

        // When - Click Settings button
        composeTestRule
            .onNodeWithText("Settings")
            .performClick()

        // Then - Verify callback is invoked (Requirement 6.4)
        assertTrue("Settings callback should be invoked", settingsClicked)
    }

    @Test
    fun secondaryActionsRow_bothButtonsClickableIndependently() {
        // Given
        var friendsClicked = false
        var settingsClicked = false
        
        composeTestRule.setContent {
            FFinderTheme {
                SecondaryActionsRow(
                    onFriends = { friendsClicked = true },
                    onSettings = { settingsClicked = true }
                )
            }
        }

        // When - Click Friends button first
        composeTestRule
            .onNodeWithText("Friends")
            .performClick()

        // Then - Only Friends callback should be invoked
        assertTrue("Friends callback should be invoked", friendsClicked)
        assertTrue("Settings callback should not be invoked yet", !settingsClicked)

        // When - Click Settings button
        composeTestRule
            .onNodeWithText("Settings")
            .performClick()

        // Then - Both callbacks should now be invoked
        assertTrue("Friends callback should still be invoked", friendsClicked)
        assertTrue("Settings callback should now be invoked", settingsClicked)
    }

    @Test
    fun secondaryActionsRow_displaysInDarkTheme() {
        // Given
        composeTestRule.setContent {
            FFinderTheme(darkTheme = true) {
                SecondaryActionsRow(
                    onFriends = {},
                    onSettings = {}
                )
            }
        }

        // Then - Verify components are displayed in dark theme (Requirement 6.5)
        composeTestRule
            .onNodeWithText("Friends")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Settings")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("👥")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("⚙️")
            .assertIsDisplayed()
    }
}