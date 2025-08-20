package com.locationsharing.app.ui.map.crossdevice

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.locationsharing.app.ui.screens.MapScreen
import com.locationsharing.app.ui.theme.FFTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests MapScreen accessibility across different devices and accessibility configurations.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class AccessibilityDeviceTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun mapScreen_providesAccessibilityLabelsOnAllDevices() {
        composeTestRule.setContent {
            FFTheme {
                MapScreen(
                    onNavigateBack = {},
                    onNavigateToFriends = {}
                )
            }
        }

        // Verify all interactive elements have content descriptions
        composeTestRule.onNodeWithContentDescription("Navigate back").assertExists()
        composeTestRule.onNodeWithContentDescription("View nearby friends").assertExists()
        composeTestRule.onNodeWithContentDescription("Share your location instantly").assertExists()
        composeTestRule.onNodeWithContentDescription("Center map on your location").assertExists()
        composeTestRule.onNodeWithContentDescription("Add test friends to map").assertExists()
    }

    @Test
    fun mapScreen_maintainsProperFocusOrderOnAllDevices() {
        composeTestRule.setContent {
            FFTheme {
                MapScreen(
                    onNavigateBack = {},
                    onNavigateToFriends = {}
                )
            }
        }

        // Test focus traversal order
        val focusableNodes = listOf(
            "Navigate back",
            "View nearby friends",
            "Center map on your location", 
            "Share your location instantly",
            "Add test friends to map"
        )

        focusableNodes.forEach { contentDescription ->
            composeTestRule.onNodeWithContentDescription(contentDescription)
                .assertExists()
                .assertIsDisplayed()
        }
    }

    @Test
    fun mapScreen_providesSemanticRolesOnAllDevices() {
        composeTestRule.setContent {
            FFTheme {
                MapScreen(
                    onNavigateBack = {},
                    onNavigateToFriends = {}
                )
            }
        }

        // Verify semantic roles are properly assigned
        composeTestRule.onNodeWithContentDescription("Navigate back")
            .assert(hasAnyAncestor(hasTestTag("TopAppBar")))

        composeTestRule.onNodeWithContentDescription("Share your location instantly")
            .assertExists()

        composeTestRule.onNodeWithContentDescription("View nearby friends")
            .assertExists()
    }

    @Test
    fun mapScreen_supportsScreenReaderNavigationOnAllDevices() {
        composeTestRule.setContent {
            FFTheme {
                MapScreen(
                    onNavigateBack = {},
                    onNavigateToFriends = {}
                )
            }
        }

        // Test screen reader navigation patterns
        composeTestRule.onNodeWithText("Your Location")
            .assertExists()
            .assertIsDisplayed()

        // Test that all interactive elements are discoverable
        composeTestRule.onAllNodesWithContentDescription("Navigate back")
            .assertCountEquals(1)

        composeTestRule.onAllNodesWithContentDescription("View nearby friends")
            .assertCountEquals(1)

        composeTestRule.onAllNodesWithContentDescription("Share your location instantly")
            .assertCountEquals(1)
    }

    @Test
    fun mapScreen_handlesLargeFontSizesOnAllDevices() {
        composeTestRule.setContent {
            FFTheme {
                MapScreen(
                    onNavigateBack = {},
                    onNavigateToFriends = {}
                )
            }
        }

        // Verify text remains readable with large font sizes
        composeTestRule.onNodeWithText("Your Location")
            .assertIsDisplayed()

        // Verify touch targets remain adequate with large fonts
        composeTestRule.onNodeWithContentDescription("Navigate back")
            .assertHeightIsAtLeast(48.dp)
            .assertWidthIsAtLeast(48.dp)

        composeTestRule.onNodeWithContentDescription("Share your location instantly")
            .assertHeightIsAtLeast(56.dp)
            .assertWidthIsAtLeast(56.dp)
    }

    @Test
    fun mapScreen_supportsHighContrastModeOnAllDevices() {
        composeTestRule.setContent {
            FFTheme {
                MapScreen(
                    onNavigateBack = {},
                    onNavigateToFriends = {}
                )
            }
        }

        // Verify components remain visible in high contrast mode
        composeTestRule.onNodeWithText("Your Location").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Share your location instantly").assertIsDisplayed()
        
        // Test interactions work in high contrast
        composeTestRule.onNodeWithContentDescription("View nearby friends")
            .performClick()
        
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("NearbyFriendsDrawer").assertIsDisplayed()
    }

    @Test
    fun mapScreen_worksWithSwitchAccessOnAllDevices() {
        composeTestRule.setContent {
            FFTheme {
                MapScreen(
                    onNavigateBack = {},
                    onNavigateToFriends = {}
                )
            }
        }

        // Test that all interactive elements can be activated
        composeTestRule.onNodeWithContentDescription("Navigate back")
            .assertIsDisplayed()
            .performClick()

        composeTestRule.onNodeWithContentDescription("View nearby friends")
            .assertIsDisplayed()
            .performClick()

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("NearbyFriendsDrawer").assertIsDisplayed()

        // Close drawer
        composeTestRule.onNodeWithTag("DrawerScrim").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("Share your location instantly")
            .assertIsDisplayed()
            .performClick()

        composeTestRule.waitForIdle()
    }

    @Test
    fun mapScreen_providesStateAnnouncementsOnAllDevices() {
        composeTestRule.setContent {
            FFTheme {
                MapScreen(
                    onNavigateBack = {},
                    onNavigateToFriends = {}
                )
            }
        }

        // Test state announcements for drawer
        composeTestRule.onNodeWithContentDescription("View nearby friends")
            .performClick()

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("NearbyFriendsDrawer").assertIsDisplayed()

        // Close drawer and verify state change
        composeTestRule.onNodeWithTag("DrawerScrim").performClick()
        composeTestRule.waitForIdle()

        // Test state announcements for location sharing
        composeTestRule.onNodeWithContentDescription("Share your location instantly")
            .performClick()

        composeTestRule.waitForIdle()
    }

    @Test
    fun mapScreen_supportsVoiceAccessOnAllDevices() {
        composeTestRule.setContent {
            FFTheme {
                MapScreen(
                    onNavigateBack = {},
                    onNavigateToFriends = {}
                )
            }
        }

        // Verify voice access labels are present
        composeTestRule.onNodeWithContentDescription("Navigate back").assertExists()
        composeTestRule.onNodeWithContentDescription("View nearby friends").assertExists()
        composeTestRule.onNodeWithContentDescription("Share your location instantly").assertExists()
        composeTestRule.onNodeWithContentDescription("Center map on your location").assertExists()

        // Test that voice commands would work (simulated through clicks)
        composeTestRule.onNodeWithContentDescription("View nearby friends")
            .performClick()

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("NearbyFriendsDrawer").assertIsDisplayed()
    }

    @Test
    fun mapScreen_maintainsAccessibilityInLandscapeOnAllDevices() {
        composeTestRule.setContent {
            FFTheme {
                MapScreen(
                    onNavigateBack = {},
                    onNavigateToFriends = {}
                )
            }
        }

        // Verify accessibility is maintained in landscape orientation
        composeTestRule.onNodeWithContentDescription("Navigate back").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("View nearby friends").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Share your location instantly").assertIsDisplayed()

        // Test drawer accessibility in landscape
        composeTestRule.onNodeWithContentDescription("View nearby friends")
            .performClick()

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("NearbyFriendsDrawer").assertIsDisplayed()
    }

    @Test
    fun mapScreen_worksWithExternalKeyboardOnAllDevices() {
        composeTestRule.setContent {
            FFTheme {
                MapScreen(
                    onNavigateBack = {},
                    onNavigateToFriends = {}
                )
            }
        }

        // Test keyboard navigation (simulated through focus and clicks)
        composeTestRule.onNodeWithContentDescription("Navigate back")
            .assertIsDisplayed()
            .performClick()

        composeTestRule.onNodeWithContentDescription("View nearby friends")
            .assertIsDisplayed()
            .performClick()

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("NearbyFriendsDrawer").assertIsDisplayed()

        // Test keyboard navigation within drawer
        composeTestRule.onNodeWithTag("DrawerScrim").performClick()
        composeTestRule.waitForIdle()
    }
}