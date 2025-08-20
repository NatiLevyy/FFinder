package com.locationsharing.app.ui.components.feedback

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VisualFeedbackManagerTest {
    
    private lateinit var visualFeedbackManager: VisualFeedbackManagerImpl
    
    @BeforeEach
    fun setUp() {
        visualFeedbackManager = VisualFeedbackManagerImpl()
    }
    
    @Test
    fun `initial state should be inactive`() {
        val initialState = visualFeedbackManager.feedbackState.value
        
        assertFalse(initialState.isActive)
        assertNull(initialState.feedbackType)
        assertEquals(1.0f, initialState.intensity)
        assertEquals(300L, initialState.duration)
    }
    
    @Test
    fun `triggerFeedback should activate feedback state`() = runTest {
        val feedbackType = FeedbackType.BUTTON_PRESS
        val intensity = 0.8f
        val duration = 500L
        
        visualFeedbackManager.triggerFeedback(feedbackType, intensity, duration)
        
        val state = visualFeedbackManager.feedbackState.value
        assertTrue(state.isActive)
        assertEquals(feedbackType, state.feedbackType)
        assertEquals(intensity, state.intensity)
        assertEquals(duration, state.duration)
    }
    
    @Test
    fun `feedback should auto-clear after duration`() = runTest {
        val feedbackType = FeedbackType.NAVIGATION_START
        val duration = 100L
        
        visualFeedbackManager.triggerFeedback(feedbackType, duration = duration)
        
        // Initially active
        assertTrue(visualFeedbackManager.feedbackState.value.isActive)
        
        // Wait for auto-clear (duration + small buffer)
        kotlinx.coroutines.delay(duration + 50)
        
        // Should be cleared
        assertFalse(visualFeedbackManager.feedbackState.value.isActive)
        assertNull(visualFeedbackManager.feedbackState.value.feedbackType)
    }
    
    @Test
    fun `clearFeedback should reset state`() = runTest {
        // Activate feedback
        visualFeedbackManager.triggerFeedback(FeedbackType.RIPPLE_EFFECT)
        assertTrue(visualFeedbackManager.feedbackState.value.isActive)
        
        // Clear feedback
        visualFeedbackManager.clearFeedback()
        
        val state = visualFeedbackManager.feedbackState.value
        assertFalse(state.isActive)
        assertNull(state.feedbackType)
        assertEquals(1.0f, state.intensity)
        assertEquals(300L, state.duration)
    }
    
    @Test
    fun `multiple feedback triggers should update state`() = runTest {
        // First feedback
        visualFeedbackManager.triggerFeedback(FeedbackType.BUTTON_PRESS, 0.5f, 200L)
        
        val firstState = visualFeedbackManager.feedbackState.value
        assertEquals(FeedbackType.BUTTON_PRESS, firstState.feedbackType)
        assertEquals(0.5f, firstState.intensity)
        assertEquals(200L, firstState.duration)
        
        // Second feedback (should override first)
        visualFeedbackManager.triggerFeedback(FeedbackType.NAVIGATION_SUCCESS, 1.0f, 400L)
        
        val secondState = visualFeedbackManager.feedbackState.value
        assertEquals(FeedbackType.NAVIGATION_SUCCESS, secondState.feedbackType)
        assertEquals(1.0f, secondState.intensity)
        assertEquals(400L, secondState.duration)
    }
    
    @Test
    fun `triggerHapticFeedback should activate button press feedback`() = runTest {
        visualFeedbackManager.triggerHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
        
        val state = visualFeedbackManager.feedbackState.value
        assertTrue(state.isActive)
        assertEquals(FeedbackType.BUTTON_PRESS, state.feedbackType)
    }
}