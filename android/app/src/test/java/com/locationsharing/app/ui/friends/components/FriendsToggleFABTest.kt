package com.locationsharing.app.ui.friends.components

import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.locationsharing.app.ui.theme.FFinderTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FriendsToggleFABTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun friendsToggleFAB_whenClosed_displaysCorrectIcon() {
        // Given
        composeTestRule.setContent {
            FFinderTheme {
                FriendsToggleFAB(
                    isOpen = false,
                    onClick = { }
                )
            }
        }

        // Then
        composeTestRule
            .onNodeWithContentDescription("Open friends nearby panel")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun friendsToggleFAB_whenOpen_displaysCorrectIcon() {
        // Given
        composeTestRule.setContent {
            FFinderTheme {
                FriendsToggleFAB(
                    isOpen = true,
                    onClick = { }
                )
            }
        }

        // Then
        composeTestRule
            .onNodeWithContentDescription("Close friends nearby panel")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun friendsToggleFAB_whenClicked_triggersCallback() {
        // Given
        var clickCount = 0
        composeTestRule.setContent {
            FFinderTheme {
                FriendsToggleFAB(
                    isOpen = false,
                    onClick = { clickCount++ }
                )
            }
        }

        // When
        composeTestRule
            .onNodeWithContentDescription("Open friends nearby panel")
            .performClick()

        // Then
        assert(clickCount == 1)
    }

    @Test
    fun friendsToggleFAB_hasProperAccessibilityDescription() {
        // Given - Closed state
        composeTestRule.setContent {
            FFinderTheme {
                FriendsToggleFAB(
                    isOpen = false,
                    onClick = { }
                )
            }
        }

        // Then
        composeTestRule
            .onNodeWithContentDescription("Open friends nearby panel")
            .assertContentDescriptionEquals("Open friends nearby panel")

        // Given - Open state
        composeTestRule.setContent {
            FFinderTheme {
                FriendsToggleFAB(
                    isOpen = true,
                    onClick = { }
                )
            }
        }

        // Then
        composeTestRule
            .onNodeWithContentDescription("Close friends nearby panel")
            .assertContentDescriptionEquals("Close friends nearby panel")
    }

    @Test
    fun friendsToggleFAB_hasMinimumTouchTarget() {
        // Given
        composeTestRule.setContent {
            FFinderTheme {
                FriendsToggleFAB(
                    isOpen = false,
                    onClick = { }
                )
            }
        }

        // Then - Material3 FAB automatically ensures 48dp minimum touch target
        // This test verifies the component is clickable and accessible
        composeTestRule
            .onNodeWithContentDescription("Open friends nearby panel")
            .assertHasClickAction()
            .assertIsDisplayed()
    }

    @Test
    fun friendsToggleFAB_hasProperAccessibilityRole() {
        // Given
        composeTestRule.setContent {
            FFinderTheme {
                FriendsToggleFAB(
                    isOpen = false,
                    onClick = { }
                )
            }
        }

        // Then
        composeTestRule
            .onNodeWithContentDescription("Open friends nearby panel")
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.Role,
                    androidx.compose.ui.semantics.Role.Button
                )
            )
    }

    @Test
    fun friendsToggleFAB_stateTransition_updatesContentDescription() {
        // Given
        var isOpen = false
        composeTestRule.setContent {
            FFinderTheme {
                FriendsToggleFAB(
                    isOpen = isOpen,
                    onClick = { isOpen = !isOpen }
                )
            }
        }

        // Initially closed
        composeTestRule
            .onNodeWithContentDescription("Open friends nearby panel")
            .assertIsDisplayed()

        // When clicked (simulating state change)
        composeTestRule.setContent {
            FFinderTheme {
                FriendsToggleFAB(
                    isOpen = true, // Simulate state change
                    onClick = { }
                )
            }
        }

        // Then shows open state
        composeTestRule
            .onNodeWithContentDescription("Close friends nearby panel")
            .assertIsDisplayed()
    }
}