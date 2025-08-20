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
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for ButtonResponseManagerImpl.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ButtonResponseManagerImplTest {
    
    private lateinit var buttonResponseManager: ButtonResponseManagerImpl
    private val testScheduler = TestCoroutineScheduler()
    private val testDispatcher = UnconfinedTestDispatcher(testScheduler)
    
    @BeforeEach
    fun setUp() {
        buttonResponseManager = ButtonResponseManagerImpl()
    }
    
    @Test
    fun `handleButtonClick should execute action when button is enabled`() = runTest(testDispatcher) {
        // Given
        val buttonId = "test_button"
        val mockAction = mock<() -> Unit>()
        
        // When
        buttonResponseManager.handleButtonClick(buttonId, mockAction)
        
        // Then
        verify(mockAction).invoke()
    }
    
    @Test
    fun `handleButtonClick should not execute action when button is disabled`() = runTest(testDispatcher) {
        // Given
        val buttonId = "test_button"
        val mockAction = mock<() -> Unit>()
        buttonResponseManager.setButtonEnabled(buttonId, false)
        
        // When
        buttonResponseManager.handleButtonClick(buttonId, mockAction)
        
        // Then
        verify(mockAction, never()).invoke()
    }
    
    @Test
    fun `handleButtonClick should not execute action when button is loading`() = runTest(testDispatcher) {
        // Given
        val buttonId = "test_button"
        val mockAction = mock<() -> Unit>()
        buttonResponseManager.setButtonLoading(buttonId, true)
        
        // When
        buttonResponseManager.handleButtonClick(buttonId, mockAction)
        
        // Then
        verify(mockAction, never()).invoke()
    }
    
    @Test
    fun `handleButtonClick should debounce rapid clicks`() = runTest(testDispatcher) {
        // Given
        val buttonId = "test_button"
        val mockAction = mock<() -> Unit>()
        
        // When - Click twice rapidly
        buttonResponseManager.handleButtonClick(buttonId, mockAction)
        buttonResponseManager.handleButtonClick(buttonId, mockAction)
        
        // Then - Only first click should execute
        verify(mockAction, times(1)).invoke()
    }
    
    @Test
    fun `handleButtonClick should allow clicks after debounce period`() = runTest(testDispatcher) {
        // Given
        val buttonId = "test_button"
        val mockAction = mock<() -> Unit>()
        
        // When - Click, wait for debounce period, then click again
        buttonResponseManager.handleButtonClick(buttonId, mockAction)
        advanceTimeBy(600L) // Wait longer than debounce delay
        buttonResponseManager.handleButtonClick(buttonId, mockAction)
        
        // Then - Both clicks should execute
        verify(mockAction, times(2)).invoke()
    }
    
    @Test
    fun `handleButtonClick should show feedback temporarily`() = runTest(testDispatcher) {
        // Given
        val buttonId = "test_button"
        val mockAction = mock<() -> Unit>()
        
        // When
        buttonResponseManager.handleButtonClick(buttonId, mockAction)
        
        // Then - Feedback should be shown initially
        val initialState = buttonResponseManager.getButtonState(buttonId).value
        assertTrue(initialState.showFeedback)
        
        // When - Wait for feedback duration
        advanceTimeBy(250L) // Wait longer than feedback duration
        
        // Then - Feedback should be removed
        val finalState = buttonResponseManager.getButtonState(buttonId).value
        assertFalse(finalState.showFeedback)
    }
    
    @Test
    fun `setButtonEnabled should update button state`() = runTest(testDispatcher) {
        // Given
        val buttonId = "test_button"
        
        // When
        buttonResponseManager.setButtonEnabled(buttonId, false)
        
        // Then
        val state = buttonResponseManager.getButtonState(buttonId).value
        assertFalse(state.isEnabled)
    }
    
    @Test
    fun `setButtonLoading should update button state`() = runTest(testDispatcher) {
        // Given
        val buttonId = "test_button"
        
        // When
        buttonResponseManager.setButtonLoading(buttonId, true)
        
        // Then
        val state = buttonResponseManager.getButtonState(buttonId).value
        assertTrue(state.isLoading)
    }
    
    @Test
    fun `showButtonFeedback should show feedback temporarily`() = runTest(testDispatcher) {
        // Given
        val buttonId = "test_button"
        
        // When
        buttonResponseManager.showButtonFeedback(buttonId)
        
        // Then - Feedback should be shown initially
        val initialState = buttonResponseManager.getButtonState(buttonId).value
        assertTrue(initialState.showFeedback)
        
        // When - Wait for feedback duration
        advanceTimeBy(250L) // Wait longer than feedback duration
        
        // Then - Feedback should be removed
        val finalState = buttonResponseManager.getButtonState(buttonId).value
        assertFalse(finalState.showFeedback)
    }
    
    @Test
    fun `getButtonState should return default state for new button`() = runTest(testDispatcher) {
        // Given
        val buttonId = "new_button"
        
        // When
        val state = buttonResponseManager.getButtonState(buttonId).value
        
        // Then
        assertEquals(ButtonState(), state)
    }
    
    @Test
    fun `clearAllStates should remove all button states`() = runTest(testDispatcher) {
        // Given
        val buttonId1 = "button1"
        val buttonId2 = "button2"
        buttonResponseManager.setButtonEnabled(buttonId1, false)
        buttonResponseManager.setButtonLoading(buttonId2, true)
        
        // When
        buttonResponseManager.clearAllStates()
        
        // Then - New states should be default
        val state1 = buttonResponseManager.getButtonState(buttonId1).value
        val state2 = buttonResponseManager.getButtonState(buttonId2).value
        assertEquals(ButtonState(), state1)
        assertEquals(ButtonState(), state2)
    }
    
    @Test
    fun `handleButtonClick should handle action exceptions gracefully`() = runTest(testDispatcher) {
        // Given
        val buttonId = "test_button"
        val throwingAction: () -> Unit = { throw RuntimeException("Test exception") }
        
        // When & Then - Should not throw exception
        buttonResponseManager.handleButtonClick(buttonId, throwingAction)
        
        // Verify state is still updated (feedback shown)
        val state = buttonResponseManager.getButtonState(buttonId).value
        assertTrue(state.showFeedback)
    }
}