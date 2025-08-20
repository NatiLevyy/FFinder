package com.locationsharing.app.ui.map.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.locationsharing.app.ui.map.MapScreenConstants
import com.locationsharing.app.ui.theme.FFinderTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

/**
 * Comprehensive test suite for QuickShareFAB component
 * Tests requirements 3.1, 3.2, 3.3, 3.4, 3.5, 8.2, 9.6
 */
@RunWith(AndroidJUnit4::class)
class QuickShareFABTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun quickShareFAB_displaysCorrectly() {
        // Given
        val mockOnClick = mock<() -> Unit>()

        // When
        composeTestRule.setContent {
            FFinderTheme {
                QuickShareFAB(
                    onClick = mockOnClick
                )
            }
        }

        // Then
        composeTestRule
            .onNodeWithContentDescription(MapScreenConstants.Accessibility.QUICK_SHARE_FAB_DESC)
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun quickShareFAB_hasCorrectAccessibilityProperties() {
        // Given
        val mockOnClick = mock<() -> Unit>()

        // When
        composeTestRule.setContent {
            FFinderTheme {
                QuickShareFAB(
                    onClick = mockOnClick
                )
            }
        }

        // Then
        composeTestRule
            .onNodeWithContentDescription(MapScreenConstants.Accessibility.QUICK_SHARE_FAB_DESC)
            .assertHasClickAction()
            .assertIsEnabled()
    }

    @Test
    fun quickShareFAB_clickTriggersCallback() {
        // Given
        val mockOnClick = mock<() -> Unit>()

        // When
        composeTestRule.setContent {
            FFinderTheme {
                QuickShareFAB(
                    onClick = mockOnClick
                )
            }
        }

        // Perform click
        composeTestRule
            .onNodeWithContentDescription(MapScreenConstants.Accessibility.QUICK_SHARE_FAB_DESC)
            .performClick()

        // Then
        verify(mockOnClick).invoke()
    }

    @Test
    fun quickShareFAB_disabledState_doesNotTriggerCallback() {
        // Given
        val mockOnClick = mock<() -> Unit>()

        // When
        composeTestRule.setContent {
            FFinderTheme {
                QuickShareFAB(
                    onClick = mockOnClick,
                    enabled = false
                )
            }
        }

        // Perform click on disabled FAB
        composeTestRule
            .onNodeWithContentDescription(MapScreenConstants.Accessibility.QUICK_SHARE_FAB_DESC)
            .performClick()

        // Then
        verifyNoInteractions(mockOnClick)
    }

    @Test
    fun quickShareFAB_activeState_hasCorrectContentDescription() {
        // Given
        val mockOnClick = mock<() -> Unit>()

        // When
        composeTestRule.setContent {
            FFinderTheme {
                QuickShareFAB(
                    onClick = mockOnClick,
                    isLocationSharingActive = true
                )
            }
        }

        // Then
        composeTestRule
            .onNodeWithContentDescription("Location sharing is active, tap to manage")
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun quickShareFAB_inactiveState_hasCorrectContentDescription() {
        // Given
        val mockOnClick = mock<() -> Unit>()

        // When
        composeTestRule.setContent {
            FFinderTheme {
                QuickShareFAB(
                    onClick = mockOnClick,
                    isLocationSharingActive = false
                )
            }
        }

        // Then
        composeTestRule
            .onNodeWithContentDescription(MapScreenConstants.Accessibility.QUICK_SHARE_FAB_DESC)
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun quickShareFAB_hasCorrectSemanticRole() {
        // Given
        val mockOnClick = mock<() -> Unit>()

        // When
        composeTestRule.setContent {
            FFinderTheme {
                QuickShareFAB(
                    onClick = mockOnClick
                )
            }
        }

        // Then - Check that it has button role (implicit through assertHasClickAction)
        composeTestRule
            .onNodeWithContentDescription(MapScreenConstants.Accessibility.QUICK_SHARE_FAB_DESC)
            .assertHasClickAction()
    }

    @Test
    fun quickShareFAB_withCustomModifier_appliesCorrectly() {
        // Given
        val mockOnClick = mock<() -> Unit>()

        // When
        composeTestRule.setContent {
            FFinderTheme {
                QuickShareFAB(
                    onClick = mockOnClick,
                    modifier = androidx.compose.ui.Modifier.testTag("custom_quick_share_fab")
                )
            }
        }

        // Then
        composeTestRule
            .onNodeWithTag("custom_quick_share_fab")
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun quickShareFAB_pressedState_maintainsAccessibility() {
        // Given
        val mockOnClick = mock<() -> Unit>()

        // When
        composeTestRule.setContent {
            FFinderTheme {
                QuickShareFAB(
                    onClick = mockOnClick,
                    isPressed = true
                )
            }
        }

        // Then
        composeTestRule
            .onNodeWithContentDescription(MapScreenConstants.Accessibility.QUICK_SHARE_FAB_DESC)
            .assertExists()
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun quickShareFAB_multipleStates_maintainsFunctionality() {
        // Given
        val mockOnClick = mock<() -> Unit>()

        // When - Test enabled + active
        composeTestRule.setContent {
            FFinderTheme {
                QuickShareFAB(
                    onClick = mockOnClick,
                    isLocationSharingActive = true,
                    enabled = true
                )
            }
        }

        // Then
        composeTestRule
            .onNodeWithContentDescription("Location sharing is active, tap to manage")
            .assertExists()
            .assertIsDisplayed()
            .performClick()

        verify(mockOnClick).invoke()
    }
}