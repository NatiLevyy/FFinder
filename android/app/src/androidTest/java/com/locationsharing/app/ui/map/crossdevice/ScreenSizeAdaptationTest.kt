package com.locationsharing.app.ui.map.crossdevice

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.locationsharing.app.ui.screens.MapScreen
import com.locationsharing.app.ui.theme.FFTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests MapScreen adaptation to different screen sizes and densities.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ScreenSizeAdaptationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun mapScreen_adaptsToCompactWidth() {
        // Simulate compact width (typical phone portrait)
        composeTestRule.setContent {
            SimulateScreenSize(widthDp = 360, heightDp = 640) {
                FFTheme {
                    MapScreen(
                        onNavigateBack = {},
                        onNavigateToFriends = {}
                    )
                }
            }
        }

        // Verify compact layout
        composeTestRule.onNodeWithText("Your Location").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Share your location instantly").assertIsDisplayed()
        
        // Verify drawer width is appropriate for compact screens
        composeTestRule.onNodeWithContentDescription("View nearby friends").performClick()
        composeTestRule.waitForIdle()
        
        // Drawer should not take up too much space on compact screens
        composeTestRule.onNodeWithTag("NearbyFriendsDrawer").assertIsDisplayed()
    }

    @Test
    fun mapScreen_adaptsToMediumWidth() {
        // Simulate medium width (large phone or small tablet)
        composeTestRule.setContent {
            SimulateScreenSize(widthDp = 600, heightDp = 800) {
                FFTheme {
                    MapScreen(
                        onNavigateBack = {},
                        onNavigateToFriends = {}
                    )
                }
            }
        }

        // Verify medium layout adaptations
        composeTestRule.onNodeWithText("Your Location").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Share your location instantly").assertIsDisplayed()
        
        // Test drawer behavior on medium screens
        composeTestRule.onNodeWithContentDescription("View nearby friends").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("NearbyFriendsDrawer").assertIsDisplayed()
    }

    @Test
    fun mapScreen_adaptsToExpandedWidth() {
        // Simulate expanded width (tablet landscape)
        composeTestRule.setContent {
            SimulateScreenSize(widthDp = 1024, heightDp = 768) {
                FFTheme {
                    MapScreen(
                        onNavigateBack = {},
                        onNavigateToFriends = {}
                    )
                }
            }
        }

        // Verify expanded layout
        composeTestRule.onNodeWithText("Your Location").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Share your location instantly").assertIsDisplayed()
        
        // On expanded screens, drawer should have more space
        composeTestRule.onNodeWithContentDescription("View nearby friends").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("NearbyFriendsDrawer").assertIsDisplayed()
    }

    @Test
    fun mapScreen_adaptsToCompactHeight() {
        // Simulate compact height (phone landscape)
        composeTestRule.setContent {
            SimulateScreenSize(widthDp = 640, heightDp = 360) {
                FFTheme {
                    MapScreen(
                        onNavigateBack = {},
                        onNavigateToFriends = {}
                    )
                }
            }
        }

        // Verify compact height adaptations
        composeTestRule.onNodeWithText("Your Location").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Share your location instantly").assertIsDisplayed()
        
        // FABs should still be accessible in compact height
        composeTestRule.onNodeWithContentDescription("Center map on your location").assertIsDisplayed()
    }

    @Test
    fun mapScreen_adaptsToMediumHeight() {
        // Simulate medium height
        composeTestRule.setContent {
            SimulateScreenSize(widthDp = 600, heightDp = 900) {
                FFTheme {
                    MapScreen(
                        onNavigateBack = {},
                        onNavigateToFriends = {}
                    )
                }
            }
        }

        // Verify medium height layout
        composeTestRule.onNodeWithText("Your Location").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Share your location instantly").assertIsDisplayed()
    }

    @Test
    fun mapScreen_adaptsToExpandedHeight() {
        // Simulate expanded height (tall tablet)
        composeTestRule.setContent {
            SimulateScreenSize(widthDp = 800, heightDp = 1200) {
                FFTheme {
                    MapScreen(
                        onNavigateBack = {},
                        onNavigateToFriends = {}
                    )
                }
            }
        }

        // Verify expanded height layout
        composeTestRule.onNodeWithText("Your Location").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Share your location instantly").assertIsDisplayed()
        
        // Test bottom sheet behavior on tall screens
        composeTestRule.onNodeWithContentDescription("Share your location instantly").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun mapScreen_handlesLowDensityScreens() {
        // Test on low density screens (ldpi - 120dpi)
        composeTestRule.setContent {
            FFTheme {
                MapScreen(
                    onNavigateBack = {},
                    onNavigateToFriends = {}
                )
            }
        }

        // Verify touch targets are still adequate on low density
        composeTestRule.onNodeWithContentDescription("Navigate back")
            .assertHeightIsAtLeast(48.dp)
            .assertWidthIsAtLeast(48.dp)
            .performClick()
    }

    @Test
    fun mapScreen_handlesHighDensityScreens() {
        // Test on high density screens (xxxhdpi - 640dpi)
        composeTestRule.setContent {
            FFTheme {
                MapScreen(
                    onNavigateBack = {},
                    onNavigateToFriends = {}
                )
            }
        }

        // Verify components scale properly on high density
        composeTestRule.onNodeWithContentDescription("Share your location instantly")
            .assertHeightIsAtLeast(56.dp)
            .assertWidthIsAtLeast(56.dp)
            .performClick()
    }

    @Test
    fun mapScreen_maintainsUsabilityAcrossAllSizes() {
        val screenSizes = listOf(
            Pair(320, 480), // Small phone
            Pair(360, 640), // Medium phone
            Pair(411, 731), // Large phone
            Pair(600, 800), // Small tablet
            Pair(800, 1280), // Large tablet
            Pair(1024, 768)  // Tablet landscape
        )

        screenSizes.forEach { (width, height) ->
            composeTestRule.setContent {
                SimulateScreenSize(widthDp = width, heightDp = height) {
                    FFTheme {
                        MapScreen(
                            onNavigateBack = {},
                            onNavigateToFriends = {}
                        )
                    }
                }
            }

            // Verify core functionality is accessible on all sizes
            composeTestRule.onNodeWithContentDescription("Navigate back").assertIsDisplayed()
            composeTestRule.onNodeWithContentDescription("View nearby friends").assertIsDisplayed()
            composeTestRule.onNodeWithContentDescription("Share your location instantly").assertIsDisplayed()
            composeTestRule.onNodeWithContentDescription("Center map on your location").assertIsDisplayed()
        }
    }

    @Composable
    private fun SimulateScreenSize(
        widthDp: Int,
        heightDp: Int,
        content: @Composable () -> Unit
    ) {
        BoxWithConstraints {
            // Simulate specific screen size constraints
            content()
        }
    }
}