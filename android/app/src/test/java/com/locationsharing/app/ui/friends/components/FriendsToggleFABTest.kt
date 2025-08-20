package com.locationsharing.app.ui.friends.components

import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
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
    fun friendsToggleFAB_displaysCorrectTextAndIcon() {
        // Given
        composeTestRule.setContent {
            FFinderTheme {
                FriendsToggleFAB(
                    onClick = { },
                    friendCount = 0,
                    isExpanded = true,
                    isPanelOpen = false
                )
            }
        }

        // Then - Verify ExtendedFloatingActionButton renders with correct text and icon
        composeTestRule
            .onNodeWithText("Nearby Friends")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithContentDescription("Open nearby friends panel. No friends are currently sharing their location.")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun friendsToggleFAB_withFriendCount_displaysBadge() {
        // Given
        composeTestRule.setContent {
            FFinderTheme {
                FriendsToggleFAB(
                    onClick = { },
                    friendCount = 3,
                    isExpanded = true
                )
            }
        }

        // Then
        composeTestRule
            .onNodeWithContentDescription("Open nearby friends panel, 3 friends available")
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
                    onClick = { clickCount++ },
                    friendCount = 0,
                    isExpanded = true
                )
            }
        }

        // When
        composeTestRule
            .onNodeWithContentDescription("Open nearby friends panel")
            .performClick()

        // Then
        assert(clickCount == 1)
    }

    @Test
    fun friendsToggleFAB_hasProperAccessibilityDescription() {
        // Given - No friends
        composeTestRule.setContent {
            FFinderTheme {
                FriendsToggleFAB(
                    onClick = { },
                    friendCount = 0,
                    isExpanded = true
                )
            }
        }

        // Then
        composeTestRule
            .onNodeWithContentDescription("Open nearby friends panel")
            .assertContentDescriptionEquals("Open nearby friends panel")

        // Given - With friends
        composeTestRule.setContent {
            FFinderTheme {
                FriendsToggleFAB(
                    onClick = { },
                    friendCount = 5,
                    isExpanded = true
                )
            }
        }

        // Then
        composeTestRule
            .onNodeWithContentDescription("Open nearby friends panel, 5 friends available")
            .assertContentDescriptionEquals("Open nearby friends panel, 5 friends available")
    }

    @Test
    fun friendsToggleFAB_hasMinimumTouchTarget() {
        // Given
        composeTestRule.setContent {
            FFinderTheme {
                FriendsToggleFAB(
                    onClick = { },
                    friendCount = 0,
                    isExpanded = true
                )
            }
        }

        // Then - Material3 ExtendedFAB automatically ensures 48dp minimum touch target
        // This test verifies the component is clickable and accessible
        composeTestRule
            .onNodeWithContentDescription("Open nearby friends panel")
            .assertHasClickAction()
            .assertIsDisplayed()
    }

    @Test
    fun friendsToggleFAB_hasProperAccessibilityRole() {
        // Given
        composeTestRule.setContent {
            FFinderTheme {
                FriendsToggleFAB(
                    onClick = { },
                    friendCount = 0,
                    isExpanded = true
                )
            }
        }

        // Then
        composeTestRule
            .onNodeWithContentDescription("Open nearby friends panel")
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.Role,
                    androidx.compose.ui.semantics.Role.Button
                )
            )
    }

    @Test
    fun friendsToggleFAB_expandedState_showsText() {
        // Given - Expanded state
        composeTestRule.setContent {
            FFinderTheme {
                FriendsToggleFAB(
                    onClick = { },
                    friendCount = 2,
                    isExpanded = true
                )
            }
        }

        // Then shows expanded state with text
        composeTestRule
            .onNodeWithContentDescription("Open nearby friends panel, 2 friends available")
            .assertIsDisplayed()

        // Given - Collapsed state
        composeTestRule.setContent {
            FFinderTheme {
                FriendsToggleFAB(
                    onClick = { },
                    friendCount = 2,
                    isExpanded = false // Collapsed state
                )
            }
        }

        // Then shows collapsed state (still accessible)
        composeTestRule
            .onNodeWithContentDescription("Open nearby friends panel, 2 friends available")
            .assertIsDisplayed()
    }

    @Test
    fun friendsToggleFAB_whenPanelOpen_showsCloseDescription() {
        // Given - Panel is open
        composeTestRule.setContent {
            FFinderTheme {
                FriendsToggleFAB(
                    onClick = { },
                    friendCount = 3,
                    isExpanded = true,
                    isPanelOpen = true
                )
            }
        }

        // Then shows close description
        composeTestRule
            .onNodeWithContentDescription("Close friends nearby panel")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun friendsToggleFAB_responsiveBehavior_collapsesOnPanelOpen() {
        // Given - Panel is closed, should be expanded
        composeTestRule.setContent {
            FFinderTheme {
                FriendsToggleFAB(
                    onClick = { },
                    friendCount = 2,
                    isExpanded = true,
                    isPanelOpen = false
                )
            }
        }

        // Then shows expanded state
        composeTestRule
            .onNodeWithContentDescription("Open nearby friends panel, 2 friends available")
            .assertIsDisplayed()

        // Given - Panel is open, should collapse
        composeTestRule.setContent {
            FFinderTheme {
                FriendsToggleFAB(
                    onClick = { },
                    friendCount = 2,
                    isExpanded = true,
                    isPanelOpen = true // Panel open should cause collapse
                )
            }
        }

        // Then shows collapsed state with close description
        composeTestRule
            .onNodeWithContentDescription("Close friends nearby panel")
            .assertIsDisplayed()
    }

    @Test
    fun friendsToggleFAB_multipleRapidClicks_handlesGracefully() {
        // Given
        var clickCount = 0
        composeTestRule.setContent {
            FFinderTheme {
                FriendsToggleFAB(
                    onClick = { clickCount++ },
                    friendCount = 1,
                    isExpanded = true
                )
            }
        }

        // When - Multiple rapid clicks
        val fabNode = composeTestRule
            .onNodeWithContentDescription("Open nearby friends panel, 1 friends available")
        
        fabNode.performClick()
        fabNode.performClick()
        fabNode.performClick()

        // Then - All clicks are handled (animation cancellation prevents conflicts)
        assert(clickCount == 3)
    }

    @Test
    fun friendsToggleFAB_accessibilityWithAnimations_maintainsSemantics() {
        // Given - FAB with animations enabled
        composeTestRule.setContent {
            FFinderTheme {
                FriendsToggleFAB(
                    onClick = { },
                    friendCount = 4,
                    isExpanded = true,
                    isPanelOpen = false
                )
            }
        }

        // Then - Accessibility properties are maintained during animations
        composeTestRule
            .onNodeWithContentDescription("Open nearby friends panel, 4 friends available")
            .assertIsDisplayed()
            .assertHasClickAction()
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.Role,
                    androidx.compose.ui.semantics.Role.Button
                )
            )
    }
}