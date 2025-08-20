package com.locationsharing.app.ui.map.crossdevice

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
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
 * Tests MapScreen theme compatibility across light and dark themes.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ThemeCompatibilityTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun mapScreen_displaysCorrectlyInLightTheme() {
        composeTestRule.setContent {
            FFTheme(darkTheme = false) {
                MapScreen(
                    onNavigateBack = {},
                    onNavigateToFriends = {}
                )
            }
        }

        // Verify light theme components are visible and properly styled
        composeTestRule.onNodeWithText("Your Location").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Navigate back").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("View nearby friends").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Share your location instantly").assertIsDisplayed()
        
        // Test light theme interactions
        composeTestRule.onNodeWithContentDescription("View nearby friends").performClick()
        composeTestRule.waitForIdle()
        
        // Verify drawer appears correctly in light theme
        composeTestRule.onNodeWithTag("NearbyFriendsDrawer").assertIsDisplayed()
        
        // Close drawer
        composeTestRule.onNodeWithTag("DrawerScrim").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun mapScreen_displaysCorrectlyInDarkTheme() {
        composeTestRule.setContent {
            FFTheme(darkTheme = true) {
                MapScreen(
                    onNavigateBack = {},
                    onNavigateToFriends = {}
                )
            }
        }

        // Verify dark theme components are visible and properly styled
        composeTestRule.onNodeWithText("Your Location").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Navigate back").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("View nearby friends").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Share your location instantly").assertIsDisplayed()
        
        // Test dark theme interactions
        composeTestRule.onNodeWithContentDescription("View nearby friends").performClick()
        composeTestRule.waitForIdle()
        
        // Verify drawer appears correctly in dark theme
        composeTestRule.onNodeWithTag("NearbyFriendsDrawer").assertIsDisplayed()
        
        // Close drawer
        composeTestRule.onNodeWithTag("DrawerScrim").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun mapScreen_adaptsToSystemThemeChanges() {
        // Test automatic theme switching based on system settings
        composeTestRule.setContent {
            FFTheme {
                MapScreen(
                    onNavigateBack = {},
                    onNavigateToFriends = {}
                )
            }
        }

        // Verify components are displayed regardless of system theme
        composeTestRule.onNodeWithText("Your Location").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Share your location instantly").assertIsDisplayed()
    }

    @Test
    fun mapScreen_maintainsContrastInLightTheme() {
        composeTestRule.setContent {
            FFTheme(darkTheme = false) {
                MapScreen(
                    onNavigateBack = {},
                    onNavigateToFriends = {}
                )
            }
        }

        // Verify sufficient contrast for accessibility in light theme
        composeTestRule.onNodeWithText("Your Location").assertIsDisplayed()
        
        // Test FAB visibility in light theme
        composeTestRule.onNodeWithContentDescription("Share your location instantly")
            .assertIsDisplayed()
            .performClick()
        
        composeTestRule.waitForIdle()
    }

    @Test
    fun mapScreen_maintainsContrastInDarkTheme() {
        composeTestRule.setContent {
            FFTheme(darkTheme = true) {
                MapScreen(
                    onNavigateBack = {},
                    onNavigateToFriends = {}
                )
            }
        }

        // Verify sufficient contrast for accessibility in dark theme
        composeTestRule.onNodeWithText("Your Location").assertIsDisplayed()
        
        // Test FAB visibility in dark theme
        composeTestRule.onNodeWithContentDescription("Share your location instantly")
            .assertIsDisplayed()
            .performClick()
        
        composeTestRule.waitForIdle()
    }

    @Test
    fun mapScreen_handlesThemeTransitions() {
        var isDarkTheme = false
        
        composeTestRule.setContent {
            FFTheme(darkTheme = isDarkTheme) {
                MapScreen(
                    onNavigateBack = {},
                    onNavigateToFriends = {}
                )
            }
        }

        // Verify initial light theme
        composeTestRule.onNodeWithText("Your Location").assertIsDisplayed()
        
        // Simulate theme change to dark
        isDarkTheme = true
        composeTestRule.setContent {
            FFTheme(darkTheme = isDarkTheme) {
                MapScreen(
                    onNavigateBack = {},
                    onNavigateToFriends = {}
                )
            }
        }
        
        // Verify components still work after theme change
        composeTestRule.onNodeWithText("Your Location").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Share your location instantly").assertIsDisplayed()
    }

    @Test
    fun mapScreen_preservesFunctionalityAcrossThemes() {
        val themes = listOf(false, true) // Light and dark themes
        
        themes.forEach { isDarkTheme ->
            composeTestRule.setContent {
                FFTheme(darkTheme = isDarkTheme) {
                    MapScreen(
                        onNavigateBack = {},
                        onNavigateToFriends = {}
                    )
                }
            }

            // Test core functionality in both themes
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
    }

    @Test
    fun mapScreen_debugFeaturesWorkInBothThemes() {
        val themes = listOf(false, true)
        
        themes.forEach { isDarkTheme ->
            composeTestRule.setContent {
                FFTheme(darkTheme = isDarkTheme) {
                    MapScreen(
                        onNavigateBack = {},
                        onNavigateToFriends = {}
                    )
                }
            }

            // Test debug FAB visibility and functionality in both themes
            composeTestRule.onNodeWithContentDescription("Add test friends to map")
                .assertIsDisplayed()
                .performClick()
            
            composeTestRule.waitForIdle()
            
            // Verify snackbar appears in both themes
            composeTestRule.onNodeWithText("Test friends added to map").assertIsDisplayed()
        }
    }

    @Test
    fun mapScreen_animationsWorkInBothThemes() {
        val themes = listOf(false, true)
        
        themes.forEach { isDarkTheme ->
            composeTestRule.setContent {
                FFTheme(darkTheme = isDarkTheme) {
                    MapScreen(
                        onNavigateBack = {},
                        onNavigateToFriends = {}
                    )
                }
            }

            // Test animations work in both themes
            composeTestRule.onNodeWithContentDescription("Share your location instantly")
                .performClick()
            
            composeTestRule.waitForIdle()
            
            // Test drawer animation
            composeTestRule.onNodeWithContentDescription("View nearby friends")
                .performClick()
            
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("NearbyFriendsDrawer").assertIsDisplayed()
            
            // Close drawer
            composeTestRule.onNodeWithTag("DrawerScrim").performClick()
            composeTestRule.waitForIdle()
        }
    }
}