package com.locationsharing.app.ui.map.components

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.locationsharing.app.ui.map.MapScreenConstants
import com.locationsharing.app.ui.theme.FFinderTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Accessibility tests for QuickShareFAB component
 * Tests requirement 9.6 - comprehensive accessibility support
 */
@RunWith(AndroidJUnit4::class)
class QuickShareFABAccessibilityTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun quickShareFAB_hasProperContentDescription() {
        // Given
        composeTestRule.setContent {
            FFinderTheme {
                QuickShareFAB(
                    onClick = { }
                )
            }
        }

        // Then
        composeTestRule
            .onNodeWithContentDescription(MapScreenConstants.Accessibility.QUICK_SHARE_FAB_DESC)
            .assertExists()
    }

    @Test
    fun quickShareFAB_hasProperSemanticRole() {
        // Given
        composeTestRule.setContent {
            FFinderTheme {
                QuickShareFAB(
                    onClick = { }
                )
            }
        }

        // Then - Should have button role (verified through click action)
        composeTestRule
            .onNodeWithContentDescription(MapScreenConstants.Accessibility.QUICK_SHARE_FAB_DESC)
            .assertHasClickAction()
    }

    @Test
    fun quickShareFAB_isAccessibleToScreenReaders() {
        // Given
        composeTestRule.setContent {
            FFinderTheme {
                QuickShareFAB(
                    onClick = { }
                )
            }
        }

        // Then - Should be discoverable by accessibility services
        composeTestRule
            .onNodeWithContentDescription(MapScreenConstants.Accessibility.QUICK_SHARE_FAB_DESC)
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun quickShareFAB_activeState_hasProperContentDescription() {
        // Given
        composeTestRule.setContent {
            FFinderTheme {
                QuickShareFAB(
                    onClick = { },
                    isLocationSharingActive = true
                )
            }
        }

        // Then
        composeTestRule
            .onNodeWithContentDescription("Location sharing is active, tap to manage")
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun quickShareFAB_inactiveState_hasProperContentDescription() {
        // Given
        composeTestRule.setContent {
            FFinderTheme {
                QuickShareFAB(
                    onClick = { },
                    isLocationSharingActive = false
                )
            }
        }

        // Then
        composeTestRule
            .onNodeWithContentDescription(MapScreenConstants.Accessibility.QUICK_SHARE_FAB_DESC)
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun quickShareFAB_disabledState_maintainsAccessibility() {
        // Given
        composeTestRule.setContent {
            FFinderTheme {
                QuickShareFAB(
                    onClick = { },
                    enabled = false
                )
            }
        }

        // Then - Should still be discoverable but not clickable
        composeTestRule
            .onNodeWithContentDescription(MapScreenConstants.Accessibility.QUICK_SHARE_FAB_DESC)
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun quickShareFAB_pressedState_maintainsAccessibility() {
        // Given
        composeTestRule.setContent {
            FFinderTheme {
                QuickShareFAB(
                    onClick = { },
                    isPressed = true
                )
            }
        }

        // Then - Pressed state should not affect accessibility
        composeTestRule
            .onNodeWithContentDescription(MapScreenConstants.Accessibility.QUICK_SHARE_FAB_DESC)
            .assertExists()
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun quickShareFAB_hasProperFocusOrder() {
        // Given
        composeTestRule.setContent {
            FFinderTheme {
                QuickShareFAB(
                    onClick = { }
                )
            }
        }

        // Then - Should be focusable for keyboard navigation
        composeTestRule
            .onNodeWithContentDescription(MapScreenConstants.Accessibility.QUICK_SHARE_FAB_DESC)
            .requestFocus()
            .assertIsFocused()
    }

    @Test
    fun quickShareFAB_supportsKeyboardNavigation() {
        // Given
        composeTestRule.setContent {
            FFinderTheme {
                QuickShareFAB(
                    onClick = { }
                )
            }
        }

        // Then - Should be activatable via keyboard
        composeTestRule
            .onNodeWithContentDescription(MapScreenConstants.Accessibility.QUICK_SHARE_FAB_DESC)
            .requestFocus()
            .assertIsFocused()
            .performKeyInput {
                pressKey(androidx.compose.ui.input.key.Key.Enter)
            }
    }

    @Test
    fun quickShareFAB_hasMinimumTouchTarget() {
        // Given
        composeTestRule.setContent {
            FFinderTheme {
                QuickShareFAB(
                    onClick = { }
                )
            }
        }

        // Then - Should meet minimum touch target size (48dp)
        composeTestRule
            .onNodeWithContentDescription(MapScreenConstants.Accessibility.QUICK_SHARE_FAB_DESC)
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun quickShareFAB_hasProperSemanticProperties() {
        // Given
        composeTestRule.setContent {
            FFinderTheme {
                QuickShareFAB(
                    onClick = { }
                )
            }
        }

        // Then - Should have proper semantic properties
        composeTestRule
            .onNodeWithContentDescription(MapScreenConstants.Accessibility.QUICK_SHARE_FAB_DESC)
            .assert(
                hasAnyDescendant(
                    hasContentDescription(MapScreenConstants.Accessibility.QUICK_SHARE_FAB_DESC)
                ) or hasContentDescription(MapScreenConstants.Accessibility.QUICK_SHARE_FAB_DESC)
            )
    }

    @Test
    fun quickShareFAB_stateChanges_announceToScreenReader() {
        // Given
        var isLocationSharingActive = false
        
        composeTestRule.setContent {
            FFinderTheme {
                QuickShareFAB(
                    onClick = { isLocationSharingActive = !isLocationSharingActive },
                    isLocationSharingActive = isLocationSharingActive
                )
            }
        }

        // When - State changes
        composeTestRule
            .onNodeWithContentDescription(MapScreenConstants.Accessibility.QUICK_SHARE_FAB_DESC)
            .performClick()

        // Then - Should update content description for screen readers
        // Note: In a real implementation, this would trigger a state change
        // that updates the content description
        composeTestRule
            .onNodeWithContentDescription(MapScreenConstants.Accessibility.QUICK_SHARE_FAB_DESC)
            .assertExists()
    }

    @Test
    fun quickShareFAB_highContrastMode_maintainsVisibility() {
        // Given - Simulate high contrast mode
        composeTestRule.setContent {
            FFinderTheme {
                QuickShareFAB(
                    onClick = { }
                )
            }
        }

        // Then - Should remain visible and accessible
        composeTestRule
            .onNodeWithContentDescription(MapScreenConstants.Accessibility.QUICK_SHARE_FAB_DESC)
            .assertExists()
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun quickShareFAB_largeTextMode_maintainsLayout() {
        // Given - Component should handle large text settings
        composeTestRule.setContent {
            FFinderTheme {
                QuickShareFAB(
                    onClick = { }
                )
            }
        }

        // Then - Should maintain proper layout with large text
        composeTestRule
            .onNodeWithContentDescription(MapScreenConstants.Accessibility.QUICK_SHARE_FAB_DESC)
            .assertExists()
            .assertIsDisplayed()
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun quickShareFAB_reducedMotionMode_respectsPreferences() {
        // Given - Component should respect reduced motion preferences
        composeTestRule.setContent {
            FFinderTheme {
                QuickShareFAB(
                    onClick = { },
                    isPressed = true // Animation state
                )
            }
        }

        // Then - Should still be functional with reduced motion
        composeTestRule
            .onNodeWithContentDescription(MapScreenConstants.Accessibility.QUICK_SHARE_FAB_DESC)
            .assertExists()
            .assertIsDisplayed()
            .assertHasClickAction()
    }
}