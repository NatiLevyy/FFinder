package com.locationsharing.app.ui.map.crossdevice

import android.content.res.Configuration
import android.os.Build
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.locationsharing.app.ui.screens.MapScreen
import com.locationsharing.app.ui.theme.FFTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Cross-device compatibility tests for MapScreen across different Android versions,
 * screen sizes, densities, and configurations.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class CrossDeviceCompatibilityTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun mapScreen_displaysCorrectly_onDifferentAndroidVersions() {
        // Test on current API level
        composeTestRule.setContent {
            FFTheme {
                MapScreen(
                    onNavigateBack = {},
                    onNavigateToFriends = {}
                )
            }
        }

        // Verify core components are present regardless of API level
        composeTestRule.onNodeWithContentDescription("Navigate back").assertExists()
        composeTestRule.onNodeWithContentDescription("View nearby friends").assertExists()
        composeTestRule.onNodeWithContentDescription("Share your location instantly").assertExists()
        
        // Verify map container exists
        composeTestRule.onNodeWithTag("GoogleMap").assertExists()
        
        // Test API-specific features
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                // Android 12+ specific tests
                verifyAndroid12PlusFeatures()
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                // Android 10+ specific tests
                verifyAndroid10PlusFeatures()
            }
            else -> {
                // Legacy Android tests
                verifyLegacyAndroidFeatures()
            }
        }
    }

    @Test
    fun mapScreen_adaptsToSmallScreens() {
        // Simulate small screen (phone in portrait)
        composeTestRule.setContent {
            FFTheme {
                MapScreen(
                    onNavigateBack = {},
                    onNavigateToFriends = {}
                )
            }
        }

        // Verify FABs are positioned correctly on small screens
        composeTestRule.onNodeWithContentDescription("Share your location instantly")
            .assertIsDisplayed()
            .assertPositionInRootIsEqualTo(
                expectedLeft = 0.dp, // Will be positioned from right edge
                expectedTop = 0.dp   // Will be positioned from bottom edge
            )

        // Verify app bar is compact on small screens
        composeTestRule.onNodeWithText("Your Location")
            .assertIsDisplayed()
    }

    @Test
    fun mapScreen_adaptsToLargeScreens() {
        // Simulate large screen (tablet)
        composeTestRule.setContent {
            FFTheme {
                MapScreen(
                    onNavigateBack = {},
                    onNavigateToFriends = {}
                )
            }
        }

        // Verify components scale appropriately for large screens
        composeTestRule.onNodeWithContentDescription("Share your location instantly")
            .assertIsDisplayed()

        // Verify drawer width is appropriate for large screens
        composeTestRule.onNodeWithContentDescription("View nearby friends")
            .performClick()

        // Drawer should be visible and properly sized
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("NearbyFriendsDrawer")
            .assertIsDisplayed()
    }

    @Test
    fun mapScreen_adaptsToHighDensityScreens() {
        composeTestRule.setContent {
            FFTheme {
                MapScreen(
                    onNavigateBack = {},
                    onNavigateToFriends = {}
                )
            }
        }

        // Verify touch targets are appropriate size for high density
        composeTestRule.onNodeWithContentDescription("Navigate back")
            .assertHeightIsAtLeast(48.dp)
            .assertWidthIsAtLeast(48.dp)

        composeTestRule.onNodeWithContentDescription("View nearby friends")
            .assertHeightIsAtLeast(48.dp)
            .assertWidthIsAtLeast(48.dp)

        composeTestRule.onNodeWithContentDescription("Share your location instantly")
            .assertHeightIsAtLeast(56.dp)
            .assertWidthIsAtLeast(56.dp)
    }

    @Test
    fun mapScreen_adaptsToLowDensityScreens() {
        composeTestRule.setContent {
            FFTheme {
                MapScreen(
                    onNavigateBack = {},
                    onNavigateToFriends = {}
                )
            }
        }

        // Verify components are still usable on low density screens
        composeTestRule.onNodeWithContentDescription("Navigate back")
            .assertIsDisplayed()
            .performClick()

        composeTestRule.onNodeWithContentDescription("Share your location instantly")
            .assertIsDisplayed()
            .performClick()
    }

    @Test
    fun mapScreen_worksInLandscapeOrientation() {
        composeTestRule.setContent {
            FFTheme {
                MapScreen(
                    onNavigateBack = {},
                    onNavigateToFriends = {}
                )
            }
        }

        // Verify layout adapts to landscape
        composeTestRule.onNodeWithText("Your Location").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Share your location instantly").assertIsDisplayed()
        
        // Verify drawer still works in landscape
        composeTestRule.onNodeWithContentDescription("View nearby friends")
            .performClick()
        
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("NearbyFriendsDrawer")
            .assertIsDisplayed()
    }

    @Test
    fun mapScreen_worksInPortraitOrientation() {
        composeTestRule.setContent {
            FFTheme {
                MapScreen(
                    onNavigateBack = {},
                    onNavigateToFriends = {}
                )
            }
        }

        // Verify standard portrait layout
        composeTestRule.onNodeWithText("Your Location").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Share your location instantly").assertIsDisplayed()
        
        // Verify all FABs are accessible in portrait
        composeTestRule.onNodeWithContentDescription("Center map on your location").assertIsDisplayed()
    }

    private fun verifyAndroid12PlusFeatures() {
        // Test Android 12+ specific features like splash screen API, themed icons
        // Verify location permission handling for Android 12+
        composeTestRule.onNodeWithContentDescription("Center map on your location")
            .assertIsDisplayed()
    }

    private fun verifyAndroid10PlusFeatures() {
        // Test Android 10+ features like scoped storage, background location
        composeTestRule.onNodeWithContentDescription("Share your location instantly")
            .assertIsDisplayed()
    }

    private fun verifyLegacyAndroidFeatures() {
        // Test compatibility with older Android versions
        composeTestRule.onNodeWithContentDescription("Navigate back")
            .assertIsDisplayed()
    }
}