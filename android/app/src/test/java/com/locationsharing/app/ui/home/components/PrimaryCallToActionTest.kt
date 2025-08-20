package com.locationsharing.app.ui.home.components

import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.locationsharing.app.ui.theme.FFinderTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Unit tests for the PrimaryCallToAction component.
 * 
 * Tests the component's behavior in both normal and narrow screen modes,
 * verifying proper display, accessibility, and interaction handling.
 */
@RunWith(AndroidJUnit4::class)
class PrimaryCallToActionTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun primaryCallToAction_normalScreen_displaysExtendedFAB() {
        composeTestRule.setContent {
            FFinderTheme {
                PrimaryCallToAction(
                    onStartShare = {},
                    isNarrowScreen = false
                )
            }
        }
        
        // Verify Extended FAB is displayed with text
        composeTestRule
            .onNodeWithText("Start Live Sharing")
            .assertIsDisplayed()
            .assertHasClickAction()
        
        // Verify accessibility content description
        composeTestRule
            .onNodeWithContentDescription("Start Live Sharing button")
            .assertIsDisplayed()
    }
    
    @Test
    fun primaryCallToAction_narrowScreen_displaysIconOnlyFAB() {
        composeTestRule.setContent {
            FFinderTheme {
                PrimaryCallToAction(
                    onStartShare = {},
                    isNarrowScreen = true
                )
            }
        }
        
        // Verify icon-only FAB is displayed
        composeTestRule
            .onNodeWithContentDescription("Start Live Sharing")
            .assertIsDisplayed()
            .assertHasClickAction()
        
        // Verify text is not displayed in narrow mode
        composeTestRule
            .onNodeWithText("Start Live Sharing")
            .assertDoesNotExist()
    }
    
    @Test
    fun primaryCallToAction_normalScreen_clickTriggersCallback() {
        var startShareClicked = false
        
        composeTestRule.setContent {
            FFinderTheme {
                PrimaryCallToAction(
                    onStartShare = { startShareClicked = true },
                    isNarrowScreen = false
                )
            }
        }
        
        composeTestRule
            .onNodeWithText("Start Live Sharing")
            .performClick()
        
        assertTrue("onStartShare callback should be invoked", startShareClicked)
    }
    
    @Test
    fun primaryCallToAction_narrowScreen_clickTriggersCallback() {
        var startShareClicked = false
        
        composeTestRule.setContent {
            FFinderTheme {
                PrimaryCallToAction(
                    onStartShare = { startShareClicked = true },
                    isNarrowScreen = true
                )
            }
        }
        
        composeTestRule
            .onNodeWithContentDescription("Start Live Sharing")
            .performClick()
        
        assertTrue("onStartShare callback should be invoked", startShareClicked)
    }
    
    @Test
    fun primaryCallToAction_hasProperAccessibilitySupport() {
        composeTestRule.setContent {
            FFinderTheme {
                PrimaryCallToAction(
                    onStartShare = {},
                    isNarrowScreen = false
                )
            }
        }
        
        // Verify accessibility properties
        composeTestRule
            .onNodeWithContentDescription("Start Live Sharing button")
            .assertIsDisplayed()
            .assertHasClickAction()
    }
    
    @Test
    fun primaryCallToAction_narrowScreen_hasTooltipAccessibility() {
        composeTestRule.setContent {
            FFinderTheme {
                PrimaryCallToAction(
                    onStartShare = {},
                    isNarrowScreen = true
                )
            }
        }
        
        // Verify icon-only FAB has proper accessibility
        composeTestRule
            .onNodeWithContentDescription("Start Live Sharing")
            .assertIsDisplayed()
            .assertHasClickAction()
    }
    
    @Test
    fun primaryCallToAction_respondsToScreenSizeChanges() {
        var isNarrow = false
        
        composeTestRule.setContent {
            FFinderTheme {
                PrimaryCallToAction(
                    onStartShare = {},
                    isNarrowScreen = isNarrow
                )
            }
        }
        
        // Initially normal screen - should show extended FAB
        composeTestRule
            .onNodeWithText("Start Live Sharing")
            .assertIsDisplayed()
        
        // Change to narrow screen
        isNarrow = true
        composeTestRule.setContent {
            FFinderTheme {
                PrimaryCallToAction(
                    onStartShare = {},
                    isNarrowScreen = isNarrow
                )
            }
        }
        
        // Should now show icon-only FAB
        composeTestRule
            .onNodeWithContentDescription("Start Live Sharing")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Start Live Sharing")
            .assertDoesNotExist()
    }
}