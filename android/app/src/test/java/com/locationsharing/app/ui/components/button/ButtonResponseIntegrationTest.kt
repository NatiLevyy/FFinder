package com.locationsharing.app.ui.components.button

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Integration tests for the complete button response system.
 * Tests the interaction between ButtonResponseManager, ButtonState, and debouncing.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ButtonResponseIntegrationTest {
    
    private lateinit var buttonResponseManager: ButtonResponseManagerImpl
    private val testScheduler = TestCoroutineScheduler()
    private val testDispatcher = UnconfinedTestDispatcher(testScheduler)
    
    @BeforeEach
    fun setUp() {
        buttonResponseManager = ButtonResponseManagerImpl()
    }
    
    @Test
    fun `complete button interaction flow should work correctly`() = runTest(testDispatcher) {
        // Given
        val buttonId = "integration_test_button"
        val mockAction = mock<() -> Unit>()
        
        // When - Initial state
        val initialState = buttonResponseManager.getButtonState(buttonId).value
        
        // Then - Should have default state
        assertTrue(initialState.isEnabled)
        assertFalse(initialState.isLoading)
        assertFalse(initialState.showFeedback)
        assertTrue(initialState.canClick)
        
        // When - Click button
        buttonResponseManager.handleButtonClick(buttonId, mockAction)
        
        // Then - Action should execute and feedback should show
        verify(mockAction).invoke()
        val clickedState = buttonResponseManager.getButtonState(buttonId).value
        assertTrue(clickedState.showFeedback)
        
        // When - Try to click again immediately (debouncing test)
        buttonResponseManager.handleButtonClick(buttonId, mockAction)
        
        // Then - Action should not execute again due to debouncing
        verify(mockAction, times(1)).invoke()
        
        // When - Wait for feedback to disappear
        advanceTimeBy(250L)
        
        // Then - Feedback should be gone
        val feedbackGoneState = buttonResponseManager.getButtonState(buttonId).value
        assertFalse(feedbackGoneState.showFeedback)
        
        // When - Wait for debounce period and click again
        advanceTimeBy(400L) // Total wait > 500ms debounce
        buttonResponseManager.handleButtonClick(buttonId, mockAction)
        
        // Then - Action should execute again
        verify(mockAction, times(2)).invoke()
    }
    
    @Test
    fun `button state changes should affect click behavior`() = runTest(testDispatcher) {
        // Given
        val buttonId = "state_test_button"
        val mockAction = mock<() -> Unit>()
        
        // When - Disable button and try to click
        buttonResponseManager.setButtonEnabled(buttonId, false)
        buttonResponseManager.handleButtonClick(buttonId, mockAction)
        
        // Then - Action should not execute
        verify(mockAction, never()).invoke()
        val disabledState = buttonResponseManager.getButtonState(buttonId).value
        assertFalse(disabledState.canClick)
        
        // When - Enable button and set loading, then try to click
        buttonResponseManager.setButtonEnabled(buttonId, true)
        buttonResponseManager.setButtonLoading(buttonId, true)
        buttonResponseManager.handleButtonClick(buttonId, mockAction)
        
        // Then - Action should still not execute due to loading
        verify(mockAction, never()).invoke()
        val loadingState = buttonResponseManager.getButtonState(buttonId).value
        assertFalse(loadingState.canClick)
        
        // When - Remove loading and click
        buttonResponseManager.setButtonLoading(buttonId, false)
        buttonResponseManager.handleButtonClick(buttonId, mockAction)
        
        // Then - Action should execute
        verify(mockAction).invoke()
        val enabledState = buttonResponseManager.getButtonState(buttonId).value
        assertTrue(enabledState.canClick)
    }
    
    @Test
    fun `multiple buttons should maintain independent states`() = runTest(testDispatcher) {
        // Given
        val button1Id = "button_1"
        val button2Id = "button_2"
        val mockAction1 = mock<() -> Unit>()
        val mockAction2 = mock<() -> Unit>()
        
        // When - Set different states for each button
        buttonResponseManager.setButtonEnabled(button1Id, false)
        buttonResponseManager.setButtonLoading(button2Id, true)
        
        // Then - States should be independent
        val button1State = buttonResponseManager.getButtonState(button1Id).value
        val button2State = buttonResponseManager.getButtonState(button2Id).value
        
        assertFalse(button1State.isEnabled)
        assertTrue(button1State.isLoading == false) // Default loading state
        
        assertTrue(button2State.isEnabled) // Default enabled state
        assertTrue(button2State.isLoading)
        
        // When - Try to click both buttons
        buttonResponseManager.handleButtonClick(button1Id, mockAction1)
        buttonResponseManager.handleButtonClick(button2Id, mockAction2)
        
        // Then - Neither should execute due to their states
        verify(mockAction1, never()).invoke()
        verify(mockAction2, never()).invoke()
        
        // When - Fix states and click
        buttonResponseManager.setButtonEnabled(button1Id, true)
        buttonResponseManager.setButtonLoading(button2Id, false)
        buttonResponseManager.handleButtonClick(button1Id, mockAction1)
        buttonResponseManager.handleButtonClick(button2Id, mockAction2)
        
        // Then - Both should execute
        verify(mockAction1).invoke()
        verify(mockAction2).invoke()
    }
    
    @Test
    fun `showButtonFeedback should work independently of clicks`() = runTest(testDispatcher) {
        // Given
        val buttonId = "feedback_test_button"
        
        // When - Show feedback without clicking
        buttonResponseManager.showButtonFeedback(buttonId)
        
        // Then - Feedback should be visible
        val feedbackState = buttonResponseManager.getButtonState(buttonId).value
        assertTrue(feedbackState.showFeedback)
        
        // When - Wait for feedback duration
        advanceTimeBy(250L)
        
        // Then - Feedback should disappear
        val noFeedbackState = buttonResponseManager.getButtonState(buttonId).value
        assertFalse(noFeedbackState.showFeedback)
    }
    
    @Test
    fun `clearAllStates should reset all button states`() = runTest(testDispatcher) {
        // Given
        val button1Id = "clear_test_button_1"
        val button2Id = "clear_test_button_2"
        
        // When - Set various states
        buttonResponseManager.setButtonEnabled(button1Id, false)
        buttonResponseManager.setButtonLoading(button2Id, true)
        buttonResponseManager.showButtonFeedback(button1Id)
        
        // Then - States should be set
        val button1State = buttonResponseManager.getButtonState(button1Id).value
        val button2State = buttonResponseManager.getButtonState(button2Id).value
        assertFalse(button1State.isEnabled)
        assertTrue(button2State.isLoading)
        
        // When - Clear all states
        buttonResponseManager.clearAllStates()
        
        // Then - All states should be reset to default
        val clearedButton1State = buttonResponseManager.getButtonState(button1Id).value
        val clearedButton2State = buttonResponseManager.getButtonState(button2Id).value
        
        assertTrue(clearedButton1State.isEnabled)
        assertFalse(clearedButton1State.isLoading)
        assertFalse(clearedButton1State.showFeedback)
        
        assertTrue(clearedButton2State.isEnabled)
        assertFalse(clearedButton2State.isLoading)
        assertFalse(clearedButton2State.showFeedback)
    }
}