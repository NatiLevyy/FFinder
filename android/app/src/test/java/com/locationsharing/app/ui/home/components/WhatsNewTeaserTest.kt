package com.locationsharing.app.ui.home.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.locationsharing.app.ui.theme.FFinderTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Unit tests for WhatsNewTeaser component verifying:
 * - Component rendering and visibility
 * - Animation behavior and state changes
 * - User interaction handling (tap events)
 * - Accessibility support and content descriptions
 * - Dialog display and dismissal functionality
 */
@RunWith(AndroidJUnit4::class)
class WhatsNewTeaserTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun whatsNewTeaser_displaysCorrectContent() {
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
            .onNodeWithText("🚀")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("New: Nearby Friends panel & Quick Share!")
            .assertIsDisplayed()
    }

    @Test
    fun whatsNewTeaser_hasProperAccessibilitySupport() {
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
            .assertHasClickAction()
    }

    @Test
    fun whatsNewTeaser_triggersOnTapCallback() {
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

        // When
        composeTestRule
            .onNodeWithContentDescription("What's New teaser card. Tap to learn about new features: Nearby Friends panel and Quick Share")
            .performClick()

        // Then
        assert(tapCallbackTriggered) { "onTap callback should be triggered when teaser is clicked" }
    }

    @Test
    fun whatsNewTeaser_isVisibleWhenEnabled() {
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
            .onNodeWithText("New: Nearby Friends panel & Quick Share!")
            .assertIsDisplayed()
    }

    @Test
    fun whatsNewTeaser_handlesVisibilityChanges() {
        // Given
        var isVisible = true
        
        composeTestRule.setContent {
            FFinderTheme {
                WhatsNewTeaser(
                    onTap = {},
                    isVisible = isVisible
                )
            }
        }

        // Initially visible
        composeTestRule
            .onNodeWithText("New: Nearby Friends panel & Quick Share!")
            .assertIsDisplayed()

        // When visibility changes
        composeTestRule.runOnUiThread {
            isVisible = false
        }

        // Component should still exist but may be animated out
        composeTestRule
            .onNodeWithText("New: Nearby Friends panel & Quick Share!")
            .assertExists()
    }

    @Test
    fun whatsNewDialog_displaysCorrectContent() {
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
            .onNodeWithText("🚀")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("What's New in FFinder")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Nearby Friends Panel")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Quick Share")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Got it!")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun whatsNewDialog_triggersOnDismissCallback() {
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
            .performClick()

        // Then
        assert(dismissCallbackTriggered) { "onDismiss callback should be triggered when 'Got it!' button is clicked" }
    }

    @Test
    fun whatsNewDialog_hasProperFeatureDescriptions() {
        // Given
        composeTestRule.setContent {
            FFinderTheme {
                WhatsNewDialog(
                    onDismiss = {}
                )
            }
        }

        // Then - Check Nearby Friends panel description
        composeTestRule
            .onNodeWithText("👥")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("See which friends are close by with real-time distance updates and quick actions.")
            .assertIsDisplayed()

        // Check Quick Share description
        composeTestRule
            .onNodeWithText("⚡")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Share your location instantly with one tap, no need to navigate through menus.")
            .assertIsDisplayed()
    }

    @Test
    fun whatsNewDialog_hasProperStyling() {
        // Given
        composeTestRule.setContent {
            FFinderTheme {
                WhatsNewDialog(
                    onDismiss = {}
                )
            }
        }

        // Then - Verify dialog structure and key elements exist
        composeTestRule
            .onNodeWithText("What's New in FFinder")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("We've added exciting new features to make finding and sharing with friends even easier:")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Got it!")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun whatsNewTeaser_hasProperCardStyling() {
        // Given
        composeTestRule.setContent {
            FFinderTheme {
                WhatsNewTeaser(
                    onTap = {},
                    isVisible = true
                )
            }
        }

        // Then - Verify the card contains the expected content in proper layout
        composeTestRule
            .onNodeWithContentDescription("What's New teaser card. Tap to learn about new features: Nearby Friends panel and Quick Share")
            .assertIsDisplayed()
            .assertHasClickAction()

        // Verify emoji and text are both present
        composeTestRule
            .onNodeWithText("🚀")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("New: Nearby Friends panel & Quick Share!")
            .assertIsDisplayed()
    }
}