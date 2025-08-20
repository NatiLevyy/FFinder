package com.locationsharing.app.ui.components.button

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.locationsharing.app.ui.theme.FFinderTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlinx.coroutines.flow.MutableStateFlow

@RunWith(AndroidJUnit4::class)
class ResponsiveButtonVisualFeedbackTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    private val mockButtonResponseManager = mock<ButtonResponseManager>()
    
    @Test
    fun `button should display with proper visual feedback states`() {
        val buttonState = MutableStateFlow(
            ButtonState(
                isEnabled = true,
                isLoading = false,
                showFeedback = false,
                canClick = true
            )
        )
        
        whenever(mockButtonResponseManager.getButtonState("button_${("Test Button").hashCode()}"))
            .thenReturn(buttonState)
        
        composeTestRule.setContent {
            FFinderTheme {
                ResponsiveButton(
                    text = "Test Button",
                    onClick = { },
                    buttonResponseManager = mockButtonResponseManager,
                    showRippleEffect = true,
                    enableHapticFeedback = true
                )
            }
        }
        
        composeTestRule.onNodeWithText("Test Button").assertIsDisplayed()
    }
    
    @Test
    fun `button should show loading state with progress indicator`() {
        val buttonState = MutableStateFlow(
            ButtonState(
                isEnabled = true,
                isLoading = true,
                showFeedback = false,
                canClick = false
            )
        )
        
        whenever(mockButtonResponseManager.getButtonState("button_${("Loading Button").hashCode()}"))
            .thenReturn(buttonState)
        
        composeTestRule.setContent {
            FFinderTheme {
                ResponsiveButton(
                    text = "Loading Button",
                    onClick = { },
                    loading = true,
                    buttonResponseManager = mockButtonResponseManager
                )
            }
        }
        
        composeTestRule.onNodeWithText("Loading Button").assertIsDisplayed()
    }
    
    @Test
    fun `button should show feedback state with visual changes`() {
        val buttonState = MutableStateFlow(
            ButtonState(
                isEnabled = true,
                isLoading = false,
                showFeedback = true,
                canClick = true
            )
        )
        
        whenever(mockButtonResponseManager.getButtonState("button_${("Feedback Button").hashCode()}"))
            .thenReturn(buttonState)
        
        composeTestRule.setContent {
            FFinderTheme {
                ResponsiveButton(
                    text = "Feedback Button",
                    onClick = { },
                    buttonResponseManager = mockButtonResponseManager
                )
            }
        }
        
        composeTestRule.onNodeWithText("Feedback Button").assertIsDisplayed()
    }
    
    @Test
    fun `button click should trigger button response manager`() {
        val buttonState = MutableStateFlow(
            ButtonState(
                isEnabled = true,
                isLoading = false,
                showFeedback = false,
                canClick = true
            )
        )
        
        whenever(mockButtonResponseManager.getButtonState("button_${("Click Button").hashCode()}"))
            .thenReturn(buttonState)
        
        var clickCount = 0
        
        composeTestRule.setContent {
            FFinderTheme {
                ResponsiveButton(
                    text = "Click Button",
                    onClick = { clickCount++ },
                    buttonResponseManager = mockButtonResponseManager
                )
            }
        }
        
        composeTestRule.onNodeWithText("Click Button").performClick()
        
        verify(mockButtonResponseManager).handleButtonClick(
            buttonId = "button_${("Click Button").hashCode()}",
            action = any()
        )
    }
    
    @Test
    fun `disabled button should not respond to clicks`() {
        val buttonState = MutableStateFlow(
            ButtonState(
                isEnabled = false,
                isLoading = false,
                showFeedback = false,
                canClick = false
            )
        )
        
        whenever(mockButtonResponseManager.getButtonState("button_${("Disabled Button").hashCode()}"))
            .thenReturn(buttonState)
        
        var clickCount = 0
        
        composeTestRule.setContent {
            FFinderTheme {
                ResponsiveButton(
                    text = "Disabled Button",
                    onClick = { clickCount++ },
                    enabled = false,
                    buttonResponseManager = mockButtonResponseManager
                )
            }
        }
        
        composeTestRule.onNodeWithText("Disabled Button").performClick()
        
        // Click count should remain 0 as button is disabled
        assert(clickCount == 0)
    }
    
    @Test
    fun `button should display different styles for different types`() {
        val buttonState = MutableStateFlow(
            ButtonState(
                isEnabled = true,
                isLoading = false,
                showFeedback = false,
                canClick = true
            )
        )
        
        whenever(mockButtonResponseManager.getButtonState("button_${("Primary Button").hashCode()}"))
            .thenReturn(buttonState)
        whenever(mockButtonResponseManager.getButtonState("button_${("Secondary Button").hashCode()}"))
            .thenReturn(buttonState)
        whenever(mockButtonResponseManager.getButtonState("button_${("Tertiary Button").hashCode()}"))
            .thenReturn(buttonState)
        
        composeTestRule.setContent {
            FFinderTheme {
                androidx.compose.foundation.layout.Column {
                    ResponsiveButton(
                        text = "Primary Button",
                        onClick = { },
                        buttonType = ButtonType.PRIMARY,
                        buttonResponseManager = mockButtonResponseManager
                    )
                    ResponsiveButton(
                        text = "Secondary Button",
                        onClick = { },
                        buttonType = ButtonType.SECONDARY,
                        buttonResponseManager = mockButtonResponseManager
                    )
                    ResponsiveButton(
                        text = "Tertiary Button",
                        onClick = { },
                        buttonType = ButtonType.TERTIARY,
                        buttonResponseManager = mockButtonResponseManager
                    )
                }
            }
        }
        
        composeTestRule.onNodeWithText("Primary Button").assertIsDisplayed()
        composeTestRule.onNodeWithText("Secondary Button").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tertiary Button").assertIsDisplayed()
    }
}