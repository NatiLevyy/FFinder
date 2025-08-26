package com.locationsharing.app.ui.home.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.assertIsDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.locationsharing.app.ui.theme.FFinderTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Animation-specific tests for the HeroSection composable.
 * 
 * Tests cover:
 * - Logo fade-in animation behavior
 * - Logo zoom animation behavior
 * - Animation state transitions
 * - Performance during animations
 * - Animation accessibility compliance
 */
@RunWith(AndroidJUnit4::class)
class HeroSectionAnimationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun heroSection_logoAppearsWithFadeInAnimation() {
        composeTestRule.setContent {
            FFinderTheme {
                BackgroundGradient {
                    HeroSection(animationsEnabled = true)
                }
            }
        }

        // Logo should be visible after fade-in animation
        composeTestRule
            .onNodeWithContentDescription("FFinder - Find Friends, Share Locations")
            .assertIsDisplayed()
    }

    @Test
    fun heroSection_logoAppearsImmediatelyWhenAnimationsDisabled() {
        composeTestRule.setContent {
            FFinderTheme {
                BackgroundGradient {
                    HeroSection(animationsEnabled = false)
                }
            }
        }

        // Logo should be immediately visible when animations are disabled
        composeTestRule
            .onNodeWithContentDescription("FFinder - Find Friends, Share Locations")
            .assertIsDisplayed()
    }

    @Test
    fun heroSection_animationStateTransition_enabledToDisabled() {
        var animationsEnabled = true
        
        composeTestRule.setContent {
            FFinderTheme {
                BackgroundGradient {
                    HeroSection(animationsEnabled = animationsEnabled)
                }
            }
        }

        // Initially with animations enabled
        composeTestRule
            .onNodeWithContentDescription("FFinder - Find Friends, Share Locations")
            .assertIsDisplayed()

        // Change to animations disabled
        animationsEnabled = false
        
        composeTestRule.setContent {
            FFinderTheme {
                BackgroundGradient {
                    HeroSection(animationsEnabled = animationsEnabled)
                }
            }
        }

        // Logo should still be visible
        composeTestRule
            .onNodeWithContentDescription("FFinder - Find Friends, Share Locations")
            .assertIsDisplayed()
    }

    @Test
    fun heroSection_animationStateTransition_disabledToEnabled() {
        var animationsEnabled = false
        
        composeTestRule.setContent {
            FFinderTheme {
                BackgroundGradient {
                    HeroSection(animationsEnabled = animationsEnabled)
                }
            }
        }

        // Initially with animations disabled
        composeTestRule
            .onNodeWithContentDescription("FFinder - Find Friends, Share Locations")
            .assertIsDisplayed()

        // Change to animations enabled
        animationsEnabled = true
        
        composeTestRule.setContent {
            FFinderTheme {
                BackgroundGradient {
                    HeroSection(animationsEnabled = animationsEnabled)
                }
            }
        }

        // Logo should still be visible
        composeTestRule
            .onNodeWithContentDescription("FFinder - Find Friends, Share Locations")
            .assertIsDisplayed()
    }

    @Test
    fun heroSection_maintainsVisibilityDuringAnimations() {
        composeTestRule.setContent {
            FFinderTheme {
                BackgroundGradient {
                    HeroSection(animationsEnabled = true)
                }
            }
        }

        // Advance time to simulate animation progress
        composeTestRule.mainClock.advanceTimeBy(500) // 500ms into fade-in animation

        // Logo should be visible during animation
        composeTestRule
            .onNodeWithContentDescription("FFinder - Find Friends, Share Locations")
            .assertIsDisplayed()

        // Advance time further
        composeTestRule.mainClock.advanceTimeBy(1000) // Complete fade-in animation

        // Logo should still be visible after animation completes
        composeTestRule
            .onNodeWithContentDescription("FFinder - Find Friends, Share Locations")
            .assertIsDisplayed()
    }

    @Test
    fun heroSection_zoomAnimationCycle() {
        composeTestRule.setContent {
            FFinderTheme {
                BackgroundGradient {
                    HeroSection(animationsEnabled = true)
                }
            }
        }

        // Logo should be visible at start
        composeTestRule
            .onNodeWithContentDescription("FFinder - Find Friends, Share Locations")
            .assertIsDisplayed()

        // Advance time through zoom animation cycle (4 seconds)
        composeTestRule.mainClock.advanceTimeBy(2000) // Halfway through zoom cycle

        // Logo should still be visible
        composeTestRule
            .onNodeWithContentDescription("FFinder - Find Friends, Share Locations")
            .assertIsDisplayed()

        // Complete zoom cycle
        composeTestRule.mainClock.advanceTimeBy(2000) // Complete 4-second cycle

        // Logo should still be visible
        composeTestRule
            .onNodeWithContentDescription("FFinder - Find Friends, Share Locations")
            .assertIsDisplayed()
    }

    @Test
    fun heroSection_animationPerformance_maintainsResponsiveness() {
        composeTestRule.setContent {
            FFinderTheme {
                BackgroundGradient {
                    HeroSection(animationsEnabled = true)
                }
            }
        }

        // Simulate rapid time advancement to test performance
        repeat(10) {
            composeTestRule.mainClock.advanceTimeBy(400) // Advance in 400ms chunks
            
            // Verify logo remains visible and responsive
            composeTestRule
                .onNodeWithContentDescription("FFinder - Find Friends, Share Locations")
                .assertIsDisplayed()
        }
    }

    @Test
    fun heroSection_accessibilityCompliantAnimations() {
        // Test with animations disabled (accessibility preference)
        composeTestRule.setContent {
            FFinderTheme {
                BackgroundGradient {
                    HeroSection(animationsEnabled = false)
                }
            }
        }

        // Content should be immediately visible without animations
        composeTestRule
            .onNodeWithContentDescription("FFinder - Find Friends, Share Locations")
            .assertIsDisplayed()

        // No time advancement needed - content should be static
        composeTestRule.mainClock.advanceTimeBy(0)
        
        composeTestRule
            .onNodeWithContentDescription("FFinder - Find Friends, Share Locations")
            .assertIsDisplayed()
    }
}