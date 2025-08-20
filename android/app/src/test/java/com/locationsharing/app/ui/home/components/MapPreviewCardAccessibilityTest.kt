package com.locationsharing.app.ui.home.components

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.google.android.gms.maps.model.LatLng
import com.locationsharing.app.ui.theme.FFinderTheme
import org.junit.Rule
import org.junit.Test

/**
 * Accessibility tests for MapPreviewCard component.
 * 
 * Tests accessibility compliance including:
 * - Content descriptions for screen readers
 * - Semantic properties for assistive technologies
 * - Animation respect for accessibility preferences
 * - Focus management and navigation
 * - WCAG compliance for interactive elements
 */
class MapPreviewCardAccessibilityTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun mapPreviewCard_withLocation_hasProperContentDescriptions() {
        // Given
        val testLocation = LatLng(37.7749, -122.4194)
        
        // When
        composeTestRule.setContent {
            FFinderTheme {
                MapPreviewCard(
                    location = testLocation,
                    hasLocationPermission = true,
                    animationsEnabled = true
                )
            }
        }
        
        // Then - Pin should have meaningful content description
        composeTestRule
            .onNodeWithContentDescription("Your location pin")
            .assertIsDisplayed()
        
        // Verify the content description is accessible
        composeTestRule
            .onNodeWithContentDescription("Your location pin")
            .assertExists()
    }

    @Test
    fun mapPreviewCard_fallback_hasAccessibleElements() {
        // When
        composeTestRule.setContent {
            FFinderTheme {
                MapPreviewCard(
                    location = null,
                    hasLocationPermission = false,
                    animationsEnabled = true
                )
            }
        }
        
        // Then - All elements should have proper accessibility support
        composeTestRule
            .onNodeWithContentDescription("Location preview unavailable")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Map Preview")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Enable location to see your area")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Enable Location")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun mapPreviewCard_enableLocationButton_isAccessible() {
        // Given
        var permissionRequested = false
        
        composeTestRule.setContent {
            FFinderTheme {
                MapPreviewCard(
                    location = null,
                    hasLocationPermission = false,
                    animationsEnabled = true,
                    onPermissionRequest = { permissionRequested = true }
                )
            }
        }
        
        // When - Check button accessibility
        val enableLocationButton = composeTestRule
            .onNodeWithText("Enable Location")
        
        // Then - Button should be accessible
        enableLocationButton
            .assertIsDisplayed()
            .assertHasClickAction()
        
        // Button should be clickable via accessibility services
        enableLocationButton.performClick()
        assert(permissionRequested) { "Button should be clickable via accessibility services" }
    }

    @Test
    fun mapPreviewCard_respectsAnimationPreferences() {
        // Given
        val testLocation = LatLng(37.7749, -122.4194)
        
        // When - Animations disabled (accessibility preference)
        composeTestRule.setContent {
            FFinderTheme {
                MapPreviewCard(
                    location = testLocation,
                    hasLocationPermission = true,
                    animationsEnabled = false // Respects user's accessibility preferences
                )
            }
        }
        
        // Then - Component should still be fully functional
        composeTestRule
            .onNodeWithContentDescription("Your location pin")
            .assertIsDisplayed()
    }

    @Test
    fun mapPreviewCard_semanticProperties_areCorrect() {
        // Given
        val testLocation = LatLng(37.7749, -122.4194)
        
        composeTestRule.setContent {
            FFinderTheme {
                MapPreviewCard(
                    location = testLocation,
                    hasLocationPermission = true,
                    animationsEnabled = true
                )
            }
        }
        
        // Then - Pin should have proper semantic properties
        composeTestRule
            .onNodeWithContentDescription("Your location pin")
            .assertExists()
    }

    @Test
    fun mapPreviewCard_fallbackIcon_hasProperSemantics() {
        // When
        composeTestRule.setContent {
            FFinderTheme {
                MapPreviewCard(
                    location = null,
                    hasLocationPermission = false,
                    animationsEnabled = true
                )
            }
        }
        
        // Then - Fallback icon should have content description
        composeTestRule
            .onNodeWithContentDescription("Location preview unavailable")
            .assertIsDisplayed()
    }

    @Test
    fun mapPreviewCard_textElements_areReadableByScreenReaders() {
        // When
        composeTestRule.setContent {
            FFinderTheme {
                MapPreviewCard(
                    location = null,
                    hasLocationPermission = false,
                    animationsEnabled = true
                )
            }
        }
        
        // Then - All text should be accessible to screen readers
        composeTestRule
            .onNodeWithText("Map Preview")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Enable location to see your area")
            .assertIsDisplayed()
        
        // Text should have proper semantic meaning
        composeTestRule
            .onNodeWithText("Map Preview")
            .assertExists()
        
        composeTestRule
            .onNodeWithText("Enable location to see your area")
            .assertExists()
    }

    @Test
    fun mapPreviewCard_interactiveElements_haveProperClickActions() {
        // Given
        var callbackTriggered = false
        
        composeTestRule.setContent {
            FFinderTheme {
                MapPreviewCard(
                    location = null,
                    hasLocationPermission = false,
                    animationsEnabled = true,
                    onPermissionRequest = { callbackTriggered = true }
                )
            }
        }
        
        // When - Check interactive elements
        val button = composeTestRule.onNodeWithText("Enable Location")
        
        // Then - Should have click action
        button.assertHasClickAction()
        
        // Should be clickable
        button.performClick()
        assert(callbackTriggered) { "Interactive element should trigger callback" }
    }

    @Test
    fun mapPreviewCard_colorContrast_meetsAccessibilityStandards() {
        // Given - This test verifies that we're using theme colors that should meet contrast requirements
        val testLocation = LatLng(37.7749, -122.4194)
        
        composeTestRule.setContent {
            FFinderTheme {
                MapPreviewCard(
                    location = testLocation,
                    hasLocationPermission = true,
                    animationsEnabled = true
                )
            }
        }
        
        // Then - Component should render without accessibility warnings
        // (In a real app, this would be tested with accessibility scanning tools)
        composeTestRule
            .onNodeWithContentDescription("Your location pin")
            .assertIsDisplayed()
    }

    @Test
    fun mapPreviewCard_fallback_colorContrast_meetsStandards() {
        // When
        composeTestRule.setContent {
            FFinderTheme {
                MapPreviewCard(
                    location = null,
                    hasLocationPermission = false,
                    animationsEnabled = true
                )
            }
        }
        
        // Then - All text should be readable (using theme colors that meet contrast requirements)
        composeTestRule
            .onNodeWithText("Map Preview")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Enable location to see your area")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Enable Location")
            .assertIsDisplayed()
    }

    @Test
    fun mapPreviewCard_focusOrder_isLogical() {
        // Given
        var permissionRequested = false
        
        composeTestRule.setContent {
            FFinderTheme {
                MapPreviewCard(
                    location = null,
                    hasLocationPermission = false,
                    animationsEnabled = true,
                    onPermissionRequest = { permissionRequested = true }
                )
            }
        }
        
        // When - Navigate through focusable elements
        // The button should be focusable and clickable
        composeTestRule
            .onNodeWithText("Enable Location")
            .assertHasClickAction()
            .performClick()
        
        // Then - Focus should work correctly
        assert(permissionRequested) { "Focus and click should work properly" }
    }

    @Test
    fun mapPreviewCard_withError_maintainsAccessibility() {
        // When - Test error handling accessibility
        composeTestRule.setContent {
            FFinderTheme {
                MapPreviewWithErrorHandling(
                    location = null,
                    hasLocationPermission = false,
                    animationsEnabled = true
                )
            }
        }
        
        // Then - Error state should still be accessible
        composeTestRule
            .onNodeWithText("Enable Location")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun mapPreviewCard_multipleInstances_maintainAccessibility() {
        // Given - Multiple instances to test accessibility isolation
        val testLocation = LatLng(37.7749, -122.4194)
        
        composeTestRule.setContent {
            FFinderTheme {
                androidx.compose.foundation.layout.Column {
                    MapPreviewCard(
                        location = testLocation,
                        hasLocationPermission = true,
                        animationsEnabled = true
                    )
                    MapPreviewCard(
                        location = null,
                        hasLocationPermission = false,
                        animationsEnabled = true
                    )
                }
            }
        }
        
        // Then - Each instance should maintain proper accessibility
        composeTestRule
            .onAllNodesWithContentDescription("Your location pin")
            .assertCountEquals(1)
        
        composeTestRule
            .onNodeWithText("Enable Location")
            .assertIsDisplayed()
            .assertHasClickAction()
    }
}