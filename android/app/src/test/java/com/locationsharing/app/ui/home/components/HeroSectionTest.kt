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
 * Unit tests for the HeroSection composable.
 * 
 * Tests cover:
 * - Logo visibility and content description
 * - Subtitle text display and content
 * - Animation state handling (enabled/disabled)
 * - Accessibility compliance
 * - Theme compatibility (light/dark)
 */
@RunWith(AndroidJUnit4::class)
class HeroSectionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun heroSection_displaysLogoWithCorrectContentDescription() {
        composeTestRule.setContent {
            FFinderTheme {
                BackgroundGradient {
                    HeroSection(animationsEnabled = true)
                }
            }
        }

        composeTestRule
            .onNodeWithContentDescription("FFinder - Find Friends, Share Locations")
            .assertIsDisplayed()
    }

    @Test
    fun heroSection_displaysSubtitleText() {
        composeTestRule.setContent {
            FFinderTheme {
                BackgroundGradient {
                    HeroSection(animationsEnabled = true)
                }
            }
        }

        composeTestRule
            .onNodeWithText("Share your live location and find friends instantly.")
            .assertIsDisplayed()
    }

    @Test
    fun heroSection_displaysWithAnimationsEnabled() {
        composeTestRule.setContent {
            FFinderTheme {
                BackgroundGradient {
                    HeroSection(animationsEnabled = true)
                }
            }
        }

        // Verify both logo and subtitle are displayed
        composeTestRule
            .onNodeWithContentDescription("FFinder - Find Friends, Share Locations")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Share your live location and find friends instantly.")
            .assertIsDisplayed()
    }

    @Test
    fun heroSection_displaysWithAnimationsDisabled() {
        composeTestRule.setContent {
            FFinderTheme {
                BackgroundGradient {
                    HeroSection(animationsEnabled = false)
                }
            }
        }

        // Verify both logo and subtitle are displayed even when animations are disabled
        composeTestRule
            .onNodeWithContentDescription("FFinder - Find Friends, Share Locations")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Share your live location and find friends instantly.")
            .assertIsDisplayed()
    }

    @Test
    fun heroSection_displaysInDarkTheme() {
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
    fun heroSection_displaysInLightTheme() {
        composeTestRule.setContent {
            FFinderTheme(darkTheme = false) {
                BackgroundGradient {
                    HeroSection(animationsEnabled = true)
                }
            }
        }

        // Verify components are displayed in light theme
        composeTestRule
            .onNodeWithContentDescription("FFinder - Find Friends, Share Locations")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Share your live location and find friends instantly.")
            .assertIsDisplayed()
    }

    @Test
    fun heroSection_hasAccessibleContentDescription() {
        composeTestRule.setContent {
            FFinderTheme {
                BackgroundGradient {
                    HeroSection(animationsEnabled = true)
                }
            }
        }

        // Verify the logo has a meaningful content description for accessibility
        composeTestRule
            .onNodeWithContentDescription("FFinder - Find Friends, Share Locations")
            .assertIsDisplayed()
    }

    @Test
    fun heroSection_subtitleHasCorrectText() {
        val expectedSubtitle = "Share your live location and find friends instantly."
        
        composeTestRule.setContent {
            FFinderTheme {
                BackgroundGradient {
                    HeroSection(animationsEnabled = true)
                }
            }
        }

        composeTestRule
            .onNodeWithText(expectedSubtitle)
            .assertIsDisplayed()
    }
}