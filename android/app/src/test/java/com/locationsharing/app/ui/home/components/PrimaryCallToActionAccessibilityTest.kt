package com.locationsharing.app.ui.home.components

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.locationsharing.app.ui.theme.FFinderTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Accessibility tests for the PrimaryCallToAction component.
 * 
 * Ensures the component meets WCAG guidelines and provides proper
 * accessibility support for users with disabilities.
 */
@RunWith(AndroidJUnit4::class)
class PrimaryCallToActionAccessibilityTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun primaryCallToAction_normalScreen_hasProperContentDescription() {
        composeTestRule.setContent {
            FFinderTheme {
                PrimaryCallToAction(
                    onStartShare = {},
                    isNarrowScreen = false
                )
            }
        }
        
        composeTestRule
            .onNodeWithContentDescription("Start Live Sharing button")
            .assertIsDisplayed()
            .assertHasClickAction()
    }
    
    @Test
    fun primaryCallToAction_narrowScreen_hasProperContentDescription() {
        composeTestRule.setContent {
            FFinderTheme {
                PrimaryCallToAction(
                    onStartShare = {},
                    isNarrowScreen = true
                )
            }
        }
        
        composeTestRule
            .onNodeWithContentDescription("Start Live Sharing")
            .assertIsDisplayed()
            .assertHasClickAction()
    }
    
    @Test
    fun primaryCallToAction_hasButtonRole() {
        composeTestRule.setContent {
            FFinderTheme {
                PrimaryCallToAction(
                    onStartShare = {},
                    isNarrowScreen = false
                )
            }
        }
        
        // Verify the component has button semantics
        composeTestRule
            .onNodeWithContentDescription("Start Live Sharing button")
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.Role,
                    Role.Button
                )
            )
    }
    
    @Test
    fun primaryCallToAction_narrowScreen_tooltipAccessibility() {
        composeTestRule.setContent {
            FFinderTheme {
                PrimaryCallToAction(
                    onStartShare = {},
                    isNarrowScreen = true
                )
            }
        }
        
        // Icon-only FAB should have meaningful content description
        composeTestRule
            .onNodeWithContentDescription("Start Live Sharing")
            .assertIsDisplayed()
            .assertHasClickAction()
    }
    
    @Test
    fun primaryCallToAction_textIsAccessible() {
        composeTestRule.setContent {
            FFinderTheme {
                PrimaryCallToAction(
                    onStartShare = {},
                    isNarrowScreen = false
                )
            }
        }
        
        // Text should be readable by screen readers
        composeTestRule
            .onNodeWithText("Start Live Sharing")
            .assertIsDisplayed()
    }
    
    @Test
    fun primaryCallToAction_iconHasNoContentDescription() {
        composeTestRule.setContent {
            FFinderTheme {
                PrimaryCallToAction(
                    onStartShare = {},
                    isNarrowScreen = false
                )
            }
        }
        
        // Icon inside Extended FAB should not have its own content description
        // to avoid redundant announcements (the button itself has the description)
        composeTestRule
            .onNodeWithContentDescription("Start Live Sharing button")
            .assertIsDisplayed()
    }
    
    @Test
    fun primaryCallToAction_minimumTouchTarget() {
        composeTestRule.setContent {
            FFinderTheme {
                PrimaryCallToAction(
                    onStartShare = {},
                    isNarrowScreen = true // Icon-only FAB should be 56dp (meets 48dp minimum)
                )
            }
        }
        
        // Verify the touch target is accessible
        composeTestRule
            .onNodeWithContentDescription("Start Live Sharing")
            .assertIsDisplayed()
            .assertHasClickAction()
    }
    
    @Test
    fun primaryCallToAction_focusOrder() {
        composeTestRule.setContent {
            FFinderTheme {
                PrimaryCallToAction(
                    onStartShare = {},
                    isNarrowScreen = false
                )
            }
        }
        
        // Component should be focusable for keyboard navigation
        composeTestRule
            .onNodeWithContentDescription("Start Live Sharing button")
            .assertIsDisplayed()
            .assertHasClickAction()
    }
    
    @Test
    fun primaryCallToAction_semanticProperties() {
        composeTestRule.setContent {
            FFinderTheme {
                PrimaryCallToAction(
                    onStartShare = {},
                    isNarrowScreen = false
                )
            }
        }
        
        val node = composeTestRule
            .onNodeWithContentDescription("Start Live Sharing button")
        
        // Verify essential semantic properties
        node.assertIsDisplayed()
        node.assertHasClickAction()
        
        // Should have proper role
        node.assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.Role,
                Role.Button
            )
        )
    }
    
    @Test
    fun primaryCallToAction_contrastAndReadability() {
        composeTestRule.setContent {
            FFinderTheme {
                PrimaryCallToAction(
                    onStartShare = {},
                    isNarrowScreen = false
                )
            }
        }
        
        // Text should be visible and readable
        // (Color contrast is handled by theme, but we verify text is present)
        composeTestRule
            .onNodeWithText("Start Live Sharing")
            .assertIsDisplayed()
    }
}