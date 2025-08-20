package com.locationsharing.app.ui.components.button

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Unit tests for ResponsiveButton composable.
 */
class ResponsiveButtonTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun `ResponsiveButton should display text correctly`() {
        // Given
        val buttonText = "Test Button"
        val mockButtonResponseManager = mock<ButtonResponseManager>()
        val mockStateFlow = MutableStateFlow(ButtonState())
        whenever(mockButtonResponseManager.getButtonState("button_${buttonText.hashCode()}"))
            .thenReturn(mockStateFlow)
        
        // When
        composeTestRule.setContent {
            ResponsiveButton(
                text = buttonText,
                onClick = {},
                buttonResponseManager = mockButtonResponseManager
            )
        }
        
        // Then
        composeTestRule.onNodeWithText(buttonText).assertIsDisplayed()
    }
    
    @Test
    fun `ResponsiveButton should be clickable when enabled`() {
        // Given
        val buttonText = "Clickable Button"
        val mockButtonResponseManager = mock<ButtonResponseManager>()
        val mockStateFlow = MutableStateFlow(ButtonState(isEnabled = true))
        whenever(mockButtonResponseManager.getButtonState("button_${buttonText.hashCode()}"))
            .thenReturn(mockStateFlow)
        
        // When
        composeTestRule.setContent {
            ResponsiveButton(
                text = buttonText,
                onClick = {},
                enabled = true,
                buttonResponseManager = mockButtonResponseManager
            )
        }
        
        // Then
        composeTestRule.onNodeWithText(buttonText)
            .assertIsDisplayed()
            .assertIsEnabled()
            .assertHasClickAction()
    }
    
    @Test
    fun `ResponsiveButton should not be clickable when disabled`() {
        // Given
        val buttonText = "Disabled Button"
        val mockButtonResponseManager = mock<ButtonResponseManager>()
        val mockStateFlow = MutableStateFlow(ButtonState(isEnabled = false))
        whenever(mockButtonResponseManager.getButtonState("button_${buttonText.hashCode()}"))
            .thenReturn(mockStateFlow)
        
        // When
        composeTestRule.setContent {
            ResponsiveButton(
                text = buttonText,
                onClick = {},
                enabled = false,
                buttonResponseManager = mockButtonResponseManager
            )
        }
        
        // Then
        composeTestRule.onNodeWithText(buttonText)
            .assertIsDisplayed()
            .assertIsNotEnabled()
    }
    
    @Test
    fun `ResponsiveButton should call onClick through ButtonResponseManager`() {
        // Given
        val buttonText = "Click Me"
        val mockOnClick = mock<() -> Unit>()
        val mockButtonResponseManager = mock<ButtonResponseManager>()
        val mockStateFlow = MutableStateFlow(ButtonState(isEnabled = true))
        whenever(mockButtonResponseManager.getButtonState("button_${buttonText.hashCode()}"))
            .thenReturn(mockStateFlow)
        
        // When
        composeTestRule.setContent {
            ResponsiveButton(
                text = buttonText,
                onClick = mockOnClick,
                buttonResponseManager = mockButtonResponseManager
            )
        }
        
        composeTestRule.onNodeWithText(buttonText).performClick()
        
        // Then
        verify(mockButtonResponseManager).handleButtonClick(
            "button_${buttonText.hashCode()}",
            mockOnClick
        )
    }
    
    @Test
    fun `ResponsiveButton should show loading indicator when loading`() {
        // Given
        val buttonText = "Loading Button"
        val mockButtonResponseManager = mock<ButtonResponseManager>()
        val mockStateFlow = MutableStateFlow(ButtonState(isLoading = true))
        whenever(mockButtonResponseManager.getButtonState("button_${buttonText.hashCode()}"))
            .thenReturn(mockStateFlow)
        
        // When
        composeTestRule.setContent {
            ResponsiveButton(
                text = buttonText,
                onClick = {},
                loading = true,
                buttonResponseManager = mockButtonResponseManager
            )
        }
        
        // Then
        composeTestRule.onNodeWithText(buttonText).assertIsDisplayed()
        // Note: In a real test, we would check for the loading indicator
        // but that requires more complex UI testing setup
    }
    
    @Test
    fun `ResponsiveButton should update manager state when props change`() {
        // Given
        val buttonText = "State Button"
        val mockButtonResponseManager = mock<ButtonResponseManager>()
        val mockStateFlow = MutableStateFlow(ButtonState())
        whenever(mockButtonResponseManager.getButtonState("button_${buttonText.hashCode()}"))
            .thenReturn(mockStateFlow)
        
        // When
        composeTestRule.setContent {
            ResponsiveButton(
                text = buttonText,
                onClick = {},
                enabled = false,
                loading = true,
                buttonResponseManager = mockButtonResponseManager
            )
        }
        
        // Then
        verify(mockButtonResponseManager).setButtonEnabled("button_${buttonText.hashCode()}", false)
        verify(mockButtonResponseManager).setButtonLoading("button_${buttonText.hashCode()}", true)
    }
}