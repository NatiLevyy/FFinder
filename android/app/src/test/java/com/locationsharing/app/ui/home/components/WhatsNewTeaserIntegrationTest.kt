package com.locationsharing.app.ui.home.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.locationsharing.app.ui.home.HomeScreenEvent
import com.locationsharing.app.ui.theme.FFinderTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for WhatsNewTeaser component verifying:
 * - Integration with HomeScreenEvent system
 * - State management and dialog visibility
 * - Animation behavior in context
 * - Theme integration and styling consistency
 * - Complete user interaction flows
 */
@RunWith(AndroidJUnit4::class)
class WhatsNewTeaserIntegrationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun whatsNewTeaser_integratesWithHomeScreenEvents() {
        // Given
        var lastEvent: HomeScreenEvent? = null
        
        composeTestRule.setContent {
            FFinderTheme {
                WhatsNewTeaser(
                    onTap = { lastEvent = HomeScreenEvent.ShowWhatsNew },
                    isVisible = true
                )
            }
        }

        // When
        composeTestRule
            .onNodeWithContentDescription("What's New teaser card. Tap to learn about new features: Nearby Friends panel and Quick Share")
            .performClick()

        // Then
        assert(lastEvent == HomeScreenEvent.ShowWhatsNew) { 
            "Tapping teaser should trigger ShowWhatsNew event" 
        }
    }

    @Test
    fun whatsNewDialog_integratesWithDismissEvent() {
        // Given
        var lastEvent: HomeScreenEvent? = null
        
        composeTestRule.setContent {
            FFinderTheme {
                WhatsNewDialog(
                    onDismiss = { lastEvent = HomeScreenEvent.DismissWhatsNew }
                )
            }
        }

        // When
        composeTestRule
            .onNodeWithText("Got it!")
            .performClick()

        // Then
        assert(lastEvent == HomeScreenEvent.DismissWhatsNew) { 
            "Dismissing dialog should trigger DismissWhatsNew event" 
        }
    }

    @Test
    fun whatsNewTeaser_worksWithStateManagement() {
        // Given
        var showDialog by mutableStateOf(false)
        
        composeTestRule.setContent {
            FFinderTheme {
                Column {
                    WhatsNewTeaser(
                        onTap = { showDialog = true },
                        isVisible = true
                    )
                    
                    if (showDialog) {
                        WhatsNewDialog(
                            onDismiss = { showDialog = false }
                        )
                    }
                }
            }
        }

        // Initially dialog should not be visible
        composeTestRule
            .onNodeWithText("What's New in FFinder")
            .assertDoesNotExist()

        // When teaser is tapped
        composeTestRule
            .onNodeWithContentDescription("What's New teaser card. Tap to learn about new features: Nearby Friends panel and Quick Share")
            .performClick()

        // Then dialog should appear
        composeTestRule
            .onNodeWithText("What's New in FFinder")
            .assertIsDisplayed()

        // When dialog is dismissed
        composeTestRule
            .onNodeWithText("Got it!")
            .performClick()

        // Then dialog should disappear
        composeTestRule
            .onNodeWithText("What's New in FFinder")
            .assertDoesNotExist()
    }

    @Test
    fun whatsNewTeaser_maintainsThemeConsistency() {
        // Given
        composeTestRule.setContent {
            FFinderTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Test with other components to ensure theme consistency
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors()
                    ) {
                        Text(
                            text = "Reference Card",
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    
                    WhatsNewTeaser(
                        onTap = {},
                        isVisible = true
                    )
                }
            }
        }

        // Then - Both components should be visible and properly themed
        composeTestRule
            .onNodeWithText("Reference Card")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("New: Nearby Friends panel & Quick Share!")
            .assertIsDisplayed()
    }

    @Test
    fun whatsNewTeaser_handlesVisibilityAnimations() {
        // Given
        var isVisible by mutableStateOf(false)
        
        composeTestRule.setContent {
            FFinderTheme {
                WhatsNewTeaser(
                    onTap = {},
                    isVisible = isVisible
                )
            }
        }

        // Initially not visible (animated out)
        composeTestRule
            .onNodeWithText("New: Nearby Friends panel & Quick Share!")
            .assertExists()

        // When visibility changes
        composeTestRule.runOnUiThread {
            isVisible = true
        }

        // Then component should animate in
        composeTestRule
            .onNodeWithText("New: Nearby Friends panel & Quick Share!")
            .assertIsDisplayed()
    }

    @Test
    fun whatsNewDialog_displaysCompleteFeatureInformation() {
        // Given
        composeTestRule.setContent {
            FFinderTheme {
                WhatsNewDialog(
                    onDismiss = {}
                )
            }
        }

        // Then - Verify all feature information is present
        composeTestRule
            .onNodeWithText("What's New in FFinder")
            .assertIsDisplayed()
        
        // Main description
        composeTestRule
            .onNodeWithText("We've added exciting new features to make finding and sharing with friends even easier:")
            .assertIsDisplayed()
        
        // Nearby Friends feature
        composeTestRule
            .onNodeWithText("👥")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Nearby Friends Panel")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("See which friends are close by with real-time distance updates and quick actions.")
            .assertIsDisplayed()
        
        // Quick Share feature
        composeTestRule
            .onNodeWithText("⚡")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Quick Share")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Share your location instantly with one tap, no need to navigate through menus.")
            .assertIsDisplayed()
        
        // Dismiss button
        composeTestRule
            .onNodeWithText("Got it!")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun whatsNewTeaser_worksInScrollableContent() {
        // Given
        composeTestRule.setContent {
            FFinderTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Simulate other home screen components
                    repeat(3) { index ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                        ) {
                            Text(
                                text = "Component $index",
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                    
                    // WhatsNewTeaser at the bottom
                    WhatsNewTeaser(
                        onTap = {},
                        isVisible = true
                    )
                }
            }
        }

        // Then - Teaser should be visible and functional
        composeTestRule
            .onNodeWithText("New: Nearby Friends panel & Quick Share!")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithContentDescription("What's New teaser card. Tap to learn about new features: Nearby Friends panel and Quick Share")
            .assertHasClickAction()
    }

    @Test
    fun whatsNewTeaser_maintainsInteractionDuringAnimation() {
        // Given
        var tapCount = 0
        
        composeTestRule.setContent {
            FFinderTheme {
                WhatsNewTeaser(
                    onTap = { tapCount++ },
                    isVisible = true
                )
            }
        }

        // When - Multiple rapid taps during animation
        repeat(3) {
            composeTestRule
                .onNodeWithContentDescription("What's New teaser card. Tap to learn about new features: Nearby Friends panel and Quick Share")
                .performClick()
        }

        // Then - All taps should be registered
        assert(tapCount == 3) { "All tap events should be handled during animation" }
    }
}