package com.locationsharing.app.ui.home.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.locationsharing.app.ui.theme.FFinderTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for the HeroSection composable with BackgroundGradient.
 * 
 * Tests the complete hero section implementation including:
 * - Integration with BackgroundGradient
 * - Logo and subtitle visibility
 * - Animation state handling
 * - Theme integration
 */
@RunWith(AndroidJUnit4::class)
class HeroSectionIntegrationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun heroSectionIntegration_displaysCorrectlyWithBackgroundGradient() {
        composeTestRule.setContent {
            FFinderTheme {
                BackgroundGradient {
                    HeroSection(animationsEnabled = true)
                }
            }
        }

        // Verify logo is displayed
        composeTestRule
            .onNodeWithContentDescription("FFinder - Find Friends, Share Locations")
            .assertIsDisplayed()

        // Verify subtitle is displayed
        composeTestRule
            .onNodeWithText("Share your live location and find friends instantly.")
            .assertIsDisplayed()
    }

    @Test
    fun heroSectionIntegration_worksWithAnimationsDisabled() {
        composeTestRule.setContent {
            FFinderTheme {
                BackgroundGradient {
                    HeroSection(animationsEnabled = false)
                }
            }
        }

        // Verify logo is displayed even with animations disabled
        composeTestRule
            .onNodeWithContentDescription("FFinder - Find Friends, Share Locations")
            .assertIsDisplayed()

        // Verify subtitle is displayed
        composeTestRule
            .onNodeWithText("Share your live location and find friends instantly.")
            .assertIsDisplayed()
    }

    @Test
    fun heroSectionIntegration_worksInDarkTheme() {
        composeTestRule.setContent {
            FFinderTheme(darkTheme = true) {
                BackgroundGradient {
                    HeroSection(animationsEnabled = true)
                }
            }
        }

        // Verify components are displayed in dark theme
        composeTestRule
            .onNodeWithContentDescription("FFinder - Find Friends, Share Locations")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Share your live location and find friends instantly.")
            .assertIsDisplayed()
    }

    @Test
    fun heroSectionIntegration_maintainsVisibilityDuringAnimationCycle() {
        composeTestRule.setContent {
            FFinderTheme {
                BackgroundGradient {
                    HeroSection(animationsEnabled = true)
                }
            }
        }

        // Verify initial state
        composeTestRule
            .onNodeWithContentDescription("FFinder - Find Friends, Share Locations")
            .assertIsDisplayed()

        // Advance time through animation cycles
        composeTestRule.mainClock.advanceTimeBy(1000) // Fade-in complete
        composeTestRule
            .onNodeWithContentDescription("FFinder - Find Friends, Share Locations")
            .assertIsDisplayed()

        composeTestRule.mainClock.advanceTimeBy(2000) // Mid zoom cycle
        composeTestRule
            .onNodeWithContentDescription("FFinder - Find Friends, Share Locations")
            .assertIsDisplayed()

        composeTestRule.mainClock.advanceTimeBy(2000) // Complete zoom cycle
        composeTestRule
            .onNodeWithContentDescription("FFinder - Find Friends, Share Locations")
            .assertIsDisplayed()
    }
}