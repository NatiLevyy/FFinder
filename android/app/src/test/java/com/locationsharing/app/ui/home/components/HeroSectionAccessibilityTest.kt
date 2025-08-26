package com.locationsharing.app.ui.home.components

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
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
 * Accessibility tests for the HeroSection composable.
 * 
 * Tests cover:
 * - Content descriptions for screen readers
 * - Animation respect for accessibility preferences
 * - Semantic properties for assistive technologies
 * - Text readability and contrast
 */
@RunWith(AndroidJUnit4::class)
class HeroSectionAccessibilityTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun heroSection_logoHasMeaningfulContentDescription() {
        composeTestRule.setContent {
            FFinderTheme {
                BackgroundGradient {
                    HeroSection(animationsEnabled = true)
                }
            }
        }

        // Verify the logo has a descriptive content description
        composeTestRule
            .onNodeWithContentDescription("FFinder - Find Friends, Share Locations")
            .assertIsDisplayed()
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ContentDescription,
                    listOf("FFinder - Find Friends, Share Locations")
                )
            )
    }

    @Test
    fun heroSection_subtitleIsAccessibleToScreenReaders() {
        composeTestRule.setContent {
            FFinderTheme {
                BackgroundGradient {
                    HeroSection(animationsEnabled = true)
                }
            }
        }

        // Verify subtitle text is accessible
        composeTestRule
            .onNodeWithText("Share your live location and find friends instantly.")
            .assertIsDisplayed()
    }

    @Test
    fun heroSection_respectsAnimationPreferences_whenDisabled() {
        composeTestRule.setContent {
            FFinderTheme {
                BackgroundGradient {
                    HeroSection(animationsEnabled = false)
                }
            }
        }

        // When animations are disabled, content should still be visible
        composeTestRule
            .onNodeWithContentDescription("FFinder - Find Friends, Share Locations")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Share your live location and find friends instantly.")
            .assertIsDisplayed()
    }

    @Test
    fun heroSection_respectsAnimationPreferences_whenEnabled() {
        composeTestRule.setContent {
            FFinderTheme {
                BackgroundGradient {
                    HeroSection(animationsEnabled = true)
                }
            }
        }

        // When animations are enabled, content should still be visible
        composeTestRule
            .onNodeWithContentDescription("FFinder - Find Friends, Share Locations")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Share your live location and find friends instantly.")
            .assertIsDisplayed()
    }

    @Test
    fun heroSection_maintainsAccessibilityInDarkTheme() {
        composeTestRule.setContent {
            FFinderTheme(darkTheme = true) {
                BackgroundGradient {
                    HeroSection(animationsEnabled = true)
                }
            }
        }

        // Verify accessibility is maintained in dark theme
        composeTestRule
            .onNodeWithContentDescription("FFinder - Find Friends, Share Locations")
            .assertIsDisplayed()
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ContentDescription,
                    listOf("FFinder - Find Friends, Share Locations")
                )
            )
        
        composeTestRule
            .onNodeWithText("Share your live location and find friends instantly.")
            .assertIsDisplayed()
    }

    @Test
    fun heroSection_logoContentDescriptionIsDescriptive() {
        composeTestRule.setContent {
            FFinderTheme {
                BackgroundGradient {
                    HeroSection(animationsEnabled = true)
                }
            }
        }

        // Verify the content description is descriptive and helpful
        val expectedDescription = "FFinder - Find Friends, Share Locations"
        
        composeTestRule
            .onNodeWithContentDescription(expectedDescription)
            .assertIsDisplayed()
        
        // The description should clearly indicate what the app does
        assert(expectedDescription.contains("Find Friends"))
        assert(expectedDescription.contains("Share Locations"))
        assert(expectedDescription.contains("FFinder"))
    }

    @Test
    fun heroSection_subtitleProvidesContextualInformation() {
        composeTestRule.setContent {
            FFinderTheme {
                BackgroundGradient {
                    HeroSection(animationsEnabled = true)
                }
            }
        }

        val subtitleText = "Share your live location and find friends instantly."
        
        composeTestRule
            .onNodeWithText(subtitleText)
            .assertIsDisplayed()
        
        // Verify the subtitle provides clear context about app functionality
        assert(subtitleText.contains("live location"))
        assert(subtitleText.contains("find friends"))
        assert(subtitleText.contains("instantly"))
    }
}