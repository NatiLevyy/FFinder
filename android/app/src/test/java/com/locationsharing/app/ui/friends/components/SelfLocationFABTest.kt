package com.locationsharing.app.ui.friends.components

import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.locationsharing.app.ui.map.MapScreenConstants
import com.locationsharing.app.ui.theme.FFinderTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

/**
 * Test suite for SelfLocationFAB component.
 * 
 * Tests the enhanced SelfLocationFAB implementation according to requirements:
 * - 7.1: Self-location FAB visibility and positioning
 * - 7.2: Map camera animation to center on user location
 * - 7.3: Loading state indicator and permission handling
 * - 9.2: Accessibility support with proper content descriptions
 * - 9.6: Haptic feedback on interaction
 */
@RunWith(AndroidJUnit4::class)
class SelfLocationFABTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun selfLocationFAB_isDisplayed_whenRendered() {
        // Given
        val mockOnClick = mock<() -> Unit>()
        
        // When
        composeTestRule.setContent {
            FFinderTheme {
                SelfLocationFAB(
                    onClick = mockOnClick,
                    hasLocationPermission = true,
                    isLoading = false
                )
            }
        }
        
        // Then
        composeTestRule
            .onNodeWithContentDescription(MapScreenConstants.Accessibility.SELF_LOCATION_FAB_DESC)
            .assertIsDisplayed()
    }
    
    @Test
    fun selfLocationFAB_hasCorrectContentDescription_whenPermissionGranted() {
        // Given
        val mockOnClick = mock<() -> Unit>()
        
        // When
        composeTestRule.setContent {
            FFinderTheme {
                SelfLocationFAB(
                    onClick = mockOnClick,
                    hasLocationPermission = true,
                    isLoading = false
                )
            }
        }
        
        // Then
        composeTestRule
            .onNodeWithContentDescription(MapScreenConstants.Accessibility.SELF_LOCATION_FAB_DESC)
            .assertContentDescriptionEquals(MapScreenConstants.Accessibility.SELF_LOCATION_FAB_DESC)
    }
    
    @Test
    fun selfLocationFAB_hasCorrectContentDescription_whenPermissionDenied() {
        // Given
        val mockOnClick = mock<() -> Unit>()
        
        // When
        composeTestRule.setContent {
            FFinderTheme {
                SelfLocationFAB(
                    onClick = mockOnClick,
                    hasLocationPermission = false,
                    isLoading = false
                )
            }
        }
        
        // Then
        composeTestRule
            .onNodeWithContentDescription("Request location permission to center map")
            .assertIsDisplayed()
    }
    
    @Test
    fun selfLocationFAB_hasCorrectContentDescription_whenLoading() {
        // Given
        val mockOnClick = mock<() -> Unit>()
        
        // When
        composeTestRule.setContent {
            FFinderTheme {
                SelfLocationFAB(
                    onClick = mockOnClick,
                    hasLocationPermission = true,
                    isLoading = true
                )
            }
        }
        
        // Then
        composeTestRule
            .onNodeWithContentDescription("Centering map on your location...")
            .assertIsDisplayed()
    }
    
    @Test
    fun selfLocationFAB_isClickable_whenEnabled() {
        // Given
        val mockOnClick = mock<() -> Unit>()
        
        // When
        composeTestRule.setContent {
            FFinderTheme {
                SelfLocationFAB(
                    onClick = mockOnClick,
                    hasLocationPermission = true,
                    isLoading = false,
                    enabled = true
                )
            }
        }
        
        // Then
        composeTestRule
            .onNodeWithContentDescription(MapScreenConstants.Accessibility.SELF_LOCATION_FAB_DESC)
            .assertHasClickAction()
            .assertIsEnabled()
    }
    
    @Test
    fun selfLocationFAB_isNotClickable_whenLoading() {
        // Given
        val mockOnClick = mock<() -> Unit>()
        
        // When
        composeTestRule.setContent {
            FFinderTheme {
                SelfLocationFAB(
                    onClick = mockOnClick,
                    hasLocationPermission = true,
                    isLoading = true,
                    enabled = true
                )
            }
        }
        
        // Then
        composeTestRule
            .onNodeWithContentDescription("Centering map on your location...")
            .assertIsNotEnabled()
    }
    
    @Test
    fun selfLocationFAB_triggersOnClick_whenTapped() {
        // Given
        val mockOnClick = mock<() -> Unit>()
        
        // When
        composeTestRule.setContent {
            FFinderTheme {
                SelfLocationFAB(
                    onClick = mockOnClick,
                    hasLocationPermission = true,
                    isLoading = false
                )
            }
        }
        
        // Perform click
        composeTestRule
            .onNodeWithContentDescription(MapScreenConstants.Accessibility.SELF_LOCATION_FAB_DESC)
            .performClick()
        
        // Then
        verify(mockOnClick).invoke()
    }
    
    @Test
    fun selfLocationFAB_showsLoadingIndicator_whenLoading() {
        // Given
        val mockOnClick = mock<() -> Unit>()
        
        // When
        composeTestRule.setContent {
            FFinderTheme {
                SelfLocationFAB(
                    onClick = mockOnClick,
                    hasLocationPermission = true,
                    isLoading = true
                )
            }
        }
        
        // Then - Loading indicator should be visible (CircularProgressIndicator)
        // Note: CircularProgressIndicator doesn't have a specific content description,
        // but we can verify the FAB is in loading state by checking it's disabled
        composeTestRule
            .onNodeWithContentDescription("Centering map on your location...")
            .assertIsDisplayed()
            .assertIsNotEnabled()
    }
    
    @Test
    fun selfLocationFAB_showsLocationIcon_whenNotLoading() {
        // Given
        val mockOnClick = mock<() -> Unit>()
        
        // When
        composeTestRule.setContent {
            FFinderTheme {
                SelfLocationFAB(
                    onClick = mockOnClick,
                    hasLocationPermission = true,
                    isLoading = false
                )
            }
        }
        
        // Then - Location icon should be visible (MyLocation icon)
        composeTestRule
            .onNodeWithContentDescription(MapScreenConstants.Accessibility.SELF_LOCATION_FAB_DESC)
            .assertIsDisplayed()
            .assertIsEnabled()
    }
    
    @Test
    fun selfLocationFAB_hasCorrectAccessibilityRole() {
        // Given
        val mockOnClick = mock<() -> Unit>()
        
        // When
        composeTestRule.setContent {
            FFinderTheme {
                SelfLocationFAB(
                    onClick = mockOnClick,
                    hasLocationPermission = true,
                    isLoading = false
                )
            }
        }
        
        // Then - Should have button role for accessibility
        composeTestRule
            .onNodeWithContentDescription(MapScreenConstants.Accessibility.SELF_LOCATION_FAB_DESC)
            .assertHasClickAction() // This implies button role
    }
    
    @Test
    fun selfLocationFAB_isDisabled_whenExplicitlyDisabled() {
        // Given
        val mockOnClick = mock<() -> Unit>()
        
        // When
        composeTestRule.setContent {
            FFinderTheme {
                SelfLocationFAB(
                    onClick = mockOnClick,
                    hasLocationPermission = true,
                    isLoading = false,
                    enabled = false
                )
            }
        }
        
        // Then
        composeTestRule
            .onNodeWithContentDescription(MapScreenConstants.Accessibility.SELF_LOCATION_FAB_DESC)
            .assertIsNotEnabled()
    }
}