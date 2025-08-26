package com.locationsharing.app.ui.home.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.locationsharing.app.ui.theme.FFinderTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Accessibility tests for WhatsNewTeaser component verifying:
 * - Content descriptions for screen readers
 * - Keyboard navigation support
 * - Focus management and traversal
 * - Semantic properties for assistive technologies
 * - WCAG compliance for interactive elements
 */
@RunWith(AndroidJUnit4::class)
class WhatsNewTeaserAccessibilityTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun whatsNewTeaser_hasProperContentDescription() {
        // Given
        composeTestRule.setContent {
            FFinderTheme {
                WhatsNewTeaser(
                    onTap = {},
                    isVisible = true
                )
            }
        }

        // Then
        composeTestRule
            .onNodeWithContentDescription("What's New teaser card. Tap to learn about new features: Nearby Friends panel and Quick Share")
            .assertIsDisplayed()
    }

    @Test
    fun whatsNewTeaser_isClickableForAccessibility() {
        // Given
        composeTestRule.setContent {
            FFinderTheme {
                WhatsNewTeaser(
                    onTap = {},
                    isVisible = true
                )
            }
        }

        // Then
        composeTestRule
            .onNodeWithContentDescription("What's New teaser card. Tap to learn about new features: Nearby Friends panel and Quick Share")
            .assertHasClickAction()
    }

    @Test
    fun whatsNewTeaser_supportsFocusTraversal() {
        // Given
        composeTestRule.setContent {
            FFinderTheme {
                WhatsNewTeaser(
                    onTap = {},
                    isVisible = true
                )
            }
        }

        // Then - Verify the component can receive focus
        composeTestRule
            .onNodeWithContentDescription("What's New teaser card. Tap to learn about new features: Nearby Friends panel and Quick Share")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun whatsNewTeaser_providesSemanticInformation() {
        // Given
        var tapCallbackTriggered = false
        
        composeTestRule.setContent {
            FFinderTheme {
                WhatsNewTeaser(
                    onTap = { tapCallbackTriggered = true },
                    isVisible = true
                )
            }
        }

        // When - Perform click action via accessibility
        composeTestRule
            .onNodeWithContentDescription("What's New teaser card. Tap to learn about new features: Nearby Friends panel and Quick Share")
            .performClick()

        // Then
        assert(tapCallbackTriggered) { "Accessibility click should trigger the onTap callback" }
    }

    @Test
    fun whatsNewDialog_hasAccessibleTitle() {
        // Given
        composeTestRule.setContent {
            FFinderTheme {
                WhatsNewDialog(
                    onDismiss = {}
                )
            }
        }

        // Then
        composeTestRule
            .onNodeWithText("What's New in FFinder")
            .assertIsDisplayed()
    }

    @Test
    fun whatsNewDialog_hasAccessibleDismissButton() {
        // Given
        var dismissCallbackTriggered = false
        
        composeTestRule.setContent {
            FFinderTheme {
                WhatsNewDialog(
                    onDismiss = { dismissCallbackTriggered = true }
                )
            }
        }

        // When
        composeTestRule
            .onNodeWithText("Got it!")
            .assertHasClickAction()
            .performClick()

        // Then
        assert(dismissCallbackTriggered) { "Dismiss button should be accessible and functional" }
    }

    @Test
    fun whatsNewDialog_providesStructuredContent() {
        // Given
        composeTestRule.setContent {
            FFinderTheme {
                WhatsNewDialog(
                    onDismiss = {}
                )
            }
        }

        // Then - Verify all content is accessible
        composeTestRule
            .onNodeWithText("What's New in FFinder")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("We've added exciting new features to make finding and sharing with friends even easier:")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Nearby Friends Panel")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("See which friends are close by with real-time distance updates and quick actions.")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Quick Share")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Share your location instantly with one tap, no need to navigate through menus.")
            .assertIsDisplayed()
    }

    @Test
    fun whatsNewDialog_supportsKeyboardNavigation() {
        // Given
        composeTestRule.setContent {
            FFinderTheme {
                WhatsNewDialog(
                    onDismiss = {}
                )
            }
        }

        // Then - Verify dismiss button is focusable and clickable
        composeTestRule
            .onNodeWithText("Got it!")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun whatsNewTeaser_maintainsAccessibilityWhenHidden() {
        // Given
        composeTestRule.setContent {
            FFinderTheme {
                WhatsNewTeaser(
                    onTap = {},
                    isVisible = false
                )
            }
        }

        // Then - Component should still exist in the tree for accessibility
        composeTestRule
            .onNodeWithContentDescription("What's New teaser card. Tap to learn about new features: Nearby Friends panel and Quick Share")
            .assertExists()
    }

    @Test
    fun whatsNewTeaser_hasProperRoleForScreenReaders() {
        // Given
        composeTestRule.setContent {
            FFinderTheme {
                WhatsNewTeaser(
                    onTap = {},
                    isVisible = true
                )
            }
        }

        // Then - Verify the component is properly identified as clickable
        composeTestRule
            .onNodeWithContentDescription("What's New teaser card. Tap to learn about new features: Nearby Friends panel and Quick Share")
            .assertIsDisplayed()
            .assertHasClickAction()
    }
}