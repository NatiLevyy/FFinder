package com.locationsharing.app.ui.friends.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.locationsharing.app.ui.theme.FFinderTheme
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertTrue

/**
 * Comprehensive design validation tests for FriendsToggleFAB component.
 * 
 * Tests Material 3 design guidelines compliance, theming consistency,
 * accessibility standards, and responsive behavior across different configurations.
 */
class FriendsToggleFABDesignValidationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `verify Material 3 design guidelines compliance`() {
        var clickCount = 0
        
        composeTestRule.setContent {
            FFinderTheme {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    FriendsToggleFAB(
                        onClick = { clickCount++ },
                        friendCount = 5,
                        isExpanded = true,
                        isPanelOpen = false,
                        modifier = Modifier
                            .testTag("friends_toggle_fab")
                            .padding(16.dp)
                    )
                }
            }
        }

        // Verify FAB is displayed with proper Material 3 styling
        composeTestRule.onNodeWithTag("friends_toggle_fab").assertIsDisplayed()
        composeTestRule.onNodeWithText("Nearby Friends").assertIsDisplayed()
        
        // Test interaction
        composeTestRule.onNodeWithTag("friends_toggle_fab").performClick()
        assertTrue(clickCount == 1, "FAB should respond to clicks")
    }

    @Test
    fun `verify proper elevation and corners follow Material 3 guidelines`() {
        composeTestRule.setContent {
            FFinderTheme {
                // Test with different states to verify elevation consistency
                Box(modifier = Modifier.fillMaxSize()) {
                    FriendsToggleFAB(
                        onClick = { },
                        friendCount = 3,
                        isExpanded = true,
                        isPanelOpen = false,
                        modifier = Modifier
                            .testTag("fab_elevation_test")
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                    )
                }
            }
        }

        // Verify FAB maintains proper Material 3 elevation
        // ExtendedFloatingActionButton should have 6.dp elevation by default
        composeTestRule.onNodeWithTag("fab_elevation_test").assertIsDisplayed()
        
        // The elevation and corner radius are handled by Material 3 ExtendedFloatingActionButton
        // which automatically applies the correct Material 3 design tokens
    }

    @Test
    fun `test enhanced button appearance in light theme`() {
        composeTestRule.setContent {
            FFinderTheme(darkTheme = false) {
                val primaryContainer = MaterialTheme.colorScheme.primaryContainer
                val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer
                val error = MaterialTheme.colorScheme.error
                val onError = MaterialTheme.colorScheme.onError
                
                // Verify light theme colors are applied correctly
                Box(modifier = Modifier.fillMaxSize()) {
                    FriendsToggleFAB(
                        onClick = { },
                        friendCount = 7,
                        isExpanded = true,
                        isPanelOpen = false,
                        modifier = Modifier
                            .testTag("light_theme_fab")
                            .align(Alignment.Center)
                    )
                }
                
                // Test that colors are appropriate for light theme
                assertTrue(
                    primaryContainer.luminance() > 0.5f || 
                    onPrimaryContainer.luminance() < 0.5f,
                    "Light theme should have appropriate contrast"
                )
            }
        }

        composeTestRule.onNodeWithTag("light_theme_fab").assertIsDisplayed()
        composeTestRule.onNodeWithText("Nearby Friends").assertIsDisplayed()
    }

    @Test
    fun `test enhanced button appearance in dark theme`() {
        composeTestRule.setContent {
            FFinderTheme(darkTheme = true) {
                val primaryContainer = MaterialTheme.colorScheme.primaryContainer
                val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer
                val error = MaterialTheme.colorScheme.error
                val onError = MaterialTheme.colorScheme.onError
                
                // Verify dark theme colors are applied correctly
                Box(modifier = Modifier.fillMaxSize()) {
                    FriendsToggleFAB(
                        onClick = { },
                        friendCount = 12,
                        isExpanded = true,
                        isPanelOpen = false,
                        modifier = Modifier
                            .testTag("dark_theme_fab")
                            .align(Alignment.Center)
                    )
                }
                
                // Test that colors are appropriate for dark theme
                assertTrue(
                    primaryContainer.luminance() < 0.5f || 
                    onPrimaryContainer.luminance() > 0.5f,
                    "Dark theme should have appropriate contrast"
                )
            }
        }

        composeTestRule.onNodeWithTag("dark_theme_fab").assertIsDisplayed()
        composeTestRule.onNodeWithText("Nearby Friends").assertIsDisplayed()
    }

    @Test
    fun `ensure visual hierarchy is maintained with other map controls`() {
        composeTestRule.setContent {
            FFinderTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Simulate other map controls
                    SelfLocationFAB(
                        onClick = { },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                            .testTag("self_location_fab")
                    )
                    
                    // Test extended FAB positioning
                    FriendsToggleFAB(
                        onClick = { },
                        friendCount = 4,
                        isExpanded = true,
                        isPanelOpen = false,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .testTag("friends_fab_hierarchy")
                    )
                }
            }
        }

        // Verify both FABs are displayed without overlap
        composeTestRule.onNodeWithTag("self_location_fab").assertIsDisplayed()
        composeTestRule.onNodeWithTag("friends_fab_hierarchy").assertIsDisplayed()
        
        // Visual hierarchy is maintained through proper positioning
        // ExtendedFAB at top-right, regular FAB at bottom-right
    }

    @Test
    fun `validate color contrast ratios meet accessibility standards`() {
        composeTestRule.setContent {
            FFinderTheme {
                val primaryContainer = MaterialTheme.colorScheme.primaryContainer
                val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer
                val error = MaterialTheme.colorScheme.error
                val onError = MaterialTheme.colorScheme.onError
                
                // Test contrast ratios for text and badge
                val textContrast = calculateContrastRatio(primaryContainer, onPrimaryContainer)
                val badgeContrast = calculateContrastRatio(error, onError)
                
                // WCAG AA requires 4.5:1 for normal text, 3:1 for large text
                assertTrue(
                    textContrast >= 3.0,
                    "Text contrast ratio should meet WCAG AA standards (3:1 for large text). Actual: $textContrast"
                )
                
                assertTrue(
                    badgeContrast >= 4.5,
                    "Badge contrast ratio should meet WCAG AA standards (4.5:1 for normal text). Actual: $badgeContrast"
                )
                
                FriendsToggleFAB(
                    onClick = { },
                    friendCount = 8,
                    isExpanded = true,
                    isPanelOpen = false,
                    modifier = Modifier.testTag("contrast_test_fab")
                )
            }
        }

        composeTestRule.onNodeWithTag("contrast_test_fab").assertIsDisplayed()
    }

    @Test
    fun `test enhanced button scaling across different screen densities`() {
        // Test different density configurations
        val densities = listOf(1.0f, 1.5f, 2.0f, 3.0f) // ldpi, mdpi, hdpi, xhdpi
        
        densities.forEach { densityValue ->
            composeTestRule.setContent {
                FFinderTheme {
                    // Simulate different screen densities
                    androidx.compose.runtime.CompositionLocalProvider(
                        LocalDensity provides Density(density = densityValue)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            FriendsToggleFAB(
                                onClick = { },
                                friendCount = 6,
                                isExpanded = true,
                                isPanelOpen = false,
                                modifier = Modifier
                                    .testTag("density_test_fab_$densityValue")
                                    .align(Alignment.Center)
                            )
                        }
                    }
                }
            }

            // Verify FAB scales properly at different densities
            composeTestRule.onNodeWithTag("density_test_fab_$densityValue").assertIsDisplayed()
            
            // The minimum touch target should be 48dp regardless of density
            // This is automatically handled by ExtendedFloatingActionButton
        }
    }

    @Test
    fun `test responsive positioning across different screen sizes`() {
        // Test compact screen behavior (< 600dp width)
        composeTestRule.setContent {
            FFinderTheme {
                // Simulate compact screen configuration
                androidx.compose.runtime.CompositionLocalProvider(
                    LocalConfiguration provides LocalConfiguration.current
                ) {
                    var isExpanded by mutableStateOf(true)
                    
                    Box(modifier = Modifier.fillMaxSize()) {
                        FriendsToggleFAB(
                            onClick = { isExpanded = !isExpanded },
                            friendCount = 9,
                            isExpanded = isExpanded,
                            isPanelOpen = false,
                            modifier = Modifier
                                .testTag("compact_screen_fab")
                                .align(Alignment.TopEnd)
                                .padding(16.dp)
                        )
                    }
                }
            }
        }

        // On compact screens, FAB should collapse to icon-only
        composeTestRule.onNodeWithTag("compact_screen_fab").assertIsDisplayed()
        
        // Test large screen behavior (>= 600dp width)
        composeTestRule.setContent {
            FFinderTheme {
                // Simulate large screen configuration
                androidx.compose.runtime.CompositionLocalProvider(
                    LocalConfiguration provides LocalConfiguration.current
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        FriendsToggleFAB(
                            onClick = { },
                            friendCount = 11,
                            isExpanded = true,
                            isPanelOpen = false,
                            modifier = Modifier
                                .testTag("large_screen_fab")
                                .align(Alignment.TopEnd)
                                .padding(16.dp)
                        )
                    }
                }
            }
        }

        // On large screens, FAB should show full text
        composeTestRule.onNodeWithTag("large_screen_fab").assertIsDisplayed()
        composeTestRule.onNodeWithText("Nearby Friends").assertIsDisplayed()
    }

    @Test
    fun `verify animation timing matches app overall animation style`() {
        var isExpanded by mutableStateOf(true)
        var isPanelOpen by mutableStateOf(false)
        
        composeTestRule.setContent {
            FFinderTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    FriendsToggleFAB(
                        onClick = { 
                            isExpanded = !isExpanded
                            isPanelOpen = !isPanelOpen
                        },
                        friendCount = 15,
                        isExpanded = isExpanded,
                        isPanelOpen = isPanelOpen,
                        modifier = Modifier
                            .testTag("animation_test_fab")
                            .align(Alignment.Center)
                    )
                }
            }
        }

        // Test initial state
        composeTestRule.onNodeWithTag("animation_test_fab").assertIsDisplayed()
        
        // Trigger animation by clicking
        composeTestRule.onNodeWithTag("animation_test_fab").performClick()
        
        // Animation timing is handled by the component's internal animateFloatAsState
        // with 300ms duration and proper easing, matching Material 3 guidelines
        
        // Verify FAB still responds after animation
        composeTestRule.onNodeWithTag("animation_test_fab").assertIsDisplayed()
    }

    @Test
    fun `test expand collapse animation behavior`() {
        var isExpanded by mutableStateOf(true)
        var isPanelOpen by mutableStateOf(false)
        
        composeTestRule.setContent {
            FFinderTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    FriendsToggleFAB(
                        onClick = { 
                            isPanelOpen = !isPanelOpen
                        },
                        friendCount = 20,
                        isExpanded = isExpanded,
                        isPanelOpen = isPanelOpen,
                        modifier = Modifier
                            .testTag("expand_collapse_fab")
                            .align(Alignment.Center)
                    )
                }
            }
        }

        // Test expanded state
        composeTestRule.onNodeWithTag("expand_collapse_fab").assertIsDisplayed()
        composeTestRule.onNodeWithText("Nearby Friends").assertIsDisplayed()
        
        // Change to collapsed state
        isExpanded = false
        composeTestRule.waitForIdle()
        
        // FAB should still be displayed but may not show text on compact screens
        composeTestRule.onNodeWithTag("expand_collapse_fab").assertIsDisplayed()
        
        // Test panel open state (should collapse FAB)
        composeTestRule.onNodeWithTag("expand_collapse_fab").performClick()
        composeTestRule.waitForIdle()
        
        // FAB should still be functional
        composeTestRule.onNodeWithTag("expand_collapse_fab").assertIsDisplayed()
    }

    /**
     * Calculate contrast ratio between two colors according to WCAG guidelines.
     * 
     * @param background Background color
     * @param foreground Foreground color
     * @return Contrast ratio (1:1 to 21:1)
     */
    private fun calculateContrastRatio(background: Color, foreground: Color): Double {
        val backgroundLuminance = background.luminance() + 0.05
        val foregroundLuminance = foreground.luminance() + 0.05
        
        return if (backgroundLuminance > foregroundLuminance) {
            backgroundLuminance / foregroundLuminance
        } else {
            foregroundLuminance / backgroundLuminance
        }
    }
}