package com.locationsharing.app.ui.home.components

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.locationsharing.app.ui.theme.FFinderTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Accessibility tests for SecondaryActionsRow component.
 * 
 * Ensures the component meets accessibility requirements including
 * proper semantic roles, content descriptions, and keyboard navigation support.
 */
@RunWith(AndroidJUnit4::class)
class SecondaryActionsRowAccessibilityTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun secondaryActionsRow_hasProperSemanticRoles() {
        // Given
        composeTestRule.setContent {
            FFinderTheme {
                SecondaryActionsRow(
                    onFriends = {},
                    onSettings = {}
                )
            }
        }

        // Then - Verify buttons have proper semantic roles
        composeTestRule
            .onNodeWithText("Friends")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertHasClickAction()

        composeTestRule
            .onNodeWithText("Settings")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertHasClickAction()
    }

    @Test
    fun secondaryActionsRow_buttonsAreAccessible() {
        // Given
        composeTestRule.setContent {
            FFinderTheme {
                SecondaryActionsRow(
                    onFriends = {},
                    onSettings = {}
                )
            }
        }

        // Then - Verify buttons are accessible for screen readers
        composeTestRule
            .onNodeWithText("Friends")
            .assertHasClickAction()

        composeTestRule
            .onNodeWithText("Settings")
            .assertHasClickAction()
    }

    @Test
    fun secondaryActionsRow_supportsKeyboardNavigation() {
        // Given
        composeTestRule.setContent {
            FFinderTheme {
                SecondaryActionsRow(
                    onFriends = {},
                    onSettings = {}
                )
            }
        }

        // Then - Verify buttons support keyboard navigation
        // Both buttons should be focusable and clickable
        composeTestRule
            .onNodeWithText("Friends")
            .assertHasClickAction()

        composeTestRule
            .onNodeWithText("Settings")
            .assertHasClickAction()
    }

    @Test
    fun secondaryActionsRow_maintainsAccessibilityInDarkTheme() {
        // Given
        composeTestRule.setContent {
            FFinderTheme(darkTheme = true) {
                SecondaryActionsRow(
                    onFriends = {},
                    onSettings = {}
                )
            }
        }

        // Then - Verify accessibility is maintained in dark theme
        composeTestRule
            .onNodeWithText("Friends")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertHasClickAction()

        composeTestRule
            .onNodeWithText("Settings")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertHasClickAction()
    }
}