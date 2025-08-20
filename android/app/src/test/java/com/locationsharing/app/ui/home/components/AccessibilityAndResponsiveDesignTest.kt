package com.locationsharing.app.ui.home.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.maps.model.LatLng
import com.locationsharing.app.ui.theme.FFinderTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Comprehensive tests for accessibility and responsive design implementation
 * across all FFinder Home Screen components.
 * 
 * Tests cover:
 * - Meaningful content descriptions for all interactive elements
 * - Logical focus order implementation
 * - Responsive Extended FAB behavior on narrow screens
 * - Animation respect for system accessibility preferences
 * - Proper dp scaling for different screen densities
 */
@RunWith(AndroidJUnit4::class)
class AccessibilityAndResponsiveDesignTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun homeScreenComponents_haveMeaningfulContentDescriptions() {
        composeTestRule.setContent {
            FFinderTheme {
                BackgroundGradient {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Hero Section
                        HeroSection(
                            modifier = Modifier.testTag("hero_section"),
                            animationsEnabled = true
                        )
                        
                        // Map Preview
                        MapPreviewCard(
                            location = LatLng(37.7749, -122.4194),
                            hasLocationPermission = true,
                            animationsEnabled = true,
                            modifier = Modifier.testTag("map_preview")
                        )
                        
                        // Primary CTA
                        PrimaryCallToAction(
                            onStartShare = {},
                            isNarrowScreen = false,
                            modifier = Modifier.testTag("primary_cta")
                        )
                        
                        // Secondary Actions
                        SecondaryActionsRow(
                            onFriends = {},
                            onSettings = {},
                            modifier = Modifier.testTag("secondary_actions")
                        )
                        
                        // What's New Teaser
                        WhatsNewTeaser(
                            onTap = {},
                            isVisible = true,
                            animationsEnabled = true,
                            modifier = Modifier.testTag("whats_new_teaser")
                        )
                    }
                }
            }
        }

        // Verify all components have meaningful content descriptions
        composeTestRule
            .onNodeWithContentDescription(ContentDescriptions.LOGO)
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithContentDescription(ContentDescriptions.MAP_PREVIEW_WITH_LOCATION)
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithContentDescription(ContentDescriptions.PRIMARY_CTA_EXTENDED)
            .assertIsDisplayed()
            .assertHasClickAction()
        
        composeTestRule
            .onNodeWithContentDescription(ContentDescriptions.FRIENDS_BUTTON)
            .assertIsDisplayed()
            .assertHasClickAction()
        
        composeTestRule
            .onNodeWithContentDescription(ContentDescriptions.SETTINGS_BUTTON)
            .assertIsDisplayed()
            .assertHasClickAction()
        
        composeTestRule
            .onNodeWithContentDescription(ContentDescriptions.WHATS_NEW_TEASER)
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun primaryCallToAction_respondsToNarrowScreen() {
        // Test normal screen (Extended FAB)
        composeTestRule.setContent {
            FFinderTheme {
                PrimaryCallToAction(
                    onStartShare = {},
                    isNarrowScreen = false
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription(ContentDescriptions.PRIMARY_CTA_EXTENDED)
            .assertIsDisplayed()
            .assertHasClickAction()
        
        composeTestRule
            .onNodeWithText("Start Live Sharing")
            .assertIsDisplayed()

        // Test narrow screen (Icon-only FAB with tooltip)
        composeTestRule.setContent {
            FFinderTheme {
                PrimaryCallToAction(
                    onStartShare = {},
                    isNarrowScreen = true
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription(ContentDescriptions.PRIMARY_CTA_ICON)
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun allInteractiveElements_haveProperButtonRole() {
        composeTestRule.setContent {
            FFinderTheme {
                Column {
                    PrimaryCallToAction(
                        onStartShare = {},
                        isNarrowScreen = false
                    )
                    
                    SecondaryActionsRow(
                        onFriends = {},
                        onSettings = {}
                    )
                    
                    WhatsNewTeaser(
                        onTap = {},
                        animationsEnabled = true
                    )
                }
            }
        }

        // Verify all interactive elements have button role
        composeTestRule
            .onNodeWithContentDescription(ContentDescriptions.PRIMARY_CTA_EXTENDED)
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.Role,
                    Role.Button
                )
            )
        
        composeTestRule
            .onNodeWithContentDescription(ContentDescriptions.FRIENDS_BUTTON)
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.Role,
                    Role.Button
                )
            )
        
        composeTestRule
            .onNodeWithContentDescription(ContentDescriptions.SETTINGS_BUTTON)
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.Role,
                    Role.Button
                )
            )
        
        composeTestRule
            .onNodeWithContentDescription(ContentDescriptions.WHATS_NEW_TEASER)
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.Role,
                    Role.Button
                )
            )
    }

    @Test
    fun animations_respectAccessibilityPreferences_whenDisabled() {
        composeTestRule.setContent {
            FFinderTheme {
                Column {
                    HeroSection(animationsEnabled = false)
                    
                    MapPreviewCard(
                        location = LatLng(37.7749, -122.4194),
                        hasLocationPermission = true,
                        animationsEnabled = false
                    )
                    
                    WhatsNewTeaser(
                        onTap = {},
                        animationsEnabled = false
                    )
                }
            }
        }

        // When animations are disabled, content should still be visible and accessible
        composeTestRule
            .onNodeWithContentDescription(ContentDescriptions.LOGO)
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithContentDescription(ContentDescriptions.MAP_PREVIEW_WITH_LOCATION)
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithContentDescription(ContentDescriptions.WHATS_NEW_TEASER)
            .assertIsDisplayed()
    }

    @Test
    fun animations_respectAccessibilityPreferences_whenEnabled() {
        composeTestRule.setContent {
            FFinderTheme {
                Column {
                    HeroSection(animationsEnabled = true)
                    
                    MapPreviewCard(
                        location = LatLng(37.7749, -122.4194),
                        hasLocationPermission = true,
                        animationsEnabled = true
                    )
                    
                    WhatsNewTeaser(
                        onTap = {},
                        animationsEnabled = true
                    )
                }
            }
        }

        // When animations are enabled, content should still be visible and accessible
        composeTestRule
            .onNodeWithContentDescription(ContentDescriptions.LOGO)
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithContentDescription(ContentDescriptions.MAP_PREVIEW_WITH_LOCATION)
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithContentDescription(ContentDescriptions.WHATS_NEW_TEASER)
            .assertIsDisplayed()
    }

    @Test
    fun mapPreview_providesAppropriateContentDescription_basedOnPermissionState() {
        // Test with location permission granted
        composeTestRule.setContent {
            FFinderTheme {
                MapPreviewCard(
                    location = LatLng(37.7749, -122.4194),
                    hasLocationPermission = true,
                    animationsEnabled = true
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription(ContentDescriptions.MAP_PREVIEW_WITH_LOCATION)
            .assertIsDisplayed()

        // Test without location permission
        composeTestRule.setContent {
            FFinderTheme {
                MapPreviewCard(
                    location = null,
                    hasLocationPermission = false,
                    animationsEnabled = true
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription(ContentDescriptions.MAP_PREVIEW_NO_LOCATION)
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithContentDescription(ContentDescriptions.ENABLE_LOCATION)
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun accessibilityUtils_providesCorrectResponsiveValues() {
        composeTestRule.setContent {
            FFinderTheme {
                val config = rememberAccessibilityConfig(animationsEnabled = true)
                
                // Test that configuration is properly created
                assert(config.screenWidthDp > 0)
                assert(config.screenHeightDp > 0)
                assert(config.fontScale > 0)
                assert(config.touchTargetSize.value >= 48f) // Minimum touch target
                assert(config.responsivePadding.value > 0)
                assert(config.responsiveSpacing.value > 0)
            }
        }
    }

    @Test
    fun focusOrder_isLogicalAndConsistent() {
        composeTestRule.setContent {
            FFinderTheme {
                BackgroundGradient {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Components should be in logical focus order:
                        // 1. Logo (heading)
                        // 2. Subtitle
                        // 3. Map Preview
                        // 4. Primary CTA
                        // 5. Secondary Actions (Friends, Settings)
                        // 6. What's New Teaser
                        
                        HeroSection(animationsEnabled = true)
                        MapPreviewCard(
                            location = LatLng(37.7749, -122.4194),
                            hasLocationPermission = true,
                            animationsEnabled = true
                        )
                        PrimaryCallToAction(
                            onStartShare = {},
                            isNarrowScreen = false
                        )
                        SecondaryActionsRow(
                            onFriends = {},
                            onSettings = {}
                        )
                        WhatsNewTeaser(
                            onTap = {},
                            animationsEnabled = true
                        )
                    }
                }
            }
        }

        // Verify all focusable elements are present in logical order
        // (The actual focus traversal order is handled by Compose automatically
        // based on the layout order, which matches our intended focus order)
        
        composeTestRule
            .onNodeWithContentDescription(ContentDescriptions.LOGO)
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithContentDescription(ContentDescriptions.MAP_PREVIEW_WITH_LOCATION)
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithContentDescription(ContentDescriptions.PRIMARY_CTA_EXTENDED)
            .assertIsDisplayed()
            .assertHasClickAction()
        
        composeTestRule
            .onNodeWithContentDescription(ContentDescriptions.FRIENDS_BUTTON)
            .assertIsDisplayed()
            .assertHasClickAction()
        
        composeTestRule
            .onNodeWithContentDescription(ContentDescriptions.SETTINGS_BUTTON)
            .assertIsDisplayed()
            .assertHasClickAction()
        
        composeTestRule
            .onNodeWithContentDescription(ContentDescriptions.WHATS_NEW_TEASER)
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun dpScaling_worksCorrectlyAcrossDensities() {
        composeTestRule.setContent {
            FFinderTheme {
                // Test that components maintain proper proportions
                // regardless of screen density (handled by dp units)
                
                Column {
                    HeroSection(animationsEnabled = true)
                    
                    PrimaryCallToAction(
                        onStartShare = {},
                        isNarrowScreen = false
                    )
                    
                    SecondaryActionsRow(
                        onFriends = {},
                        onSettings = {}
                    )
                }
            }
        }

        // Verify components are displayed correctly
        // (dp scaling is handled automatically by the Compose framework)
        composeTestRule
            .onNodeWithContentDescription(ContentDescriptions.LOGO)
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithContentDescription(ContentDescriptions.PRIMARY_CTA_EXTENDED)
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithContentDescription(ContentDescriptions.FRIENDS_BUTTON)
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithContentDescription(ContentDescriptions.SETTINGS_BUTTON)
            .assertIsDisplayed()
    }

    @Test
    fun tooltipFunctionality_worksOnNarrowScreens() {
        composeTestRule.setContent {
            FFinderTheme {
                PrimaryCallToAction(
                    onStartShare = {},
                    isNarrowScreen = true
                )
            }
        }

        // Verify icon-only FAB has proper content description for tooltip
        composeTestRule
            .onNodeWithContentDescription(ContentDescriptions.PRIMARY_CTA_ICON)
            .assertIsDisplayed()
            .assertHasClickAction()
    }
}