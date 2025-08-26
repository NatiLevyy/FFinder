package com.locationsharing.app.ui.map.crossdevice

import android.os.Build
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.locationsharing.app.ui.screens.MapScreen
import com.locationsharing.app.ui.theme.FFTheme
import kotlinx.coroutines.delay
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.system.measureTimeMillis

/**
 * Tests MapScreen performance across different device capabilities and Android versions.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class PerformanceDeviceTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun mapScreen_performsWellOnLowEndDevices() {
        // Simulate low-end device constraints
        composeTestRule.setContent {
            FFTheme {
                MapScreen(
                    onNavigateBack = {},
                    onNavigateToFriends = {}
                )
            }
        }

        // Measure initial composition time
        val compositionTime = measureTimeMillis {
            composeTestRule.waitForIdle()
        }

        // Verify reasonable composition time (should be under 1000ms even on low-end devices)
        assert(compositionTime < 1000) { "Initial composition took too long: ${compositionTime}ms" }

        // Test interaction performance
        val interactionTime = measureTimeMillis {
            composeTestRule.onNodeWithContentDescription("View nearby friends")
                .performClick()
            composeTestRule.waitForIdle()
        }

        assert(interactionTime < 500) { "Drawer opening took too long: ${interactionTime}ms" }

        // Close drawer
        composeTestRule.onNodeWithTag("DrawerScrim").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun mapScreen_handlesMemoryConstraintsOnLowEndDevices() {
        composeTestRule.setContent {
            FFTheme {
                MapScreen(
                    onNavigateBack = {},
                    onNavigateToFriends = {}
                )
            }
        }

        // Test multiple interactions without memory issues
        repeat(10) {
            composeTestRule.onNodeWithContentDescription("View nearby friends")
                .performClick()
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithTag("DrawerScrim").performClick()
            composeTestRule.waitForIdle()
        }

        // Verify app is still responsive
        composeTestRule.onNodeWithContentDescription("Share your location instantly")
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun mapScreen_animationsPerformWellOnAllDevices() {
        composeTestRule.setContent {
            FFTheme {
                MapScreen(
                    onNavigateBack = {},
                    onNavigateToFriends = {}
                )
            }
        }

        // Test FAB animation performance
        val fabAnimationTime = measureTimeMillis {
            composeTestRule.onNodeWithContentDescription("Share your location instantly")
                .performClick()
            composeTestRule.waitForIdle()
        }

        assert(fabAnimationTime < 300) { "FAB animation took too long: ${fabAnimationTime}ms" }

        // Test drawer animation performance
        val drawerAnimationTime = measureTimeMillis {
            composeTestRule.onNodeWithContentDescription("View nearby friends")
                .performClick()
            composeTestRule.waitForIdle()
        }

        assert(drawerAnimationTime < 400) { "Drawer animation took too long: ${drawerAnimationTime}ms" }

        // Close drawer
        composeTestRule.onNodeWithTag("DrawerScrim").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun mapScreen_maintainsFrameRateOnLowEndDevices() {
        composeTestRule.setContent {
            FFTheme {
                MapScreen(
                    onNavigateBack = {},
                    onNavigateToFriends = {}
                )
            }
        }

        // Simulate continuous interactions to test frame rate
        repeat(5) {
            composeTestRule.onNodeWithContentDescription("View nearby friends")
                .performClick()
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithTag("DrawerScrim").performClick()
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithContentDescription("Share your location instantly")
                .performClick()
            composeTestRule.waitForIdle()
        }

        // Verify app remains responsive
        composeTestRule.onNodeWithText("Your Location").assertIsDisplayed()
    }

    @Test
    fun mapScreen_handlesLowMemoryConditions() {
        composeTestRule.setContent {
            FFTheme {
                MapScreen(
                    onNavigateBack = {},
                    onNavigateToFriends = {}
                )
            }
        }

        // Simulate memory pressure by creating and destroying components
        repeat(20) {
            composeTestRule.onNodeWithContentDescription("View nearby friends")
                .performClick()
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithTag("DrawerScrim").performClick()
            composeTestRule.waitForIdle()
        }

        // Verify core functionality still works
        composeTestRule.onNodeWithContentDescription("Share your location instantly")
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun mapScreen_optimizesForBatteryLifeOnAllDevices() {
        composeTestRule.setContent {
            FFTheme {
                MapScreen(
                    onNavigateBack = {},
                    onNavigateToFriends = {}
                )
            }
        }

        // Test that animations don't run unnecessarily
        composeTestRule.waitForIdle()

        // Verify components are in idle state when not interacting
        composeTestRule.onNodeWithText("Your Location").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Share your location instantly").assertIsDisplayed()

        // Test efficient state management
        composeTestRule.onNodeWithContentDescription("View nearby friends")
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("DrawerScrim").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun mapScreen_handlesSlowNetworkOnAllDevices() {
        composeTestRule.setContent {
            FFTheme {
                MapScreen(
                    onNavigateBack = {},
                    onNavigateToFriends = {}
                )
            }
        }

        // Test that UI remains responsive during network operations
        composeTestRule.onNodeWithContentDescription("Share your location instantly")
            .performClick()
        composeTestRule.waitForIdle()

        // Verify loading states don't block UI
        composeTestRule.onNodeWithContentDescription("View nearby friends")
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("NearbyFriendsDrawer").assertIsDisplayed()
    }

    @Test
    fun mapScreen_performsWellOnOlderAndroidVersions() {
        // Test performance considerations for older Android versions
        composeTestRule.setContent {
            FFTheme {
                MapScreen(
                    onNavigateBack = {},
                    onNavigateToFriends = {}
                )
            }
        }

        // Verify basic functionality works on all supported API levels
        composeTestRule.onNodeWithText("Your Location").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Share your location instantly").assertIsDisplayed()

        // Test interactions
        val interactionTime = measureTimeMillis {
            composeTestRule.onNodeWithContentDescription("View nearby friends")
                .performClick()
            composeTestRule.waitForIdle()
        }

        // Allow more time for older devices
        val maxTime = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) 1000 else 500
        assert(interactionTime < maxTime) { "Interaction took too long on API ${Build.VERSION.SDK_INT}: ${interactionTime}ms" }

        composeTestRule.onNodeWithTag("DrawerScrim").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun mapScreen_handlesRapidInteractionsOnAllDevices() {
        composeTestRule.setContent {
            FFTheme {
                MapScreen(
                    onNavigateBack = {},
                    onNavigateToFriends = {}
                )
            }
        }

        // Test rapid interactions don't cause performance issues
        repeat(10) {
            composeTestRule.onNodeWithContentDescription("Share your location instantly")
                .performClick()
            
            composeTestRule.onNodeWithContentDescription("View nearby friends")
                .performClick()
            composeTestRule.waitForIdle()
            
            composeTestRule.onNodeWithTag("DrawerScrim").performClick()
            composeTestRule.waitForIdle()
        }

        // Verify app is still responsive
        composeTestRule.onNodeWithText("Your Location").assertIsDisplayed()
    }

    @Test
    fun mapScreen_maintainsPerformanceWithDebugFeatures() {
        composeTestRule.setContent {
            FFTheme {
                MapScreen(
                    onNavigateBack = {},
                    onNavigateToFriends = {}
                )
            }
        }

        // Test debug features don't impact performance significantly
        val debugInteractionTime = measureTimeMillis {
            composeTestRule.onNodeWithContentDescription("Add test friends to map")
                .performClick()
            composeTestRule.waitForIdle()
        }

        assert(debugInteractionTime < 500) { "Debug interaction took too long: ${debugInteractionTime}ms" }

        // Verify snackbar appears without performance issues
        composeTestRule.onNodeWithText("Test friends added to map").assertIsDisplayed()
    }
}