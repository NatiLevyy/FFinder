package com.locationsharing.app.ui.components.button

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for ButtonState data class.
 */
class ButtonStateTest {
    
    @Test
    fun `default ButtonState should have correct initial values`() {
        // Given & When
        val buttonState = ButtonState()
        
        // Then
        assertTrue(buttonState.isEnabled)
        assertFalse(buttonState.isLoading)
        assertFalse(buttonState.showFeedback)
        assertEquals(0L, buttonState.lastClickTime)
        assertTrue(buttonState.canClick)
    }
    
    @Test
    fun `canClick should return false when button is disabled`() {
        // Given
        val buttonState = ButtonState(isEnabled = false)
        
        // When & Then
        assertFalse(buttonState.canClick)
    }
    
    @Test
    fun `canClick should return false when button is loading`() {
        // Given
        val buttonState = ButtonState(isLoading = true)
        
        // When & Then
        assertFalse(buttonState.canClick)
    }
    
    @Test
    fun `canClick should return false when button is both disabled and loading`() {
        // Given
        val buttonState = ButtonState(isEnabled = false, isLoading = true)
        
        // When & Then
        assertFalse(buttonState.canClick)
    }
    
    @Test
    fun `canClick should return true when button is enabled and not loading`() {
        // Given
        val buttonState = ButtonState(isEnabled = true, isLoading = false)
        
        // When & Then
        assertTrue(buttonState.canClick)
    }
    
    @Test
    fun `withEnabled should create new state with updated enabled status`() {
        // Given
        val originalState = ButtonState(isEnabled = true)
        
        // When
        val newState = originalState.withEnabled(false)
        
        // Then
        assertTrue(originalState.isEnabled)
        assertFalse(newState.isEnabled)
        assertEquals(originalState.isLoading, newState.isLoading)
        assertEquals(originalState.showFeedback, newState.showFeedback)
        assertEquals(originalState.lastClickTime, newState.lastClickTime)
    }
    
    @Test
    fun `withLoading should create new state with updated loading status`() {
        // Given
        val originalState = ButtonState(isLoading = false)
        
        // When
        val newState = originalState.withLoading(true)
        
        // Then
        assertFalse(originalState.isLoading)
        assertTrue(newState.isLoading)
        assertEquals(originalState.isEnabled, newState.isEnabled)
        assertEquals(originalState.showFeedback, newState.showFeedback)
        assertEquals(originalState.lastClickTime, newState.lastClickTime)
    }
    
    @Test
    fun `withFeedback should create new state with updated feedback status`() {
        // Given
        val originalState = ButtonState(showFeedback = false)
        
        // When
        val newState = originalState.withFeedback(true)
        
        // Then
        assertFalse(originalState.showFeedback)
        assertTrue(newState.showFeedback)
        assertEquals(originalState.isEnabled, newState.isEnabled)
        assertEquals(originalState.isLoading, newState.isLoading)
        assertEquals(originalState.lastClickTime, newState.lastClickTime)
    }
    
    @Test
    fun `withLastClickTime should create new state with updated click time`() {
        // Given
        val originalState = ButtonState(lastClickTime = 0L)
        val newTime = 12345L
        
        // When
        val newState = originalState.withLastClickTime(newTime)
        
        // Then
        assertEquals(0L, originalState.lastClickTime)
        assertEquals(newTime, newState.lastClickTime)
        assertEquals(originalState.isEnabled, newState.isEnabled)
        assertEquals(originalState.isLoading, newState.isLoading)
        assertEquals(originalState.showFeedback, newState.showFeedback)
    }
}